package app.cachew.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.cachew.R
import app.cachew.data.Game
import app.cachew.data.Store
import app.cachew.data.WaitItem
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * "Your 48 hours are up" reminders (screen 18), one WorkManager job per item.
 *
 * WorkManager survives app restarts and phone reboots, and "unique work" named after the
 * item id means rescheduling an item replaces its old job instead of adding a second one.
 */
object Reminders {

    const val CHANNEL = "reminders"
    const val KEY_ITEM_ID = "item_id"
    const val KEY_MOVED = "moved_to_morning"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.reminders_channel), NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    /**
     * Plans the reminder for [item]. If it would come at night it moves to the morning
     * (quiet hours); debug 1-minute test items are never moved.
     */
    fun schedule(context: Context, item: WaitItem, replace: Boolean = true, now: Long = System.currentTimeMillis()) {
        val zone = ZoneId.systemDefault()
        val due = LocalDateTime.ofInstant(Instant.ofEpochMilli(item.dueAt), zone)
        val isTestMinute = item.waitMillis < 3_600_000L
        val (at, moved) = if (isTestMinute) due to false else Game.reminderTime(due)
        val delay = (at.atZone(zone).toInstant().toEpochMilli() - now).coerceAtLeast(0)

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_ITEM_ID to item.id, KEY_MOVED to moved))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            item.id,
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(context: Context, itemId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(itemId)
        context.getSystemService(NotificationManager::class.java).cancel(itemId.hashCode())
    }

    /** On app start: every waiting item that hasn't been reminded yet has a job (KEEP = don't reset existing ones). */
    fun ensureAll(context: Context) {
        Store.get(context).waitItems()
            .filter { it.status == WaitItem.Status.Waiting && it.remindedAt == null }
            .forEach { schedule(context, it, replace = false) }
    }
}
