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
import app.cachew.data.WaitItem
import java.time.LocalTime
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
    /** "Buy it" from the waiting list: only the 10 s countdown, then the item's link. */
    private var buyUrl by mutableStateOf<String?>(null)
    private var buying by mutableStateOf(false)
    /** Shop name for items shared from a website (no app package). */
    private var shopNameHint by mutableStateOf<String?>(null)
    /** Night shield hours: the shop is blocked until this time (null = not blocked). */
    private var blockedUntil by mutableStateOf<LocalTime?>(null)

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
            if (!demo && !buying && blockedUntil == null) Store.get(this@PauseActivity).logPause(PauseRecord(openedAt, shopPackage, null, null, EntryAction.Left))
            goHome()
        }

        setContent {
            CachewTheme {
                // key(): a new shop event restarts the countdown from 10.
                key(shopPackage, openedAt) {
                    PauseFlow(
                        shopPackage = shopPackage,
                        openedAt = openedAt,
                        buying = buying,
                        shopNameHint = shopNameHint,
                        blockedUntil = blockedUntil,
                        onStartSession = ::startSession,
                        onClose = ::closeShop,
                        onBuy = ::continueToBuy,
                        onBlockClose = ::goHome,
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
        buying = intent.getBooleanExtra(EXTRA_BUY, false)
        buyUrl = intent.getStringExtra(EXTRA_URL)
        shopNameHint = intent.getStringExtra(EXTRA_SHOP_NAME)
        // Decided when the shop was opened: inside the night shield's hours it's a full block.
        val shield = Store.get(this).nightShield
        blockedUntil = shield.to.takeIf { shield.isNight(LocalTime.now()) }
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

    /**
     * After the short "Buy it" pause: open the item's own link (in the shop's app when it can show
     * it, otherwise in the browser) and give that app a 5-minute buying session.
     */
    private fun continueToBuy() {
        val target = BuyTarget.resolve(this, shopPackage, buyUrl)
        if (target == null) {
            finish()
            return
        }
        Store.get(this).startBuySession(target.packageName)
        finish()
        startActivity(target.intent)
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
        private const val EXTRA_BUY = "buy"
        private const val EXTRA_URL = "url"
        private const val EXTRA_SHOP_NAME = "shop_name"

        /** Package whose pause is on screen right now; the service won't start a second one. */
        @Volatile var showingFor: String? = null
            private set

        fun intent(context: Context, shopPackage: String, openedAt: Long, demo: Boolean = false): Intent =
            Intent(context, PauseActivity::class.java)
                .putExtra(EXTRA_PACKAGE, shopPackage)
                .putExtra(EXTRA_OPENED_AT, openedAt)
                .putExtra(EXTRA_DEMO, demo)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        /** The short pause before buying [item] ("Buy it" in the waiting list / "Still want it?"). */
        fun buyIntent(context: Context, item: WaitItem): Intent =
            intent(context, item.shopPackage, openedAt = System.currentTimeMillis())
                .putExtra(EXTRA_BUY, true)
                .putExtra(EXTRA_URL, item.url)
                .putExtra(EXTRA_SHOP_NAME, item.shopName)
    }
}
