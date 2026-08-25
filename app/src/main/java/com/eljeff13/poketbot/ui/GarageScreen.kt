package com.eljeff13.poketbot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eljeff13.poketbot.R
import com.eljeff13.poketbot.game.BotCatalog
import com.eljeff13.poketbot.game.BotSpec
import com.eljeff13.poketbot.game.Progression
import com.eljeff13.poketbot.game.SaveState
import com.eljeff13.poketbot.game.Special
import com.eljeff13.poketbot.game.UpgradeTrack
import com.eljeff13.poketbot.ui.theme.Amber
import com.eljeff13.poketbot.ui.theme.Coolant
import com.eljeff13.poketbot.ui.theme.Danger
import com.eljeff13.poketbot.ui.theme.Ink
import com.eljeff13.poketbot.ui.theme.Panel
import com.eljeff13.poketbot.ui.theme.PanelHigh

@Composable
fun GarageScreen(
    save: SaveState,
    onSelect: (String) -> Unit,
    onUnlock: (String) -> Unit,
    onUpgrade: (String, UpgradeTrack) -> Unit,
    onBack: () -> Unit,
) {
    val active = save.activeBot
    val stats = Progression.statsFor(save, save.activeBotId)
    val upgrades = save.upgradesFor(save.activeBotId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .padding(horizontal = 16.dp),
    ) {
        ScreenHeader(
            title = stringResource(R.string.garage_title),
            subtitle = stringResource(R.string.garage_subtitle),
            onBack = onBack,
            trailing = {
                StatChip(
                    label = stringResource(R.string.label_scrap),
                    value = save.scrap.toString(),
                    accent = Amber,
                )
            },
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(BotCatalog.all) { spec ->
                RosterCard(
                    spec = spec,
                    unlocked = save.isUnlocked(spec.id),
                    selected = spec.id == save.activeBotId,
                    affordable = save.scrap >= spec.unlockCost,
                    onClick = {
                        if (save.isUnlocked(spec.id)) onSelect(spec.id) else onUnlock(spec.id)
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Panel, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BotSprite(art = active.art, modifier = Modifier.size(96.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = active.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Coolant,
                    )
                    Text(
                        text = stringResource(specialNameRes(active.special)),
                        style = MaterialTheme.typography.titleMedium,
                        color = Amber,
                    )
                    Text(
                        text = stringResource(specialDescriptionRes(active.special)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.special_cost, active.special.cost),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.garage_upgrades),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            UpgradeTrack.entries.forEach { track ->
                val level = upgrades.level(track)
                val cost = Progression.upgradeCost(level)
                val maxed = level >= Progression.MAX_LEVEL
                UpgradeRow(
                    label = stringResource(trackNameRes(track)),
                    value = when (track) {
                        UpgradeTrack.HP -> stats.hp
                        UpgradeTrack.ATTACK -> stats.attack
                        UpgradeTrack.DEFENSE -> stats.defense
                        UpgradeTrack.SPEED -> stats.speed
                    },
                    level = level,
                    cost = cost,
                    maxed = maxed,
                    affordable = !maxed && save.scrap >= cost,
                    onUpgrade = { onUpgrade(save.activeBotId, track) },
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RosterCard(
    spec: BotSpec,
    unlocked: Boolean,
    selected: Boolean,
    affordable: Boolean,
    onClick: () -> Unit,
) {
    val border = when {
        selected -> Coolant
        unlocked -> MaterialTheme.colorScheme.outline
        affordable -> Amber
        else -> Danger.copy(alpha = 0.5f)
    }

    Column(
        modifier = Modifier
            .width(104.dp)
            .background(Panel, RoundedCornerShape(14.dp))
            .border(2.dp, border, RoundedCornerShape(14.dp))
            .clickable(enabled = unlocked || affordable, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BotSprite(
            art = spec.art,
            dimmed = !unlocked,
            modifier = Modifier.size(64.dp),
        )
        Text(
            text = spec.name,
            style = MaterialTheme.typography.titleMedium,
            color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = when {
                selected -> stringResource(R.string.roster_active)
                unlocked -> stringResource(R.string.roster_ready)
                else -> stringResource(R.string.roster_price, spec.unlockCost)
            },
            style = MaterialTheme.typography.bodySmall,
            color = when {
                selected -> Coolant
                unlocked -> MaterialTheme.colorScheme.onSurfaceVariant
                affordable -> Amber
                else -> Danger
            },
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun UpgradeRow(
    label: String,
    value: Int,
    level: Int,
    cost: Int,
    maxed: Boolean,
    affordable: Boolean,
    onUpgrade: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Coolant,
                )
            }
            Spacer(Modifier.height(6.dp))
            LevelDots(level = level, max = Progression.MAX_LEVEL)
        }

        Spacer(Modifier.width(10.dp))

        ArcadeButton(
            label = if (maxed) stringResource(R.string.upgrade_max) else stringResource(R.string.upgrade_cost, cost),
            onClick = onUpgrade,
            enabled = affordable,
            container = Amber,
            modifier = Modifier.width(124.dp),
        )
    }
}

@Composable
private fun LevelDots(level: Int, max: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(max) { index ->
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 5.dp)
                    .background(
                        if (index < level) Coolant else PanelHigh,
                        RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

fun trackNameRes(track: UpgradeTrack): Int = when (track) {
    UpgradeTrack.HP -> R.string.track_hp
    UpgradeTrack.ATTACK -> R.string.track_attack
    UpgradeTrack.DEFENSE -> R.string.track_defense
    UpgradeTrack.SPEED -> R.string.track_speed
}

fun specialNameRes(special: Special): Int = when (special) {
    Special.OVERCLOCK -> R.string.special_overclock
    Special.EMP -> R.string.special_emp
    Special.DOUBLE_STRIKE -> R.string.special_double_strike
    Special.SHIELD_WALL -> R.string.special_shield_wall
    Special.DRAIN -> R.string.special_drain
    Special.SCRAP_BOMB -> R.string.special_scrap_bomb
}

fun specialDescriptionRes(special: Special): Int = when (special) {
    Special.OVERCLOCK -> R.string.special_overclock_desc
    Special.EMP -> R.string.special_emp_desc
    Special.DOUBLE_STRIKE -> R.string.special_double_strike_desc
    Special.SHIELD_WALL -> R.string.special_shield_wall_desc
    Special.DRAIN -> R.string.special_drain_desc
    Special.SCRAP_BOMB -> R.string.special_scrap_bomb_desc
}
