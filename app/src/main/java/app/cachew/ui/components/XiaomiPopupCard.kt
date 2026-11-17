package app.cachew.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cachew.R
import app.cachew.data.Store
import app.cachew.data.Xiaomi
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewType

/**
 * Xiaomi-only card: asks the user to allow "pop-up windows while running in the background",
 * without which the pause can't appear over a shop. Hidden on other phones and after "It's on".
 */
@Composable
fun XiaomiPopupCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    var done by remember { mutableStateOf(store.xiaomiPopupsDone) }
    if (!Xiaomi.isXiaomi || done) return

    GlassCard(modifier.fillMaxWidth(), selected = true) {
        Text(stringResource(R.string.xiaomi_title), style = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp), color = CachewColors.Title)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.xiaomi_body), style = CachewType.Body.copy(fontSize = 14.sp, lineHeight = 20.sp), color = CachewColors.TextSecondary)
        Spacer(Modifier.height(10.dp))
        Row {
            CardAction(stringResource(R.string.xiaomi_open), CachewColors.PurpleText) { Xiaomi.openPopupPermission(context) }
            Spacer(Modifier.width(8.dp))
            CardAction(stringResource(R.string.xiaomi_done), CachewColors.TextSecondary) {
                store.xiaomiPopupsDone = true
                done = true
            }
        }
    }
}

@Composable
private fun CardAction(text: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Text(
        text = text,
        style = CachewType.Button.copy(fontSize = 15.sp),
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    )
}
