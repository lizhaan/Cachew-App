package app.cachew.share

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import app.cachew.BuildConfig
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.cachew.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cachew.data.RewardInfo
import app.cachew.data.ShopLabel
import app.cachew.data.Store
import app.cachew.pause.RewardScreen
import java.time.LocalTime
import app.cachew.ui.components.CachewBackground
import app.cachew.ui.components.TileIcon
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewTheme
import java.io.File
import java.net.URI
import java.util.UUID

/**
 * Receives "Share" from a shop (ACTION_SEND, text/plain) and shows screen 16
 * "Into the waiting list". This is the second way to add an item, independent of the pause.
 */
class ShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        val text = listOfNotNull(
            intent.getStringExtra(Intent.EXTRA_SUBJECT),
            intent.getStringExtra(Intent.EXTRA_TEXT),
        ).joinToString(" ")
        val url = ProductParser.extractUrl(text)
        // Which app shared it, if Android tells us (e.g. android-app://com.zzkko).
        val fromPackage = referrer?.takeIf { it.scheme == "android-app" }?.host?.takeIf { it != packageName }
        if (BuildConfig.DEBUG) {
            // Debug builds only: what the shop actually sent, to tune the parser per shop.
            val extras = intent.extras?.keySet()?.joinToString { k -> "$k=${intent.extras?.get(k)}" }
            Log.d(ProductFetcher.TAG, "share from=$fromPackage referrer=$referrer type=${intent.type} url=$url extras=[$extras]")
        }
        // Survives rotation/recreation, so the photo file name stays the same.
        val itemId = savedInstanceState?.getString(KEY_ID) ?: UUID.randomUUID().toString()
        intent.putExtra(KEY_ID, itemId)

        setContent {
            CachewTheme {
                CachewBackground(night = true) {
                    val r = reward
                    if (r == null) {
                        ShareScreen(
                            itemId = itemId,
                            sharedText = text,
                            url = url,
                            fromPackage = fromPackage,
                            webShop = url?.let(::shopFromDomain),
                            thumbFile = File(filesDir, "thumbs/$itemId.jpg"),
                            onSaved = { reward = it },
                        )
                    } else {
                        val night = remember { Store.get(this@ShareActivity).nightShield.isNight(LocalTime.now()) }
                        RewardScreen(r, night = night, onClose = ::afterReward)
                    }
                }
            }
        }
    }

    /** Set after "Wait N hours": the screen switches to "Nice pause." */
    private var reward by mutableStateOf<RewardInfo?>(null)

    /**
     * Leaving "Nice pause.": back into the shop while its session is still running
     * (it sits right underneath this screen), otherwise to the phone's home screen.
     */
    private fun afterReward() {
        val inSession = Store.get(this).session?.let { System.currentTimeMillis() < it.endsAt } == true
        if (!inSession) {
            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_ID, intent.getStringExtra(KEY_ID))
    }

    companion object {
        private const val KEY_ID = "share_item_id"

        /** "https://m.rozetka.com.ua/..." → "Rozetka" with a letter tile. */
        fun shopFromDomain(url: String): ShopLabel? {
            val host = runCatching { URI(url).host }.getOrNull() ?: return null
            val main = host.removePrefix("www.").removePrefix("m.").substringBefore('.')
            if (main.isBlank()) return null
            val name = main.replaceFirstChar { it.uppercase() }
            return ShopLabel(name, TileIcon.Letter(name.take(1), CachewColors.Purple))
        }
    }
}
