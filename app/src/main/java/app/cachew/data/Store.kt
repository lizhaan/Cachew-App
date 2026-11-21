package app.cachew.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/**
 * The app's single data repository: SharedPreferences, with lists stored as JSON strings.
 * One instance per process (Store.get). Values the UI watches are exposed as StateFlows, so
 * every screen sees a change immediately (e.g. an app added in the picker shows up in the grid).
 *
 * Later stages add the night shield here.
 */
class Store private constructor(private val prefs: SharedPreferences) {

    init {
        migrate()
    }

    /**
     * One-off data upgrades, run once per step on the phones that still have older data.
     * [KEY_DATA_VERSION] remembers how far a phone got; new steps go at the end.
     */
    private fun migrate() {
        var version = prefs.getInt(KEY_DATA_VERSION, if (prefs.getBoolean(KEY_USD_MIGRATED, false)) 1 else 0)
        if (version < 1) {
            // Early test builds saved items in the phone's currency (UAH); everything is dollars now.
            val items = readList<WaitItem>(KEY_ITEMS).map { it.copy(currency = Money.DEFAULT_CURRENCY) }
            prefs.edit {
                putString(KEY_ITEMS, Json.encodeToString(items))
                remove(KEY_CURRENCY)
            }
            version = 1
        }
        // Stage 3 (v2) adds step 2 here: the pause log gets a new format.
        prefs.edit {
            putInt(KEY_DATA_VERSION, version)
            remove(KEY_USD_MIGRATED)
        }
    }

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

    // --- Game: cashews, streak, hearts, pause log, waiting list -------------------------
    // Rules live in Game.kt; Store only keeps the raw facts (dates, counts, lists).

    /** Bumped after every game change, so screens that show counters can refresh. */
    private val _gameVersion = MutableStateFlow(0)
    val gameVersion: StateFlow<Int> = _gameVersion.asStateFlow()

    /** Night shield (screen 10). Times are stored as minutes since midnight. */
    var nightShield: NightShield
        get() = NightShield(
            on = prefs.getBoolean(KEY_SHIELD_ON, true),
            from = LocalTime.ofSecondOfDay(prefs.getInt(KEY_SHIELD_FROM, Rules.DEFAULT_NIGHT_FROM.toMinutes()) * 60L),
            to = LocalTime.ofSecondOfDay(prefs.getInt(KEY_SHIELD_TO, Rules.DEFAULT_NIGHT_TO.toMinutes()) * 60L),
            strict = prefs.getBoolean(KEY_SHIELD_STRICT, true),
        )
        set(value) {
            prefs.edit {
                putBoolean(KEY_SHIELD_ON, value.on)
                putInt(KEY_SHIELD_FROM, value.from.toMinutes())
                putInt(KEY_SHIELD_TO, value.to.toMinutes())
                putBoolean(KEY_SHIELD_STRICT, value.strict)
            }
            changed()
        }

    /** Default "Wait for" choice (Me → Waiting time in stage 6). */
    var defaultWaitHours: Int
        get() = prefs.getInt(KEY_WAIT_HOURS, Rules.DEFAULT_WAIT_HOURS)
        set(value) = prefs.edit { putInt(KEY_WAIT_HOURS, value) }

    /** Currency for new items (a Me setting later); US dollars by default. */
    var currency: String
        get() = prefs.getString(KEY_CURRENCY, null) ?: Money.DEFAULT_CURRENCY
        set(value) = prefs.edit { putString(KEY_CURRENCY, value) }

    fun snapshot(today: LocalDate = LocalDate.now()): GameSnapshot = GameSnapshot(
        cashews = cashews,
        streak = Game.streak(today, firstDay, lastBuyAnywayDay),
        heartsLeft = heartsLeft(today),
        heartsRefill = Game.heartsRefillDate(today),
        boughtAnywayToday = lastBuyAnywayDay == today,
        waiting = waitItems().filter { it.status == WaitItem.Status.Waiting },
    )

    fun waitItems(): List<WaitItem> = readList(KEY_ITEMS)

