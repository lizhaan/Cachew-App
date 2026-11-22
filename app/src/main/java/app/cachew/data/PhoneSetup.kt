package app.cachew.data

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.cachew.service.ShopWatchService

/**
 * Everything a phone needs so the pause appears on time, for every brand (Set up Cachew screen).
 *
 * Android itself needs Accessibility (and on 13+ "Allow restricted settings") and notifications;
 * those two we can check. Phone makers add their own background switches (Xiaomi pop-ups and
 * Autostart, Huawei app launch, …): apps can't read those, so the user ticks them off once
 * ([Store.setupConfirmed]).
 */
object PhoneSetup {

    enum class Brand { Xiaomi, Samsung, Huawei, Oppo, Vivo, Other }

    /** Brand family from the manufacturer/brand name (Redmi and POCO are Xiaomi, Honor is like Huawei, …). */
    val brand: Brand
        get() {
            val name = (Build.MANUFACTURER + " " + Build.BRAND).lowercase()
            return when {
                listOf("xiaomi", "redmi", "poco").any { it in name } -> Brand.Xiaomi
                "samsung" in name -> Brand.Samsung
                listOf("huawei", "honor").any { it in name } -> Brand.Huawei
                listOf("oppo", "realme", "oneplus").any { it in name } -> Brand.Oppo
                listOf("vivo", "iqoo").any { it in name } -> Brand.Vivo
                else -> Brand.Other
            }
        }

    /**
     * The one step we can't check: letting Cachew work in the background. Every brand names it
     * differently (Xiaomi: pop-up windows in background + Autostart; Huawei: app launch; Oppo,
     * Vivo: auto launch / background activity), so the text and the button adapt to the phone.
     */
    const val STEP_BACKGROUND = "background"

    // ---- What we can check ---------------------------------------------------------------

    fun accessibilityOn(context: Context): Boolean = ShopWatchService.isEnabled(context)

    fun notificationsOn(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }


    /** Steps still to do: the two checks plus the background step if not ticked yet. */
    fun stepsLeft(context: Context, confirmed: Set<String>): Int =
        listOf(accessibilityOn(context), notificationsOn(context)).count { !it } +
            (if (STEP_BACKGROUND in confirmed) 0 else 1)

    // ---- Where each button goes ----------------------------------------------------------

    fun openAccessibility(context: Context) =
        start(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    /** App info: Android 13+ "⋮ → Allow restricted settings", and on most brands the background switches. */
    fun openAppInfo(context: Context) =
        start(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))

    fun openNotificationSettings(context: Context) =
        start(context, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))


    /** The phone maker's background screen (Xiaomi: pop-ups in background), falling back to app info. */
    fun openBackground(context: Context) {
        when (brand) {
            Brand.Xiaomi -> Xiaomi.openPopupPermission(context)
            Brand.Huawei -> openFirst(context, "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
            Brand.Oppo -> openFirst(
                context,
                "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
                "com.oplus.safecenter" to "com.oplus.safecenter.permission.startup.StartupAppListActivity",
            )
            Brand.Vivo -> openFirst(context, "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
            else -> openAppInfo(context)
        }
    }

    /** Xiaomi only: the second switch, Autostart. */
    fun openAutostart(context: Context) = Xiaomi.openAutostart(context)

    private fun openFirst(context: Context, vararg screens: Pair<String, String>) {
        val opened = screens.any { (pkg, cls) -> start(context, Intent().setComponent(ComponentName(pkg, cls))) }
        if (!opened) openAppInfo(context)
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: Exception) {
        false // this firmware doesn't have that screen, or won't let us open it
    }
}
