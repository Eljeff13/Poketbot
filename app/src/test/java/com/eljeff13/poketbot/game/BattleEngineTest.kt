package com.eljeff13.poketbot.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BattleEngineTest {

    private fun combatant(
        name: String,
        spec: BotSpec = BotCatalog.chippy,
        stats: Stats = spec.base,
    ) = Combatant.of(name, spec, stats)

    @Test
    fun `attack removes health from the target`() {
        val state = BattleEngine.start(combatant("P"), combatant("E"))
        val next = BattleEngine.takeTurn(state, Action.ATTACK, Random(1))

        assertTrue("enemy should have taken damage", next.enemy.hp < next.enemy.maxHp)
        assertTrue("log should have grown", next.log.size > state.log.size)
        assertTrue(next.lastEvents.isNotEmpty())
    }

    @Test
    fun `energy accumulates one point per round and is capped`() {
        // Both sides are made unkillable so the rounds actually run to the cap.
        val tank = BotCatalog.chippy.base.copy(hp = 100_000)
        var state = BattleEngine.start(
            combatant("P", stats = tank),
            combatant("E", stats = tank),
        )
        repeat(MAX_ENERGY + 4) { state = BattleEngine.takeTurn(state, Action.DEFEND, Random(7)) }

        assertFalse(state.isOver)
        assertEquals(MAX_ENERGY, state.player.energy)
    }

    @Test
    fun `special is refused without enough energy and charges nothing`() {
        val state = BattleEngine.start(combatant("P"), combatant("E"))
        val next = BattleEngine.takeTurn(state, Action.SPECIAL, Random(3))

        assertTrue(next.lastEvents.any { it.kind == LogKind.NO_ENERGY && it.actor == "P" })
        assertEquals(1, next.player.energy)
    }

    @Test
    fun `repair restores health and is limited per battle`() {
        val hurt = combatant("P").copy(hp = 10)
        var state = BattleEngine.start(hurt, combatant("E"))

        repeat(MAX_REPAIRS) { state = BattleEngine.takeTurn(state, Action.REPAIR, Random(5)) }
        assertEquals(0, state.player.repairsLeft)

        val exhausted = BattleEngine.takeTurn(state, Action.REPAIR, Random(5))
        assertTrue(exhausted.lastEvents.any { it.kind == LogKind.NO_REPAIRS })
    }

    @Test
    fun `repair never pushes health above the maximum`() {
        val state = BattleEngine.start(combatant("P").copy(hp = 5), combatant("E"))
        var current = state
        repeat(MAX_REPAIRS) { current = BattleEngine.takeTurn(current, Action.REPAIR, Random(11)) }

        assertTrue(current.player.hp <= current.player.maxHp)
    }

    @Test
    fun `defending reduces the damage taken`() {
        // The player is fast enough to raise the guard before the enemy swings,
        // and tanky enough that a single round never ends the fight.
        val fastPlayer = combatant("P", stats = BotCatalog.chippy.base.copy(hp = 100_000, speed = 99))
        val enemy = combatant("E", spec = BotCatalog.bunker)

        fun averageDamageTaken(action: Action): Double = (1..200).sumOf { seed ->
            val start = BattleEngine.start(fastPlayer, enemy)
            val next = BattleEngine.takeTurn(start, action, Random(seed.toLong()))
            (start.player.hp - next.player.hp).toLong()
        } / 200.0

        val guarded = averageDamageTaken(Action.DEFEND)
        val open = averageDamageTaken(Action.ATTACK)

        assertTrue("the guard should actually block something", guarded > 0.0)
        assertTrue(
            "guarding ($guarded) should take clearly less than standing open ($open)",
            guarded < open * 0.7,
        )
    }

    @Test
    fun `a battle always terminates with a winner`() {
        var state = BattleEngine.start(combatant("P"), combatant("E"))
        var rounds = 0
        while (!state.isOver && rounds < 500) {
            state = BattleEngine.takeTurn(state, Action.ATTACK, Random(rounds.toLong()))
            rounds++
        }

        assertTrue("battle should end", state.isOver)
        assertTrue(state.outcome == Outcome.PLAYER_WON || state.outcome == Outcome.PLAYER_LOST)
        assertTrue("nobody should end on negative health", state.player.hp >= 0 && state.enemy.hp >= 0)
    }

    @Test
    fun `a finished battle ignores further actions`() {
        var state = BattleEngine.start(combatant("P"), combatant("E").copy(hp = 1))
        while (!state.isOver) state = BattleEngine.takeTurn(state, Action.ATTACK, Random(2))

        val after = BattleEngine.takeTurn(state, Action.ATTACK, Random(2))
        assertEquals(state, after)
    }

    @Test
    fun `every special resolves without throwing`() {
        BotCatalog.all.forEach { spec ->
            val charged = combatant("P", spec = spec).copy(energy = MAX_ENERGY)
            val state = BattleEngine.start(charged, combatant("E", spec = BotCatalog.bunker))
            val next = BattleEngine.takeTurn(state, Action.SPECIAL, Random(9))

            assertFalse(
                "${spec.name} should not report missing energy",
                next.lastEvents.any { it.kind == LogKind.NO_ENERGY },
            )
        }
    }

    @Test
    fun `emp stuns the target so it loses its next action`() {
        val voltik = combatant("P", spec = BotCatalog.voltik).copy(energy = MAX_ENERGY)
        val slowEnemy = combatant("E", spec = BotCatalog.bunker)

        // Voltik outruns Bunker, so the pulse lands before the enemy can act and
        // costs it the turn it was about to take.
        val afterEmp = BattleEngine.takeTurn(
            BattleEngine.start(voltik, slowEnemy),
            Action.SPECIAL,
            Random(4),
        )

        assertTrue(afterEmp.lastEvents.any { it.kind == LogKind.EMP })
        assertTrue(afterEmp.lastEvents.any { it.kind == LogKind.STUNNED && it.actor == "E" })
        assertFalse("a stun should not linger past the turn it cost", afterEmp.enemy.stunned)
    }

    @Test
    fun `enemy ai heals when badly damaged and able`() {
        val nearlyDead = combatant("E").copy(hp = 5)
        val action = BattleEngine.chooseEnemyAction(nearlyDead, Random(1))
        assertNotNull(action)

        // Over many rolls the AI must reach for repairs at least sometimes.
        val healed = (1..200).count {
            BattleEngine.chooseEnemyAction(nearlyDead, Random(it.toLong())) == Action.REPAIR
        }
        assertTrue("AI should sometimes repair when nearly dead", healed > 0)
    }
}
