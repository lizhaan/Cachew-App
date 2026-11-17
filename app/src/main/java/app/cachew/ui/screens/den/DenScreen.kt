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
import app.cachew.data.ShopApps
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
@Composable
fun DenScreen(onEditShops: () -> Unit, onTurnOn: () -> Unit) {
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
            Penny(PennyPose.Look, 180.dp, Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(16.dp))

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
