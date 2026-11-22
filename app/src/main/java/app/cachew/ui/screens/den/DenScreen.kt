package app.cachew.ui.screens.den

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cachew.R
import app.cachew.data.Game
import app.cachew.data.Money
import app.cachew.data.Store
import app.cachew.service.ShopWatchService
import app.cachew.ui.components.GlassCard
import app.cachew.ui.components.HeartsRow
import app.cachew.ui.components.Penny
import app.cachew.ui.components.PennyPose
import app.cachew.ui.components.PrimaryButton
import app.cachew.data.PhoneSetup
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType
import app.cachew.ui.theme.Inter
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Den, the home tab (design-v2/01): money saved this month with hearts, the savings goal and
 * Penny's level; Penny's winter stash (cashews) with her next reward; the night shield.
 */
@Composable
fun DenScreen(onTurnOn: () -> Unit, onSetup: () -> Unit, onNightShield: () -> Unit, onGoal: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }

    // Recomputed after every game change and whenever the den comes back (date, service state).
    val gameVersion by store.gameVersion.collectAsStateWithLifecycle()
    var resumes by remember { mutableIntStateOf(0) }
    var serviceState by remember { mutableStateOf(ShopWatchService.state(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        resumes++
        serviceState = ShopWatchService.state(context)
    }
    val game = remember(gameVersion, resumes) { store.snapshot() }
    val savedThisMonth = remember(gameVersion, resumes) { store.keptIn(YearMonth.now()) }
    val goal = remember(gameVersion, resumes) { store.goal }
    val goalProgress = remember(gameVersion, resumes) { store.goalProgress() }
    val shield = remember(gameVersion, resumes) { store.nightShield }
    val now = LocalTime.now()
    val goalReached = goal != null && goalProgress >= goal.amount

    // Penny: asleep at night, back turned after "I already bought it" today, celebrating a reached goal.
    val pose = when {
        shield.isNight(now) -> PennyPose.Sleep
        game.boughtAnywayToday -> PennyPose.Back
        goalReached -> PennyPose.Love
        else -> PennyPose.Look
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = CachewSize.ScreenPadding, vertical = 16.dp),
    ) {
        // Greeting + streak and cashew chips.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(greeting(now)), style = CachewType.Body, color = CachewColors.TextSecondary)
                Text(stringResource(R.string.den_title), style = CachewType.H1.copy(fontSize = 26.sp), color = CachewColors.Title, maxLines = 1)
            }
            CounterChip(game.streak.toString(), R.drawable.ic_flame)
            Spacer(Modifier.width(8.dp))
            CounterChip(game.cashews.toString(), R.drawable.ic_cashew)
        }
        Spacer(Modifier.height(16.dp))

        if (serviceState != ShopWatchService.State.On) {
            ServiceCard(state = serviceState, onTurnOn = onTurnOn)
            Spacer(Modifier.height(CachewSize.Gap))
        } else {
            // Service is on: remind about the other setup steps until they're all done.
            val left = remember(resumes, gameVersion) { PhoneSetup.stepsLeft(context, store.setupConfirmed) }
            if (left > 0) {
                GlassCard(Modifier.fillMaxWidth(), selected = true, onClick = onSetup) {
                    Text(stringResource(R.string.den_setup_title), style = CardTitle, color = CachewColors.Title)
                    Spacer(Modifier.height(4.dp))
                    Text(pluralStringResource(R.plurals.den_setup_body, left, left), style = CachewType.Body, color = CachewColors.TextSecondary)
                }
                Spacer(Modifier.height(CachewSize.Gap))
            }
        }

        // MONEY SAVED THIS MONTH
        val month = LocalDate.now().month.getDisplayName(TextStyle.FULL, Locale.US)
        val level = Game.levelFor(game.cashews)
        val next = Game.nextLevel(level)
        val levelNames = stringArrayResource(R.array.level_names)
        GlassCard(Modifier.fillMaxWidth()) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.den_saved_label).uppercase(), style = CachewType.Label, color = CachewColors.PurpleText)
                    Spacer(Modifier.height(4.dp))
                    Text(Money.format(savedThisMonth, Money.DEFAULT_CURRENCY), style = CachewType.BigNumber.copy(fontSize = 48.sp), color = CachewColors.Title)
                    Spacer(Modifier.height(8.dp))
                    HeartsRow(left = game.heartsLeft, total = Game.HEARTS_PER_MONTH, size = 20.dp, gap = 8.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        pluralStringResource(R.plurals.hearts_left_in_month, game.heartsLeft, game.heartsLeft, month),
                        style = CachewType.Body.copy(fontSize = 14.sp),
                        color = CachewColors.TextSecondary,
                    )
                    if (goal != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(
                                R.string.den_goal_line, goal.name,
                                Money.format(goalProgress.coerceAtMost(goal.amount), Money.DEFAULT_CURRENCY),
                                Money.format(goal.amount, Money.DEFAULT_CURRENCY),
                            ),
                            style = CachewType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            color = CachewColors.Gold,
                        )
                    }
                }
                Penny(pose, 104.dp, Modifier.align(Alignment.CenterVertically))
            }
            Spacer(Modifier.height(12.dp))
            Row {
                Text(stringResource(R.string.den_level, level.number), style = CachewType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = CachewColors.Text, modifier = Modifier.weight(1f))
                Text(levelNames[level.number - 1], style = CachewType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = CachewColors.PurpleText)
            }
            Spacer(Modifier.height(6.dp))
            ProgressBar(levelFraction(game.cashews, level, next), CachewColors.Purple)
        }
        Spacer(Modifier.height(CachewSize.Gap))

        // Goal: none yet → ask; reached → celebrate and offer a new one.
        if (goal == null) {
            GlassCard(Modifier.fillMaxWidth(), selected = true, onClick = onGoal) {
                Text(stringResource(R.string.den_goal_ask), style = CardTitle, color = CachewColors.Title)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.den_goal_ask_body), style = CachewType.Body, color = CachewColors.TextSecondary)
            }
            Spacer(Modifier.height(CachewSize.Gap))
        } else if (goalReached) {
            GlassCard(Modifier.fillMaxWidth(), selected = true) {
                Text(stringResource(R.string.den_goal_done, goal.name), style = CardTitle, color = CachewColors.Title)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.den_goal_done_body), style = CachewType.Body, color = CachewColors.TextSecondary)
                Spacer(Modifier.height(12.dp))
                PrimaryButton(stringResource(R.string.den_goal_new), onClick = {
                    store.completeGoal()
                    onGoal()
                })
            }
            Spacer(Modifier.height(CachewSize.Gap))
        }

        // Penny's winter stash: cashews and her next reward.
        val rewards = stringArrayResource(R.array.level_rewards)
        val rewardIcons = stringArrayResource(R.array.level_reward_icons)
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.den_stash_title), style = CardTitle, color = CachewColors.Title, modifier = Modifier.weight(1f))
                Image(painterResource(R.drawable.ic_cashew), contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Text(game.cashews.toString(), style = CardTitle, color = CachewColors.Title)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(rewardIcons[(next ?: level).number - 1], fontSize = 30.sp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (next != null) stringResource(R.string.den_next_reward, rewards[next.number - 1])
                        else stringResource(R.string.den_max_level),
                        style = CachewType.Body.copy(fontWeight = FontWeight.Bold),
                        color = CachewColors.Title,
                    )
                    Spacer(Modifier.height(6.dp))
                    ProgressBar(levelFraction(game.cashews, level, next), CachewColors.Gold)
                    if (next != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.den_next_at, next.from, next.number),
                            style = CachewType.Body.copy(fontSize = 13.sp),
                            color = CachewColors.TextSecondary,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(CachewSize.Gap))

        // Night shield with the real schedule.
        GlassCard(Modifier.fillMaxWidth(), onClick = onNightShield) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_moon), contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(14.dp))
                Text(stringResource(R.string.shield_title), style = CardTitle, color = CachewColors.Title, modifier = Modifier.weight(1f))
                Text(
                    if (shield.on) "${shield.from.format(HHMM)} – ${shield.to.format(HHMM)}" else stringResource(R.string.den_shield_off),
                    style = CachewType.Body.copy(fontSize = 16.sp),
                    color = CachewColors.TextSecondary,
                )
            }
        }
    }
}

