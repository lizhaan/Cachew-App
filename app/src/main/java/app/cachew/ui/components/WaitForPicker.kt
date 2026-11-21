package app.cachew.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cachew.BuildConfig
import app.cachew.R
import app.cachew.data.Rules
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType

/** Special value for the debug-only 1-minute wait (long-press "24h"). */
const val TEST_MINUTE = -1

/** Hours chosen in the picker → milliseconds. */
fun waitMillisFor(hours: Int): Long = if (hours == TEST_MINUTE) 60_000L else hours * 3_600_000L

/** "Wait for" 24h / 48h / 72h. In debug builds a long press on 24h means 1 minute (to test reminders). */
@Composable
fun WaitForPicker(hours: Int, onHours: (Int) -> Unit) {
    Column {
        Text(stringResource(R.string.item_wait_for), style = CachewType.Body.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold), color = CachewColors.TextSecondary)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(CachewSize.Gap)) {
            Rules.WAIT_HOURS_OPTIONS.forEach { h ->
                val testMinute = h == 24 && hours == TEST_MINUTE
                ChoiceTile(
                    selected = hours == h || testMinute,
                    onClick = { onHours(h) },
                    onLongClick = if (BuildConfig.DEBUG && h == 24) ({ onHours(TEST_MINUTE) }) else null,
                    modifier = Modifier.weight(1f),
                ) {
                    ChoiceLabel(if (testMinute) "1m" else stringResource(R.string.item_hours_short, h))
                }
            }
        }
    }
}

/** Label for the main button: "Wait 48 hours" / "Wait 1 minute (test)". */
@Composable
fun waitButtonText(hours: Int): String =
    if (hours == TEST_MINUTE) stringResource(R.string.item_wait_button_minute)
    else stringResource(R.string.item_wait_button, hours)
