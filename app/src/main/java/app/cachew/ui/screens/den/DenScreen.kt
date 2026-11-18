package app.cachew.ui.screens.den

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cachew.R
import app.cachew.data.Money
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter
import app.cachew.data.WaitItem
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.KeyboardType
import app.cachew.data.ShopApps
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.mutableIntStateOf
import app.cachew.ui.components.StatCard
import app.cachew.data.ShopLabels
import app.cachew.data.Store
import app.cachew.pause.PauseActivity
import app.cachew.service.ShopWatchService
import app.cachew.ui.components.CachewBackground
import app.cachew.ui.components.GlassCard
import app.cachew.ui.components.Penny
import app.cachew.ui.components.PennyPose
import app.cachew.ui.components.PrimaryButton
import app.cachew.ui.components.XiaomiPopupCard
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType

/**
 * TEMPORARY den (stages 1–5): saved shops, whether the pause service is on, and a demo pause.
 * Replaced by the real screen 12 in stage 6.
 */
private val HHMM = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun DenScreen(onEditShops: () -> Unit, onTurnOn: () -> Unit, onOpenItem: (String) -> Unit, onNightShield: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val selected by store.selectedPackages.collectAsStateWithLifecycle()

    // Re-checked every time the den comes back to the front (e.g. after Settings).
    var serviceState by remember { mutableStateOf(ShopWatchService.state(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { serviceState = ShopWatchService.state(context) }
    val serviceOn = serviceState == ShopWatchService.State.On

    val names by produceState(emptyList<String>(), selected) {
        value = selected.map { ShopLabels.load(context, it).name }
    }

    // Counters: recomputed after every game change and when the den comes back (the date may have changed).
    val gameVersion by store.gameVersion.collectAsStateWithLifecycle()
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    val game = remember(gameVersion, resumes) { store.snapshot() }
    var editing by remember { mutableStateOf<WaitItem?>(null) }
    editing?.let { item ->
        EditItemDialog(
            item = item,
            onDismiss = { editing = null },
            onSave = {
                store.updateItem(it)
                editing = null
            },
        )
    }

    CachewBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CachewSize.ScreenPadding, vertical = 16.dp),
        ) {
            Text(stringResource(R.string.den_greeting), style = CachewType.Body, color = CachewColors.TextSecondary)
            Text(stringResource(R.string.den_title), style = CachewType.H1, color = CachewColors.Title)
            Spacer(Modifier.height(16.dp))
            // After "Buy it anyway" Cachew sits with its back turned until the end of the day.
            Penny(if (game.boughtAnywayToday) PennyPose.Back else PennyPose.Look, 180.dp, Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(CachewSize.Gap)) {
                StatCard(game.streak.toString(), stringResource(R.string.stat_streak), Modifier.weight(1f), R.drawable.ic_flame)
                StatCard(game.nuts.toString(), stringResource(R.string.den_nuts), Modifier.weight(1f), R.drawable.ic_cashew)
                StatCard(game.heartsLeft.toString(), stringResource(R.string.stat_hearts_left), Modifier.weight(1f), R.drawable.ic_heart)
            }
            Spacer(Modifier.height(CachewSize.Gap))

            // Night shield card (as on mockup 12), with the real schedule.
            val shield = remember(gameVersion, resumes) { store.nightShield }
            GlassCard(Modifier.fillMaxWidth(), onClick = onNightShield) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.ic_moon), contentDescription = null, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(stringResource(R.string.shield_title), style = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), color = CachewColors.Title, modifier = Modifier.weight(1f))
                    Text(
                        if (shield.on) "${shield.from.format(HHMM)} – ${shield.to.format(HHMM)}" else stringResource(R.string.den_shield_off),
                        style = CachewType.Body.copy(fontSize = 17.sp),
                        color = CachewColors.TextSecondary,
                    )
                }
            }
            Spacer(Modifier.height(CachewSize.Gap))

            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.den_waiting_label).uppercase(), style = CachewType.Label, color = CachewColors.PurpleText)
                Spacer(Modifier.height(8.dp))
                if (game.waiting.isEmpty()) {
                    Text(stringResource(R.string.den_waiting_empty), style = CachewType.Body, color = CachewColors.TextSecondary)
                }
                // Ready items open "Still want it?"; items still waiting can be renamed / re-priced.
                game.waiting.forEach { item ->
                    val price = item.price?.let { " · " + Money.format(it, item.currency) }.orEmpty()
                    val left = item.dueAt - System.currentTimeMillis()
                    val ready = left <= 0
                    val status = when {
                        ready -> stringResource(R.string.den_item_ready)
                        left < 3_600_000L -> stringResource(R.string.den_item_minutes_left, (left / 60_000L).toInt() + 1)
                        else -> stringResource(R.string.den_item_hours_left, (left / 3_600_000L).toInt())
                    }
                    Text(
                        text = "${item.title}$price · $status",
                        style = CachewType.Body.copy(fontWeight = if (ready) FontWeight.Bold else FontWeight.Normal),
                        color = if (ready) CachewColors.PurpleText else CachewColors.Text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { if (ready) onOpenItem(item.id) else editing = item }
                            .padding(vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(CachewSize.Gap))

            ServiceCard(state = serviceState, onTurnOn = onTurnOn)
            Spacer(Modifier.height(CachewSize.Gap))
            if (serviceOn) {
                XiaomiPopupCard()
                Spacer(Modifier.height(CachewSize.Gap))
            }

            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.den_watching_label).uppercase(), style = CachewType.Label, color = CachewColors.PurpleText)
                Spacer(Modifier.height(8.dp))
                Text(names.joinToString(" · "), style = CachewType.Body, color = CachewColors.Text)
            }
            Spacer(Modifier.height(24.dp))

            PrimaryButton(
                text = stringResource(R.string.den_try_pause),
                onClick = {
                    // Demo uses the first picked shop (or Amazon), so the pill looks real.
                    val pkg = selected.firstOrNull() ?: ShopApps.recommended.first().packageName
                    context.startActivity(PauseActivity.intent(context, pkg, System.currentTimeMillis(), demo = true))
                },
            )
            Text(
                text = stringResource(R.string.den_edit_shops),
                style = CachewType.Button,
                color = CachewColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CachewSize.ButtonRadius))
                    .clickable(onClick = onEditShops)
                    .padding(vertical = 16.dp),
            )
            Text(stringResource(R.string.den_placeholder_note), style = CachewType.Body, color = CachewColors.TextSecondary.copy(alpha = 0.5f))
        }
    }
}

