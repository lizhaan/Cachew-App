package app.cachew.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import app.cachew.data.RewardInfo
import app.cachew.ui.components.ChoiceLabel
import app.cachew.ui.components.ChoiceTile
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.cachew.R
import app.cachew.data.Money
import app.cachew.data.ShopLabel
import app.cachew.data.ShopLabels
import app.cachew.data.Store
import app.cachew.data.WaitItem
import app.cachew.reminders.Reminders
import app.cachew.ui.components.EditableItemCard
import app.cachew.ui.components.GlassCard
import app.cachew.ui.components.ItemThumb
import app.cachew.ui.components.PrimaryButton
import app.cachew.ui.components.TileIcon
import app.cachew.ui.components.WaitForPicker
import app.cachew.ui.components.rememberNotificationAsker
import app.cachew.ui.components.waitButtonText
import app.cachew.ui.components.waitMillisFor
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType
import java.io.File

/**
 * Screen 16 "Into the waiting list".
 * Starts with what the shared text says (name, a price written in it), then quietly
 * improves it from the product page. The user can edit name and price at any moment;
 * once they've typed in a field, the page no longer overwrites it.
 */
@Composable
fun ShareScreen(
    itemId: String,
    sharedText: String,
    url: String?,
    fromPackage: String?,
    webShop: ShopLabel?,
    thumbFile: File,
    onSaved: (RewardInfo) -> Unit,
) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val askNotifications = rememberNotificationAsker()

    // Shop: the sharing app if we know it, else the website's name.
    val shop by produceState(webShop, fromPackage) {
        if (fromPackage != null) value = ShopLabels.load(context, fromPackage)
    }
    val shopName = shop?.name ?: stringResource(R.string.share_unknown_shop)

    var name by rememberSaveable { mutableStateOf(ProductParser.titleFromShareText(sharedText).orEmpty()) }
    var priceText by rememberSaveable { mutableStateOf(ProductParser.priceFromText(sharedText)?.let(::plain).orEmpty()) }
    var nameEdited by rememberSaveable { mutableStateOf(false) }
    var priceEdited by rememberSaveable { mutableStateOf(false) }
    var loading by rememberSaveable { mutableStateOf(url != null) }
    var thumbPath by rememberSaveable { mutableStateOf<String?>(thumbFile.takeIf { it.exists() }?.path) }
    var hours by rememberSaveable { mutableIntStateOf(store.defaultWaitHours) }
    /** True once we've tried the page: then, if there's still no price, the quick chips appear. */
    var priceSearchDone by rememberSaveable { mutableStateOf(false) }
    var pickedChip by rememberSaveable { mutableIntStateOf(-1) }

    // Read the product page once. Failure is silent: whatever we had stays on screen.
    LaunchedEffect(url) {
        if (url == null || !loading) return@LaunchedEffect
        var info = ProductFetcher.fetch(url)
        // Nothing useful (bot check)? Try once more with a real browser engine.
        if (info?.title == null) {
            WebPageLoader.load(context, url)?.let { page ->
                val fromBrowser = ProductParser.parse(org.jsoup.Jsoup.parse(page.html, page.url))
                info = fromBrowser.copy(imageUrl = info?.imageUrl ?: fromBrowser.imageUrl)
            }
        }
        val got = info
        if (got != null) {
            val info = got
            if (!nameEdited && info.title != null) name = info.title
            // Prices are kept in dollars. A price in another currency (Temu Ukraine shows ₴144.62)
            // is converted at a fixed rate ($3.48); an unknown currency is left for the user to type.
            val price = info.price?.let { p ->
                if (info.currency == null || info.currency == store.currency) p else Money.toUsd(p, info.currency)
            }
            if (!priceEdited && price != null) priceText = plain(price)
        }
        loading = false
        priceSearchDone = true
        val image = info?.imageUrl
        if (image != null && ProductFetcher.downloadImage(image, thumbFile)) thumbPath = thumbFile.path
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = CachewSize.ScreenPadding),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.share_label_from, shopName).uppercase(), style = CachewType.Label, color = CachewColors.PurpleText)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.share_title), style = CachewType.H1, color = CachewColors.Title)
            Spacer(Modifier.height(20.dp))

            EditableItemCard(
                thumb = {
                    ItemThumb(
                        thumbPath = thumbPath,
                        fallback = shop?.icon ?: TileIcon.Letter(shopName.take(1), CachewColors.Purple),
                        loading = loading,
                    )
                },
                name = name,
                onName = { name = it; nameEdited = true },
                nameHint = stringResource(R.string.item_name_hint),
                priceText = priceText,
                onPrice = { priceText = it; priceEdited = true; pickedChip = -1 },
                priceHint = stringResource(R.string.item_price_hint),
                currency = store.currency,
                loading = loading,
            )

            // No price found: quick ranges instead of typing (the middle of the range is saved).
            val showChips = (priceSearchDone || url == null) && (priceText.isBlank() || pickedChip >= 0)
            if (showChips) {
                Spacer(Modifier.height(CachewSize.Gap))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRICE_CHIPS.forEachIndexed { i, (label, value) ->
                        ChoiceTile(
                            selected = pickedChip == i,
                            onClick = {
                                pickedChip = i
                                priceText = plain(value)
                                priceEdited = true
                            },
                            height = 44.dp,
                            radius = 14.dp,
                            modifier = Modifier.weight(1f),
                        ) { ChoiceLabel(label) }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            WaitForPicker(hours = hours, onHours = { hours = it })
            Spacer(Modifier.height(CachewSize.Gap))
            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.item_tip), style = CachewType.Body, color = CachewColors.TextSecondary)
            }
            Spacer(Modifier.height(16.dp))
        }

        val defaultName = stringResource(R.string.item_default_name, shopName)
        PrimaryButton(
            text = waitButtonText(hours),
            onClick = {
                val item = WaitItem(
                    id = itemId,
                    title = name.trim().ifEmpty { defaultName },
                    price = Money.parse(priceText),
                    currency = store.currency,
                    shopPackage = fromPackage.orEmpty(),
                    shopName = shopName,
                    addedAt = System.currentTimeMillis(),
                    waitMillis = waitMillisFor(hours),
                    thumbPath = thumbPath,
                    url = url,
                )
                val reward = store.addSharedItem(item) // +15 cashews
                Reminders.schedule(context, item)
                askNotifications { onSaved(reward) }
            },
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** Quick price ranges when the page gave no price: label → saved value (middle; "$100+" → 100). */
private val PRICE_CHIPS = listOf("<$20" to 10.0, "$20–50" to 35.0, "$50–100" to 75.0, "$100+" to 100.0)

/** 64.0 → "64", 64.5 → "64.5" (what goes into the editable price field). */
private fun plain(price: Double): String =
    if (price % 1.0 == 0.0) price.toLong().toString() else "%.2f".format(java.util.Locale.US, price)
