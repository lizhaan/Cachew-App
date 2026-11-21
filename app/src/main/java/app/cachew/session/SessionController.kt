package app.cachew.session

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import app.cachew.R
import app.cachew.data.Rules
import app.cachew.data.SessionMode
import app.cachew.data.ShopApps
import app.cachew.data.ShopLabels
import app.cachew.data.ShopSession
import app.cachew.data.Store

/**
 * Runs the 10-minute shop session (design-v2/03, 04). Lives inside ShopWatchService.
 *
 *  - Every second while a session exists: update the pill's countdown, check the 5-minute
 *    check-in ("just looking") or the "2 minutes left" peek ("specific"), and end it at 10 min.
 *  - Overlays are shown only while the session's shop is in front. Leaving and coming back
 *    keeps the same timer (no new pause).
 *  - Time's up while the shop is open → the phone's home screen. The next visit gets a new pause.
 *
 * All session facts are read from Store each time, so a restarted service just carries on.
 */
class SessionController(private val service: AccessibilityService) {

    private val store = Store.get(service)
    private val overlays = SessionOverlays(service)
    private val handler = Handler(Looper.getMainLooper())
    private val tick = Runnable { update(entered = false) }
    private val clearBubble = Runnable { overlays.state.bubble = null }

    /** The app the user is really in right now (from ShopWatchService). */
    private var foreground: String? = null

    /** A new app came to the front. */
    fun onForeground(pkg: String) {
        val entered = pkg != foreground
        val s = store.session
        // A "Buy it" visit ends as soon as the user leaves the shop: next time it's a normal pause.
        if (s != null && s.mode == SessionMode.Buy && foreground == s.shopPackage && pkg != s.shopPackage) {
            foreground = pkg
            finish(s)
            return
        }
        foreground = pkg
        update(entered)
    }

    /** Cachew's own Share screen is on top of the shop: step out of its way until the shop is back. */
    fun onShareScreen() {
        foreground = null
        overlays.hideAll()
    }

    /** Service (re)started: pick up a session that may still be running. */
    fun resume() = update(entered = false)

    fun shutdown() {
        handler.removeCallbacksAndMessages(null)
        overlays.hideAll()
    }

    private fun update(entered: Boolean) {
        handler.removeCallbacks(tick)
        val s = store.session ?: return overlays.hideAll()
        val now = System.currentTimeMillis()
        if (now >= s.endsAt) return finish(s)
        handler.postDelayed(tick, 1_000L - now % 1_000L) // next whole second

        if (foreground != s.shopPackage) return overlays.hideAll()

        overlays.state.pill = pillText(s, now)
        if (s.mode == SessionMode.Buy) {
            overlays.showSession(withPenny = false, onPennyTap = {}) // just the timer: she knows you waited
            return
        }
        overlays.showSession(withPenny = true, onPennyTap = { say(service.getString(R.string.session_share_hint)) })
        if (entered) say(service.getString(R.string.session_penny_hint))

        val elapsed = now - s.startedAt
        when (s.mode) {
            SessionMode.JustLook -> if (!s.checkInShown && elapsed >= Rules.CHECK_IN_MINUTE * 60_000L) {
                store.saveSession(s.copy(checkInShown = true))
                showCheckIn(s)
            }
            SessionMode.Specific -> if (!s.warned && s.endsAt - now <= Rules.SPECIFIC_WARNING_MINUTES_LEFT * 60_000L) {
                store.saveSession(s.copy(warned = true))
                say(service.getString(R.string.session_two_minutes))
                overlays.state.bounce++
            }
            SessionMode.Buy -> Unit
        }
    }

    private fun showCheckIn(s: ShopSession) {
        val shop = shopName(s.shopPackage)
        val texts = CheckInTexts(
            title = service.getString(R.string.checkin_title, Rules.CHECK_IN_MINUTE),
            body = service.getString(R.string.checkin_body),
            saveTitle = service.getString(R.string.checkin_save_title),
            saveBody = service.getString(R.string.checkin_save_body, store.defaultWaitHours, Rules.CASHEWS_PER_SAVED_ITEM),
            more = service.getString(R.string.checkin_more, Rules.SESSION_MINUTES - Rules.CHECK_IN_MINUTE),
            done = service.getString(R.string.checkin_done, shop),
        )
        overlays.showSheet(
            texts,
            onSave = {
                overlays.hideSheet()
                say(service.getString(R.string.session_share_hint))
            },
            // The session keeps its original end time; "5 more min" is simply what is left.
            onMore = { overlays.hideSheet() },
            onDone = {
                val earned = store.rewardClose()
                if (earned > 0) Toast.makeText(service, service.getString(R.string.close_reward_toast, earned), Toast.LENGTH_SHORT).show()
                store.session?.let(::finish)
            },
        )
    }

    /** Session over (time up or "I'm done"): log its length, clear the overlays, leave the shop if it's open. */
    private fun finish(s: ShopSession) {
        handler.removeCallbacks(tick)
        store.endSession()
        overlays.hideAll()
        if (foreground == s.shopPackage) {
            if (s.mode == SessionMode.Buy) {
                Toast.makeText(service, R.string.buy_session_over, Toast.LENGTH_LONG).show()
            }
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        }
    }

    /** Shows Penny's speech bubble for a few seconds. */
    private fun say(text: String) {
        overlays.state.bubble = text
        handler.removeCallbacks(clearBubble)
        handler.postDelayed(clearBubble, BUBBLE_MS)
    }

    private fun pillText(s: ShopSession, now: Long): String {
        val left = ((s.endsAt - now + 999) / 1000).coerceAtLeast(0)
        val time = "%d:%02d".format(left / 60, left % 60)
        val mode = service.getString(
            when (s.mode) {
                SessionMode.JustLook -> R.string.session_mode_look
                SessionMode.Specific -> R.string.session_mode_specific
                SessionMode.Buy -> R.string.session_mode_buy
            }
        )
        return service.getString(R.string.session_pill, time, mode)
    }

    /** Recommended shops use our name; any other app its launcher label. */
    private fun shopName(pkg: String): String = ShopApps.find(pkg)?.name ?: runCatching {
        val pm = service.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrElse { ShopLabels.quickName(pkg) }

    private companion object {
        const val BUBBLE_MS = 6_000L
    }
}
