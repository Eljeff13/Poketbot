package com.eljeff13.poketbot.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eljeff13.poketbot.R
import com.eljeff13.poketbot.game.Campaign
import com.eljeff13.poketbot.game.SaveState
import com.eljeff13.poketbot.ui.theme.Amber
import com.eljeff13.poketbot.ui.theme.Coolant
import com.eljeff13.poketbot.ui.theme.Ink
import com.eljeff13.poketbot.ui.theme.Panel

@Composable
fun TitleScreen(
    save: SaveState,
    onPlay: () -> Unit,
    onGarage: () -> Unit,
    onToggleSound: () -> Unit,
    onReset: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "title")
    val bob by transition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "bob",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Panel, Ink))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.app_name).uppercase(),
                style = MaterialTheme.typography.displaySmall,
                color = Coolant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.title_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))

            BotSprite(
                art = save.activeBot.art,
                bobOffset = bob,
                modifier = Modifier.size(180.dp),
            )

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatChip(
                    label = stringResource(R.string.label_scrap),
                    value = save.scrap.toString(),
                    accent = Amber,
                )
                StatChip(
                    label = stringResource(R.string.label_progress),
                    value = "${save.stagesCleared}/${Campaign.STAGES}",
                )
            }

            Spacer(Modifier.height(28.dp))

            val buttonModifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)

            ArcadeButton(
                label = if (save.stagesCleared == 0) {
                    stringResource(R.string.action_play)
                } else {
                    stringResource(R.string.action_continue)
                },
                onClick = onPlay,
                modifier = buttonModifier,
            )
            Spacer(Modifier.height(12.dp))
            ArcadeButton(
                label = stringResource(R.string.action_garage),
                onClick = onGarage,
                modifier = buttonModifier,
                container = Amber,
            )

            Spacer(Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onToggleSound) {
                    Text(
                        text = if (save.soundEnabled) {
                            stringResource(R.string.action_sound_on)
                        } else {
                            stringResource(R.string.action_sound_off)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onReset) {
                    Text(
                        text = stringResource(R.string.action_reset),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
