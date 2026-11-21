package app.cachew.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType

/**
 * Full-width glass button, the same size as [PrimaryButton] but quieter: for the second and third
 * choice ("Close Shein +5", "Still looking · 5 more min"). Built on [GlassCard], so it matches the cards.
 * [trailing] is an optional tag on the right, e.g. "+5" with a cashew.
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = CachewSize.ButtonHeight)
            .alpha(if (enabled) 1f else 0.55f),
        radius = CachewSize.ButtonRadius,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        onClick = if (enabled) onClick else null,
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                style = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                color = CachewColors.Title,
                modifier = Modifier.weight(1f),
            )
            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                trailing()
            }
        }
    }
}
