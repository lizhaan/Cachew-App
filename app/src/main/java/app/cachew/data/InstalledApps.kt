package app.cachew.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** An app the user could put a pause on: label + real launcher icon. */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap,
)

/**
 * Reads launchable apps from PackageManager.
 *
 * Since Android 11 an app only "sees" packages it declares interest in. We declare
 * <queries> for the MAIN/LAUNCHER intent in the manifest, which reveals every app with a
 * home-screen icon — enough for us, without the sensitive QUERY_ALL_PACKAGES permission.
 */
object InstalledApps {

    private const val ICON_PX = 96

    private fun launcherIntent() =
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

    private fun PackageManager.queryLaunchers(): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            queryIntentActivities(launcherIntent(), PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            queryIntentActivities(launcherIntent(), 0)
        }

    /** All launchable apps except Cachew itself, sorted A–Z. Loads icons, so call off the main thread. */
    suspend fun loadAll(context: Context): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        pm.queryLaunchers()
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != context.packageName }
            .map { info ->
                InstalledApp(
                    packageName = info.activityInfo.packageName,
                    label = info.loadLabel(pm).toString(),
                    icon = info.loadIcon(pm).toBitmap(ICON_PX, ICON_PX).asImageBitmap(),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    /** Label + icon of one package, or null if it isn't installed (or isn't visible to us). */
    suspend fun load(context: Context, packageName: String): InstalledApp? = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        try {
            val info = pm.getApplicationInfo(packageName, 0)
            InstalledApp(
                packageName = packageName,
                label = pm.getApplicationLabel(info).toString(),
                icon = pm.getApplicationIcon(info).toBitmap(ICON_PX, ICON_PX).asImageBitmap(),
            )
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    /** True if the package has a launcher icon on this phone. Cheap enough for the main thread. */
    fun isInstalled(context: Context, packageName: String): Boolean =
        context.packageManager.getLaunchIntentForPackage(packageName) != null
}
