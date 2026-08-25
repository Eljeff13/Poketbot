package com.eljeff13.poketbot.data

import android.content.Context
import android.content.SharedPreferences
import com.eljeff13.poketbot.game.BotCatalog
import com.eljeff13.poketbot.game.BotUpgrades
import com.eljeff13.poketbot.game.Campaign
import com.eljeff13.poketbot.game.SaveState

/**
 * Persists the save game. Everything is a primitive, so plain SharedPreferences
 * is enough and there is no serialization library to keep in step.
 */
class SaveStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun load(): SaveState {
        val unlocked = prefs.getStringSet(KEY_UNLOCKED, null)
            ?.filter { id -> BotCatalog.all.any { it.id == id } }
            ?.toSet()
            .orEmpty() + BotCatalog.starterId

        val activeId = prefs.getString(KEY_ACTIVE, BotCatalog.starterId) ?: BotCatalog.starterId

        val upgrades = BotCatalog.all.associate { spec ->
            spec.id to decodeUpgrades(prefs.getString(upgradeKey(spec.id), null))
        }.filterValues { it != BotUpgrades() }

        return SaveState(
            scrap = prefs.getInt(KEY_SCRAP, 0).coerceAtLeast(0),
            unlocked = unlocked,
            activeBotId = if (activeId in unlocked) activeId else BotCatalog.starterId,
            upgrades = upgrades,
            stagesCleared = prefs.getInt(KEY_STAGES, 0).coerceIn(0, Campaign.STAGES),
            battlesWon = prefs.getInt(KEY_WINS, 0).coerceAtLeast(0),
            soundEnabled = prefs.getBoolean(KEY_SOUND, true),
        )
    }

    fun save(state: SaveState) {
        prefs.edit().apply {
            putInt(KEY_SCRAP, state.scrap)
            putStringSet(KEY_UNLOCKED, state.unlocked)
            putString(KEY_ACTIVE, state.activeBotId)
            putInt(KEY_STAGES, state.stagesCleared)
            putInt(KEY_WINS, state.battlesWon)
            putBoolean(KEY_SOUND, state.soundEnabled)
            BotCatalog.all.forEach { spec ->
                putString(upgradeKey(spec.id), encodeUpgrades(state.upgradesFor(spec.id)))
            }
        }.apply()
    }

    fun clear() = prefs.edit().clear().apply()

    private fun upgradeKey(botId: String) = "upgrades_$botId"

    private fun encodeUpgrades(u: BotUpgrades) = "${u.hp},${u.attack},${u.defense},${u.speed}"

    private fun decodeUpgrades(raw: String?): BotUpgrades {
        val parts = raw?.split(',') ?: return BotUpgrades()
        if (parts.size != 4) return BotUpgrades()
        val values = parts.map { it.trim().toIntOrNull() ?: return BotUpgrades() }
        return BotUpgrades(
            hp = values[0].coerceIn(0, LEVEL_CAP),
            attack = values[1].coerceIn(0, LEVEL_CAP),
            defense = values[2].coerceIn(0, LEVEL_CAP),
            speed = values[3].coerceIn(0, LEVEL_CAP),
        )
    }

    private companion object {
        const val FILE_NAME = "poketbot_save"
        const val KEY_SCRAP = "scrap"
        const val KEY_UNLOCKED = "unlocked"
        const val KEY_ACTIVE = "active_bot"
        const val KEY_STAGES = "stages_cleared"
        const val KEY_WINS = "battles_won"
        const val KEY_SOUND = "sound_enabled"
        const val LEVEL_CAP = 10
    }
}
