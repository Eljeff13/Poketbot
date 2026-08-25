package com.eljeff13.poketbot.game

import kotlin.random.Random

/** The 20-battle ladder that makes up the single-player campaign. */
object Campaign {

    const val STAGES = 20

    private val enemyNames = listOf(
        "Chatarrín", "Tuerca", "Zumbido", "Percutor", "Kilovatio", "Remache",
        "Cortocircuito", "Yunque", "Pistón", "Escoria", "Diodo", "Trituro",
        "Cascote", "Fulgor", "Hollín", "Perno",
    )

    private val bossNames = mapOf(
        5 to "Capataz Hierro",
        10 to "Sierra Doble",
        15 to "Reactor Nueve",
        20 to "Rey Chatarra",
    )

    fun isBoss(stage: Int): Boolean = bossNames.containsKey(stage)

    /** Difficulty ramp: roughly 3.5x the starting power by the final battle. */
    fun difficulty(stage: Int): Float {
        val base = 1f + (stage - 1) * 0.13f
        return if (isBoss(stage)) base * 1.18f else base
    }

    fun enemyFor(stage: Int, rng: Random = Random(stage * 7919L)): Combatant {
        val spec = BotCatalog.all[(stage - 1) % BotCatalog.all.size]
        val name = bossNames[stage] ?: enemyNames[(stage * 3 + spec.name.length) % enemyNames.size]
        val stats = spec.base.scaledBy(difficulty(stage))
        return Combatant.of(name = name, spec = spec, stats = stats)
    }

    /** Scrap paid out for a win. Replays of a cleared stage pay 40%. */
    fun scrapReward(stage: Int, flawless: Boolean, replay: Boolean): Int {
        var reward = 25 + stage * 10
        if (isBoss(stage)) reward += 60
        if (flawless) reward += 40
        if (replay) reward = (reward * 0.4f).toInt()
        return reward.coerceAtLeast(5)
    }
}
