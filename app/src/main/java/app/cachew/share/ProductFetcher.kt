package app.cachew.share

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File

/** Network side of Share: download the product page and its photo. Never throws. */
object ProductFetcher {

    private const val TIMEOUT_MS = 6_000
    private const val MAX_IMAGE_BYTES = 5 * 1024 * 1024

    // A phone browser's User-Agent: many shops send bots a stripped page without prices.
    private const val MOBILE_UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    /** Page → product info, or null if the page can't be loaded in time. */
    suspend fun fetch(url: String): ProductInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val doc = Jsoup.connect(url)
                .userAgent(MOBILE_UA)
                .timeout(TIMEOUT_MS)
                .followRedirects(true)
                .get()
            ProductParser.parse(doc)
        }.getOrNull()
    }

    /** Saves the photo to [target]; returns false if it couldn't be downloaded. */
    suspend fun downloadImage(url: String, target: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = Jsoup.connect(url)
                .userAgent(MOBILE_UA)
                .timeout(TIMEOUT_MS)
                .ignoreContentType(true)
                .maxBodySize(MAX_IMAGE_BYTES)
                .execute()
                .bodyAsBytes()
            target.parentFile?.mkdirs()
            target.writeBytes(bytes)
            bytes.isNotEmpty()
        }.getOrDefault(false)
    }
}
