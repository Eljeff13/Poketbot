package com.eljeff13.poketbot.game

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/** Repairs available to each combatant per battle. */
const val MAX_REPAIRS = 3

/** Energy cap; one point is gained at the end of every round. */
const val MAX_ENERGY = 6

enum class Action { ATTACK, DEFEND, SPECIAL, REPAIR }

enum class Outcome { ONGOING, PLAYER_WON, PLAYER_LOST }

enum class LogKind {
    BATTLE_START,
    ATTACK,
    CRIT,
    DEFEND,
    REPAIR,
    NO_REPAIRS,
    NO_ENERGY,
    STUNNED,
    OVERCLOCK,
    EMP,
    DOUBLE_STRIKE,
    SHIELD_WALL,
    DRAIN,
    SCRAP_BOMB,
    DEFEATED,
}

/**
 * A single line of the battle log. Text lives in string resources, so entries
 * carry only the kind and its numbers.
 */
data class LogEntry(
    val kind: LogKind,
    val actor: String = "",
    val target: String = "",
    val amount: Int = 0,
    val extra: Int = 0,
)

data class Combatant(
    val name: String,
    val spec: BotSpec,
    val stats: Stats,
    val hp: Int,
    val energy: Int = 0,
    val repairsLeft: Int = MAX_REPAIRS,
    val defending: Boolean = false,
    val stunned: Boolean = false,
    val overclocked: Boolean = false,
    val shieldTurns: Int = 0,
) {
    val maxHp: Int get() = stats.hp
    val isDown: Boolean get() = hp <= 0
    val hpFraction: Float get() = (hp.toFloat() / maxHp).coerceIn(0f, 1f)

    companion object {
        fun of(name: String, spec: BotSpec, stats: Stats) =
            Combatant(name = name, spec = spec, stats = stats, hp = stats.hp)
    }
}

data class BattleState(
    val player: Combatant,
    val enemy: Combatant,
    val turn: Int = 1,
    val log: List<LogEntry> = emptyList(),
    /** Entries produced by the most recent round, for animation and sound cues. */
    val lastEvents: List<LogEntry> = emptyList(),
    val outcome: Outcome = Outcome.ONGOING,
    val playerTookDamage: Boolean = false,
    val playerUsedRepair: Boolean = false,
) {
    val isOver: Boolean get() = outcome != Outcome.ONGOING

    /** A flawless win — no repairs used and never dropped below full health. */
    val flawless: Boolean
        get() = outcome == Outcome.PLAYER_WON && !playerUsedRepair && !playerTookDamage
}

/**
 * The turn-based combat engine. Every function is pure: it takes a state and a
 * [Random] and returns the next state, which makes the whole thing testable and
 * keeps the UI layer free of rules.
 */
object BattleEngine {

    private const val LOG_LIMIT = 60

    fun start(player: Combatant, enemy: Combatant): BattleState = BattleState(
        player = player,
        enemy = enemy,
        log = listOf(LogEntry(LogKind.BATTLE_START, actor = enemy.name)),
        lastEvents = emptyList(),
    )

