package app.cachew.pause

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cachew.R
import app.cachew.ui.components.Penny
import app.cachew.ui.components.PennyPose
import app.cachew.ui.components.PrimaryButton
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Night shield block: during the night hours a picked shop doesn't open at all.
 * No questions, no choices, hearts don't help — just "Close" (phone's home screen).
 */
@Composable
fun NightBlockScreen(shopName: String, opensAt: LocalTime, onClose: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = CachewSize.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Penny(PennyPose.Sleep, 260.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.block_title, shopName, opensAt.format(DateTimeFormatter.ofPattern("HH:mm"))),
            style = CachewType.H1.copy(fontSize = 32.sp),
            color = CachewColors.Title,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.block_body), style = CachewType.Body.copy(fontSize = 17.sp), color = CachewColors.TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1.2f))
        PrimaryButton(text = stringResource(R.string.block_close), onClick = onClose)
        Spacer(Modifier.height(16.dp))
    }
}
