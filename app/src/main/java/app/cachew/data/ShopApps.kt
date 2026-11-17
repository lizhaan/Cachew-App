package app.cachew.data

import androidx.compose.ui.graphics.Color
import app.cachew.BuildConfig

/** A recommended shop shown as a letter tile on screen 08. */
data class ShopApp(
    val name: String,
    val packageName: String,
    val letter: String,
    val color: Color,
)

object ShopApps {
    // Order matters: this is the order of tiles in the grid.
    private val shops = listOf(
        ShopApp("Amazon", "com.amazon.mShop.android.shopping", "A", Color(0xFFF59E0B)),
        ShopApp("Shein", "com.zzkko", "S", Color(0xFF141826)),
        ShopApp("Temu", "com.einnovation.temu", "T", Color(0xFFF97316)),
        ShopApp("Sephora", "com.sephora", "S", Color(0xFF222838)),
        ShopApp("Etsy", "com.etsy.android", "E", Color(0xFFEA580C)),
        ShopApp("eBay", "com.ebay.mobile", "e", Color(0xFF2563EB)),
        ShopApp("Walmart", "com.walmart.android", "W", Color(0xFF0284C7)),
        ShopApp("Zara", "com.inditex.zara", "Z", Color(0xFF3A4150)),
        // Ukrainian shops: the APK is also installed by people in Ukraine.
        ShopApp("Rozetka", "ua.com.rozetka.shop", "R", Color(0xFF00A046)),
        ShopApp("Prom", "ua.prom.b2c", "P", Color(0xFF6C2BD9)),
        ShopApp("OLX", "ua.slando", "O", Color(0xFF0E5E66)),
    )

    // Chrome is a stand-in "shop" for testing the pause; debug builds only.
    private val debugOnly = listOf(
        ShopApp("Chrome", "com.android.chrome", "C", Color(0xFF1A73E8)),
    )

    val recommended: List<ShopApp> = if (BuildConfig.DEBUG) shops + debugOnly else shops

    private val byPackage = recommended.associateBy { it.packageName }

    fun find(packageName: String): ShopApp? = byPackage[packageName]

    fun isRecommended(packageName: String) = packageName in byPackage
}
