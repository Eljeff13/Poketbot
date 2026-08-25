package com.eljeff13.poketbot.game

/** The four upgrade tracks a bot can spend scrap on. */
enum class UpgradeTrack { HP, ATTACK, DEFENSE, SPEED }

data class BotUpgrades(
    val hp: Int = 0,
    val attack: Int = 0,
    val defense: Int = 0,
    val speed: Int = 0,
) {
    fun level(track: UpgradeTrack): Int = when (track) {
        UpgradeTrack.HP -> hp
        UpgradeTrack.ATTACK -> attack
        UpgradeTrack.DEFENSE -> defense
        UpgradeTrack.SPEED -> speed
    }

    fun withIncrement(track: UpgradeTrack): BotUpgrades = when (track) {
        UpgradeTrack.HP -> copy(hp = hp + 1)
        UpgradeTrack.ATTACK -> copy(attack = attack + 1)
        UpgradeTrack.DEFENSE -> copy(defense = defense + 1)
        UpgradeTrack.SPEED -> copy(speed = speed + 1)
    }
}

data class SaveState(
    val scrap: Int = 0,
    val unlocked: Set<String> = setOf(BotCatalog.starterId),
    val activeBotId: String = BotCatalog.starterId,
    val upgrades: Map<String, BotUpgrades> = emptyMap(),
    val stagesCleared: Int = 0,
    val battlesWon: Int = 0,
    val soundEnabled: Boolean = true,
) {
    val activeBot: BotSpec get() = BotCatalog.byId(activeBotId)

    fun upgradesFor(botId: String): BotUpgrades = upgrades[botId] ?: BotUpgrades()

    fun isUnlocked(botId: String): Boolean = botId in unlocked

    /** The next campaign battle, capped at the last stage once everything is cleared. */
    val nextStage: Int get() = (stagesCleared + 1).coerceAtMost(Campaign.STAGES)

    val campaignComplete: Boolean get() = stagesCleared >= Campaign.STAGES

    fun isStagePlayable(stage: Int): Boolean = stage in 1..Campaign.STAGES && stage <= stagesCleared + 1
}

object Progression {

    const val MAX_LEVEL = 10

    private const val HP_PER_LEVEL = 9
    private const val ATTACK_PER_LEVEL = 3
    private const val DEFENSE_PER_LEVEL = 3
    private const val SPEED_PER_LEVEL = 2

    fun gainPerLevel(track: UpgradeTrack): Int = when (track) {
        UpgradeTrack.HP -> HP_PER_LEVEL
        UpgradeTrack.ATTACK -> ATTACK_PER_LEVEL
        UpgradeTrack.DEFENSE -> DEFENSE_PER_LEVEL
        UpgradeTrack.SPEED -> SPEED_PER_LEVEL
    }

    /** Cost of moving a track from [currentLevel] to the next one. */
    fun upgradeCost(currentLevel: Int): Int = 45 + currentLevel * 35

    fun statsFor(spec: BotSpec, upgrades: BotUpgrades): Stats = spec.base + Stats(
        hp = upgrades.hp * HP_PER_LEVEL,
        attack = upgrades.attack * ATTACK_PER_LEVEL,
        defense = upgrades.defense * DEFENSE_PER_LEVEL,
        speed = upgrades.speed * SPEED_PER_LEVEL,
    )

    fun statsFor(state: SaveState, botId: String): Stats =
        statsFor(BotCatalog.byId(botId), state.upgradesFor(botId))

    fun canUpgrade(state: SaveState, botId: String, track: UpgradeTrack): Boolean {
        val level = state.upgradesFor(botId).level(track)
        return level < MAX_LEVEL && state.scrap >= upgradeCost(level)
    }

    /** Applies an upgrade, or returns the state untouched when it is not affordable. */
    fun applyUpgrade(state: SaveState, botId: String, track: UpgradeTrack): SaveState {
        if (!canUpgrade(state, botId, track)) return state
        val current = state.upgradesFor(botId)
        val cost = upgradeCost(current.level(track))
        return state.copy(
            scrap = state.scrap - cost,
            upgrades = state.upgrades + (botId to current.withIncrement(track)),
        )
    }

    fun canUnlock(state: SaveState, botId: String): Boolean {
        val spec = BotCatalog.byId(botId)
        return !state.isUnlocked(botId) && state.scrap >= spec.unlockCost
    }

    fun applyUnlock(state: SaveState, botId: String): SaveState {
        if (!canUnlock(state, botId)) return state
        val spec = BotCatalog.byId(botId)
        return state.copy(
            scrap = state.scrap - spec.unlockCost,
            unlocked = state.unlocked + botId,
            activeBotId = botId,
        )
    }

    /** Records the outcome of a finished battle and pays out scrap. */
    fun applyBattleResult(state: SaveState, stage: Int, won: Boolean, flawless: Boolean): SaveState {
        if (!won) return state
        val replay = stage <= state.stagesCleared
        val reward = Campaign.scrapReward(stage, flawless, replay)
        return state.copy(
            scrap = state.scrap + reward,
            stagesCleared = maxOf(state.stagesCleared, stage),
            battlesWon = state.battlesWon + 1,
        )
    }
}
