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
import app.cachew.ui.screens.pickshops.PickShopsScreen
import kotlinx.serialization.Serializable

// Type-safe routes: navigation-compose turns these @Serializable classes into route strings.

/** Screen 08. [onboarding] = first run (progress bar, Continue goes to the den). Later reused from Me. */
@Serializable data class PickShopsRoute(val onboarding: Boolean)

@Serializable object AppPickerRoute

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
                    if (route.onboarding) {
                        store.onboardingDone = true
                        // Replace onboarding with the den so Back doesn't return to it.
                        nav.navigate(DenRoute) { popUpTo<PickShopsRoute> { inclusive = true } }
                    } else {
                        nav.popBackStack()
                    }
                },
            )
        }
        composable<AppPickerRoute> {
            AppPickerScreen(onDone = { nav.popBackStack() })
        }
        composable<DenRoute> {
            DenScreen(onEditShops = { nav.navigate(PickShopsRoute(onboarding = false)) })
        }
    }
}
