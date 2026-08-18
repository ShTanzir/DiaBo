package com.tanzir.diabo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Frosted-glass card used across Home / Project List / IDE panels for DiaBo's
 * signature glassmorphism look: translucent gradient fill + a thin light border.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 20,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val bg = MaterialTheme.colorScheme.background
    val isDark = (0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue) < 0.5f
    val glassTint = if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.55f)
    val borderTint = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.7f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(
                Brush.linearGradient(listOf(glassTint, glassTint.copy(alpha = glassTint.alpha * 0.6f)))
            )
            .border(1.dp, borderTint, RoundedCornerShape(cornerRadius.dp))
            .padding(contentPadding),
        content = content
    )
}
