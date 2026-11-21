package app.cachew.ui.screens.stillwant

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.cachew.R
import app.cachew.data.Game
import app.cachew.data.Money
import app.cachew.data.ShopLabel
import app.cachew.data.ShopLabels
import app.cachew.data.Store
import app.cachew.data.WaitItem
import app.cachew.data.WinInfo
import app.cachew.pause.BoughtScreen
import app.cachew.pause.HeartUsedScreen
import app.cachew.pause.startBuyPause
import app.cachew.ui.components.SecondaryButton
import app.cachew.reminders.Reminders
import app.cachew.ui.components.CachewBackground
import app.cachew.ui.components.ChoiceLabel
import app.cachew.ui.components.ChoiceTile
import app.cachew.ui.components.GlassCard
import app.cachew.ui.components.ItemCard
import app.cachew.ui.components.PrimaryButton
import app.cachew.ui.components.TextAction
import app.cachew.ui.components.TileIcon
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private sealed interface StillStep {
    data object Ask : StillStep
    data class Win(val info: WinInfo) : StillStep
    data object AlreadyBought : StillStep
}

/**
 * Screen 19 "Still want it?" (after the wait) and what follows:
 *  Let it go → "Kept, not spent"; Buy it → short pause → the item's link;
 *  I already bought it → screen 21 (streak back to 0).
 */
@Composable
fun StillWantFlow(itemId: String, onDone: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val item = remember(itemId) { store.item(itemId) }
    var step by remember { mutableStateOf<StillStep>(StillStep.Ask) }

    if (item == null || (item.status != WaitItem.Status.Waiting && step == StillStep.Ask)) {
        LaunchedEffect(Unit) { onDone() } // already decided (e.g. an old notification)
        return
    }
    val shop by produceState(shopLabelOf(item), item.shopPackage) {
        if (item.shopPackage.isNotEmpty()) value = ShopLabels.load(context, item.shopPackage)
    }

    when (val s = step) {
        StillStep.Ask -> CachewBackground {
            AskScreen(
                item = item,
                shop = shop,
                keptThisMonth = remember { store.keptIn(YearMonth.now()) },
                onLetGo = {
                    Reminders.cancel(context, item.id)
                    store.letGo(item.id)?.let { step = StillStep.Win(it) }
                },
                onBuy = {
                    Reminders.cancel(context, item.id)
                    // Asked after the wait, so it's free; tapped from the list earlier, it costs a heart.
                    if (store.buy(item.id)) startBuyPause(context, item)
                    onDone()
                },
                onAlreadyBought = {
                    Reminders.cancel(context, item.id)
                    store.alreadyBought(item.id)
                    step = StillStep.AlreadyBought
                },
            )
        }
        is StillStep.Win -> CachewBackground {
            WinScreen(s.info, onDone = onDone)
        }
        StillStep.AlreadyBought -> CachewBackground {
            BoughtScreen(game = remember { store.snapshot() }, onDone = onDone)
        }
    }
}

/**
 * "Buy it" on an item whose wait isn't over: costs a heart → screen 22 "−1 heart" →
 * "Continue to <shop>" → short pause → the item's link. The heart is spent by the caller.
 */
@Composable
fun BoughtWithHeartFlow(itemId: String, onDone: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val item = remember(itemId) { store.item(itemId) } ?: run {
        LaunchedEffect(Unit) { onDone() }
        return
    }
    val shop by produceState(shopLabelOf(item), item.shopPackage) {
        if (item.shopPackage.isNotEmpty()) value = ShopLabels.load(context, item.shopPackage)
    }
    CachewBackground {
        HeartUsedScreen(
            game = remember { store.snapshot() },
            shopName = shop.name,
            onBackToDen = onDone,
            onContinueToShop = {
                startBuyPause(context, item)
                onDone()
            },
        )
    }
}

private fun shopLabelOf(item: WaitItem): ShopLabel {
    val name = item.shopName ?: ShopLabels.quickName(item.shopPackage).ifEmpty { "Shop" }
    return ShopLabel(name, TileIcon.Letter(name.take(1).uppercase(), CachewColors.Purple))
}

@Composable
private fun AskScreen(
    item: WaitItem,
    shop: ShopLabel,
    keptThisMonth: Double,
    onLetGo: () -> Unit,
    onBuy: () -> Unit,
    onAlreadyBought: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val added = Instant.ofEpochMilli(item.addedAt).atZone(zone)
    val waited = if (item.waitMillis < 3_600_000L) "1m" else "${item.waitMillis / 3_600_000L}h"
    val time = added.format(DateTimeFormatter.ofPattern("HH:mm", Locale.US))

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = CachewSize.ScreenPadding),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.still_label, waited).uppercase(), style = CachewType.Label, color = CachewColors.PurpleText)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.still_title), style = CachewType.H1, color = CachewColors.Title)
            Spacer(Modifier.height(20.dp))
            ItemCard(
                item = item,
                fallback = shop.icon,
                subtitle = stringResource(R.string.still_added, added.format(DateTimeFormatter.ofPattern("EEE, HH:mm", Locale.US))),
            )
            Spacer(Modifier.height(CachewSize.Gap))
            // A nudge from real data (instead of the mockup's "You bought 2 sweaters in October").
            val insight = when {
                Game.isNight(added.toLocalTime()) -> stringResource(R.string.still_insight_night, time)
                keptThisMonth > 0 -> stringResource(R.string.still_insight_kept, Money.format(keptThisMonth, item.currency))
                else -> stringResource(R.string.still_insight_first)
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Text(insight, style = CachewType.Body, color = CachewColors.TextSecondary)
            }
            Spacer(Modifier.height(16.dp))
        }

        PrimaryButton(
            text = item.price?.let { stringResource(R.string.still_let_go_keep, Money.format(it, item.currency)) }
                ?: stringResource(R.string.still_let_go),
            onClick = onLetGo,
        )
        Spacer(Modifier.height(CachewSize.Gap))
        SecondaryButton(stringResource(R.string.still_buy_waited), onBuy)
        TextAction(stringResource(R.string.still_already_bought), onAlreadyBought)
    }
}
