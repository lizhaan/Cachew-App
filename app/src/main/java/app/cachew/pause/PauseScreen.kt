package app.cachew.pause

import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cachew.R
import app.cachew.data.Decision
import app.cachew.data.Game
import app.cachew.data.Mood
import app.cachew.data.PauseRecord
import app.cachew.data.RewardInfo
import app.cachew.data.ShopLabel
import app.cachew.data.ShopLabels
import app.cachew.data.Store
import app.cachew.data.WaitItem
import app.cachew.reminders.Reminders
import app.cachew.ui.components.rememberNotificationAsker
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import app.cachew.ui.components.CachewBackground
import app.cachew.ui.components.Penny
import app.cachew.ui.components.PennyPose
import app.cachew.ui.components.PrimaryButton
import app.cachew.ui.components.TileIconBox
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType
import app.cachew.ui.theme.Inter
import java.util.Date
import kotlin.math.ceil

private const val PAUSE_SECONDS = 10

/** The steps of one pause. Saved as an enum so it survives the activity being recreated. */
private enum class Step { Countdown, Questions, Decision, Item, Reward, HeartUsed, Bought }

/**
 * One whole pause, step by step:
 * 13 countdown → 14 questions → 15 decision →
 *   Add to waiting list → "What did you want?" → 17 reward → close the shop (phone home)
 *   Use a heart → 22 → den / continue to shop (quiet window)
 *   Buy it anyway → 21 → back to shop (quiet window) / den
 *
 * Store changes happen only on the final button of a branch, so backing out halfway
 * (Back = phone home screen) changes nothing.
 */
@Composable
fun PauseFlow(
    shopPackage: String,
    openedAt: Long,
    onGoHome: () -> Unit,
    onBackToDen: () -> Unit,
    onContinueToShop: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val label by produceState<ShopLabel?>(null, shopPackage) { value = ShopLabels.load(context, shopPackage) }
    val shopName = label?.name ?: ShopLabels.quickName(shopPackage)

    // Night is decided once, when the shop was opened (default 23:00–07:00 until stage 5).
    val night = remember(openedAt) {
        Game.isNight(Instant.ofEpochMilli(openedAt).atZone(ZoneId.systemDefault()).toLocalTime())
    }

    var step by rememberSaveable { mutableStateOf(Step.Countdown) }
    var mood by rememberSaveable { mutableStateOf<Mood?>(null) }
    var onList by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var reward by remember { mutableStateOf<RewardInfo?>(null) }
    val askNotifications = rememberNotificationAsker()

    fun record(decision: Decision) = PauseRecord(
        at = openedAt, shopPackage = shopPackage, mood = mood ?: Mood.Fine,
        onList = onList ?: false, night = night, decision = decision,
    )

    // 22 "Used a heart" is the one day-background screen in the flow (as in the mockups).
    CachewBackground(night = step != Step.HeartUsed) {
        when (step) {
            Step.Countdown -> PauseCountdown(
                label = label, shopName = shopName, openedAt = openedAt, night = night,
                onAnswer = { step = Step.Questions },
            )
            Step.Questions -> QuestionsScreen(
                night = night, mood = mood, onList = onList,
                onMood = { mood = it }, onOnList = { onList = it },
                onContinue = { step = Step.Decision },
            )
            Step.Decision -> {
                val game = remember { store.snapshot() }
                DecisionScreen(
                    mood = mood ?: Mood.Fine, onList = onList ?: false, night = night,
                    heartsLeft = game.heartsLeft, heartsRefill = game.heartsRefill,
                    showBuyAnyway = true, // stage 5: false at night in Strict mode
                    onWait = { step = Step.Item },
                    onHeart = {
                        store.useHeart(record(Decision.Heart))
                        step = Step.HeartUsed
                    },
                    onBuyAnyway = {
                        store.buyAnyway(record(Decision.Bought))
                        step = Step.Bought
                    },
                )
            }
            Step.Item -> ItemEntryScreen(
                shop = label, shopName = shopName, currency = store.currency, defaultWaitHours = store.defaultWaitHours,
                onSave = { name, price, waitMillis ->
                    val item = WaitItem(
                        id = UUID.randomUUID().toString(),
                        title = name ?: context.getString(R.string.item_default_name, shopName),
                        price = price,
                        currency = store.currency,
                        shopPackage = shopPackage,
                        addedAt = System.currentTimeMillis(),
                        waitMillis = waitMillis,
                    )
                    reward = store.addToWaitingList(record(Decision.Waiting), item)
                    Reminders.schedule(context, item)
                    askNotifications { step = Step.Reward }
                },
            )
            Step.Reward -> {
                val r = reward
                if (r != null) {
                    RewardScreen(r, night = night, onClose = onGoHome)
                } else {
                    // Only if the activity was recreated mid-flow: nothing left to show.
                    LaunchedEffect(Unit) { onGoHome() }
                }
            }
            Step.HeartUsed -> HeartUsedScreen(
                game = remember { store.snapshot() }, shopName = shopName,
                onBackToDen = onBackToDen, onContinueToShop = onContinueToShop,
            )
            Step.Bought -> BoughtScreen(
                game = remember { store.snapshot() },
                onBackToShop = onContinueToShop, onBackToDen = onBackToDen,
            )
        }
    }
}

@Composable
private fun PauseCountdown(label: ShopLabel?, shopName: String, openedAt: Long, night: Boolean, onAnswer: () -> Unit) {
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
            stringResource(if (night) R.string.pause_body_night else R.string.pause_body_day),
            style = CachewType.Body,
            color = CachewColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(text = stringResource(R.string.pause_answer), enabled = done, onClick = onAnswer)
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

