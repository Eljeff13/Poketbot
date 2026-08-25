package com.eljeff13.poketbot.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import com.eljeff13.poketbot.game.BotArt
import com.eljeff13.poketbot.game.EyeStyle
import com.eljeff13.poketbot.game.HeadShape

/**
 * Every bot is drawn procedurally on a Canvas. No bitmaps means no asset
 * licensing, a tiny download, and sprites that stay sharp at any density.
 */
@Composable
fun BotSprite(
    art: BotArt,
    modifier: Modifier = Modifier,
    facingRight: Boolean = true,
    bobOffset: Float = 0f,
    hurtFlash: Float = 0f,
    dimmed: Boolean = false,
) {
    Canvas(modifier = modifier) {
        val unit = size.minDimension / 100f
        translate(top = bobOffset * unit) {
            drawBot(art, unit, facingRight, hurtFlash, dimmed)
        }
    }
}

private fun DrawScope.drawBot(
    art: BotArt,
    unit: Float,
    facingRight: Boolean,
    hurtFlash: Float,
    dimmed: Boolean,
) {
    fun tint(argb: Long): Color {
        val base = Color(argb)
        val greyed = if (dimmed) base.copy(alpha = 0.35f) else base
        return if (hurtFlash > 0f) lerpColor(greyed, Color.White, hurtFlash) else greyed
    }

    val body = tint(art.bodyColor)
    val accent = tint(art.accentColor)
    val glow = tint(art.glowColor)
    val shade = Color.Black.copy(alpha = if (dimmed) 0.15f else 0.28f)

    val cx = size.width / 2f
    val baseY = size.height * 0.92f
    val facing = if (facingRight) 1f else -1f

    // Ground shadow.
    drawOval(
        color = Color.Black.copy(alpha = 0.35f),
        topLeft = Offset(cx - 26f * unit, baseY - 5f * unit),
        size = Size(52f * unit, 10f * unit),
    )

    if (art.treads) {
        drawRoundedBox(cx - 26f * unit, baseY - 16f * unit, 52f * unit, 14f * unit, 6f * unit, accent)
        repeat(5) { i ->
            drawRoundedBox(
                cx - 22f * unit + i * 9.5f * unit,
                baseY - 13f * unit,
                5f * unit,
                8f * unit,
                1.5f * unit,
                shade,
            )
        }
    } else {
        // Two piston legs.
        listOf(-11f, 7f).forEach { dx ->
            drawRoundedBox(cx + dx * unit, baseY - 20f * unit, 8f * unit, 16f * unit, 3f * unit, accent)
            drawRoundedBox(cx + (dx - 2f) * unit, baseY - 6f * unit, 12f * unit, 6f * unit, 3f * unit, body)
        }
    }

    val torsoTop = baseY - (if (art.treads) 54f else 58f) * unit
    val torsoHeight = (if (art.treads) 38f else 38f) * unit

    // Arms, drawn behind the torso.
    listOf(-1f, 1f).forEach { side ->
        val armX = cx + side * 25f * unit
        drawRoundedBox(armX - 5f * unit, torsoTop + 6f * unit, 10f * unit, 24f * unit, 4f * unit, accent)
        drawCircleAt(armX, torsoTop + 30f * unit, 6f * unit, body)
    }

    // Torso.
    drawRoundedBox(cx - 22f * unit, torsoTop, 44f * unit, torsoHeight, 9f * unit, body)
    drawRoundedBox(cx - 15f * unit, torsoTop + 6f * unit, 30f * unit, 20f * unit, 5f * unit, shade)

    // Power core.
    drawCircleAt(cx, torsoTop + 16f * unit, 8f * unit, glow.copy(alpha = 0.35f))
    drawCircleAt(cx, torsoTop + 16f * unit, 5f * unit, glow)

    // Neck.
    drawRoundedBox(cx - 5f * unit, torsoTop - 6f * unit, 10f * unit, 8f * unit, 2f * unit, accent)

    val headBottom = torsoTop - 4f * unit
    drawHead(art, cx, headBottom, unit, facing, body, accent, glow, shade)
}