    /**
     * Resolves one full round: the player's chosen [action] and the enemy's
     * reply, ordered by speed.
     */
    fun takeTurn(state: BattleState, action: Action, rng: Random = Random.Default): BattleState {
        if (state.isOver) return state

        var player = state.player
        var enemy = state.enemy
        val log = mutableListOf<LogEntry>()
        var usedRepair = state.playerUsedRepair

        val enemyAction = chooseEnemyAction(enemy, rng)
        val playerFirst = player.stats.speed >= enemy.stats.speed

        fun runPlayer() {
            if (player.isDown || enemy.isDown) return
            if (action == Action.REPAIR && player.repairsLeft > 0) usedRepair = true
            val result = resolve(player, enemy, action, rng, log)
            player = result.first
            enemy = result.second
        }

        fun runEnemy() {
            if (player.isDown || enemy.isDown) return
            val result = resolve(enemy, player, enemyAction, rng, log)
            enemy = result.first
            player = result.second
        }

        if (playerFirst) {
            runPlayer()
            runEnemy()
        } else {
            runEnemy()
            runPlayer()
        }

        player = endOfTurn(player)
        enemy = endOfTurn(enemy)

        val outcome = when {
            enemy.isDown -> Outcome.PLAYER_WON
            player.isDown -> Outcome.PLAYER_LOST
            else -> Outcome.ONGOING
        }
        if (enemy.isDown) log += LogEntry(LogKind.DEFEATED, actor = enemy.name)
        if (player.isDown) log += LogEntry(LogKind.DEFEATED, actor = player.name)

        return state.copy(
            player = player,
            enemy = enemy,
            turn = state.turn + 1,
            log = (state.log + log).takeLast(LOG_LIMIT),
            lastEvents = log.toList(),
            outcome = outcome,
            playerTookDamage = state.playerTookDamage || player.hp < player.maxHp,
            playerUsedRepair = usedRepair,
        )
    }

    /** Enemy AI: heal when hurt, spend a charged special, otherwise trade blows. */
    fun chooseEnemyAction(enemy: Combatant, rng: Random): Action {
        val hpPct = enemy.hpFraction
        if (hpPct < 0.35f && enemy.repairsLeft > 0 && rng.nextFloat() < 0.75f) return Action.REPAIR
        if (enemy.energy >= enemy.spec.special.cost && rng.nextFloat() < 0.8f) return Action.SPECIAL
        if (hpPct < 0.5f && rng.nextFloat() < 0.22f) return Action.DEFEND
        return Action.ATTACK
    }

    /** Returns the (actor, target) pair after [action] has been applied. */
    private fun resolve(
        actorIn: Combatant,
        targetIn: Combatant,
        action: Action,
        rng: Random,
        log: MutableList<LogEntry>,
    ): Pair<Combatant, Combatant> {
        // Guarding only lasts until your own next action.
        var actor = actorIn.copy(defending = false)
        var target = targetIn

        if (actor.stunned) {
            log += LogEntry(LogKind.STUNNED, actor = actor.name)
            return actor.copy(stunned = false) to target
        }

        when (action) {
            Action.ATTACK -> {
                val hit = strike(actor, target, power = 1f, rng = rng)
                actor = hit.consumeOverclock(actor)
                target = target.damaged(hit.damage)
                log += LogEntry(
                    kind = if (hit.critical) LogKind.CRIT else LogKind.ATTACK,
                    actor = actor.name,
                    target = target.name,
                    amount = hit.damage,
                )
            }

            Action.DEFEND -> {
                actor = actor.copy(defending = true)
                log += LogEntry(LogKind.DEFEND, actor = actor.name)
            }

            Action.REPAIR -> {
                if (actor.repairsLeft <= 0) {
                    log += LogEntry(LogKind.NO_REPAIRS, actor = actor.name)
                } else {
                    val healed = healAmount(actor)
                    actor = actor.copy(
                        hp = min(actor.maxHp, actor.hp + healed),
                        repairsLeft = actor.repairsLeft - 1,
                    )
                    log += LogEntry(
                        LogKind.REPAIR,
                        actor = actor.name,
                        amount = healed,
                        extra = actor.repairsLeft,
                    )
                }
            }

            Action.SPECIAL -> {
                val cost = actor.spec.special.cost
                if (actor.energy < cost) {
                    log += LogEntry(LogKind.NO_ENERGY, actor = actor.name)
                } else {
                    actor = actor.copy(energy = actor.energy - cost)
                    val applied = applySpecial(actor, target, rng, log)
                    actor = applied.first
                    target = applied.second
                }
            }
        }
        return actor to target
    }

