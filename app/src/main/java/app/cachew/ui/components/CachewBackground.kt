package app.cachew.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import app.cachew.ui.theme.CachewBrush
import app.cachew.ui.theme.CachewColors

/**
 * Full-screen background. Day = green gradient, night = purple-dark gradient (screens 13–15, 17, 18).
 * On top of the vertical gradient we add a soft radial glow at the top centre, as in the mockups.
 */
@Composable
fun CachewBackground(
    modifier: Modifier = Modifier,
    night: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val glow = if (night) CachewColors.NightTop else CachewColors.BgTop
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (night) CachewBrush.Night else CachewBrush.Day)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(glow.copy(alpha = 0.7f), Color.Transparent),
                        center = Offset(size.width / 2f, 0f),
                        radius = size.width * 0.9f,
                    )
                )
            },
        content = content,
    )
}
