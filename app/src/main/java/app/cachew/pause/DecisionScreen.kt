package app.cachew.pause

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cachew.R
import app.cachew.data.Game
import app.cachew.data.Mood
import app.cachew.ui.components.GlassCard
import app.cachew.ui.components.HeartsRow
import app.cachew.ui.components.Penny
import app.cachew.ui.components.PennyPose
import app.cachew.ui.components.TextAction
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType
import app.cachew.ui.theme.Inter
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Screen 15: headline from the answers + the three choices. */
@Composable
fun DecisionScreen(
    mood: Mood,
    onList: Boolean,
    night: Boolean,
    heartsLeft: Int,
    heartsRefill: LocalDate,
    /** Set at night in Strict mode: the button is replaced by "no buy anyway until <time>". */
    noBuyAnywayUntil: LocalTime?,
    onWait: () -> Unit,
    onHeart: () -> Unit,
    onBuyAnyway: () -> Unit,
) {
    val parts = Game.headlineParts(mood, onList, night)
    val headline = headlineText(parts, onList)

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
            Spacer(Modifier.height(24.dp))
            Penny(PennyPose.Calm, 170.dp)
            Spacer(Modifier.height(16.dp))
            Text(headline, style = CachewType.H1, color = CachewColors.Title, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(if (parts.isEmpty()) R.string.decision_body_planned else R.string.decision_body_risky),
                style = CachewType.Body,
                color = CachewColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            // Add to waiting list: the highlighted, recommended choice.
            GlassCard(Modifier.fillMaxWidth(), selected = true, onClick = onWait) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.decision_wait_title), style = CardTitle, color = CachewColors.Title, modifier = Modifier.weight(1f))
                    Text("+${Game.CASHEWS_PER_PAUSE}", fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = CachewColors.PurpleText)
                    Spacer(Modifier.width(6.dp))
                    Image(painterResource(R.drawable.ic_cashew), contentDescription = null, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.decision_wait_body), style = CachewType.Body, color = CachewColors.TextSecondary)
            }
            Spacer(Modifier.height(CachewSize.Gap))

            // Use a heart: disabled (faded, with the refill date) when none are left.
            val hasHearts = heartsLeft > 0
            GlassCard(
                Modifier.fillMaxWidth().alpha(if (hasHearts) 1f else 0.55f),
                onClick = if (hasHearts) onHeart else null,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.decision_heart_title), style = CardTitle, color = CachewColors.Title, modifier = Modifier.weight(1f))
                    HeartsRow(left = heartsLeft, total = Game.HEARTS_PER_MONTH, size = 24.dp, gap = 8.dp)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (hasHearts) stringResource(R.string.decision_heart_body)
                    else stringResource(R.string.decision_heart_empty, heartsRefill.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))),
                    style = CachewType.Body,
                    color = CachewColors.TextSecondary,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        if (noBuyAnywayUntil == null) {
            TextAction(stringResource(R.string.decision_buy_anyway), onBuyAnyway, Modifier.padding(bottom = 8.dp))
        } else {
            Text(
                stringResource(R.string.decision_no_buy_until, noBuyAnywayUntil.format(DateTimeFormatter.ofPattern("HH:mm"))),
                style = CachewType.Body,
                color = CachewColors.TextSecondary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).padding(bottom = 8.dp),
            )
        }
    }
}

private val CardTitle = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)

/** "Tired, late, not on your list." / "Stressed, but on your list." / "On your list and feeling fine." */
@Composable
private fun headlineText(parts: List<Game.Part>, onList: Boolean): String {
    if (parts.isEmpty()) return stringResource(R.string.decision_all_good)
    val words = parts.map {
        stringResource(
            when (it) {
                Game.Part.Tired -> R.string.part_tired
                Game.Part.Bored -> R.string.part_bored
                Game.Part.Stressed -> R.string.part_stressed
                Game.Part.Late -> R.string.part_late
                Game.Part.NotOnList -> R.string.part_not_on_list
            }
        )
    }
    val joined = words.joinToString(", ").replaceFirstChar { it.uppercase() }
    return stringResource(if (onList) R.string.decision_headline_but_on_list else R.string.decision_headline_end, joined)
}
