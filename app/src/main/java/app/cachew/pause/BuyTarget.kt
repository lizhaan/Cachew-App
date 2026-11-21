package app.cachew.pause

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Where "Buy it" goes: the item's own link, opened in the shop's app when that app can show it,
 * otherwise in whatever opens web links (the browser). Items without a link (old test items)
 * just open the shop app.
 *
 * [packageName] is the app that will be in front, so the 5-minute buying session can follow it.
 */
class BuyTarget(val intent: Intent, val packageName: String) {

    companion object {
        fun resolve(context: Context, shopPackage: String, url: String?): BuyTarget? {
            val pm = context.packageManager
            if (url != null) {
                val view = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (shopPackage.isNotEmpty()) {
                    val inShop = Intent(view).setPackage(shopPackage)
                    if (pm.resolveActivity(inShop, PackageManager.MATCH_DEFAULT_ONLY) != null) return BuyTarget(inShop, shopPackage)
                }
                // Default handler for the link. "android" = the "Open with" chooser: we can't know
                // which app the user will pick, so follow the shop (if any) instead.
                val handler = pm.resolveActivity(view, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
                val pkg = handler?.takeIf { it != "android" } ?: shopPackage
                return BuyTarget(view, pkg)
            }
            val launch = pm.getLaunchIntentForPackage(shopPackage) ?: return null
            return BuyTarget(launch, shopPackage)
        }
    }
}

/** "Buy it" → the short pause (10 s, no questions) → the item's link. */
fun startBuyPause(context: Context, item: app.cachew.data.WaitItem) {
    context.startActivity(PauseActivity.buyIntent(context, item))
}
