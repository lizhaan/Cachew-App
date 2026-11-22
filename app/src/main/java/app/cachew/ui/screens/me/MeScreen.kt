package app.cachew.ui.screens.me

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cachew.R
import app.cachew.data.Game
import app.cachew.data.Money
import app.cachew.data.Rules
import app.cachew.data.ShopApps
import app.cachew.data.Store
import app.cachew.pause.PauseActivity
import app.cachew.reminders.Reminders
import app.cachew.service.ShopWatchService
import app.cachew.ui.components.ChoiceLabel
import app.cachew.ui.components.ChoiceTile
import app.cachew.ui.components.GlassCard
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType
import java.io.File
import java.time.format.DateTimeFormatter

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")

/** Me tab (mockup 25): Penny's level and the real settings, as a list. */
@Composable
fun MeScreen(
    onNightShield: () -> Unit,
    onShops: () -> Unit,
    onGoal: () -> Unit,
    onPermission: () -> Unit,
    onShowIntro: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val version by store.gameVersion.collectAsStateWithLifecycle()
    val selected by store.selectedPackages.collectAsStateWithLifecycle()
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    val cashews = remember(version, resumes) { store.cashews }
    val shield = remember(version, resumes) { store.nightShield }
    val goal = remember(version, resumes) { store.goal }
    val goalProgress = remember(version, resumes) { store.goalProgress() }
    val serviceOn = remember(resumes) { ShopWatchService.isEnabled(context) }
    var waitHours by remember { mutableIntStateOf(store.defaultWaitHours) }
    var pickingWait by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = CachewSize.ScreenPadding, vertical = 16.dp),
    ) {
        // Penny's card: avatar, name, level.
        val level = Game.levelFor(cashews)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.app_icon), contentDescription = null, modifier = Modifier.size(76.dp).clip(RoundedCornerShape(20.dp)))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(stringResource(R.string.me_name), style = CachewType.H1, color = CachewColors.Title)
                Text(
                    stringResource(R.string.me_level, level.number, stringArrayResource(R.array.level_names)[level.number - 1], cashews),
                    style = CachewType.Body.copy(fontSize = 16.sp),
                    color = CachewColors.TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        SettingRow(stringResource(R.string.shield_title), if (shield.on) "${shield.from.format(HHMM)} – ${shield.to.format(HHMM)}" else stringResource(R.string.den_shield_off), onNightShield)
        SettingRow(stringResource(R.string.me_shops), pluralStringResource(R.plurals.me_apps, selected.size, selected.size), onShops)
        SettingRow(stringResource(R.string.me_waiting_time), stringResource(R.string.me_hours, waitHours)) { pickingWait = true }
        SettingRow(
            stringResource(R.string.me_goal),
            goal?.let {
                stringResource(R.string.me_goal_value, it.name, Money.format(goalProgress.coerceAtMost(it.amount), Money.DEFAULT_CURRENCY), Money.format(it.amount, Money.DEFAULT_CURRENCY))
            } ?: stringResource(R.string.me_goal_none),
            onGoal,
        )
        SettingRow(stringResource(R.string.me_pauses), stringResource(if (serviceOn) R.string.me_on else R.string.me_off), onPermission)
        SettingRow(stringResource(R.string.den_try_pause), "") {
            // Demo uses the first picked shop (or Amazon), so the pill looks real.
            val pkg = selected.firstOrNull() ?: ShopApps.recommended.first().packageName
            context.startActivity(PauseActivity.intent(context, pkg, System.currentTimeMillis(), demo = true))
        }
        SettingRow(stringResource(R.string.me_show_intro), "", onShowIntro)
        SettingRow(stringResource(R.string.me_reset), "") { confirmReset = true }
    }

    if (pickingWait) {
        AlertDialog(
            onDismissRequest = { pickingWait = false },
            containerColor = CachewColors.BgMid,
            titleContentColor = CachewColors.Title,
            title = { Text(stringResource(R.string.me_waiting_time)) },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Rules.WAIT_HOURS_OPTIONS.forEach { h ->
                        ChoiceTile(
                            selected = h == waitHours,
                            onClick = {
                                store.defaultWaitHours = h
                                waitHours = h
                                pickingWait = false
                            },
                            modifier = Modifier.weight(1f),
                        ) { ChoiceLabel(stringResource(R.string.item_hours_short, h)) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickingWait = false }) { Text(stringResource(R.string.ok)) } },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = CachewColors.BgMid,
            titleContentColor = CachewColors.Title,
            textContentColor = CachewColors.TextSecondary,
            title = { Text(stringResource(R.string.me_reset_title)) },
            text = { Text(stringResource(R.string.me_reset_body)) },
            confirmButton = {
                TextButton(onClick = {
                    store.waitItems().forEach { Reminders.cancel(context, it.id) }
                    File(context.filesDir, "thumbs").deleteRecursively()
                    store.resetDemoData()
                    confirmReset = false
                }) { Text(stringResource(R.string.me_reset_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** "Night shield            23:00 – 07:00 ›" */
@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().padding(bottom = CachewSize.Gap), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            Text(title, style = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), color = CachewColors.Title, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Text(
                "$value ›".trim(),
                style = CachewType.Body.copy(fontSize = 16.sp),
                color = CachewColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}
