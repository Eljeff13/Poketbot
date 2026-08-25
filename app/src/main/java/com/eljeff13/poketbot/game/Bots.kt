package com.eljeff13.poketbot.game

import kotlin.math.roundToInt

/** The four upgradeable stat tracks shared by every bot. */
data class Stats(
    val hp: Int,
    val attack: Int,
    val defense: Int,
    val speed: Int,
) {
    operator fun plus(other: Stats) = Stats(
        hp = hp + other.hp,
        attack = attack + other.attack,
        defense = defense + other.defense,
        speed = speed + other.speed,
    )

    fun scaledBy(factor: Float) = Stats(
        hp = (hp * factor).roundToInt().coerceAtLeast(1),
        attack = (attack * factor).roundToInt().coerceAtLeast(1),
        defense = (defense * factor).roundToInt().coerceAtLeast(1),
        speed = (speed * factor).roundToInt().coerceAtLeast(1),
    )
}

/** Special moves. [cost] is charged against the combatant's energy meter. */
enum class Special(val cost: Int) {
    OVERCLOCK(3),
    EMP(4),
    DOUBLE_STRIKE(3),
    SHIELD_WALL(3),
    DRAIN(4),
    SCRAP_BOMB(4),
}

enum class HeadShape { BOX, DOME, VISOR, CRESTED }

enum class EyeStyle { SINGLE, DUAL, TRIPLE, SLIT }

/**
 * Pure data description of how a bot is drawn. Kept free of Compose types so the
 * whole [game] package stays unit-testable on the JVM.
 */
data class BotArt(
    val bodyColor: Long,
    val accentColor: Long,
    val glowColor: Long,
    val head: HeadShape,
    val eyes: EyeStyle,
    val antenna: Boolean,
    val treads: Boolean,
)

data class BotSpec(
    val id: String,
    val name: String,
    val base: Stats,
    val special: Special,
    val art: BotArt,
    val unlockCost: Int,
)

/** The six bots the player can collect. The first one is free. */
object BotCatalog {

    val chippy = BotSpec(
        id = "chippy",
        name = "Chippy",
        base = Stats(hp = 130, attack = 26, defense = 20, speed = 22),
        special = Special.OVERCLOCK,
        art = BotArt(0xFF4FD1C5, 0xFF1F6F68, 0xFF9BF6EE, HeadShape.BOX, EyeStyle.DUAL, antenna = true, treads = false),
        unlockCost = 0,
    )

    val voltik = BotSpec(
        id = "voltik",
        name = "Voltik",
        base = Stats(hp = 108, attack = 28, defense = 15, speed = 38),
        special = Special.EMP,
        art = BotArt(0xFF7C6CF5, 0xFF3A2FA8, 0xFFC9C2FF, HeadShape.VISOR, EyeStyle.SLIT, antenna = true, treads = false),
        unlockCost = 220,
    )

    val bunker = BotSpec(
        id = "bunker",
        name = "Búnker",
        base = Stats(hp = 190, attack = 22, defense = 38, speed = 12),
        special = Special.SHIELD_WALL,
        art = BotArt(0xFF8A9BA8, 0xFF4A5763, 0xFFD7E3ED, HeadShape.DOME, EyeStyle.SINGLE, antenna = false, treads = true),
        unlockCost = 300,
    )

    val ripclaw = BotSpec(
        id = "ripclaw",
        name = "Ripclaw",
        base = Stats(hp = 118, attack = 38, defense = 16, speed = 28),
        special = Special.DOUBLE_STRIKE,
        art = BotArt(0xFFF2564B, 0xFF8E241C, 0xFFFFB0A8, HeadShape.CRESTED, EyeStyle.DUAL, antenna = false, treads = false),
        unlockCost = 380,
    )

    val medix = BotSpec(
        id = "medix",
        name = "Medix",
        base = Stats(hp = 155, attack = 25, defense = 26, speed = 20),
        special = Special.DRAIN,
        art = BotArt(0xFF56C271, 0xFF20603A, 0xFFB9F5C8, HeadShape.DOME, EyeStyle.TRIPLE, antenna = true, treads = false),
        unlockCost = 460,
    )

    val oxide = BotSpec(
        id = "oxide",
        name = "Óxido",
        base = Stats(hp = 168, attack = 34, defense = 24, speed = 18),
        special = Special.SCRAP_BOMB,
        art = BotArt(0xFFE0A32E, 0xFF8A5D12, 0xFFFFE39B, HeadShape.BOX, EyeStyle.SINGLE, antenna = false, treads = true),
        unlockCost = 600,
    )

    val all: List<BotSpec> = listOf(chippy, voltik, bunker, ripclaw, medix, oxide)

    val starterId: String = chippy.id

    fun byId(id: String): BotSpec = all.firstOrNull { it.id == id } ?: chippy
}
