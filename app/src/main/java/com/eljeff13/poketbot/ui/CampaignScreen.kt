package com.eljeff13.poketbot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.eljeff13.poketbot.ui.theme.PanelHigh

@Composable
fun CampaignScreen(
    save: SaveState,
    onStage: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .padding(horizontal = 16.dp),
    ) {
        ScreenHeader(
            title = stringResource(R.string.campaign_title),
            subtitle = stringResource(R.string.campaign_subtitle, save.stagesCleared, Campaign.STAGES),
            onBack = onBack,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatChip(
                label = stringResource(R.string.label_pilot),
                value = save.activeBot.name,
            )
            StatChip(
                label = stringResource(R.string.label_scrap),
                value = save.scrap.toString(),
                accent = Amber,
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 88.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items((1..Campaign.STAGES).toList()) { stage ->
                StageTile(
                    stage = stage,
                    cleared = stage <= save.stagesCleared,
                    playable = save.isStagePlayable(stage),
                    onClick = { onStage(stage) },
                )
            }
        }
    }
}

@Composable
private fun StageTile(
    stage: Int,
    cleared: Boolean,
    playable: Boolean,
    onClick: () -> Unit,
) {
    val boss = Campaign.isBoss(stage)
    val accent = when {
        !playable -> MaterialTheme.colorScheme.outline
        boss -> Amber
        else -> Coolant
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(if (playable) Panel else PanelHigh.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .border(1.5.dp, accent.copy(alpha = if (playable) 1f else 0.4f), RoundedCornerShape(14.dp))
            .clickable(enabled = playable, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stage.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = if (playable) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = when {
                    !playable -> stringResource(R.string.stage_locked)
                    cleared -> stringResource(R.string.stage_cleared)
                    boss -> stringResource(R.string.stage_boss)
                    else -> stringResource(R.string.stage_ready)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) {
            Text(
                text = stringResource(R.string.action_back),
                color = Coolant,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(Modifier.size(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing()
    }
}

/** Thin divider used between panels. */
@Composable
fun PanelDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.06f)),
    )
}
