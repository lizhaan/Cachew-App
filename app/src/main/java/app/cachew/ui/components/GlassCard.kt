package app.cachew.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    var m = modifier
    if (selected) {
        m = m.shadow(14.dp, shape, ambientColor = CachewColors.Purple, spotColor = CachewColors.Purple)
    }
    m = m.clip(shape)
    m = if (selected) {
        m.background(SelectedFill)
            .border(CachewSize.BorderWidthSelected, CachewColors.SelectedBorder, shape)
    } else {
        m.background(CachewColors.CardFill)
            .border(CachewSize.BorderWidth, CachewColors.CardBorder, shape)
    }
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(
        modifier = m.padding(contentPadding),
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

// Selected cards in the mockups are a cool purple-navy glass, lighter at the top.
private val SelectedFill = Brush.verticalGradient(
    listOf(CachewColors.Purple.copy(alpha = 0.22f), CachewColors.NightTop.copy(alpha = 0.55f))
)
