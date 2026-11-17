package app.cachew.data

import android.content.Context
import app.cachew.ui.components.TileIcon
import app.cachew.ui.theme.CachewColors

/** How a shop is shown in small UI bits (pause pill, buttons): its name and a tile icon. */
data class ShopLabel(val name: String, val icon: TileIcon)

object ShopLabels {
    /** Recommended shop → our name + letter tile; any other app → launcher label + real icon. */
    suspend fun load(context: Context, packageName: String): ShopLabel {
        ShopApps.find(packageName)?.let { return ShopLabel(it.name, TileIcon.Letter(it.letter, it.color)) }
        InstalledApps.load(context, packageName)?.let { return ShopLabel(it.label, TileIcon.AppIcon(it.icon)) }
        val fallback = packageName.substringAfterLast('.')
        return ShopLabel(fallback, TileIcon.Letter(fallback.take(1).uppercase(), CachewColors.Purple))
    }

    /** Name only, without touching PackageManager (good enough before the icon loads). */
    fun quickName(packageName: String): String =
        ShopApps.find(packageName)?.name ?: packageName.substringAfterLast('.')
}
