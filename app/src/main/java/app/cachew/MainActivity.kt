package app.cachew

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import app.cachew.reminders.Reminders
import app.cachew.ui.CachewNavHost
import app.cachew.ui.theme.CachewTheme

class MainActivity : ComponentActivity() {

    /** Item to open in "Still want it?" when the app is opened from a reminder notification. */
    private val openItemId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the status/navigation bars; light icons because the app is always dark.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        Reminders.ensureChannel(this)
        Reminders.ensureAll(this)
        if (savedInstanceState == null) readIntent(intent)

        setContent {
            CachewTheme {
                CachewNavHost(
                    openItemId = openItemId.value,
                    onItemOpened = { openItemId.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
    }

    private fun readIntent(intent: Intent) {
        intent.getStringExtra(EXTRA_ITEM_ID)?.let { openItemId.value = it }
    }

    companion object {
        const val EXTRA_ITEM_ID = "item_id"
    }
}
