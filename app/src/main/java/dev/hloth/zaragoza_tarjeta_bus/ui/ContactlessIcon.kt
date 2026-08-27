package dev.hloth.zaragoza_tarjeta_bus.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

private const val ARCS = 3

@Composable
fun ContactlessIcon(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.08f
        val centerX = size.width * 0.1f
        val centerY = size.height / 2f
        val outerRadius = size.width * 0.72f
        for (arc in 1..ARCS) {
            val radius = outerRadius * arc / ARCS
            drawArc(
                color = color,
                startAngle = -52f,
                sweepAngle = 104f,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}
