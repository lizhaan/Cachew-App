package app.cachew.data

import kotlinx.serialization.Serializable

/**
 * One time a shop was opened and Cachew paused it, kept in the pause log (JSON in Store).
 * Stats are computed from this log (pauses, riskiest hour, top trigger shop).
 * [mood]/[onList] are null when the user left before answering.
 */
@Serializable
data class PauseRecord(
    val at: Long,
    val shopPackage: String,
    val mood: Mood?,
    val onList: Boolean?,
    val action: EntryAction,
    /** How long the shop session lasted; null if no session was started (or it's still running). */
    val sessionMillis: Long? = null,
)

/** What the user did on the choice screen. */
@Serializable
enum class EntryAction {
    /** "Just look, save, don't buy" → 10-min session with the 5-min check-in. */
    JustLook,
    /** "I need something specific" → 10-min session, Penny peeks in once. */
    Specific,
    /** "Close <shop>" (+5 cashews). */
    Closed,
    /** Left the pause with Back before choosing. */
    Left,
}

@Serializable
enum class SessionMode { JustLook, Specific }

/** The shop session after the choice screen (stored in Store so it survives a service restart). */
@Serializable
data class ShopSession(
    val shopPackage: String,
    val mode: SessionMode,
    /** The pause's [PauseRecord.at], to find its log entry when the session ends. */
    val openedAt: Long,
    val startedAt: Long,
    val endsAt: Long,
    /** "Just look": the 5-minute check-in sheet was already shown. */
    val checkInShown: Boolean = false,
    /** "Specific": Penny already did her "2 minutes left" peek. */
    val warned: Boolean = false,
)

/** Something waiting in the waiting list. [price] is null when the user skipped it. */
@Serializable
data class WaitItem(
    val id: String,
    val title: String,
    val price: Double?,
    val currency: String,
    val shopPackage: String,
    val addedAt: Long,
    val waitMillis: Long,
    val status: Status = Status.Waiting,
    /** Photo saved from the product page (filesDir/thumbs/<id>.jpg), if any. */
    val thumbPath: String? = null,
    /** Display name of the shop when it isn't an installed app we know (e.g. from a web link). */
    val shopName: String? = null,
    val decidedAt: Long? = null,
    /** When the "time's up" notification was shown; null = still to come. */
    val remindedAt: Long? = null,
) {
    @Serializable
    enum class Status { Waiting, Kept, BoughtWithHeart }

    val dueAt: Long get() = addedAt + waitMillis
}

/** Counters the UI shows, computed for "now" (streak and hearts depend on the date). */
data class GameSnapshot(
    /** Penny's winter stash; decides her level. */
    val cashews: Int,
    val streak: Int,
    val heartsLeft: Int,
    val heartsRefill: java.time.LocalDate,
    /** "Buy it anyway" happened today: Cachew sits with its back turned in the den. */
    val boughtAnywayToday: Boolean,
    val waiting: List<WaitItem>,
)

/** What screen 20 needs after "Let it go". [amount] is null for items without a price. */
data class WinInfo(
    val amount: Double?,
    val currency: String,
    /** Cashews this "Let it go" added (+1 per dollar). */
    val cashewsEarned: Int,
    /** Cashews in Penny's stash after it. */
    val stash: Int,
    val keptThisMonth: Double,
    val level: Game.Level,
    val levelledUp: Boolean,
    val streak: Int,
    val heartsLeft: Int,
)

/** What screen 17 needs after "Add to waiting list". */
data class RewardInfo(
    val cashews: Int,
    val streak: Int,
    /** First pause of the day: the "Pause before one shop" quest is done. */
    val questDone: Boolean,
    val item: WaitItem,
)
