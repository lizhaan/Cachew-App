package app.cachew.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.cachew.ui.theme.CachewBrush
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType

/**
 * Main call-to-action: purple gradient with a soft purple glow.
 * When disabled it turns into a dark glass button (like "Answer Penny" during the countdown).
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(CachewSize.ButtonRadius)
    val base = modifier
        .fillMaxWidth()
        .height(CachewSize.ButtonHeight)
    val styled = if (enabled) {
        base
            .shadow(18.dp, shape, ambientColor = CachewColors.Purple, spotColor = CachewColors.Purple)
            .clip(shape)
            .background(CachewBrush.PrimaryButton)
    } else {
        base
            .clip(shape)
            .background(CachewColors.CardFill)
            .border(CachewSize.BorderWidth, CachewColors.CardBorder, shape)
    }
    Box(
        modifier = styled.clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = CachewType.Button,
            color = if (enabled) CachewColors.Title else CachewColors.TextSecondary,
        )
    }
}
