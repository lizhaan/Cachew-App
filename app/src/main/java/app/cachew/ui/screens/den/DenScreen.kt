package app.cachew.ui.screens.den

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cachew.R
import app.cachew.data.InstalledApps
import app.cachew.data.ShopApps
import app.cachew.data.Store
import app.cachew.ui.components.CachewBackground
import app.cachew.ui.components.GlassCard
import app.cachew.ui.components.Penny
import app.cachew.ui.components.PennyPose
import app.cachew.ui.components.PrimaryButton
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType

/**
 * TEMPORARY den for stage 1: shows which apps are saved (to check they survive a restart)
 * and lets you reopen the shop picker. Replaced by the real screen 12 in stage 6.
 */
@Composable
fun DenScreen(onEditShops: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val selected by store.selectedPackages.collectAsStateWithLifecycle()

    // Readable names: recommended shops by our name, other apps by their launcher label.
    val names by produceState(emptyList<String>(), selected) {
        value = selected.map { pkg ->
            ShopApps.find(pkg)?.name ?: InstalledApps.load(context, pkg)?.label ?: pkg
        }
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
            Penny(PennyPose.Look, 200.dp, Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(16.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.den_watching_label).uppercase(),
                    style = CachewType.Label,
                    color = CachewColors.PurpleText,
                )
                Spacer(Modifier.height(8.dp))
                Text(names.joinToString(" · "), style = CachewType.Body, color = CachewColors.Text)
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.den_placeholder_note), style = CachewType.Body, color = CachewColors.TextSecondary)
            Spacer(Modifier.height(24.dp))
            PrimaryButton(text = stringResource(R.string.den_edit_shops), onClick = onEditShops)
        }
    }
}
