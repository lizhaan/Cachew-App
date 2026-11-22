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
        if (version < 2) {
            // v2: the pause log has a new format (choice + session length instead of v1 decisions),
            // and v1's quiet windows and daily quest are gone.
            prefs.edit {
                remove(KEY_LOG)
                remove("quiet_until")
                remove("quest_pause_day")
            }
            version = 2
        }
        if (version < 3) {
            // Phones that finished onboarding before this flag existed have set their shield already.
            if (prefs.getBoolean(KEY_ONBOARDING_DONE, false)) prefs.edit { putBoolean("shield_confirmed", true) }
            version = 3
        }
        if (version < 4) {
            // The old Xiaomi card's "Both on" becomes the ticked background step.
            if (prefs.getBoolean(KEY_XIAOMI_POPUPS, false)) {
                prefs.edit { putStringSet(KEY_SETUP_CONFIRMED, setOf(PhoneSetup.STEP_BACKGROUND)) }
            }
            version = 4
        }
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

    /**
     * True once the night shield was saved for the first time. Before that (first setup) its
     * 2-hour lock doesn't apply; after it, not even replaying the intro can skip the lock.
     */
    var shieldConfirmed: Boolean
        get() = prefs.getBoolean(KEY_SHIELD_CONFIRMED, false)
        set(value) = prefs.edit { putBoolean(KEY_SHIELD_CONFIRMED, value) }

    /**
     * Onboarding answers ("About you" and the 2-question quiz), kept only on the phone.
     * Keys like "age", "gender", "when", "spend"; values are the option ids.
     */
    var profile: Map<String, String>
        get() = prefs.getString(KEY_PROFILE, null)
            ?.let { runCatching { Json.decodeFromString<Map<String, String>>(it) }.getOrNull() } ?: emptyMap()
        set(value) = prefs.edit { putString(KEY_PROFILE, Json.encodeToString(value)) }

    /**
     * Setup steps the user ticked on the Set up Cachew screen (PhoneSetup.STEP_BACKGROUND):
     * the phone maker's background switches, which apps can't read.
     */
    var setupConfirmed: Set<String>
        get() = prefs.getStringSet(KEY_SETUP_CONFIRMED, emptySet())!!.toSet()
        set(value) = prefs.edit { putStringSet(KEY_SETUP_CONFIRMED, value.toSet()) }

    // --- Shop session (stage 3) -------------------------------------------------------
    // After "Just look" / "I need something specific" the shop is open for 10 minutes.
    // Saved here (not only in the service's memory) so it survives the service restarting.

    /** The running session, or null. Expired sessions are still returned until [endSession]. */
    val session: ShopSession?
        get() = prefs.getString(KEY_SESSION, null)?.let { runCatching { Json.decodeFromString<ShopSession>(it) }.getOrNull() }

    /** The running, not yet expired session for [packageName], if any. */
    fun activeSession(packageName: String, now: Long = System.currentTimeMillis()): ShopSession? =
        session?.takeIf { it.shopPackage == packageName && now < it.endsAt }

    fun saveSession(value: ShopSession) = prefs.edit { putString(KEY_SESSION, Json.encodeToString(value)) }

    /** Starts a session: [record] (the pause that led to it) goes into the log, its duration is filled in at the end. */
    fun startSession(record: PauseRecord, mode: SessionMode, now: Long = System.currentTimeMillis()): ShopSession {
        val s = ShopSession(
            shopPackage = record.shopPackage, mode = mode, openedAt = record.at,
            startedAt = now, endsAt = now + Rules.SESSION_MINUTES * 60_000L,
        )
        prefs.edit {
            putString(KEY_SESSION, Json.encodeToString(s))
            putString(KEY_LOG, Json.encodeToString(pauseLog() + record))
        }
        changed()
        return s
    }

    /** Ends the session (time up or "I'm done") and writes how long it lasted into its log entry. */
    fun endSession(now: Long = System.currentTimeMillis()) {
        val s = session ?: return
        val lasted = minOf(now, s.endsAt) - s.startedAt
        val log = pauseLog().map { if (it.at == s.openedAt && it.shopPackage == s.shopPackage) it.copy(sessionMillis = lasted) else it }
        prefs.edit {
            remove(KEY_SESSION)
            putString(KEY_LOG, Json.encodeToString(log))
        }
        changed()
    }

    /** A pause that didn't start a session ("Close <shop>", or Back during the pause). */
    fun logPause(record: PauseRecord) {
        prefs.edit { putString(KEY_LOG, Json.encodeToString(pauseLog() + record)) }
        changed()
    }

    /** Would "Close <shop>" still earn cashews today (+5, at most 3 times a day)? */
    fun closeRewardLeft(today: LocalDate = LocalDate.now()): Boolean = closesRewardedOn(today) < Rules.REWARDED_CLOSES_PER_DAY

    /** "Close <shop>" / "I'm done": +5 cashews unless today's limit is used up. Returns the cashews added (0 or 5). */
    fun rewardClose(today: LocalDate = LocalDate.now()): Int {
        if (!closeRewardLeft(today)) return 0
        prefs.edit {
            putLong(KEY_CLOSE_DAY, today.toEpochDay())
            putInt(KEY_CLOSE_COUNT, closesRewardedOn(today) + 1)
            putInt(KEY_CASHEWS, cashews + Rules.CASHEWS_PER_CLOSE)
        }
        changed()
        return Rules.CASHEWS_PER_CLOSE
    }

    private fun closesRewardedOn(today: LocalDate): Int =
        if (prefs.getLong(KEY_CLOSE_DAY, NO_DAY) == today.toEpochDay()) prefs.getInt(KEY_CLOSE_COUNT, 0) else 0

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
        )
        set(value) {
            prefs.edit {
                putBoolean(KEY_SHIELD_ON, value.on)
                putInt(KEY_SHIELD_FROM, value.from.toMinutes())
                putInt(KEY_SHIELD_TO, value.to.toMinutes())
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

    fun snapshot(today: LocalDate = LocalDate.now()): GameSnapshot {
        applyBonuses(today)
        return GameSnapshot(
            cashews = cashews,
            streak = Game.streak(today, firstDay, lastBuyAnywayDay),
            heartsLeft = heartsLeft(today),
            heartsRefill = Game.heartsRefillDate(today),
            boughtAnywayToday = lastBuyAnywayDay == today,
            waiting = waitItems().filter { it.status == WaitItem.Status.Waiting },
        )
    }

    // --- Bonus cashews -----------------------------------------------------------------

    /**
     * Pays the bonuses that came due since the last check (run whenever counters are shown):
     *  - on the 1st of a month: +50 for every heart left unused in the month(s) before;
     *  - streak milestones: +50 at 7 days, +200 at 30 days, once per streak.
     * Returns the cashews added (0 most of the time).
     */
    fun applyBonuses(today: LocalDate = LocalDate.now()): Int {
        var bonus = 0
        val thisMonth = YearMonth.from(today)
        val paidUpTo = prefs.getString(KEY_BONUS_MONTH, null)?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
        if (paidUpTo != null) {
            var m: YearMonth = paidUpTo
            while (m < thisMonth) {
                bonus += heartsLeftIn(m) * Rules.CASHEWS_PER_UNUSED_HEART
                m = m.plusMonths(1)
            }
        }
        // A streak is identified by the day it started (first day, or the day after "I already bought it").
        val streakStart = (lastBuyAnywayDay ?: firstDay).toEpochDay()
        val streak = Game.streak(today, firstDay, lastBuyAnywayDay)
        val paid = prefs.getString(KEY_STREAK_BONUS, null)?.split(':')
            ?.takeIf { it.size == 2 && it[0].toLongOrNull() == streakStart }?.get(1)?.toIntOrNull() ?: 0
        var paidNow = paid
        for ((days, cashews) in Rules.STREAK_BONUSES.toSortedMap()) {
            if (streak >= days && days > paid) {
                bonus += cashews
                paidNow = days
            }
        }
        prefs.edit {
            putString(KEY_BONUS_MONTH, thisMonth.toString())
            putString(KEY_STREAK_BONUS, "$streakStart:$paidNow")
            if (bonus > 0) putInt(KEY_CASHEWS, cashews + bonus)
        }
        if (bonus > 0) changed()
        return bonus
    }

    private fun heartsLeftIn(month: YearMonth): Int {
        val usedIn = prefs.getString(KEY_HEARTS_MONTH, null)?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
        return Game.heartsLeft(month, usedIn, prefs.getInt(KEY_HEARTS_USED, 0))
    }

    fun waitItems(): List<WaitItem> = readList(KEY_ITEMS)

    fun pauseLog(): List<PauseRecord> = readList(KEY_LOG)

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

    /** Item saved through Share (screen 16): +15 cashews for every one, with or without a session. */
    fun addSharedItem(item: WaitItem, today: LocalDate = LocalDate.now()): RewardInfo {
        prefs.edit {
            putString(KEY_ITEMS, Json.encodeToString(waitItems() + item))
            putInt(KEY_CASHEWS, cashews + Rules.CASHEWS_PER_SAVED_ITEM)
        }
        changed()
        return RewardInfo(Rules.CASHEWS_PER_SAVED_ITEM, Game.streak(today, firstDay, lastBuyAnywayDay), item)
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

    /**
     * "Buy it". After the wait it's free; before the wait is over it costs a heart
     * (status "bought with a heart"). The streak survives either way.
     * Returns false if a heart was needed and none are left.
     */
    fun buy(id: String, now: Long = System.currentTimeMillis()): Boolean {
        val item = item(id) ?: return false
        val early = now < item.dueAt
        if (early) {
            if (heartsLeft(epochToDate(now)) <= 0) return false
            spendHeart(epochToDate(now))
        }
        updateItem(item.copy(status = if (early) WaitItem.Status.BoughtWithHeart else WaitItem.Status.Bought, decidedAt = now))
        return true
    }

    /** 19 "I already bought it": bought without waiting, so the streak starts again from today. */
    fun alreadyBought(id: String, now: Long = System.currentTimeMillis()) {
        item(id)?.let { updateItem(it.copy(status = WaitItem.Status.Bought, decidedAt = now)) }
        prefs.edit { putLong(KEY_LAST_BUY_ANYWAY, epochToDate(now).toEpochDay()) }
        changed()
    }

    /** "Buy it": after the short pause the shop opens for 5 minutes (no pause log entry). */
    fun startBuySession(packageName: String, now: Long = System.currentTimeMillis()) {
        saveSession(
            ShopSession(
                shopPackage = packageName, mode = SessionMode.Buy, openedAt = 0L,
                startedAt = now, endsAt = now + Rules.BUY_SESSION_MINUTES * 60_000L,
            )
        )
        changed()
    }

    fun markReminded(id: String, now: Long = System.currentTimeMillis()) {
        item(id)?.let { updateItem(it.copy(remindedAt = now)) }
    }

    // --- Savings goal ---------------------------------------------------------------

    /** The goal being saved for, or null (then the den asks "What are you saving for?"). */
    val goal: SavingsGoal?
        get() = prefs.getString(KEY_GOAL, null)?.let { runCatching { Json.decodeFromString<SavingsGoal>(it) }.getOrNull() }

    /** Reached goals, newest first (Me → Savings goal). */
    fun goalsDone(): List<SavingsGoal> = readList<SavingsGoal>(KEY_GOALS_DONE).sortedByDescending { it.doneAt }

    /** New goal, or the current one edited (then its progress so far is kept). */
    fun setGoal(name: String, amount: Double, now: Long = System.currentTimeMillis()) {
        val setAt = goal?.setAt ?: now
        prefs.edit { putString(KEY_GOAL, Json.encodeToString(SavingsGoal(name, amount, setAt = setAt))) }
        changed()
    }

    /** Money kept towards the current goal since it was set. */
    fun goalProgress(): Double = goal?.let { Stats.goalProgress(waitItems(), it) } ?: 0.0

    /** Goal reached and celebrated: it moves to the "done" list, ready for a new one. */
    fun completeGoal(now: Long = System.currentTimeMillis()) {
        val g = goal ?: return
        prefs.edit {
            putString(KEY_GOALS_DONE, Json.encodeToString(readList<SavingsGoal>(KEY_GOALS_DONE) + g.copy(doneAt = now)))
            remove(KEY_GOAL)
        }
        changed()
    }

    /**
     * Me → "Reset demo data": a clean start for a demo. Waiting items, cashews, streak, hearts,
     * pause log, goals and the running session go; picked shops, onboarding, night shield and
     * waiting time stay. (The caller cancels reminders and deletes photos.)
     */
    fun resetDemoData() {
        prefs.edit {
            listOf(
                KEY_ITEMS, KEY_LOG, KEY_CASHEWS, KEY_FIRST_DAY, KEY_LAST_BUY_ANYWAY, KEY_HEARTS_MONTH, KEY_HEARTS_USED,
                KEY_SESSION, KEY_CLOSE_DAY, KEY_CLOSE_COUNT, KEY_BONUS_MONTH, KEY_STREAK_BONUS, KEY_GOAL, KEY_GOALS_DONE,
            ).forEach(::remove)
        }
        changed()
    }

    /** We ask for the notification permission once, when the first item is added. */
    var notificationsAsked: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_ASKED, false)
        set(value) = prefs.edit { putBoolean(KEY_NOTIF_ASKED, value) }

    private fun epochToDate(millis: Long): LocalDate =
        java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()

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
        private const val KEY_SESSION = "session"
        private const val KEY_PROFILE = "onboarding_profile"
        private const val KEY_SETUP_CONFIRMED = "setup_confirmed"
        private const val KEY_SHIELD_CONFIRMED = "shield_confirmed"
        private const val KEY_GOAL = "goal"
        private const val KEY_GOALS_DONE = "goals_done"
        private const val KEY_BONUS_MONTH = "bonus_paid_month"
        private const val KEY_STREAK_BONUS = "streak_bonus_paid"
        private const val KEY_CLOSE_DAY = "close_reward_day"
        private const val KEY_CLOSE_COUNT = "close_reward_count"
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
        private const val KEY_ITEMS = "wait_items"
        private const val KEY_LOG = "pause_log"
        private const val KEY_USD_MIGRATED = "usd_migrated_v1"
        private const val KEY_NOTIF_ASKED = "notifications_asked"
        private const val KEY_SHIELD_ON = "shield_on"
        private const val KEY_SHIELD_FROM = "shield_from_min"
        private const val KEY_SHIELD_TO = "shield_to_min"
        private const val NO_DAY = Long.MIN_VALUE

        @Volatile private var instance: Store? = null

        fun get(context: Context): Store = instance ?: synchronized(this) {
            instance ?: Store(context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE))
                .also { instance = it }
        }
    }
}
