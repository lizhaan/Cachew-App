package app.cachew.ui.screens.waiting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cachew.R
import app.cachew.data.Money
import app.cachew.data.ShopApps
import app.cachew.data.ShopLabels
import app.cachew.data.Stats
import app.cachew.data.Store
import app.cachew.data.Total
import app.cachew.data.WaitItem
import app.cachew.reminders.Reminders
import app.cachew.ui.components.GlassCard
import app.cachew.ui.components.ItemThumb
import app.cachew.ui.components.Penny
import app.cachew.ui.components.PennyPose
import app.cachew.ui.components.TileIcon
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType
import app.cachew.ui.theme.Inter
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Waiting list tab (design-v2/08): this month's totals, then WAITING (soonest first) and
 * DECIDED (newest first). Ready items open "Still want it?"; waiting ones can be edited
 * or bought early with a heart.
 */
@Composable
fun WaitingListScreen(onOpenItem: (String) -> Unit, onBoughtWithHeart: (String) -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val version by store.gameVersion.collectAsStateWithLifecycle()
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    val items = remember(version, resumes) { store.waitItems() }
    val game = remember(version, resumes) { store.snapshot() }
    val totals = remember(items) { Stats.listTotals(items, YearMonth.now()) }
    val now = System.currentTimeMillis()

    var editing by remember { mutableStateOf<WaitItem?>(null) }
    editing?.let { item ->
        EditItemDialog(
            item = item,
            heartsLeft = game.heartsLeft,
            heartsRefill = game.heartsRefill.format(DateTimeFormatter.ofPattern("MMM d", Locale.US)),
            onDismiss = { editing = null },
            onSave = {
                store.updateItem(it)
                editing = null
            },
            onBuy = {
                editing = null
                // The wait isn't over: buying now costs a heart (streak kept), then screen 22.
                if (store.buy(item.id)) {
                    Reminders.cancel(context, item.id)
                    onBoughtWithHeart(item.id)
                }
            },
        )
    }

    val waiting = items.filter { it.status == WaitItem.Status.Waiting }.sortedBy { it.dueAt }
    val decided = items.filter { it.status != WaitItem.Status.Waiting }.sortedByDescending { it.decidedAt ?: 0 }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = CachewSize.ScreenPadding, vertical = 16.dp),
    ) {
        Text(stringResource(R.string.waiting_title), style = CachewType.H1, color = CachewColors.Title)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.waiting_subtitle), style = CachewType.Body, color = CachewColors.TextSecondary)
        Spacer(Modifier.height(16.dp))

        // $ waiting · N | $ saved · N (gold) | $ bought · N
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TotalCell(totals.waiting, stringResource(R.string.waiting_total_waiting), CachewColors.Title, Modifier.weight(1f))
                Divider()
                TotalCell(totals.saved, stringResource(R.string.waiting_total_saved), CachewColors.Gold, Modifier.weight(1f))
                Divider()
                TotalCell(totals.bought, stringResource(R.string.waiting_total_bought), CachewColors.Title, Modifier.weight(1f))
            }
        }

        if (items.isEmpty()) {
            Spacer(Modifier.height(32.dp))
            Penny(PennyPose.Run, 160.dp, Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.waiting_empty), style = CachewType.H2, color = CachewColors.Title, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.waiting_empty_hint), style = CachewType.Body, color = CachewColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        if (waiting.isNotEmpty()) {
            SectionLabel(stringResource(R.string.waiting_section_waiting))
            waiting.forEach { item ->
                val left = item.dueAt - now
                val status = when {
                    left <= 0 -> stringResource(R.string.den_item_ready)
                    left < 3_600_000L -> stringResource(R.string.den_item_minutes_left, (left / 60_000L).toInt() + 1)
                    else -> stringResource(R.string.den_item_hours_left, (left / 3_600_000L).toInt())
                }
                ItemRow(
                    item = item,
                    subtitle = stringResource(R.string.waiting_added, shopName(item), dayTime(item.addedAt)),
                    status = status,
                    statusColor = CachewColors.PurpleText,
                    onClick = { if (left <= 0) onOpenItem(item.id) else editing = item },
                )
            }
        }

        if (decided.isNotEmpty()) {
            SectionLabel(stringResource(R.string.waiting_section_decided))
            decided.forEach { item ->
                val price = item.price?.let { Money.format(it, item.currency) }
                val (subtitle, status, color) = when (item.status) {
                    WaitItem.Status.Kept -> Triple(
                        stringResource(R.string.waiting_let_go, shopName(item), day(item.decidedAt ?: 0)),
                        price?.let { stringResource(R.string.waiting_kept, it) } ?: stringResource(R.string.win_let_go),
                        CachewColors.Gold,
                    )
                    WaitItem.Status.BoughtWithHeart -> Triple(
                        stringResource(R.string.waiting_bought_heart, shopName(item)),
                        stringResource(R.string.waiting_heart_used),
                        CachewColors.Heart,
                    )
                    else -> Triple(
                        stringResource(R.string.waiting_bought, shopName(item)),
                        stringResource(R.string.waiting_bought_status),
                        CachewColors.TextSecondary,
                    )
                }
                ItemRow(item = item, subtitle = subtitle, status = status, statusColor = color, onClick = null)
            }
        }
    }
}

@Composable
private fun TotalCell(total: Total, label: String, color: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(Money.format(total.amount, Money.DEFAULT_CURRENCY), fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = color)
        Text("$label · ${total.count}", style = CachewType.Body.copy(fontSize = 13.sp), color = CachewColors.TextSecondary)
    }
}

@Composable
private fun Divider() {
    Box(Modifier.width(1.dp).height(36.dp).background(CachewColors.CardBorder))
}

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(text.uppercase(), style = CachewType.Label, color = CachewColors.PurpleText)
    Spacer(Modifier.height(10.dp))
}

/** Photo (or a letter tile), name, "<Shop> · added Fri 22:10", price and a coloured status. */
@Composable
private fun ItemRow(item: WaitItem, subtitle: String, status: String, statusColor: Color, onClick: (() -> Unit)?) {
    GlassCard(Modifier.fillMaxWidth().padding(bottom = CachewSize.Gap), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val shop = shopName(item)
            ItemThumb(item.thumbPath, TileIcon.Letter(shop.take(1).uppercase(), ShopApps.find(item.shopPackage)?.color ?: CachewColors.Purple), size = 56.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CachewColors.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = CachewType.Body.copy(fontSize = 13.sp), color = CachewColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                item.price?.let { Text(Money.format(it, item.currency), fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = CachewColors.Title) }
                Text(status, fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = statusColor)
            }
        }
    }
}

private fun shopName(item: WaitItem): String =
    ShopApps.find(item.shopPackage)?.name ?: item.shopName ?: ShopLabels.quickName(item.shopPackage)

private fun dayTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE HH:mm", Locale.US))

private fun day(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE", Locale.US))

/** Rename / re-price a waiting item, or "Buy it" early with a heart. */
@Composable
private fun EditItemDialog(
    item: WaitItem,
    heartsLeft: Int,
    heartsRefill: String,
    onDismiss: () -> Unit,
    onSave: (WaitItem) -> Unit,
    onBuy: () -> Unit,
) {
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
                Spacer(Modifier.height(16.dp))
                // "Buy it" before the wait is over: costs one of this month's hearts.
                TextButton(onClick = onBuy, enabled = heartsLeft > 0) {
                    Text(
                        if (heartsLeft > 0) stringResource(R.string.buy_with_heart, heartsLeft)
                        else stringResource(R.string.buy_no_hearts, heartsRefill)
                    )
                }
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
