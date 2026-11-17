package app.cachew.pause

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cachew.data.Store
import app.cachew.ui.theme.CachewTheme

/**
 * The pause that opens on top of a shop (screen 13, then the temporary "after" screen).
 *
 * Manifest setup and why:
 *  - taskAffinity="app.cachew.pause": lives in its own task, so it doesn't drag the den along
 *    and finishing it returns to whatever was underneath (the shop).
 *  - launchMode="singleTask": a second shop event reuses this screen instead of stacking copies.
 *  - excludeFromRecents: the pause never shows up as a separate card in Recents.
 */
class PauseActivity : ComponentActivity() {

    // Compose state, so a new intent (onNewIntent) redraws the screen.
    private var shopPackage by mutableStateOf("")
    private var openedAt by mutableLongStateOf(0L)
    private var demo by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readIntent(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        // Back must not drop the user into the shop: send them to the phone's home screen.
        onBackPressedDispatcher.addCallback(this) { goHome() }

        setContent {
            CachewTheme {
                // key(): a new shop event restarts the countdown from 10.
                key(shopPackage, openedAt) {
                    PauseFlow(
                        shopPackage = shopPackage,
                        openedAt = openedAt,
                        onBackToDen = ::backToDen,
                        onContinueToShop = ::continueToShop,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        showingFor = shopPackage
    }

    override fun onPause() {
        super.onPause()
        showingFor = null
    }

    private fun readIntent(intent: Intent) {
        shopPackage = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        openedAt = intent.getLongExtra(EXTRA_OPENED_AT, System.currentTimeMillis())
        demo = intent.getBooleanExtra(EXTRA_DEMO, false)
        showingFor = shopPackage
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    private fun backToDen() {
        packageManager.getLaunchIntentForPackage(packageName)?.let(::startActivity)
        finish()
    }

    private fun continueToShop() {
        if (demo) {
            finish() // demo pause: just go back to Cachew
            return
        }
        Store.get(this).startQuiet(shopPackage)
        val launch = packageManager.getLaunchIntentForPackage(shopPackage)
        finish()
        // Brings the shop's existing task to the front (or starts it).
        launch?.let(::startActivity)
    }

    companion object {
        private const val EXTRA_PACKAGE = "package"
        private const val EXTRA_OPENED_AT = "opened_at"
        private const val EXTRA_DEMO = "demo"

        /** Package whose pause is on screen right now; the service won't start a second one. */
        @Volatile var showingFor: String? = null
            private set

        fun intent(context: Context, shopPackage: String, openedAt: Long, demo: Boolean = false): Intent =
            Intent(context, PauseActivity::class.java)
                .putExtra(EXTRA_PACKAGE, shopPackage)
                .putExtra(EXTRA_OPENED_AT, openedAt)
                .putExtra(EXTRA_DEMO, demo)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
