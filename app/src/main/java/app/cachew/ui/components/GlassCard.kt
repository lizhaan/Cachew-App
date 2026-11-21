package app.cachew.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize

/**
 * Frosted "glass" card used everywhere: translucent white fill + thin sand border.
 * selected = purple-tinted fill, 1.5dp purple border and a soft purple glow (shop tiles, decision cards).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    radius: Dp = CachewSize.CardRadius,
    contentPadding: PaddingValues = PaddingValues(CachewSize.CardPadding),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    var m = modifier
    if (selected) {
        // Soft purple glow drawn as fading outlines around the card. (A platform shadow shows
        // through the see-through glass as a dark rectangle, so it isn't used here.)
        m = m.drawBehind {
            val r = radius.toPx()
            for (i in 1..GLOW_STEPS) {
                val grow = i * 2.dp.toPx()
                drawRoundRect(
                    color = CachewColors.Purple.copy(alpha = 0.16f * (GLOW_STEPS + 1 - i) / GLOW_STEPS),
                    topLeft = Offset(-grow, -grow),
                    size = Size(size.width + grow * 2, size.height + grow * 2),
                    cornerRadius = CornerRadius(r + grow),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
    }
    m = m.clip(shape)
    m = if (selected) {
        m.background(SelectedFill)
            .border(CachewSize.BorderWidthSelected, CachewColors.SelectedBorder, shape)
    } else {
        m.background(CachewColors.CardFill)
            .border(CachewSize.BorderWidth, CachewColors.CardBorder, shape)
    }
    if (onLongClick != null) {
        m = m.combinedClickable(onClick = onClick ?: {}, onLongClick = onLongClick)
    } else if (onClick != null) {
        m = m.clickable(onClick = onClick)
    }
    Column(
        modifier = m.padding(contentPadding),
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

private const val GLOW_STEPS = 6

// Selected cards in the mockups are a cool purple-navy glass, lighter at the top.
private val SelectedFill = Brush.verticalGradient(
    listOf(CachewColors.Purple.copy(alpha = 0.22f), CachewColors.NightTop.copy(alpha = 0.55f))
)