private fun DrawScope.drawHead(
    art: BotArt,
    cx: Float,
    headBottom: Float,
    unit: Float,
    facing: Float,
    body: Color,
    accent: Color,
    glow: Color,
    shade: Color,
) {
    val headWidth = 34f * unit
    val headHeight = 28f * unit
    val headTop = headBottom - headHeight
    val left = cx - headWidth / 2f

    when (art.head) {
        HeadShape.BOX -> drawRoundedBox(left, headTop, headWidth, headHeight, 6f * unit, body)
        HeadShape.DOME -> {
            drawRoundedBox(left, headTop + headHeight * 0.35f, headWidth, headHeight * 0.65f, 4f * unit, body)
            drawArcTop(left, headTop, headWidth, headHeight * 0.9f, body)
        }
        HeadShape.VISOR -> {
            drawRoundedBox(left, headTop, headWidth, headHeight, 12f * unit, body)
            drawRoundedBox(
                left + 3f * unit,
                headTop + headHeight * 0.3f,
                headWidth - 6f * unit,
                headHeight * 0.34f,
                4f * unit,
                shade,
            )
        }
        HeadShape.CRESTED -> {
            drawRoundedBox(left, headTop, headWidth, headHeight, 5f * unit, body)
            drawRoundedBox(cx - 3f * unit, headTop - 9f * unit, 6f * unit, 11f * unit, 2f * unit, accent)
            drawRoundedBox(cx - 13f * unit, headTop - 5f * unit, 6f * unit, 7f * unit, 2f * unit, accent)
            drawRoundedBox(cx + 7f * unit, headTop - 5f * unit, 6f * unit, 7f * unit, 2f * unit, accent)
        }
    }

    if (art.antenna) {
        val ax = cx + facing * 10f * unit
        drawLine(
            color = accent,
            start = Offset(ax, headTop + 2f * unit),
            end = Offset(ax + facing * 5f * unit, headTop - 12f * unit),
            strokeWidth = 2.5f * unit,
        )
        drawCircleAt(ax + facing * 5f * unit, headTop - 13f * unit, 3f * unit, glow)
    }

    val eyeY = headTop + headHeight * 0.46f
    when (art.eyes) {
        EyeStyle.SINGLE -> {
            drawCircleAt(cx, eyeY, 6.5f * unit, glow.copy(alpha = 0.3f))
            drawCircleAt(cx, eyeY, 4f * unit, glow)
        }
        EyeStyle.DUAL -> listOf(-7f, 7f).forEach { dx ->
            drawCircleAt(cx + dx * unit, eyeY, 3.2f * unit, glow)
        }
        EyeStyle.TRIPLE -> listOf(-9f, 0f, 9f).forEach { dx ->
            drawCircleAt(cx + dx * unit, eyeY, 2.6f * unit, glow)
        }
        EyeStyle.SLIT -> drawRoundedBox(
            cx - 11f * unit,
            eyeY - 1.8f * unit,
            22f * unit,
            3.6f * unit,
            1.8f * unit,
            glow,
        )
    }

    // Jaw grille.
    drawRoundedBox(cx - 8f * unit, headBottom - 6f * unit, 16f * unit, 3.5f * unit, 1.5f * unit, shade)
}

private fun DrawScope.drawRoundedBox(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    radius: Float,
    color: Color,
) = drawRoundRect(
    color = color,
    topLeft = Offset(x, y),
    size = Size(w, h),
    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
)

private fun DrawScope.drawCircleAt(x: Float, y: Float, radius: Float, color: Color) =
    drawCircle(color = color, radius = radius, center = Offset(x, y))

private fun DrawScope.drawArcTop(x: Float, y: Float, w: Float, h: Float, color: Color) = drawArc(
    color = color,
    startAngle = 180f,
    sweepAngle = 180f,
    useCenter = true,
    topLeft = Offset(x, y),
    size = Size(w, h),
)

private fun lerpColor(from: Color, to: Color, t: Float): Color = Color(
    red = from.red + (to.red - from.red) * t,
    green = from.green + (to.green - from.green) * t,
    blue = from.blue + (to.blue - from.blue) * t,
    alpha = from.alpha + (to.alpha - from.alpha) * t,
)
