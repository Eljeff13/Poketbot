package com.eljeff13.poketbot.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eljeff13.poketbot.R
import com.eljeff13.poketbot.game.Campaign
import com.eljeff13.poketbot.ui.theme.Amber
import com.eljeff13.poketbot.ui.theme.Coolant
import com.eljeff13.poketbot.ui.theme.Danger
import com.eljeff13.poketbot.ui.theme.Ink
import com.eljeff13.poketbot.ui.theme.Panel

@Composable
fun PoketbotApp(viewModel: GameViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmReset by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .safeDrawingPadding(),
    ) {
        when (state.screen) {
            Screen.TITLE -> TitleScreen(
                save = state.save,
                onPlay = { viewModel.goTo(Screen.CAMPAIGN) },
                onGarage = { viewModel.goTo(Screen.GARAGE) },
                onToggleSound = viewModel::toggleSound,
                onReset = { confirmReset = true },
            )

            Screen.CAMPAIGN -> CampaignScreen(
                save = state.save,
                onStage = viewModel::startBattle,
                onBack = viewModel::backToTitle,
            )

            Screen.GARAGE -> GarageScreen(
                save = state.save,
                onSelect = viewModel::selectBot,
                onUnlock = viewModel::unlockBot,
                onUpgrade = viewModel::upgrade,
                onBack = viewModel::backToTitle,
            )

            Screen.BATTLE -> {
                val battle = state.battle
                if (battle == null) {
                    // Defensive: a battle screen with no battle means state was
                    // restored oddly, so bounce back to the map after composition.
                    LaunchedEffect(Unit) { viewModel.dismissResult(Screen.CAMPAIGN) }
                } else {
                    BattleScreen(
                        battle = battle,
                        stage = state.battleStage,
                        hurtTarget = state.hurtTarget,
                        inputLocked = state.result != null,
                        onAction = viewModel::takeAction,
                        onHurtShown = viewModel::clearHurt,
                        onFlee = { viewModel.dismissResult(Screen.CAMPAIGN) },
                    )
                }
            }
        }

        state.result?.let { result ->
            ResultOverlay(
                result = result,
                onReplay = viewModel::replayStage,
                onNext = viewModel::nextStage,
                onGarage = { viewModel.dismissResult(Screen.GARAGE) },
                onMap = { viewModel.dismissResult(Screen.CAMPAIGN) },
            )
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = Panel,
            title = { Text(stringResource(R.string.reset_title)) },
            text = { Text(stringResource(R.string.reset_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        viewModel.resetProgress()
                    },
                ) {
                    Text(stringResource(R.string.reset_confirm), color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(stringResource(R.string.action_cancel), color = Coolant)
                }
            },
        )
    }

    // System back mirrors the in-game back button instead of leaving the app.
    BackHandler(enabled = state.screen != Screen.TITLE) {
        if (state.result != null) {
            viewModel.dismissResult(Screen.CAMPAIGN)
        } else {
            when (state.screen) {
                Screen.BATTLE -> viewModel.dismissResult(Screen.CAMPAIGN)
                else -> viewModel.backToTitle()
            }
        }
    }
}

@Composable
private fun ResultOverlay(
    result: BattleResult,
    onReplay: () -> Unit,
    onNext: () -> Unit,
    onGarage: () -> Unit,
    onMap: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(Panel, RoundedCornerShape(20.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = when {
                    result.campaignCompleted -> stringResource(R.string.result_campaign_complete)
                    result.won -> stringResource(R.string.result_victory)
                    else -> stringResource(R.string.result_defeat)
                },
                style = MaterialTheme.typography.titleLarge,
                color = if (result.won) Coolant else Danger,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (result.won) {
                    stringResource(R.string.result_scrap_earned, result.scrapEarned)
                } else {
                    stringResource(R.string.result_try_again)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (result.flawless) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.result_flawless),
                    style = MaterialTheme.typography.titleMedium,
                    color = Amber,
                )
            }

            Spacer(Modifier.height(18.dp))

            val hasNextStage = result.won && result.stage < Campaign.STAGES

            if (hasNextStage) {
                ArcadeButton(
                    label = stringResource(R.string.result_next),
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }

            ArcadeButton(
                label = stringResource(R.string.result_replay),
                onClick = onReplay,
                container = Amber,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TextButton(onClick = onGarage) {
                    Text(stringResource(R.string.result_to_garage), color = Coolant)
                }
                TextButton(onClick = onMap) {
                    Text(stringResource(R.string.result_to_map), color = Coolant)
                }
            }
        }
    }
}
