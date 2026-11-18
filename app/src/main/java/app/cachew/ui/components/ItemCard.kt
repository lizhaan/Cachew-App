package app.cachew.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cachew.data.Money
import app.cachew.data.WaitItem
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.Inter
import coil3.compose.AsyncImage
import java.io.File

val ItemNameStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = CachewColors.Title)
val ItemPriceStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = CachewColors.Title)

/** Product photo (saved file, shown with Coil), or the shop's letter tile when there's none. */
@Composable
fun ItemThumb(thumbPath: String?, fallback: TileIcon, size: Dp = 84.dp, loading: Boolean = false) {
    val shape = RoundedCornerShape(size * 0.2f)
    when {
        loading -> Box(Modifier.size(size).clip(shape).background(CachewColors.Sand.copy(alpha = 0.12f)).alpha(pulse()))
        thumbPath != null && File(thumbPath).exists() -> AsyncImage(
            model = File(thumbPath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(shape).background(CachewColors.Sand),
        )
        else -> TileIconBox(fallback, size = size)
    }
}

/** Slowly pulsing alpha for skeleton placeholders. */
@Composable
private fun pulse(): Float {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(0.4f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "alpha")
    return alpha
}

/** Grey bar standing in for text while the product page loads. */
@Composable
fun SkeletonBar(width: Dp, height: Dp = 16.dp) {
    Box(Modifier.width(width).height(height).clip(RoundedCornerShape(6.dp)).background(CachewColors.Sand.copy(alpha = 0.12f)).alpha(pulse()))
}

/**
 * Card with editable name and price (screen 16 and "What did you want?").
 * [loading] shows skeleton bars instead of the hints while the page is being read;
 * the fields stay editable the whole time.
 */
@Composable
fun EditableItemCard(
    thumb: @Composable () -> Unit,
    name: String,
    onName: (String) -> Unit,
    nameHint: String,
    priceText: String,
    onPrice: (String) -> Unit,
    priceHint: String,
    currency: String,
    loading: Boolean = false,
    nameModifier: Modifier = Modifier,
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            thumb()
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                if (loading && name.isEmpty()) SkeletonBar(150.dp, 20.dp)
                ItemField(
                    value = name,
                    onValueChange = onName,
                    hint = if (loading) "" else nameHint,
                    style = ItemNameStyle,
                    keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    modifier = nameModifier,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Money.symbol(currency), style = ItemPriceStyle)
                    Spacer(Modifier.width(2.dp))
                    if (loading && priceText.isEmpty()) {
                        SkeletonBar(80.dp, 24.dp)
                    } else {
                        ItemField(
                            value = priceText,
                            onValueChange = { new -> onPrice(new.filter { it.isDigit() || it == '.' || it == ',' || it == ' ' }) },
                            hint = priceHint,
                            style = ItemPriceStyle,
                            keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        )
                    }
                }
            }
        }
    }
}

/** Read-only card (screen 19): photo, name, a subtitle like "Added Thu, 23:43", price. */
@Composable
fun ItemCard(item: WaitItem, fallback: TileIcon, subtitle: String) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ItemThumb(item.thumbPath, fallback)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = ItemNameStyle, maxLines = 2)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontFamily = Inter, fontSize = 15.sp, color = CachewColors.TextSecondary)
                if (item.price != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(Money.format(item.price, item.currency), style = ItemPriceStyle)
                }
            }
        }
    }
}

/** Borderless single-line field with a faded hint, so it reads like card text. */
@Composable
private fun ItemField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    style: TextStyle,
    keyboard: KeyboardOptions,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = style,
        keyboardOptions = keyboard,
        cursorBrush = SolidColor(CachewColors.PurpleLight),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty() && hint.isNotEmpty()) {
                    Text(hint, style = style.copy(color = CachewColors.TextSecondary.copy(alpha = 0.45f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold))
                }
                inner()
            }
        },
    )
}
