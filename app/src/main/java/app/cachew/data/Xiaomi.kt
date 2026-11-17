package app.cachew.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Xiaomi / Redmi / POCO (MIUI, HyperOS) add their own permission on top of Android:
 * "Display pop-up windows while running in the background" (internal op 10021).
 * While it's off, the pause that our service starts is held back until the user opens
 * Cachew themselves. Apps can't switch it on or even read it, so we guide the user there.
 */
object Xiaomi {

    val isXiaomi: Boolean
        get() = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)

    /** Opens Cachew's "Other permissions" page in Xiaomi's Security app, or the app info page as a fallback. */
    fun openPopupPermission(context: Context) {
        val editor = Intent("miui.intent.action.APP_PERM_EDITOR")
            .setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
            .putExtra("extra_pkgname", context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(editor)
            return
        } catch (_: Exception) {
            // Not found or not allowed on this firmware: use the standard app info page.
        }
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
