package app.cachew.ui.screens.stillwant

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cachew.R
import app.cachew.data.Money
import app.cachew.data.WinInfo
import app.cachew.ui.components.Penny
import app.cachew.ui.components.PennyPose
import app.cachew.ui.components.PrimaryButton
import app.cachew.ui.components.StatCard
import app.cachew.ui.components.TextAction
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType
import app.cachew.ui.theme.Inter
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Screen 20: "+$64", Penny lying on a pile of cashews, level and three stats. */
@Composable
fun WinScreen(info: WinInfo, onDone: () -> Unit) {
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = CachewSize.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.win_label).uppercase(), style = CachewType.Label, color = CachewColors.PurpleText)
            Spacer(Modifier.height(4.dp))
            Text(
                text = info.amount?.let { "+" + Money.format(it, info.currency) } ?: stringResource(R.string.win_let_go),
                fontFamily = Inter,
                fontWeight = FontWeight.Black,
                fontSize = if (info.amount != null) 76.sp else 54.sp,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.verticalGradient(listOf(CachewColors.Title, CachewColors.PurpleSoft)),
                ),
            )
            Spacer(Modifier.height(8.dp))
            StashPile()
            Spacer(Modifier.height(10.dp))
            Text(
                if (info.cashewsEarned > 0) stringResource(R.string.win_stash_earned, info.cashewsEarned, info.stash)
                else stringResource(R.string.win_stash, info.stash),
                style = CachewType.Body.copy(fontWeight = FontWeight.SemiBold), color = CachewColors.Text)
            Text(
                stringResource(if (info.levelledUp) R.string.win_levelled_up else R.string.win_stash_grew),
                style = CachewType.H1.copy(fontSize = 32.sp),
                color = CachewColors.Title,
                textAlign = TextAlign.Center,
            )
            val levelName = stringArrayResource(R.array.level_names)[info.level.number - 1]
            val reward = stringArrayResource(R.array.level_rewards)[info.level.number - 1]
            Text(
                if (info.levelledUp && reward.isNotEmpty()) stringResource(R.string.win_level_reward, info.level.number, levelName, reward)
                else stringResource(R.string.win_level, info.level.number, levelName),
                textAlign = TextAlign.Center,
                style = CachewType.Body.copy(fontSize = 18.sp), color = CachewColors.TextSecondary)
            Spacer(Modifier.height(20.dp))
            val month = LocalDate.now().month.getDisplayName(TextStyle.SHORT, Locale.US)
            Row(horizontalArrangement = Arrangement.spacedBy(CachewSize.Gap)) {
                StatCard(Money.format(info.keptThisMonth, info.currency), stringResource(R.string.win_kept_in, month), Modifier.weight(1f))
                StatCard(info.streak.toString(), stringResource(R.string.win_day_streak), Modifier.weight(1f), R.drawable.ic_flame)
                StatCard(info.heartsLeft.toString(), stringResource(R.string.stat_hearts_left), Modifier.weight(1f), R.drawable.ic_heart)
            }
            Spacer(Modifier.height(16.dp))
        }

        PrimaryButton(
            text = stringResource(R.string.win_share),
            onClick = {
                val text = info.amount?.let {
                    context.getString(R.string.win_share_text_amount, Money.format(it, info.currency), info.stash)
                } ?: context.getString(R.string.win_share_text, info.stash)
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                context.startActivity(Intent.createChooser(send, null))
            },
        )
        TextAction(stringResource(R.string.pause_back_to_den), onDone)
    }
}

/** Cachew (pose "lie") on a heap of cashew icons, as in mockup 20. */
@Composable
private fun StashPile() {
    // (x, y, rotation) of each nut, arranged into a low mound.
    val nuts = listOf(
        Triple(-120, 40, -20f), Triple(-90, 22, 15f), Triple(-60, 44, -5f), Triple(-30, 18, 30f),
        Triple(0, 42, -25f), Triple(30, 16, 10f), Triple(60, 44, 35f), Triple(90, 22, -15f),
        Triple(120, 40, 20f), Triple(-75, 0, -35f), Triple(-15, -4, 5f), Triple(45, -2, -30f),
        Triple(-45, 30, 50f), Triple(15, 30, -45f), Triple(75, 30, 0f),
    )
    Box(Modifier.size(320.dp, 250.dp), contentAlignment = Alignment.BottomCenter) {
        nuts.forEach { (x, y, r) ->
            Image(
                painterResource(R.drawable.ic_cashew),
                contentDescription = null,
                modifier = Modifier.offset(x.dp, (-y).dp - 6.dp).size(54.dp).rotate(r),
            )
        }
        Penny(PennyPose.Lie, 230.dp, Modifier.offset(y = (-48).dp))
    }
}
