package app.cachew.ui

import androidx.compose.runtime.Composable
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

@Serializable object DenRoute

@Composable
fun CachewNavHost() {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val nav = rememberNavController()
    val start: Any = if (store.onboardingDone) DenRoute else PickShopsRoute(onboarding = true)

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
            PermissionScreen(
                onboarding = route.onboarding,
                onContinue = {
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
            )
        }
    }
}
