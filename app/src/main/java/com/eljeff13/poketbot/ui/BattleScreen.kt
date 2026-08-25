package com.eljeff13.poketbot.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eljeff13.poketbot.R
import com.eljeff13.poketbot.game.Action
import com.eljeff13.poketbot.game.BattleState
import com.eljeff13.poketbot.game.Campaign
import com.eljeff13.poketbot.game.Combatant
import com.eljeff13.poketbot.game.LogEntry
import com.eljeff13.poketbot.game.LogKind
import com.eljeff13.poketbot.ui.theme.Amber
import com.eljeff13.poketbot.ui.theme.Coolant
import com.eljeff13.poketbot.ui.theme.Danger
import com.eljeff13.poketbot.ui.theme.Ink
import com.eljeff13.poketbot.ui.theme.Panel
import com.eljeff13.poketbot.ui.theme.PanelHigh

@Composable
fun BattleScreen(
    battle: BattleState,
    stage: Int,
    hurtTarget: HurtTarget,
    inputLocked: Boolean,
    onAction: (Action) -> Unit,
    onHurtShown: () -> Unit,
    onFlee: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    val playerFlash by animateFloatAsState(
        targetValue = if (hurtTarget == HurtTarget.PLAYER) 1f else 0f,
        animationSpec = tween(180),
        label = "playerFlash",
    )
    val enemyFlash by animateFloatAsState(
        targetValue = if (hurtTarget == HurtTarget.ENEMY) 1f else 0f,
        animationSpec = tween(180),
        label = "enemyFlash",
    )

    LaunchedEffect(hurtTarget, battle.turn) {
        if (hurtTarget != HurtTarget.NONE) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            kotlinx.coroutines.delay(220)
            onHurtShown()
        }
    }

    val transition = rememberInfiniteTransition(label = "arena")
    val bob by transition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
        label = "bob",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Panel, Ink)))
            .padding(horizontal = 14.dp),
    ) {
        ScreenHeader(
            title = stringResource(R.string.battle_stage, stage),
            subtitle = if (Campaign.isBoss(stage)) {
                stringResource(R.string.battle_boss)
            } else {
                stringResource(R.string.battle_turn, battle.turn)
            },
            onBack = onFlee,
        )

        CombatantBar(
            combatant = battle.enemy,
            alignEnd = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                BotSprite(
                    art = battle.player.spec.art,
                    facingRight = true,
                    bobOffset = bob,
                    hurtFlash = playerFlash,
                    dimmed = battle.player.isDown,
                    modifier = Modifier
                        .weight(1f)
                        .height(150.dp),
                )
                BotSprite(
                    art = battle.enemy.spec.art,
                    facingRight = false,
                    bobOffset = -bob,
                    hurtFlash = enemyFlash,
                    dimmed = battle.enemy.isDown,
                    modifier = Modifier
                        .weight(1f)
                        .height(150.dp),
                )
            }
        }

        CombatantBar(
            combatant = battle.player,
            alignEnd = false,
            modifier = Modifier.fillMaxWidth(),
        )

        BattleLog(
            entries = battle.log,
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .padding(vertical = 8.dp),
        )

        ActionPad(
            player = battle.player,
            enabled = !battle.isOver && !inputLocked,
            onAction = { action ->
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onAction(action)
            },
        )

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun CombatantBar(
    combatant: Combatant,
    alignEnd: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Panel, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = combatant.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${combatant.hp}/${combatant.maxHp}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(5.dp))
        HealthBar(
            fraction = combatant.hpFraction,
            modifier = Modifier.fillMaxWidth(),
            contentDescription = stringResource(
                R.string.a11y_health,
                combatant.name,
                combatant.hp,
                combatant.maxHp,
            ),
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EnergyPips(energy = combatant.energy, cost = combatant.spec.special.cost)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (combatant.shieldTurns > 0) {
                    StatusTag(stringResource(R.string.status_shield, combatant.shieldTurns), Coolant)
                }
                if (combatant.overclocked) {
                    StatusTag(stringResource(R.string.status_overclock), Amber)
                }
                if (combatant.stunned) {
                    StatusTag(stringResource(R.string.status_stunned), Danger)
                }
                StatusTag(stringResource(R.string.status_repairs, combatant.repairsLeft), MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StatusTag(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = Modifier
            .background(PanelHigh, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun BattleLog(entries: List<LogEntry>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) listState.animateScrollToItem(entries.lastIndex)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .background(Ink.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(entries) { index, entry ->
            Text(
                text = logText(entry),
                style = MaterialTheme.typography.bodySmall,
                color = if (index == entries.lastIndex) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun logText(entry: LogEntry): String = when (entry.kind) {
    LogKind.BATTLE_START -> stringResource(R.string.log_start, entry.actor)
    LogKind.ATTACK -> stringResource(R.string.log_attack, entry.actor, entry.amount)
    LogKind.CRIT -> stringResource(R.string.log_crit, entry.actor, entry.amount)
    LogKind.DEFEND -> stringResource(R.string.log_defend, entry.actor)
    LogKind.REPAIR -> stringResource(R.string.log_repair, entry.actor, entry.amount, entry.extra)
    LogKind.NO_REPAIRS -> stringResource(R.string.log_no_repairs, entry.actor)
    LogKind.NO_ENERGY -> stringResource(R.string.log_no_energy, entry.actor)
    LogKind.STUNNED -> stringResource(R.string.log_stunned, entry.actor)
    LogKind.OVERCLOCK -> stringResource(R.string.log_overclock, entry.actor)
    LogKind.EMP -> stringResource(R.string.log_emp, entry.actor, entry.amount)
    LogKind.DOUBLE_STRIKE -> stringResource(R.string.log_double_strike, entry.actor, entry.amount)
    LogKind.SHIELD_WALL -> stringResource(R.string.log_shield_wall, entry.actor)
    LogKind.DRAIN -> stringResource(R.string.log_drain, entry.actor, entry.amount, entry.extra)
    LogKind.SCRAP_BOMB -> stringResource(R.string.log_scrap_bomb, entry.actor, entry.amount)
    LogKind.DEFEATED -> stringResource(R.string.log_defeated, entry.actor)
}

@Composable
private fun ActionPad(
    player: Combatant,
    enabled: Boolean,
    onAction: (Action) -> Unit,
) {
    val specialReady = player.energy >= player.spec.special.cost
    val canRepair = player.repairsLeft > 0

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ArcadeButton(
                label = stringResource(R.string.action_attack),
                onClick = { onAction(Action.ATTACK) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            ArcadeButton(
                label = stringResource(R.string.action_defend),
                onClick = { onAction(Action.DEFEND) },
                enabled = enabled,
                container = PanelHigh,
                content = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ArcadeButton(
                label = stringResource(specialNameRes(player.spec.special)),
                onClick = { onAction(Action.SPECIAL) },
                enabled = enabled && specialReady,
                container = Amber,
                modifier = Modifier.weight(1f),
            )
            ArcadeButton(
                label = stringResource(R.string.action_repair, player.repairsLeft),
                onClick = { onAction(Action.REPAIR) },
                enabled = enabled && canRepair,
                container = Danger,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