private val CardTitle = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)

private fun greeting(now: LocalTime): Int = when (now.hour) {
    in 5..11 -> R.string.den_greeting_morning
    in 12..17 -> R.string.den_greeting_afternoon
    else -> R.string.den_greeting_evening
}

/** How far the stash is from this level's start to the next level (full at the top level). */
private fun levelFraction(cashews: Int, level: Game.Level, next: Game.Level?): Float =
    if (next == null) 1f else ((cashews - level.from).toFloat() / (next.from - level.from)).coerceIn(0f, 1f)

/** "🔥 1" / "🥜 20" chip in the top right corner. */
@Composable
private fun CounterChip(value: String, icon: Int) {
    GlassCard(radius = 14.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(5.dp))
            Text(value, fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = CachewColors.Title)
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float, color: Color) {
    val shape = RoundedCornerShape(50)
    Box(Modifier.fillMaxWidth().height(8.dp).clip(shape).background(Color.White.copy(alpha = 0.1f))) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(shape).background(color))
    }
}

/** Shown only when pauses can't work: the accessibility switch is off or Android stopped the service. */
@Composable
private fun ServiceCard(state: ShopWatchService.State, onTurnOn: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), selected = true, onClick = onTurnOn) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (state == ShopWatchService.State.Stuck) R.string.den_service_stuck else R.string.den_service_off),
                style = CachewType.Body.copy(fontWeight = FontWeight.SemiBold),
                color = CachewColors.Text,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(if (state == ShopWatchService.State.Stuck) R.string.den_fix else R.string.den_turn_on),
                style = CachewType.Button,
                color = CachewColors.PurpleText,
            )
        }
    }
}
