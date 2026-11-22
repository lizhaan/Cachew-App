package app.cachew.ui.screens.pickshops

import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cachew.R
import app.cachew.data.InstalledApp
import app.cachew.data.InstalledApps
import app.cachew.data.ShopApps
import app.cachew.data.Store
import app.cachew.ui.components.CachewBackground
import app.cachew.ui.components.OnboardingProgress
import app.cachew.ui.components.PrimaryButton
import app.cachew.ui.components.ShopTile
import app.cachew.ui.components.TileIcon
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType

/**
 * Screen 08 "Where do impulse buys find you?".
 *
 * Grid order: recommended shops → apps added via "+ Other app" → the "+ Other app" tile.
 * Every tap is saved to [Store] right away, so the choice survives a restart even without "Continue".
 *
 * @param onboarding true on first run (shows the progress bar); false when opened from the den / Me.
 */
@Composable
fun PickShopsScreen(
    onboarding: Boolean,
    onOtherApp: () -> Unit,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val selected by store.selectedPackages.collectAsStateWithLifecycle()

    // Night shield lock (from 2 h before the night until the morning): shops can be added, not
    // removed, so the shield can't be dodged by unticking the shop. Not during onboarding.
    val shield = remember { store.nightShield }
    val shopsLocked = remember { !onboarding && shield.isLocked(LocalTime.now()) }
    val lockedUntil = shield.to.format(DateTimeFormatter.ofPattern("HH:mm"))
    val lockedMessage = stringResource(R.string.shops_locked, lockedUntil)
    fun tap(pkg: String) {
        if (shopsLocked && pkg in selected) {
            Toast.makeText(context, lockedMessage, Toast.LENGTH_SHORT).show()
        } else {
            store.toggle(pkg)
        }
    }

    // Custom apps shown as tiles. An app stays visible after it's unticked until the user
    // leaves the screen, so a mis-tap can be undone. Saved as one string so it survives
    // the trip to the picker and back (rememberSaveable needs Bundle-friendly types).
    var shownCustomRaw by rememberSaveable {
        mutableStateOf(selected.filterNot(ShopApps::isRecommended).joinToString(","))
    }
    val shownCustom = shownCustomRaw.split(",").filter { it.isNotEmpty() }
    LaunchedEffect(selected) {
        val added = selected.filter { !ShopApps.isRecommended(it) && it !in shownCustom }
        if (added.isNotEmpty()) shownCustomRaw = (shownCustom + added).joinToString(",")
    }

    // Which recommended shops are on this phone (checked once per screen visit).
    val installed = remember { ShopApps.recommended.associate { it.packageName to InstalledApps.isInstalled(context, it.packageName) } }

    // Label + real icon for each custom app, loaded off the main thread.
    val customInfo by produceState(emptyMap<String, InstalledApp?>(), shownCustomRaw) {
        value = shownCustom.associateWith { InstalledApps.load(context, it) }
    }

    CachewBackground {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = CachewSize.ScreenPadding,
                end = CachewSize.ScreenPadding,
                bottom = 120.dp, // room for the pinned button
            ),
            horizontalArrangement = Arrangement.spacedBy(CachewSize.Gap),
            verticalArrangement = Arrangement.spacedBy(CachewSize.Gap),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Header(onboarding, if (shopsLocked) lockedMessage else null)
            }
            items(ShopApps.recommended, key = { it.packageName }) { shop ->
                val isInstalled = installed[shop.packageName] == true
                ShopTile(
                    label = shop.name,
                    icon = TileIcon.Letter(shop.letter, shop.color),
                    selected = shop.packageName in selected,
                    dimmed = !isInstalled,
                    caption = if (isInstalled) null else stringResource(R.string.pick_shops_not_installed),
                    onClick = { tap(shop.packageName) },
                )
            }
            items(shownCustom, key = { it }) { pkg ->
                val info = customInfo[pkg]
                ShopTile(
                    label = info?.label ?: pkg.substringAfterLast('.'),
                    icon = info?.let { TileIcon.AppIcon(it.icon) }
                        ?: TileIcon.Letter(pkg.substringAfterLast('.').take(1).uppercase(), CachewColors.Purple),
                    selected = pkg in selected,
                    dimmed = info == null && pkg in customInfo,
                    onClick = { tap(pkg) },
                )
            }
            item(key = "other") {
                ShopTile(
                    label = stringResource(R.string.pick_shops_other_app),
                    icon = TileIcon.Plus,
                    selected = false,
                    onClick = onOtherApp,
                )
            }
        }

        // Pinned button with a fade so tiles scroll softly underneath it.
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, CachewColors.BgBottom)))
                .navigationBarsPadding()
                .padding(horizontal = CachewSize.ScreenPadding)
                .padding(top = 28.dp, bottom = 16.dp),
        ) {
            val count = selected.size
            PrimaryButton(
                text = if (count == 0) stringResource(R.string.pick_shops_continue_empty)
                else pluralStringResource(R.plurals.pick_shops_continue, count, count),
                enabled = count > 0,
                onClick = onContinue,
            )
        }
    }
}

@Composable
private fun Header(onboarding: Boolean, lockedMessage: String?) {
    Column(Modifier.statusBarsPadding().padding(top = 16.dp, bottom = 12.dp)) {
        if (onboarding) {
            // Step 3 of 4 as in the mockup: welcome → Penny → shops → permission.
            OnboardingProgress(done = 3, total = 4)
            Spacer(Modifier.height(28.dp))
        } else {
            Spacer(Modifier.height(12.dp))
        }
        Text(stringResource(R.string.pick_shops_title), style = CachewType.H1, color = CachewColors.Title)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.pick_shops_subtitle), style = CachewType.Body, color = CachewColors.TextSecondary)
        if (lockedMessage != null) {
            Spacer(Modifier.height(10.dp))
            Text("🔒 $lockedMessage", style = CachewType.Body.copy(fontWeight = FontWeight.SemiBold), color = CachewColors.PurpleText)
        }
        Box(Modifier.height(8.dp))
    }
}
