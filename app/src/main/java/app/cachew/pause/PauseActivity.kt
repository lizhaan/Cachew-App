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
import android.widget.Toast
import app.cachew.R
import app.cachew.data.EntryAction
import app.cachew.data.PauseRecord
import app.cachew.data.SessionMode
import app.cachew.data.Store
import app.cachew.ui.theme.CachewTheme

/**
 * The pause that opens on top of a shop: countdown, two questions, choice screen.
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
        // It still counts as a pause in the log (for Stats), with no answers.
        onBackPressedDispatcher.addCallback(this) {
            if (!demo) Store.get(this@PauseActivity).logPause(PauseRecord(openedAt, shopPackage, null, null, EntryAction.Left))
            goHome()
        }

        setContent {
            CachewTheme {
                // key(): a new shop event restarts the countdown from 10.
                key(shopPackage, openedAt) {
                    PauseFlow(
                        shopPackage = shopPackage,
                        openedAt = openedAt,
                        onStartSession = ::startSession,
                        onClose = ::closeShop,
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

    /** Leave the shop for the phone's home screen. */
    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    /**
     * "Just look" / "I need something specific": save the session, then bring the shop back.
     * From here ShopWatchService takes over: it sees the shop in front, finds the running session
     * (so no new pause) and draws the timer pill and Penny's circle on top.
     */
    private fun startSession(record: PauseRecord, mode: SessionMode) {
        val launch = packageManager.getLaunchIntentForPackage(shopPackage)
        if (demo && launch == null) {
            finish() // demo with a shop that isn't installed: nothing to open, back to Cachew
            return
        }
        Store.get(this).startSession(record, mode)
        finish()
        // Brings the shop's existing task to the front (or starts it).
        launch?.let(::startActivity)
    }

    /** "Close <shop>": +5 cashews (if today's limit allows), then the phone's home screen. */
    private fun closeShop(record: PauseRecord) {
        val store = Store.get(this)
        store.logPause(record)
        val earned = store.rewardClose()
        if (earned > 0) Toast.makeText(this, getString(R.string.close_reward_toast, earned), Toast.LENGTH_SHORT).show()
        if (demo) finish() else goHome()
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
