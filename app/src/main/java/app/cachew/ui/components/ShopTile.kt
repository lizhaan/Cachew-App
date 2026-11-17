package app.cachew.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewType
import app.cachew.ui.theme.Inter

/** What goes into the 44dp square at the top of a tile. */
sealed interface TileIcon {
    /** Brand letter on a coloured square (recommended shops, as in mockup 08). */
    data class Letter(val letter: String, val color: Color) : TileIcon
    /** The app's real launcher icon (apps picked via "+ Other app"). */
    data class AppIcon(val bitmap: ImageBitmap) : TileIcon
    /** The "+" square of the "Other app" tile. */
    data object Plus : TileIcon
}

/**
 * One tile in the 3-column shop grid (screen 08).
 * [dimmed] = recommended shop that isn't installed: faded, with a "not installed" caption, still selectable.
 */
@Composable
fun ShopTile(
    label: String,
    icon: TileIcon,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    caption: String? = null,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth().heightIn(min = 118.dp),
        selected = selected,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        onClick = onClick,
    ) {
        val fade = if (dimmed && !selected) 0.45f else 1f
        TileIconBox(icon, Modifier.alpha(fade))
        Spacer(Modifier.height(10.dp))
        Text(
            text = label,
            style = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 19.sp),
            color = CachewColors.Text.copy(alpha = fade),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (caption != null) {
            Text(
                text = caption,
                fontFamily = Inter,
                fontSize = 11.sp,
                color = CachewColors.TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/** The coloured-letter / app-icon / "+" square. [size] 44dp in the grid, smaller in the pause pill. */
@Composable
fun TileIconBox(icon: TileIcon, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val shape = RoundedCornerShape(size * 0.27f)
    when (icon) {
        is TileIcon.Letter -> Box(
            modifier.size(size).clip(shape).background(icon.color),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon.letter, fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.45f).sp, color = Color.White)
        }
        is TileIcon.AppIcon -> Image(
            bitmap = icon.bitmap,
            contentDescription = null,
            modifier = modifier.size(size).clip(shape),
        )
        TileIcon.Plus -> Box(
            modifier.size(size).clip(shape).background(CachewColors.Purple.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = CachewColors.PurpleText)
        }
    }
}