    fun pauseLog(): List<PauseRecord> = readList(KEY_LOG)

    /**
     * "Add to waiting list" + "Wait N hours" / "Skip": +15 cashews, ticks today's quest,
     * logs the pause and saves the item. No quiet window here (the pause should come back).
     * (v1 flow; stage 3 replaces it.)
     */
    fun addToWaitingList(record: PauseRecord, item: WaitItem, today: LocalDate = LocalDate.now()): RewardInfo {
        val questDone = prefs.getLong(KEY_QUEST_DAY, NO_DAY) != today.toEpochDay()
        prefs.edit {
            putInt(KEY_CASHEWS, cashews + Game.CASHEWS_PER_PAUSE)
            putLong(KEY_QUEST_DAY, today.toEpochDay())
            putString(KEY_ITEMS, Json.encodeToString(waitItems() + item))
            putString(KEY_LOG, Json.encodeToString(pauseLog() + record))
        }
        changed()
        return RewardInfo(Game.CASHEWS_PER_PAUSE, Game.streak(today, firstDay, lastBuyAnywayDay), questDone, item)
    }

    /** "Use a heart": one heart less this month, streak untouched. */
    fun useHeart(record: PauseRecord, today: LocalDate = LocalDate.now()) {
        spendHeart(today)
        prefs.edit { putString(KEY_LOG, Json.encodeToString(pauseLog() + record)) }
        changed()
    }

    private fun spendHeart(today: LocalDate) {
        val month = YearMonth.from(today).toString()
        val usedThisMonth = if (prefs.getString(KEY_HEARTS_MONTH, null) == month) prefs.getInt(KEY_HEARTS_USED, 0) else 0
        prefs.edit {
            putString(KEY_HEARTS_MONTH, month)
            putInt(KEY_HEARTS_USED, usedThisMonth + 1)
        }
    }

    // --- Waiting list items ---------------------------------------------------------

    fun item(id: String): WaitItem? = waitItems().firstOrNull { it.id == id }

    /** Item added through Share (screen 16): no nuts, no pause log entry. */
    fun addSharedItem(item: WaitItem) {
        prefs.edit { putString(KEY_ITEMS, Json.encodeToString(waitItems() + item)) }
        changed()
    }

    fun updateItem(item: WaitItem) {
        prefs.edit { putString(KEY_ITEMS, Json.encodeToString(waitItems().map { if (it.id == item.id) item else it })) }
        changed()
    }

    /** Penny's winter stash. Only ever grows; decides her level (Game.levelFor). */
    val cashews: Int get() = prefs.getInt(KEY_CASHEWS, 0)

    fun keptIn(month: YearMonth): Double = keptItems()
        .filter { it.decidedAt != null && YearMonth.from(epochToDate(it.decidedAt)) == month }
        .sumOf { it.price ?: 0.0 }

    private fun keptItems() = waitItems().filter { it.status == WaitItem.Status.Kept }

    /**
     * 19 "Let it go": the item is kept (not bought), its price counts as money saved,
     * and Penny gets one cashew per dollar.
     */
    fun letGo(id: String, now: Long = System.currentTimeMillis()): WinInfo? {
        val item = item(id) ?: return null
        val before = cashews
        val earned = Game.cashewsForLetGo(item.price)
        prefs.edit { putInt(KEY_CASHEWS, before + earned) }
        updateItem(item.copy(status = WaitItem.Status.Kept, decidedAt = now))
        val today = epochToDate(now)
        val level = Game.levelFor(before + earned)
        return WinInfo(
            amount = item.price,
            currency = item.currency,
            cashewsEarned = earned,
            stash = before + earned,
            keptThisMonth = keptIn(YearMonth.from(today)),
            level = level,
            levelledUp = level.number > Game.levelFor(before).number,
            streak = Game.streak(today, firstDay, lastBuyAnywayDay),
            heartsLeft = heartsLeft(today),
        )
    }

