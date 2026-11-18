package app.cachew.pause

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cachew.BuildConfig
import app.cachew.R
import app.cachew.data.Money
import app.cachew.data.ShopLabel
import app.cachew.ui.components.ChoiceLabel
import app.cachew.ui.components.ChoiceTile
import app.cachew.ui.components.GlassCard
import app.cachew.ui.components.PrimaryButton
import app.cachew.ui.components.TextAction
import app.cachew.ui.components.TileIconBox
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType
import app.cachew.ui.theme.Inter

/** A wait choice: 24/48/72 hours, or 1 minute (debug builds only, long-press 24h). */
private const val TEST_MINUTE = -1

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

    val waitMillis = if (hours == TEST_MINUTE) 60_000L else hours * 3_600_000L

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

            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (shop != null) {
                        TileIconBox(shop.icon, size = 72.dp)
                        Spacer(Modifier.width(14.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Field(
                            value = name,
                            onValueChange = { name = it },
                            hint = stringResource(R.string.item_name_hint),
                            style = NameStyle,
                            keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                            modifier = Modifier.focusRequester(nameFocus),
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(Money.symbol(currency), style = PriceStyle, color = CachewColors.Title)
                            Spacer(Modifier.width(2.dp))
                            Field(
                                value = priceText,
                                onValueChange = { new -> priceText = new.filter { it.isDigit() || it == '.' || it == ',' || it == ' ' } },
                                hint = stringResource(R.string.item_price_hint),
                                style = PriceStyle,
                                keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.item_wait_for), style = CachewType.Body.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold), color = CachewColors.TextSecondary)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(CachewSize.Gap)) {
                listOf(24, 48, 72).forEach { h ->
                    ChoiceTile(
                        selected = hours == h || (h == 24 && hours == TEST_MINUTE),
                        onClick = { hours = h },
                        // Debug builds: long-press 24h = 1 minute, to test reminders quickly.
                        onLongClick = if (BuildConfig.DEBUG && h == 24) ({ hours = TEST_MINUTE }) else null,
                        modifier = Modifier.weight(1f),
                    ) {
                        ChoiceLabel(if (h == 24 && hours == TEST_MINUTE) "1m" else stringResource(R.string.item_hours_short, h))
                    }
                }
            }
            Spacer(Modifier.height(CachewSize.Gap))
            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.item_tip), style = CachewType.Body, color = CachewColors.TextSecondary)
            }
            Spacer(Modifier.height(16.dp))
        }

        Box(Modifier.fillMaxWidth()) {
            PrimaryButton(
                text = if (hours == TEST_MINUTE) stringResource(R.string.item_wait_button_minute)
                else stringResource(R.string.item_wait_button, hours),
                onClick = { onSave(name.trim().ifEmpty { null }, Money.parse(priceText), waitMillis) },
            )
        }
        TextAction(stringResource(R.string.item_skip), onClick = { onSave(null, null, waitMillis) })
    }
}

private val NameStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = CachewColors.Title)
private val PriceStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = CachewColors.Title)

/** Borderless text field with a faded hint, so it reads like the card text in mockup 16. */
@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    style: TextStyle,
    keyboard: KeyboardOptions,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = style,
        keyboardOptions = keyboard,
        keyboardActions = KeyboardActions.Default,
        cursorBrush = SolidColor(CachewColors.PurpleLight),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(hint, style = style.copy(color = CachewColors.TextSecondary.copy(alpha = 0.45f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold))
                inner()
            }
        },
    )
}
