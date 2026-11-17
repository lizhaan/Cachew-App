package app.cachew.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * The app's single data repository: SharedPreferences, with lists stored as JSON strings.
 * One instance per process (Store.get). Values the UI watches are exposed as StateFlows, so
 * every screen sees a change immediately (e.g. an app added in the picker shows up in the grid).
 *
 * Later stages add nuts, streak, hearts, pause log, waiting list, night shield here.
 */
class Store private constructor(private val prefs: SharedPreferences) {

    private val _selectedPackages = MutableStateFlow(readPackages())

    /** Package names that get a pause: recommended shops and any app picked via "+ Other app". */
    val selectedPackages: StateFlow<Set<String>> = _selectedPackages.asStateFlow()

    fun setSelected(packageName: String, selected: Boolean) {
        val next = _selectedPackages.value.let { if (selected) it + packageName else it - packageName }
        _selectedPackages.value = next
        // List keeps insertion order, so custom tiles appear in the order they were added.
        prefs.edit { putString(KEY_SELECTED, Json.encodeToString(next.toList())) }
    }

    fun toggle(packageName: String) = setSelected(packageName, packageName !in _selectedPackages.value)

    /** True once the user has gone past the shop picker the first time. */
    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit { putBoolean(KEY_ONBOARDING_DONE, value) }

    /** User said they turned on Xiaomi's "pop-up windows in background" (we can't check it ourselves). */
    var xiaomiPopupsDone: Boolean
        get() = prefs.getBoolean(KEY_XIAOMI_POPUPS, false)
        set(value) = prefs.edit { putBoolean(KEY_XIAOMI_POPUPS, value) }

    // --- Quiet windows -------------------------------------------------------------
    // After "Continue to <shop>" we leave that shop alone for a while, otherwise the
    // pause would pop up again the moment the shop comes back to the front.

    /** True if [packageName] is inside its quiet window right now. */
    fun isQuiet(packageName: String, now: Long = System.currentTimeMillis()): Boolean =
        (readQuiet()[packageName] ?: 0L) > now

    /** Don't pause [packageName] for the next [QUIET_MINUTES] minutes. */
    fun startQuiet(packageName: String, now: Long = System.currentTimeMillis()) {
        // Drop expired entries so the map doesn't grow forever.
        val map = readQuiet().filterValues { it > now }.toMutableMap()
        map[packageName] = now + QUIET_MINUTES * 60_000L
        prefs.edit { putString(KEY_QUIET, Json.encodeToString(map)) }
    }

    private fun readQuiet(): Map<String, Long> {
        val raw = prefs.getString(KEY_QUIET, null) ?: return emptyMap()
        return runCatching { Json.decodeFromString<Map<String, Long>>(raw) }.getOrDefault(emptyMap())
    }

    private fun readPackages(): Set<String> {
        val raw = prefs.getString(KEY_SELECTED, null) ?: return emptySet()
        return runCatching { Json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList()).toCollection(LinkedHashSet())
    }

    companion object {
        private const val PREFS = "cachew"
        private const val KEY_SELECTED = "selected_packages"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_QUIET = "quiet_until"
        // v2: the card gained the Autostart step, so show it again to people who dismissed v1.
        private const val KEY_XIAOMI_POPUPS = "xiaomi_steps_done_v2"

        const val QUIET_MINUTES = 15

        @Volatile private var instance: Store? = null

        fun get(context: Context): Store = instance ?: synchronized(this) {
            instance ?: Store(context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE))
                .also { instance = it }
        }
    }
}
