package app.cachew.pause

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cachew.R
import app.cachew.data.Game
import app.cachew.data.GameSnapshot
import app.cachew.data.Money
import app.cachew.data.RewardInfo
import app.cachew.ui.components.GlassCard
import app.cachew.ui.components.HeartsRow
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
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

/** Common layout: scrollable content, buttons pinned at the bottom. */
@Composable
private fun OutcomeLayout(
    content: @Composable ColumnScope.() -> Unit,
    buttons: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = CachewSize.ScreenPadding),
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
        Column(Modifier.padding(bottom = 8.dp), content = buttons)
    }
}

/** Screen 17 "Nice pause." The only way out is closing the shop (no "continue to shop"). */
@Composable
fun RewardScreen(reward: RewardInfo, night: Boolean, onClose: () -> Unit) {
    OutcomeLayout(
        content = {
            Spacer(Modifier.height(28.dp))
            NutsChip(reward.nuts)
            Spacer(Modifier.height(12.dp))
            Penny(PennyPose.Love2, 280.dp)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.reward_title), style = CachewType.H1.copy(fontSize = 36.sp), color = CachewColors.Title)
            Spacer(Modifier.height(10.dp))
            val item = reward.item
            val hours = (item.waitMillis / 3_600_000L).toInt()
            Text(
                when {
                    item.waitMillis < 3_600_000L -> stringResource(R.string.reward_body_minute)
                    item.price != null -> stringResource(R.string.reward_body_price, Money.format(item.price, item.currency), hours)
                    else -> stringResource(R.string.reward_body_no_price, hours)
                },
                style = CachewType.Body,
                color = CachewColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.ic_flame), contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        pluralStringResource(R.plurals.streak_days, reward.streak, reward.streak),
                        style = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                        color = CachewColors.Title,
                        modifier = Modifier.weight(1f),
                    )
                    if (reward.questDone) {
                        Text(stringResource(R.string.reward_quest_done), style = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp), color = CachewColors.PurpleText)
                    }
                }
            }
        },
        buttons = {
            PrimaryButton(
                text = stringResource(if (night) R.string.reward_goodnight else R.string.pause_back_to_den),
                onClick = onClose,
            )
        },
    )
}

@Composable
private fun NutsChip(nuts: Int) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(CachewColors.CardFill)
            .border(1.dp, CachewColors.CardBorder, shape)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.ic_cashew), contentDescription = null, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.reward_nuts, nuts), fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = CachewColors.Title)
    }
}

/** Screen 21 "Cachew turned away" (bought without a heart). */
@Composable
fun BoughtScreen(game: GameSnapshot, onBackToShop: () -> Unit, onBackToDen: () -> Unit) {
    OutcomeLayout(
        content = {
            Spacer(Modifier.height(36.dp))
            Penny(PennyPose.Back, 280.dp)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.bought_title), style = CachewType.H1.copy(fontSize = 34.sp), color = CachewColors.Title, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.bought_body), style = CachewType.Body, color = CachewColors.TextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(CachewSize.Gap)) {
                StatCard(game.streak.toString(), stringResource(R.string.stat_streak), Modifier.weight(1f), R.drawable.ic_flame)
                StatCard(game.heartsLeft.toString(), stringResource(R.string.stat_hearts_left), Modifier.weight(1f), R.drawable.ic_heart)
            }
        },
        buttons = {
            // They decided to buy: the main button takes them back to the shop (quiet window).
            PrimaryButton(text = stringResource(R.string.bought_ok), onClick = onBackToShop)
            TextAction(stringResource(R.string.pause_back_to_den), onBackToDen)
        },
    )
}

/** Screen 22 "−1 heart" (day background, as in the mockup). */
@Composable
fun HeartUsedScreen(game: GameSnapshot, shopName: String, onBackToDen: () -> Unit, onContinueToShop: () -> Unit) {
    OutcomeLayout(
        content = {
            Spacer(Modifier.height(36.dp))
            Penny(PennyPose.Back, 250.dp)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.heart_title), style = CachewType.H1.copy(fontSize = 36.sp), color = CachewColors.Title)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.heart_body), style = CachewType.Body, color = CachewColors.TextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            GlassCard(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                HeartsRow(left = game.heartsLeft, total = Game.HEARTS_PER_MONTH, size = 30.dp, gap = 14.dp)
                Spacer(Modifier.height(12.dp))
                val month = LocalDate.now().month.getDisplayName(DateTextStyle.FULL, Locale.US)
                Text(
                    pluralStringResource(R.plurals.hearts_left_in_month, game.heartsLeft, game.heartsLeft, month),
                    style = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                    color = CachewColors.Title,
                )
            }
            Spacer(Modifier.height(CachewSize.Gap))
            Row(horizontalArrangement = Arrangement.spacedBy(CachewSize.Gap)) {
                StatCard(game.streak.toString(), stringResource(R.string.stat_streak_kept), Modifier.weight(1f), R.drawable.ic_flame)
                StatCard(
                    game.heartsRefill.format(DateTimeFormatter.ofPattern("MMM d", Locale.US)),
                    stringResource(R.string.stat_hearts_refill),
                    Modifier.weight(1f),
                )
            }
        },
        buttons = {
            PrimaryButton(text = stringResource(R.string.pause_back_to_den), onClick = onBackToDen)
            TextAction(stringResource(R.string.pause_continue_to, shopName), onContinueToShop)
        },
    )
}
