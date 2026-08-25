package com.eljeff13.poketbot.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eljeff13.poketbot.game.MAX_ENERGY
import com.eljeff13.poketbot.ui.theme.Amber
import com.eljeff13.poketbot.ui.theme.Coolant
import com.eljeff13.poketbot.ui.theme.Danger
import com.eljeff13.poketbot.ui.theme.Ink
import com.eljeff13.poketbot.ui.theme.PanelHigh

/** Animated health bar; turns amber then red as the bot takes damage. */
@Composable
fun HealthBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 420),
        label = "hp",
    )
    val color = when {
        animated > 0.5f -> Coolant
        animated > 0.22f -> Amber
        else -> Danger
    }

    Box(
        modifier = modifier
            .height(14.dp)
            .background(PanelHigh, RoundedCornerShape(7.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(7.dp))
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .padding(2.dp)
                .background(
                    Brush.horizontalGradient(listOf(color.copy(alpha = 0.7f), color)),
                    RoundedCornerShape(6.dp),
                ),
        )
    }
}

/** Energy meter drawn as discrete pips so the special's cost is easy to read. */
@Composable
fun EnergyPips(
    energy: Int,
    cost: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(MAX_ENERGY) { index ->
            val filled = index < energy
            val required = index < cost
            Box(
                modifier = Modifier
                    .size(width = 14.dp, height = 8.dp)
                    .background(
                        when {
                            filled -> Amber
                            required -> Amber.copy(alpha = 0.22f)
                            else -> PanelHigh
                        },
                        RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

/** The chunky primary button used across the menus. */
@Composable
fun ArcadeButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = Coolant,
    content: Color = Ink,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = PanelHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

/** Small stat readout: label above value. */
@Composable
fun StatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = Coolant,
) {
    Row(
        modifier = modifier
            .background(PanelHigh, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = accent,
        )
    }
}
