package app.cachew.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.cachew.MainActivity
import app.cachew.R
import app.cachew.data.Money
import app.cachew.data.Store
import app.cachew.data.WaitItem

/** Runs when an item's wait is over and shows the "Still want it?" notification. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getString(Reminders.KEY_ITEM_ID) ?: return Result.success()
        val moved = inputData.getBoolean(Reminders.KEY_MOVED, false)
        val store = Store.get(applicationContext)
        val item = store.item(id) ?: return Result.success()
        if (item.status != WaitItem.Status.Waiting) return Result.success() // already decided

        store.markReminded(id)
        if (!canNotify()) return Result.success() // permission refused: the den still shows "ready"

        Reminders.ensureChannel(applicationContext)
        val ctx = applicationContext
        val hours = (item.waitMillis / 3_600_000L).toInt()
        val title = if (hours == 0) ctx.getString(R.string.reminder_title_short) else ctx.getString(R.string.reminder_title, hours)
        val question = if (item.price != null) {
            ctx.getString(R.string.reminder_body_price, item.title, Money.format(item.price, item.currency))
        } else {
            ctx.getString(R.string.reminder_body, item.title)
        }
        val body = if (moved) question + " " + ctx.getString(R.string.reminder_waited_till_morning) else question

        // Tapping opens "Still want it?" (screen 19) for this item.
        val open = Intent(ctx, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_ITEM_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(
            ctx, id.hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(ctx, Reminders.CHANNEL)
            .setSmallIcon(R.drawable.ic_cashew)
            .setColor(ContextCompat.getColor(ctx, R.color.purple))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        @Suppress("MissingPermission") // checked in canNotify()
        NotificationManagerCompat.from(ctx).notify(id.hashCode(), notification)
        return Result.success()
    }

    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}
