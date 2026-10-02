package com.example.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.neuShadow(
    lightShadow: Color,
    darkShadow: Color,
    cornerRadius: Dp,
    offsetX: Dp = 6.dp,
    offsetY: Dp = 6.dp,
    blurRadius: Dp = 12.dp,
    isPressed: Boolean = false
) = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = android.graphics.Color.TRANSPARENT

        // Dark Shadow
        frameworkPaint.setShadowLayer(
            blurRadius.toPx(),
            if (isPressed) -offsetX.toPx() else offsetX.toPx(),
            if (isPressed) -offsetY.toPx() else offsetY.toPx(),
            darkShadow.toArgb()
        )
        canvas.drawRoundRect(
            0f, 0f, size.width, size.height,
            cornerRadius.toPx(), cornerRadius.toPx(),
            paint
        )

        // Light Shadow
        frameworkPaint.setShadowLayer(
            blurRadius.toPx(),
            if (isPressed) offsetX.toPx() else -offsetX.toPx(),
            if (isPressed) offsetY.toPx() else -offsetY.toPx(),
            lightShadow.toArgb()
        )
        canvas.drawRoundRect(
            0f, 0f, size.width, size.height,
            cornerRadius.toPx(), cornerRadius.toPx(),
            paint
        )
    }
}