    private fun applySpecial(
        actorIn: Combatant,
        targetIn: Combatant,
        rng: Random,
        log: MutableList<LogEntry>,
    ): Pair<Combatant, Combatant> {
        var actor = actorIn
        var target = targetIn
        when (actor.spec.special) {
            Special.OVERCLOCK -> {
                actor = actor.copy(overclocked = true)
                log += LogEntry(LogKind.OVERCLOCK, actor = actor.name)
            }

            Special.EMP -> {
                val hit = strike(actor, target, power = 0.7f, rng = rng)
                actor = hit.consumeOverclock(actor)
                target = target.damaged(hit.damage).copy(stunned = true)
                log += LogEntry(LogKind.EMP, actor = actor.name, target = target.name, amount = hit.damage)
            }

            Special.DOUBLE_STRIKE -> {
                val first = strike(actor, target, power = 0.62f, rng = rng)
                actor = first.consumeOverclock(actor)
                target = target.damaged(first.damage)
                var total = first.damage
                if (!target.isDown) {
                    val second = strike(actor, target, power = 0.62f, rng = rng)
                    target = target.damaged(second.damage)
                    total += second.damage
                }
                log += LogEntry(LogKind.DOUBLE_STRIKE, actor = actor.name, target = target.name, amount = total)
            }

            Special.SHIELD_WALL -> {
                actor = actor.copy(shieldTurns = 3)
                log += LogEntry(LogKind.SHIELD_WALL, actor = actor.name, amount = 3)
            }

            Special.DRAIN -> {
                val hit = strike(actor, target, power = 0.9f, rng = rng)
                actor = hit.consumeOverclock(actor)
                target = target.damaged(hit.damage)
                val healed = max(1, hit.damage / 2)
                actor = actor.copy(hp = min(actor.maxHp, actor.hp + healed))
                log += LogEntry(
                    LogKind.DRAIN,
                    actor = actor.name,
                    target = target.name,
                    amount = hit.damage,
                    extra = healed,
                )
            }

            Special.SCRAP_BOMB -> {
                val hit = strike(actor, target, power = 1.35f, defensePierce = 0.5f, rng = rng)
                actor = hit.consumeOverclock(actor)
                target = target.damaged(hit.damage)
                log += LogEntry(LogKind.SCRAP_BOMB, actor = actor.name, target = target.name, amount = hit.damage)
            }
        }
        return actor to target
    }

    private data class Hit(val damage: Int, val critical: Boolean, val spentOverclock: Boolean) {
        fun consumeOverclock(actor: Combatant) =
            if (spentOverclock) actor.copy(overclocked = false) else actor
    }

    private fun strike(
        actor: Combatant,
        target: Combatant,
        power: Float,
        defensePierce: Float = 0f,
        rng: Random,
    ): Hit {
        val effectiveDefense = target.stats.defense *
            (1f - defensePierce) *
            (if (target.shieldTurns > 0) 2f else 1f)
        val mitigation = effectiveDefense / (effectiveDefense + 60f)

        var damage = actor.stats.attack * power * (1f - mitigation)
        damage *= 0.85f + rng.nextFloat() * 0.30f

        if (actor.overclocked) damage *= 2f

        val critChance = min(0.30f, actor.stats.speed / 400f)
        val critical = rng.nextFloat() < critChance
        if (critical) damage *= 1.6f

        if (target.defending) damage *= 0.5f

        return Hit(
            damage = max(1, damage.roundToInt()),
            critical = critical,
            spentOverclock = actor.overclocked,
        )
    }

    private fun healAmount(actor: Combatant): Int = max(1, (actor.maxHp * 0.28f).roundToInt())

    private fun Combatant.damaged(amount: Int) = copy(hp = max(0, hp - amount))

    private fun endOfTurn(c: Combatant): Combatant = c.copy(
        energy = min(MAX_ENERGY, c.energy + 1),
        shieldTurns = max(0, c.shieldTurns - 1),
    )
}
