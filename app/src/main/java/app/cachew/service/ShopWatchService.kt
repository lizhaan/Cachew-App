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

    // The same shop often fires several window events in a row (splash → main screen).
    private var lastPackage: String? = null
    private var lastEventAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return // our own screens (pause, den)

        val now = System.currentTimeMillis()
        val isRepeat = pkg == lastPackage && now - lastEventAt < DEBOUNCE_MS
        lastPackage = pkg
        lastEventAt = now
        if (isRepeat) return

        val store = Store.get(this)
        if (pkg !in store.selectedPackages.value) return
        if (store.isQuiet(pkg, now)) return
        if (PauseActivity.showingFor == pkg) return // pause for this shop is already on screen

        // Accessibility services may start activities from the background.
        startActivity(PauseActivity.intent(this, pkg, openedAt = now))
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
        private const val DEBOUNCE_MS = 2_000L

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
