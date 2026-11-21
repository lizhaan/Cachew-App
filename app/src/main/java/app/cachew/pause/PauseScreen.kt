package app.cachew.pause

import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cachew.R
import app.cachew.data.EntryAction
import app.cachew.data.Mood
import app.cachew.data.PauseRecord
import app.cachew.data.Rules
import app.cachew.data.SessionMode
import app.cachew.data.ShopLabel
import app.cachew.data.ShopLabels
import app.cachew.data.Store
import app.cachew.ui.components.CachewBackground
import app.cachew.ui.components.Penny
import app.cachew.ui.components.PennyPose
import app.cachew.ui.components.PrimaryButton
import app.cachew.ui.components.TileIconBox
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType
import app.cachew.ui.theme.Inter
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import kotlin.math.ceil

private const val PAUSE_SECONDS = Rules.PAUSE_SECONDS

/** The steps of one pause. Saved as an enum so it survives the activity being recreated. */
private enum class Step { Countdown, Questions, Decision }

/**
 * One whole pause, step by step (v2):
 * 13 countdown (10 s) → 14 questions → choice screen →
 *   "Just look" / "I need something specific" → the shop opens for a 10-minute session
 *   "Close <shop>" → +5 cashews (max 3 a day), phone's home screen
 *
 * The Store changes only on the final choice, so backing out halfway changes nothing
 * (PauseActivity logs that as "Left").
 */
@Composable
fun PauseFlow(
    shopPackage: String,
    openedAt: Long,
    /** "Buy it" from the waiting list: just the countdown, then [onBuy] (no questions). */
    buying: Boolean,
    shopNameHint: String?,
    onStartSession: (PauseRecord, SessionMode) -> Unit,
    onClose: (PauseRecord) -> Unit,
    onBuy: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val label by produceState<ShopLabel?>(null, shopPackage) {
        if (shopPackage.isNotEmpty()) value = ShopLabels.load(context, shopPackage)
    }
    val shopName = label?.name ?: shopNameHint ?: ShopLabels.quickName(shopPackage)

    // Night is decided once, when the shop was opened, by the night shield's schedule.
    // (Stage 5 replaces the night pause with a full block.)
    val night = remember(openedAt) {
        store.nightShield.isNight(Instant.ofEpochMilli(openedAt).atZone(ZoneId.systemDefault()).toLocalTime())
    }

    var step by rememberSaveable { mutableStateOf(Step.Countdown) }
    var mood by rememberSaveable { mutableStateOf<Mood?>(null) }
    var onList by rememberSaveable { mutableStateOf<Boolean?>(null) }

    fun record(action: EntryAction) = PauseRecord(
        at = openedAt, shopPackage = shopPackage, mood = mood, onList = onList, action = action,
    )

    // Pause, questions and choice always use the night background, to stand apart from the shop.
    CachewBackground(night = true) {
        if (buying) {
            PauseCountdown(
                label = label, shopName = shopName, openedAt = openedAt,
                body = stringResource(R.string.pause_body_buy),
                button = stringResource(R.string.pause_continue_to, shopName),
                onAnswer = onBuy,
            )
        } else when (step) {
            Step.Countdown -> PauseCountdown(
                label = label, shopName = shopName, openedAt = openedAt,
                body = stringResource(if (night) R.string.pause_body_night else R.string.pause_body_day),
                button = stringResource(R.string.pause_answer),
                onAnswer = { step = Step.Questions },
            )
            Step.Questions -> QuestionsScreen(
                night = night, mood = mood, onList = onList,
                onMood = { mood = it }, onOnList = { onList = it },
                onContinue = { step = Step.Decision },
            )
            Step.Decision -> DecisionScreen(
                mood = mood ?: Mood.Fine,
                onList = onList ?: false,
                shopName = shopName,
                waitHours = store.defaultWaitHours,
                closeRewardLeft = remember { store.closeRewardLeft() },
                onJustLook = { onStartSession(record(EntryAction.JustLook), SessionMode.JustLook) },
                onSpecific = { onStartSession(record(EntryAction.Specific), SessionMode.Specific) },
                onClose = { onClose(record(EntryAction.Closed)) },
            )
        }
    }
}


@Composable
private fun PauseCountdown(
    label: ShopLabel?,
    shopName: String,
    openedAt: Long,
    body: String,
    button: String,
    onAnswer: () -> Unit,
) {
    val context = LocalContext.current
    val time = remember(openedAt) { DateFormat.getTimeFormat(context).format(Date(openedAt)) }

    // 0 → 1 over 10 seconds; drives both the ring and the number.
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = PAUSE_SECONDS * 1000, easing = LinearEasing))
    }
    val secondsLeft = ceil(PAUSE_SECONDS * (1f - progress.value)).toInt()
    val done = progress.value >= 1f

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = CachewSize.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        ShopPill(label = label, text = stringResource(R.string.pause_pill, shopName, time))
        Spacer(Modifier.weight(0.6f))
        PauseRing(progress = progress.value)
        Spacer(Modifier.height(28.dp))
        Text(
            text = secondsLeft.toString(),
            style = CachewType.BigNumber.copy(fontSize = 72.sp, fontWeight = FontWeight.Black),
            color = CachewColors.Title.copy(alpha = 0.92f),
        )
        Text(stringResource(R.string.pause_title), style = CachewType.H1.copy(fontSize = 32.sp), color = CachewColors.Title)
        Spacer(Modifier.height(12.dp))
        Text(
            body,
            style = CachewType.Body,
            color = CachewColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(text = button, enabled = done, onClick = onAnswer)
        Spacer(Modifier.height(16.dp))
    }
}

/** "S  Shein · opened at 23:42" */
@Composable
private fun ShopPill(label: ShopLabel?, text: String) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .clip(shape)
            .background(CachewColors.CardFill)
            .border(CachewSize.BorderWidth, CachewColors.CardBorder, shape)
            .padding(start = 10.dp, end = 16.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (label != null) {
            TileIconBox(label.icon, size = 24.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = CachewColors.Text)
    }
}

/** Glowing ring that fills clockwise from the top, with Cachew peeking inside. */
@Composable
private fun PauseRing(progress: Float) {
    Box(Modifier.size(272.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            val radius = size.minDimension / 2f - 14.dp.toPx()
            // Soft purple halo behind the ring.
            drawCircle(
                Brush.radialGradient(
                    listOf(CachewColors.Purple.copy(alpha = 0.28f), Color.Transparent),
                    center = center,
                    radius = size.minDimension / 2f,
                ),
            )
            // Inner disc.
            drawCircle(CachewColors.Purple.copy(alpha = 0.16f), radius = radius)
            // Track, then the filled part (purple at the top fading to lavender at the bottom).
            drawCircle(CachewColors.Purple.copy(alpha = 0.18f), radius = radius, style = Stroke(stroke))
            drawArc(
                brush = Brush.verticalGradient(
                    listOf(CachewColors.Purple, CachewColors.PurpleSoft),
                    startY = center.y - radius,
                    endY = center.y + radius,
                ),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Penny(PennyPose.Peek, 180.dp)
    }
}