    /** 19 "Buy it with a heart": a planned treat, costs one heart, streak untouched. */
    fun buyWithHeart(id: String, now: Long = System.currentTimeMillis()) {
        val item = item(id) ?: return
        spendHeart(epochToDate(now))
        updateItem(item.copy(status = WaitItem.Status.BoughtWithHeart, decidedAt = now))
    }

    /** 19 "Wait another day": +24 hours on the clock. Returns the updated item. */
    fun waitAnotherDay(id: String): WaitItem? {
        val item = item(id) ?: return null
        val updated = item.copy(waitMillis = item.waitMillis + 24 * 3_600_000L, remindedAt = null)
        updateItem(updated)
        return updated
    }

    fun markReminded(id: String, now: Long = System.currentTimeMillis()) {
        item(id)?.let { updateItem(it.copy(remindedAt = now)) }
    }

    /** We ask for the notification permission once, when the first item is added. */
    var notificationsAsked: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_ASKED, false)
        set(value) = prefs.edit { putBoolean(KEY_NOTIF_ASKED, value) }

    private fun epochToDate(millis: Long): LocalDate =
        java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()

    /** "Buy it anyway": the streak starts again from today. */
    fun buyAnyway(record: PauseRecord, today: LocalDate = LocalDate.now()) {
        prefs.edit {
            putLong(KEY_LAST_BUY_ANYWAY, today.toEpochDay())
            putString(KEY_LOG, Json.encodeToString(pauseLog() + record))
        }
        changed()
    }

    fun heartsLeft(today: LocalDate = LocalDate.now()): Int {
        val month = prefs.getString(KEY_HEARTS_MONTH, null)?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
        return Game.heartsLeft(YearMonth.from(today), month, prefs.getInt(KEY_HEARTS_USED, 0))
    }

    /** First day of using Cachew; the streak counts from here until the first "Buy it anyway". */
    private val firstDay: LocalDate
        get() {
            val stored = prefs.getLong(KEY_FIRST_DAY, NO_DAY)
            if (stored != NO_DAY) return LocalDate.ofEpochDay(stored)
            val today = LocalDate.now()
            prefs.edit { putLong(KEY_FIRST_DAY, today.toEpochDay()) }
            return today
        }

    private val lastBuyAnywayDay: LocalDate?
        get() = prefs.getLong(KEY_LAST_BUY_ANYWAY, NO_DAY).takeIf { it != NO_DAY }?.let(LocalDate::ofEpochDay)

    private fun LocalTime.toMinutes(): Int = hour * 60 + minute

    private fun changed() {
        _gameVersion.value += 1
    }

    private inline fun <reified T> readList(key: String): List<T> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching { Json.decodeFromString<List<T>>(raw) }.getOrDefault(emptyList())
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

        private const val KEY_WAIT_HOURS = "default_wait_hours"
        private const val KEY_CURRENCY = "currency"
        // Stored under the v1 name so cashews collected so far are kept.
        private const val KEY_CASHEWS = "nuts"
        private const val KEY_DATA_VERSION = "data_version"
        private const val KEY_FIRST_DAY = "first_day"
        private const val KEY_LAST_BUY_ANYWAY = "last_buy_anyway_day"
        private const val KEY_HEARTS_MONTH = "hearts_month"
        private const val KEY_HEARTS_USED = "hearts_used"
        private const val KEY_QUEST_DAY = "quest_pause_day"
        private const val KEY_ITEMS = "wait_items"
        private const val KEY_LOG = "pause_log"
        private const val KEY_USD_MIGRATED = "usd_migrated_v1"
        private const val KEY_NOTIF_ASKED = "notifications_asked"
        private const val KEY_SHIELD_ON = "shield_on"
        private const val KEY_SHIELD_FROM = "shield_from_min"
        private const val KEY_SHIELD_TO = "shield_to_min"
        private const val KEY_SHIELD_STRICT = "shield_strict"
        private const val NO_DAY = Long.MIN_VALUE

        const val QUIET_MINUTES = 15

        @Volatile private var instance: Store? = null

        fun get(context: Context): Store = instance ?: synchronized(this) {
            instance ?: Store(context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE))
                .also { instance = it }
        }
    }
}
