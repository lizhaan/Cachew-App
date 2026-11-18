package app.cachew.pause

import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.cachew.R
import app.cachew.data.Money
import app.cachew.data.ShopLabel
import app.cachew.ui.components.EditableItemCard
import app.cachew.ui.components.GlassCard
import app.cachew.ui.components.PrimaryButton
import app.cachew.ui.components.TextAction
import app.cachew.ui.components.TileIcon
import app.cachew.ui.components.TileIconBox
import app.cachew.ui.components.WaitForPicker
import app.cachew.ui.components.waitButtonText
import app.cachew.ui.components.waitMillisFor
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType

/**
 * "What did you want?" after "Add to waiting list" (styled like the card in mockup 16).
 * Name gets focus and the keyboard right away; price is optional. Skip saves
 * "Something from <shop>" with no price (editable later in the waiting list).
 *
 * [onSave] gets: name (null = use the default), price (null = none), wait in milliseconds.
 */
@Composable
fun ItemEntryScreen(
    shop: ShopLabel?,
    shopName: String,
    currency: String,
    defaultWaitHours: Int,
    onSave: (name: String?, price: Double?, waitMillis: Long) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var priceText by rememberSaveable { mutableStateOf("") }
    var hours by rememberSaveable { mutableIntStateOf(defaultWaitHours) }
    val nameFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { nameFocus.requestFocus() }

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
            Text(stringResource(R.string.item_label, shopName).uppercase(), style = CachewType.Label, color = CachewColors.PurpleText)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.item_title), style = CachewType.H1, color = CachewColors.Title)
            Spacer(Modifier.height(20.dp))

            EditableItemCard(
                thumb = { TileIconBox(shop?.icon ?: TileIcon.Letter(shopName.take(1), CachewColors.Purple), size = 72.dp) },
                name = name,
                onName = { name = it },
                nameHint = stringResource(R.string.item_name_hint),
                priceText = priceText,
                onPrice = { priceText = it },
                priceHint = stringResource(R.string.item_price_hint),
                currency = currency,
                nameModifier = Modifier.focusRequester(nameFocus),
            )

            Spacer(Modifier.height(20.dp))
            WaitForPicker(hours = hours, onHours = { hours = it })
            Spacer(Modifier.height(CachewSize.Gap))
            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.item_tip), style = CachewType.Body, color = CachewColors.TextSecondary)
            }
            Spacer(Modifier.height(16.dp))
        }

        PrimaryButton(
            text = waitButtonText(hours),
            onClick = { onSave(name.trim().ifEmpty { null }, Money.parse(priceText), waitMillisFor(hours)) },
        )
        TextAction(stringResource(R.string.item_skip), onClick = { onSave(null, null, waitMillisFor(hours)) })
    }
}
