package app.cachew.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Xiaomi / Redmi / POCO (MIUI, HyperOS) add two permissions of their own that the pause needs:
 *  - Autostart (internal op 10008): lets Android restart our Accessibility service after Xiaomi
 *    kills the app (swipe away in Recents, battery cleanup).
 *  - "Display pop-up windows while running in the background" (op 10021): without it the pause
 *    is held back until the user opens Cachew themselves.
 * Apps can't switch these on or even read them, so we guide the user there.
 */
object Xiaomi {

    val isXiaomi: Boolean
        get() = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)

    /**
     * Opens Xiaomi's Autostart list. Without Autostart, when Xiaomi kills Cachew (e.g. the user
     * swipes it away in Recents) Android is not allowed to restart the Accessibility service,
     * so pauses silently stop until the switch is toggled again.
     */
    fun openAutostart(context: Context) {
        val autostart = Intent()
            .setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(autostart)
            return
        } catch (_: Exception) {
            // Not found or not allowed: the app info page also has an Autostart switch on HyperOS.
        }
        openAppInfo(context)
    }

    private fun openAppInfo(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

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
        openAppInfo(context)
    }
}
