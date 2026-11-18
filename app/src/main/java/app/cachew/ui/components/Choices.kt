package app.cachew.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cachew.R
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType
import app.cachew.ui.theme.Inter

/** Selectable glass tile with centred content (mood tiles, Yes/No, 24h/48h/72h). */
@Composable
fun ChoiceTile(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    radius: Dp = CachewSize.CardRadius,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth().height(height),
        selected = selected,
        radius = radius,
        contentPadding = PaddingValues(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        Spacer(Modifier.weight(1f))
        content()
        Spacer(Modifier.weight(1f))
    }
}

/** Big centred label used inside ChoiceTile. */
@Composable
fun ChoiceLabel(text: String) {
    Text(text, fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = CachewColors.Text, textAlign = TextAlign.Center)
}

/** Secondary action under a PrimaryButton: plain text, full width, easy to tap. */
@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Text(
        text = text,
        style = CachewType.Button,
        color = CachewColors.TextSecondary.copy(alpha = if (enabled) 1f else 0.4f),
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CachewSize.ButtonRadius))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
    )
}

/** Small stat card: "0 🔥 / streak", "3 ♥ / hearts left", "Dec 1 / hearts refill". */
@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier, icon: Int? = null) {
    GlassCard(modifier, horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(vertical = 16.dp, horizontal = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = CachewColors.Title)
            if (icon != null) {
                Spacer(Modifier.width(8.dp))
                Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(label, style = CachewType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = CachewColors.TextSecondary)
    }
}

/** Row of hearts: [left] filled, the rest of [total] empty. */
@Composable
fun HeartsRow(left: Int, total: Int, size: Dp = 26.dp, gap: Dp = 10.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        repeat(total) { i ->
            Image(
                painterResource(if (i < left) R.drawable.ic_heart else R.drawable.ic_heart_empty),
                contentDescription = null,
                modifier = Modifier.size(size),
            )
        }
    }
}
