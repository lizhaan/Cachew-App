package app.cachew.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import app.cachew.data.Store
import app.cachew.pause.PauseActivity
import app.cachew.session.SessionController
import app.cachew.share.ShareActivity

/**
 * Notices when a picked shop comes to the front and opens the pause on top of it.
 *
 * Privacy: the config (res/xml/shop_watch_service.xml) subscribes only to
 * typeWindowStateChanged and sets canRetrieveWindowContent=false, so all we ever get is the
 * package name of the app whose window just opened. We never see the screen's content.
 *
 * TODO(Google Play): Play reviews Accessibility apps strictly; for a Play release we will
 *  probably have to switch to another mechanism (e.g. UsageStats). Not needed for the APK demo.
 */
class ShopWatchService : AccessibilityService() {

    /**
     * The last app the user was really "in". Overlays that pop up on top of an app (share sheet,
     * notification shade, keyboard, our own Share screen) don't count, so coming back from them
     * to the same shop is not "opening" it again. A shop gets a pause only when the user moves
     * into it from a different app.
     */
    private var lastRealPackage: String? = null

    // Safety net: one shop can fire several events before the pause is on screen.
    private var lastPausePackage: String? = null
    private var lastPauseAt = 0L

    /** The 10-minute shop session and its overlays (timer pill, Penny's circle, check-in sheet). */
    private val sessions by lazy { SessionController(this) }

    override fun onServiceConnected() {
        super.onServiceConnected()
        sessions.resume()
    }

    override fun onDestroy() {
        sessions.shutdown()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val className = event.className?.toString()
        if (pkg == packageName && className == ShareActivity::class.java.name) {
            sessions.onShareScreen()
            return
        }
        if (isOverlay(pkg, className)) return

        val previous = lastRealPackage
        lastRealPackage = pkg
        sessions.onForeground(pkg) // shows / hides the session overlays
        if (pkg == previous) return // still in the same app (new screen, dialog, back from share sheet)

        val store = Store.get(this)
        if (pkg !in store.selectedPackages.value) return
        val now = System.currentTimeMillis()
        // Session running for this shop (came back within its 10 minutes): no new pause.
        if (store.activeSession(pkg, now) != null) return
        if (PauseActivity.showingFor == pkg) return // pause for this shop is already on screen
        if (pkg == lastPausePackage && now - lastPauseAt < DEBOUNCE_MS) return
        lastPausePackage = pkg
        lastPauseAt = now

        // Accessibility services may start activities from the background.
        startActivity(PauseActivity.intent(this, pkg, openedAt = now))
    }

    private fun isOverlay(pkg: String, className: String?): Boolean {
        if (pkg in OVERLAY_PACKAGES) return true
        // Our own windows that aren't screens (the session overlays) don't change the app in front.
        if (pkg == packageName && (className == null || !className.endsWith("Activity"))) return true
        if (className != null && ("Chooser" in className || "ResolverActivity" in className)) return true
        // The keyboard currently in use.
        val ime = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return ime != null && ime.substringBefore('/') == pkg
    }

    override fun onInterrupt() = Unit

    /** Possible states of the service, as the user needs to understand them. */
    enum class State {
        /** Switch is off in Settings → Accessibility. */
        Off,
        /** Switch is on, but Android isn't running the service (e.g. after the app was updated). */
        Stuck,
        /** Running: shops will be paused. */
        On,
    }

    companion object {
        private const val DEBOUNCE_MS = 3_000L

        /** System windows that sit on top of the current app rather than replacing it. */
        private val OVERLAY_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.android.intentresolver",
            "com.google.android.intentresolver",
        )

        fun state(context: Context): State {
            val me = ComponentName(context, ShopWatchService::class.java)
            val switchedOn = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )?.split(':')?.any { ComponentName.unflattenFromString(it) == me } == true
            if (!switchedOn) return State.Off

            // AccessibilityManager lists only services Android has actually bound. When our process
            // is killed while the service runs (an app update does that), Android marks it "crashed"
            // and won't restart it until the switch is turned off and on again.
            val am = context.getSystemService(AccessibilityManager::class.java)
            val running = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { it.resolveInfo.serviceInfo.packageName == context.packageName }
            return if (running) State.On else State.Stuck
        }

        /** Is Cachew switched on and actually running? */
        fun isEnabled(context: Context): Boolean = state(context) == State.On
    }
}
