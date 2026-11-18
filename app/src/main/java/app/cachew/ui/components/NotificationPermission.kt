package app.cachew.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import app.cachew.data.Store

/**
 * Asks for the notification permission once, the first time an item is added (Android 13+).
 * Returns a function: call it with what to do next; that runs right away if there's nothing
 * to ask, or after the user answers. A "no" breaks nothing: the den still shows "ready".
 */
@Composable
fun rememberNotificationAsker(): (onDone: () -> Unit) -> Unit {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val pending = remember { arrayOfNulls<() -> Unit>(1) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pending[0]?.invoke()
        pending[0] = null
    }
    return { onDone ->
        val needed = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !store.notificationsAsked
        if (needed) {
            store.notificationsAsked = true
            pending[0] = onDone
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onDone()
        }
    }
}
