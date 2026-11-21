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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cachew.R
import app.cachew.data.Game
import app.cachew.data.Mood
import app.cachew.data.Rules
import app.cachew.ui.components.GlassCard
import app.cachew.ui.components.Penny
import app.cachew.ui.components.PennyPose
import app.cachew.ui.components.SecondaryButton
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType
import app.cachew.ui.theme.Inter

/**
 * The choice screen after the two questions (design-v2/02): headline from the answers,
 * then three full-width actions. "Just look" is highlighted as the recommended one.
 */
@Composable
fun DecisionScreen(
    mood: Mood,
    onList: Boolean,
    shopName: String,
    waitHours: Int,
    /** False once "Close" has earned its +5 three times today: the tag is hidden then. */
    closeRewardLeft: Boolean,
    onJustLook: () -> Unit,
    onSpecific: () -> Unit,
    onClose: () -> Unit,
) {
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
            Text(headlineText(mood, onList), style = CachewType.H1, color = CachewColors.Title, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(if (Game.isRisky(mood, onList)) R.string.choice_body_risky else R.string.choice_body_planned),
                style = CachewType.Body,
                color = CachewColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            // Just look, save, don't buy: the recommended choice (purple glow).
            ChoiceCard(
                title = stringResource(R.string.choice_look_title),
                body = stringResource(R.string.choice_look_body, shopName, Rules.SESSION_MINUTES, waitHours),
                selected = true,
                onClick = onJustLook,
            ) {
                Text("+${Rules.CASHEWS_PER_SAVED_ITEM}", style = TagStyle, color = CachewColors.PurpleText)
                Spacer(Modifier.width(4.dp))
                CashewIcon(18.dp)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.choice_look_tag), style = TagStyle.copy(fontWeight = FontWeight.SemiBold), color = CachewColors.PurpleText)
            }
            Spacer(Modifier.height(CachewSize.Gap))

            ChoiceCard(
                title = stringResource(R.string.choice_specific_title),
                body = stringResource(R.string.choice_specific_body),
                selected = false,
                onClick = onSpecific,
            ) {
                Text(stringResource(R.string.choice_minutes_tag, Rules.SESSION_MINUTES), style = TagStyle.copy(fontWeight = FontWeight.SemiBold), color = CachewColors.TextSecondary)
            }
            Spacer(Modifier.height(CachewSize.Gap))

            // Close <shop>: a full-size button (not small text, see design-v2/README).
            SecondaryButton(
                text = stringResource(R.string.choice_close, shopName),
                onClick = onClose,
                trailing = if (closeRewardLeft) {
                    {
                        Text("+${Rules.CASHEWS_PER_CLOSE}", style = TagStyle, color = CachewColors.PurpleText)
                        Spacer(Modifier.width(4.dp))
                        CashewIcon(20.dp)
                    }
                } else null,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Glass card with a bold title, a small tag on the right and a description underneath. */
@Composable
private fun ChoiceCard(
    title: String,
    body: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: @Composable () -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth(), selected = selected, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = CardTitle, color = CachewColors.Title, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { tag() }
        }
        Spacer(Modifier.height(4.dp))
        Text(body, style = CachewType.Body, color = CachewColors.TextSecondary)
    }
}

@Composable
private fun CashewIcon(size: Dp) {
    Image(painterResource(R.drawable.ic_cashew), contentDescription = null, modifier = Modifier.size(size))
}

private val CardTitle = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
private val TagStyle = androidx.compose.ui.text.TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color.Unspecified)

/** "Tired, just scrolling." / "Stressed, but on your list." / "On your list and feeling fine." */
@Composable
private fun headlineText(mood: Mood, onList: Boolean): String {
    val moodWord = stringResource(
        when (mood) {
            Mood.Tired -> R.string.mood_tired
            Mood.Bored -> R.string.mood_bored
            Mood.Stressed -> R.string.mood_stressed
            Mood.Fine -> R.string.mood_fine
        }
    )
    return when (Game.headline(mood, onList)) {
        Game.Headline.MoodScrolling -> stringResource(R.string.headline_mood_scrolling, moodWord)
        Game.Headline.MoodButOnList -> stringResource(R.string.headline_mood_on_list, moodWord)
        Game.Headline.FineScrolling -> stringResource(R.string.headline_fine_scrolling)
        Game.Headline.AllGood -> stringResource(R.string.headline_all_good)
    }
}
