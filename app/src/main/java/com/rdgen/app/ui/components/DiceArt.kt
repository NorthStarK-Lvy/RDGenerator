package com.rdgen.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 空状态里的骰子插画：纯 Canvas 画，不占资源、跟着主题换色。
 */
@Composable
fun DiceArt(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    val container = MaterialTheme.colorScheme.primaryContainer
    val accent = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier.size(size).rotate(-8f)) {
        val side = this.size.minDimension * 0.74f
        val origin = Offset(
            x = (this.size.width - side) / 2f,
            y = (this.size.height - side) / 2f,
        )
        val radius = CornerRadius(side * 0.24f)

        drawRoundRect(
            color = container,
            topLeft = origin,
            size = Size(side, side),
            cornerRadius = radius,
        )
        drawRoundRect(
            color = outline,
            topLeft = origin,
            size = Size(side, side),
            cornerRadius = radius,
            style = Stroke(width = 1.5.dp.toPx()),
        )

        val dotRadius = side * 0.072f
        val facePositions = listOf(
            0.27f to 0.27f,
            0.73f to 0.27f,
            0.50f to 0.50f,
            0.27f to 0.73f,
            0.73f to 0.73f,
        )
        facePositions.forEach { (fx, fy) ->
            drawCircle(
                color = accent,
                radius = dotRadius,
                center = Offset(origin.x + side * fx, origin.y + side * fy),
            )
        }
    }
}
