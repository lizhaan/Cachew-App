package app.cachew.share

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.cachew.R
import app.cachew.data.ShopLabel
import app.cachew.data.WaitItem
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
        // Survives rotation/recreation, so the photo file name stays the same.
        val itemId = savedInstanceState?.getString(KEY_ID) ?: UUID.randomUUID().toString()
        intent.putExtra(KEY_ID, itemId)

        setContent {
            CachewTheme {
                CachewBackground(night = true) {
                    ShareScreen(
                        itemId = itemId,
                        sharedText = text,
                        url = url,
                        fromPackage = fromPackage,
                        webShop = url?.let(::shopFromDomain),
                        thumbFile = File(filesDir, "thumbs/$itemId.jpg"),
                        onSaved = ::onSaved,
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_ID, intent.getStringExtra(KEY_ID))
    }

    /** Back to the shop the user shared from; a small toast confirms. */
    private fun onSaved(item: WaitItem) {
        val hours = (item.waitMillis / 3_600_000L).toInt()
        val msg = if (hours == 0) getString(R.string.share_saved_minute) else getString(R.string.share_saved, hours)
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        finish()
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
