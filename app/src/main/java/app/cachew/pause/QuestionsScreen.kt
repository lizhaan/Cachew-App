package app.cachew.pause

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cachew.R
import app.cachew.data.Mood
import app.cachew.ui.components.ChoiceLabel
import app.cachew.ui.components.ChoiceTile
import app.cachew.ui.components.Penny
import app.cachew.ui.components.PennyPose
import app.cachew.ui.components.PrimaryButton
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType

/**
 * Screen 14: mood (2×2 tiles) + "Did you open <shop> for something specific, or just to scroll?".
 * [specific] = true for "Something specific", false for "Just scrolling". Continue needs both answers.
 */
@Composable
fun QuestionsScreen(
    shopName: String,
    /** "22:42": when the shop was opened, shown top right. */
    time: String,
    mood: Mood?,
    onList: Boolean?,
    onMood: (Mood) -> Unit,
    onOnList: (Boolean) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = CachewSize.ScreenPadding),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.questions_label).uppercase(), style = CachewType.Label, color = CachewColors.PurpleText, modifier = Modifier.weight(1f))
                Text(time, style = CachewType.Body.copy(fontSize = 14.sp), color = CachewColors.TextSecondary)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    stringResource(R.string.questions_title),
                    style = CachewType.H1,
                    color = CachewColors.Title,
                    modifier = Modifier.weight(1f).padding(top = 12.dp),
                )
                Penny(PennyPose.Question, 112.dp)
            }
            Spacer(Modifier.height(24.dp))

            val moods = listOf(
                Mood.Tired to ("😴" to R.string.mood_tired),
                Mood.Bored to ("😐" to R.string.mood_bored),
                Mood.Stressed to ("😣" to R.string.mood_stressed),
                Mood.Fine to ("🙂" to R.string.mood_fine),
            )
            moods.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(CachewSize.Gap)) {
                    row.forEach { (m, labels) ->
                        ChoiceTile(selected = mood == m, onClick = { onMood(m) }, height = 108.dp, modifier = Modifier.weight(1f)) {
                            Text(labels.first, fontSize = 32.sp)
                            Spacer(Modifier.height(6.dp))
                            ChoiceLabel(stringResource(labels.second))
                        }
                    }
                }
                Spacer(Modifier.height(CachewSize.Gap))
            }

            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.questions_specific, shopName),
                style = CachewType.H2,
                color = CachewColors.Title,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(CachewSize.Gap)) {
                ChoiceTile(selected = onList == true, onClick = { onOnList(true) }, height = 72.dp, modifier = Modifier.weight(1f)) {
                    ChoiceLabel(stringResource(R.string.answer_specific))
                }
                ChoiceTile(selected = onList == false, onClick = { onOnList(false) }, height = 72.dp, modifier = Modifier.weight(1f)) {
                    ChoiceLabel(stringResource(R.string.answer_scrolling))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            PrimaryButton(
                text = stringResource(R.string.questions_continue),
                enabled = mood != null && onList != null,
                onClick = onContinue,
            )
        }
    }
}
