package com.eljeff13.poketbot.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eljeff13.poketbot.audio.Sfx
import com.eljeff13.poketbot.audio.SfxCue
import com.eljeff13.poketbot.data.SaveStore
import com.eljeff13.poketbot.game.Action
import com.eljeff13.poketbot.game.BattleEngine
import com.eljeff13.poketbot.game.BattleState
import com.eljeff13.poketbot.game.Campaign
import com.eljeff13.poketbot.game.Combatant
import com.eljeff13.poketbot.game.LogKind
import com.eljeff13.poketbot.game.Outcome
import com.eljeff13.poketbot.game.Progression
import com.eljeff13.poketbot.game.SaveState
import com.eljeff13.poketbot.game.UpgradeTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class Screen { TITLE, CAMPAIGN, GARAGE, BATTLE }

/** Result of the battle that just finished, shown in the end-of-battle sheet. */
data class BattleResult(
    val stage: Int,
    val won: Boolean,
    val flawless: Boolean,
    val scrapEarned: Int,
    val campaignCompleted: Boolean,
)

data class UiState(
    val screen: Screen = Screen.TITLE,
    val save: SaveState = SaveState(),
    val battle: BattleState? = null,
    val battleStage: Int = 1,
    val result: BattleResult? = null,
    val busy: Boolean = false,
    val hurtTarget: HurtTarget = HurtTarget.NONE,
)

enum class HurtTarget { NONE, PLAYER, ENEMY }

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SaveStore(app)
    private val sfx = Sfx()
    private val rng = Random.Default

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        val loaded = store.load()
        sfx.enabled = loaded.soundEnabled
        _state.value = UiState(save = loaded)
    }

    // --- Navigation -------------------------------------------------------

    fun goTo(screen: Screen) {
        sfx.play(SfxCue.TAP)
        _state.value = _state.value.copy(screen = screen, result = null)
    }

    fun backToTitle() = goTo(Screen.TITLE)

    // --- Settings ---------------------------------------------------------

    fun toggleSound() {
        val save = _state.value.save.copy(soundEnabled = !_state.value.save.soundEnabled)
        sfx.enabled = save.soundEnabled
        persist(save)
        if (save.soundEnabled) sfx.play(SfxCue.TAP)
    }

    fun resetProgress() {
        store.clear()
        val fresh = SaveState()
        sfx.enabled = fresh.soundEnabled
        _state.value = UiState(save = fresh)
        store.save(fresh)
    }

    // --- Garage -----------------------------------------------------------

    fun selectBot(botId: String) {
        val save = _state.value.save
        if (!save.isUnlocked(botId)) return
        sfx.play(SfxCue.TAP)
        persist(save.copy(activeBotId = botId))
    }

    fun unlockBot(botId: String) {
        val save = _state.value.save
        val updated = Progression.applyUnlock(save, botId)
        if (updated === save) return
        sfx.play(SfxCue.UNLOCK)
        persist(updated)
    }

    fun upgrade(botId: String, track: UpgradeTrack) {
        val save = _state.value.save
        val updated = Progression.applyUpgrade(save, botId, track)
        if (updated === save) return
        sfx.play(SfxCue.HEAL)
        persist(updated)
    }

    // --- Battle -----------------------------------------------------------

    fun startBattle(stage: Int) {
        val save = _state.value.save
        if (!save.isStagePlayable(stage)) return

        val spec = save.activeBot
        val stats = Progression.statsFor(save, save.activeBotId)
        val player = Combatant.of(name = spec.name, spec = spec, stats = stats)
        val enemy = Campaign.enemyFor(stage)

        sfx.play(SfxCue.TAP)
        _state.value = _state.value.copy(
            screen = Screen.BATTLE,
            battle = BattleEngine.start(player, enemy),
            battleStage = stage,
            result = null,
            busy = false,
            hurtTarget = HurtTarget.NONE,
        )
    }

    fun takeAction(action: Action) {
        val current = _state.value
        val battle = current.battle ?: return
        if (battle.isOver || current.busy) return

        val next = BattleEngine.takeTurn(battle, action, rng)
        playCuesFor(next.lastEvents.map { it.kind })

        val hurt = when {
            next.player.hp < battle.player.hp -> HurtTarget.PLAYER
            next.enemy.hp < battle.enemy.hp -> HurtTarget.ENEMY
            else -> HurtTarget.NONE
        }

        _state.value = current.copy(battle = next, hurtTarget = hurt, busy = next.isOver)

        if (next.isOver) finishBattle(next, current.battleStage)
    }

    fun clearHurt() {
        if (_state.value.hurtTarget != HurtTarget.NONE) {
            _state.value = _state.value.copy(hurtTarget = HurtTarget.NONE)
        }
    }

    private fun finishBattle(battle: BattleState, stage: Int) {
        val save = _state.value.save
        val won = battle.outcome == Outcome.PLAYER_WON
        val flawless = battle.flawless
        val updated = Progression.applyBattleResult(save, stage, won, flawless)
        val earned = updated.scrap - save.scrap

        sfx.play(if (won) SfxCue.WIN else SfxCue.LOSE)

        val result = BattleResult(
            stage = stage,
            won = won,
            flawless = flawless,
            scrapEarned = earned,
            campaignCompleted = won && !save.campaignComplete && updated.campaignComplete,
        )
        _state.value = _state.value.copy(save = updated, result = result, busy = false)
        viewModelScope.launch { store.save(updated) }
    }

    fun dismissResult(returnTo: Screen) {
        _state.value = _state.value.copy(result = null, battle = null, screen = returnTo)
    }

    fun replayStage() {
        val stage = _state.value.battleStage
        _state.value = _state.value.copy(result = null)
        startBattle(stage)
    }

    fun nextStage() {
        val save = _state.value.save
        val stage = (_state.value.battleStage + 1).coerceAtMost(Campaign.STAGES)
        _state.value = _state.value.copy(result = null)
        if (save.isStagePlayable(stage)) startBattle(stage) else dismissResult(Screen.CAMPAIGN)
    }

    private fun playCuesFor(kinds: List<LogKind>) {
        val cue = when {
            kinds.contains(LogKind.CRIT) -> SfxCue.CRIT
            kinds.any { it in SPECIAL_KINDS } -> SfxCue.SPECIAL
            kinds.contains(LogKind.REPAIR) -> SfxCue.HEAL
            kinds.contains(LogKind.ATTACK) -> SfxCue.HIT
            kinds.contains(LogKind.DEFEND) -> SfxCue.GUARD
            else -> null
        }
        cue?.let(sfx::play)
    }

    private fun persist(save: SaveState) {
        _state.value = _state.value.copy(save = save)
        viewModelScope.launch { store.save(save) }
    }

    override fun onCleared() {
        sfx.release()
        super.onCleared()
    }

    private companion object {
        val SPECIAL_KINDS = setOf(
            LogKind.OVERCLOCK,
            LogKind.EMP,
            LogKind.DOUBLE_STRIKE,
            LogKind.SHIELD_WALL,
            LogKind.DRAIN,
            LogKind.SCRAP_BOMB,
        )
    }
}
