package app.cachew.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.cachew.ui.screens.stillwant.BoughtWithHeartFlow
import app.cachew.ui.screens.stillwant.StillWantFlow
import app.cachew.ui.screens.nightshield.NightShieldScreen
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.cachew.data.Store
import app.cachew.ui.screens.apppicker.AppPickerScreen
import app.cachew.ui.screens.den.DenScreen
import app.cachew.ui.screens.permission.PermissionScreen
import app.cachew.ui.screens.pickshops.PickShopsScreen
import kotlinx.serialization.Serializable

// Type-safe routes: navigation-compose turns these @Serializable classes into route strings.
// [onboarding] = part of the first-run flow (08 → 09 → den); false = opened later from the den / Me.

/** Screen 08. */
@Serializable data class PickShopsRoute(val onboarding: Boolean)

@Serializable object AppPickerRoute

/** Screen 09. */
@Serializable data class PermissionRoute(val onboarding: Boolean)

/** Screen 10. In onboarding it comes after 09 (order 08 → 09 → 10 → den). */
@Serializable data class NightShieldRoute(val onboarding: Boolean)

@Serializable object DenRoute

/** Screen 19 "Still want it?" for one waiting-list item (then 20 or 22). */
@Serializable data class StillWantRoute(val itemId: String)

/** Screen 22 "−1 heart" after "Buy it" on an item whose wait isn't over yet. */
@Serializable data class HeartBuyRoute(val itemId: String)

@Composable
fun CachewNavHost(openItemId: String? = null, onItemOpened: () -> Unit = {}) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val nav = rememberNavController()
    val start: Any = if (store.onboardingDone) DenRoute else PickShopsRoute(onboarding = true)

    // Opened from a reminder notification: go straight to "Still want it?".
    LaunchedEffect(openItemId) {
        if (openItemId != null && store.onboardingDone) {
            nav.navigate(StillWantRoute(openItemId)) { launchSingleTop = true }
        }
        if (openItemId != null) onItemOpened()
    }

    NavHost(navController = nav, startDestination = start) {
        composable<PickShopsRoute> { entry ->
            val route = entry.toRoute<PickShopsRoute>()
            PickShopsScreen(
                onboarding = route.onboarding,
                onOtherApp = { nav.navigate(AppPickerRoute) },
                onContinue = {
                    if (route.onboarding) nav.navigate(PermissionRoute(onboarding = true)) else nav.popBackStack()
                },
            )
        }
        composable<AppPickerRoute> {
            AppPickerScreen(onDone = { nav.popBackStack() })
        }
        composable<PermissionRoute> { entry ->
            val route = entry.toRoute<PermissionRoute>()
            // Continue (service on) and Not now (service off) lead to the same place.
            val leave = {
                if (route.onboarding) nav.navigate(NightShieldRoute(onboarding = true)) else nav.popBackStack()
            }
            PermissionScreen(onboarding = route.onboarding, onContinue = { leave() }, onSkip = { leave() })
        }
        composable<NightShieldRoute> { entry ->
            val route = entry.toRoute<NightShieldRoute>()
            NightShieldScreen(
                onboarding = route.onboarding,
                onSaved = {
                    if (route.onboarding) {
                        store.onboardingDone = true
                        // Drop onboarding from the back stack so Back from the den leaves the app.
                        nav.navigate(DenRoute) { popUpTo(0) { inclusive = true } }
                    } else {
                        nav.popBackStack()
                    }
                },
            )
        }
        composable<DenRoute> {
            DenScreen(
                onEditShops = { nav.navigate(PickShopsRoute(onboarding = false)) },
                onTurnOn = { nav.navigate(PermissionRoute(onboarding = false)) },
                onOpenItem = { nav.navigate(StillWantRoute(it)) },
                onBoughtWithHeart = { nav.navigate(HeartBuyRoute(it)) },
                onNightShield = { nav.navigate(NightShieldRoute(onboarding = false)) },
            )
        }
        composable<StillWantRoute> { entry ->
            StillWantFlow(
                itemId = entry.toRoute<StillWantRoute>().itemId,
                onDone = { if (!nav.popBackStack()) nav.navigate(DenRoute) },
            )
        }
        composable<HeartBuyRoute> { entry ->
            BoughtWithHeartFlow(
                itemId = entry.toRoute<HeartBuyRoute>().itemId,
                onDone = { if (!nav.popBackStack()) nav.navigate(DenRoute) },
            )
        }
    }
}