/** Rename / re-price an item (e.g. "Something from Temu" after Skip). */
@Composable
private fun EditItemDialog(item: WaitItem, onDismiss: () -> Unit, onSave: (WaitItem) -> Unit) {
    var name by remember { mutableStateOf(item.title) }
    var price by remember { mutableStateOf(item.price?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CachewColors.BgMid,
        titleContentColor = CachewColors.Title,
        title = { Text(stringResource(R.string.edit_item_title)) },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.edit_item_name)) }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    price, { price = it },
                    label = { Text(stringResource(R.string.edit_item_price) + " (" + Money.symbol(item.currency) + ")") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(item.copy(title = name.trim().ifEmpty { item.title }, price = Money.parse(price)))
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ServiceCard(state: ShopWatchService.State, onTurnOn: () -> Unit) {
    val serviceOn = state == ShopWatchService.State.On
    GlassCard(Modifier.fillMaxWidth(), selected = !serviceOn, onClick = if (serviceOn) null else onTurnOn) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when (state) {
                    ShopWatchService.State.On -> "✓  " + stringResource(R.string.den_service_on)
                    ShopWatchService.State.Stuck -> stringResource(R.string.den_service_stuck)
                    ShopWatchService.State.Off -> stringResource(R.string.den_service_off)
                },
                style = CachewType.Body.copy(fontWeight = FontWeight.SemiBold),
                color = if (serviceOn) CachewColors.PurpleText else CachewColors.Text,
                modifier = Modifier.weight(1f),
            )
            if (!serviceOn) {
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(if (state == ShopWatchService.State.Stuck) R.string.den_fix else R.string.den_turn_on),
                    style = CachewType.Button,
                    color = CachewColors.PurpleText,
                )
            }
        }
    }
}
