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

    private fun readPackages(): Set<String> {
        val raw = prefs.getString(KEY_SELECTED, null) ?: return emptySet()
        return runCatching { Json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList()).toCollection(LinkedHashSet())
    }

    companion object {
        private const val PREFS = "cachew"
        private const val KEY_SELECTED = "selected_packages"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"

        @Volatile private var instance: Store? = null

        fun get(context: Context): Store = instance ?: synchronized(this) {
            instance ?: Store(context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE))
                .also { instance = it }
        }
    }
}
