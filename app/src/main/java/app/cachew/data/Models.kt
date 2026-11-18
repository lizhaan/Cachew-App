package app.cachew.data

import kotlinx.serialization.Serializable

/** One pass through the pause, kept in the pause log (stored as JSON in Store). */
@Serializable
data class PauseRecord(
    val at: Long,
    val shopPackage: String,
    val mood: Mood,
    val onList: Boolean,
    val night: Boolean,
    val decision: Decision,
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
    val nuts: Int,
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
    val stash: Double,
    val keptThisMonth: Double,
    val level: Game.Level,
    val levelledUp: Boolean,
    val streak: Int,
    val heartsLeft: Int,
)

/** What screen 17 needs after "Add to waiting list". */
data class RewardInfo(
    val nuts: Int,
    val streak: Int,
    /** First pause of the day: the "Pause before one shop" quest is done. */
    val questDone: Boolean,
    val item: WaitItem,
)
