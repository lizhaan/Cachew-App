package app.cachew.share

import android.util.Log
import app.cachew.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.File
import java.net.URL

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
            val res = openFollowingRedirects(url)
            val doc = res.parse()
            // Some share links carry the product photo in the link itself (Temu: ?thumb_url=...).
            // It's the clean photo (Temu's og:image is a promo card), and it works even when the
            // page hides its data from non-browsers, so it wins.
            val info = ProductParser.parse(doc).let { p ->
                p.copy(imageUrl = ProductParser.imageFromLink(res.url().toString()) ?: p.imageUrl)
            }
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "fetch status=${res.statusCode()} final=${res.url()} title='${doc.title()}' " +
                    "ldjson=${doc.select("script[type=application/ld+json]").size} " +
                    "og:title=${doc.selectFirst("meta[property=og:title]") != null} chars=${doc.html().length} -> $info")            }
            // An error page ("Access denied", captcha) must not overwrite what the shared text gave us.
            if (res.statusCode() in 200..299) info else ProductInfo(imageUrl = info.imageUrl)
        }.onFailure { if (BuildConfig.DEBUG) Log.d(TAG, "fetch failed for $url", it) }.getOrNull()
    }

    /**
     * Follows redirects ourselves instead of letting jsoup do it, so that an "http://" hop
     * becomes "https://". Short share links (share.temu.com) sometimes redirect to plain http,
     * and Android blocks unencrypted traffic, which made the whole fetch fail.
     */
    private fun openFollowingRedirects(start: String): Connection.Response {
        var url = toHttps(start)
        // Cookies set along the way are sent on the next hop, like a browser does.
        val cookies = mutableMapOf<String, String>()
        repeat(MAX_REDIRECTS) {
            val res = Jsoup.connect(url)
                .userAgent(MOBILE_UA)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .cookies(cookies)
                .timeout(TIMEOUT_MS)
                .followRedirects(false)
                .ignoreHttpErrors(true)
                .maxBodySize(MAX_PAGE_BYTES) // Temu's product page is ~0.7 MB
                .execute()
            cookies.putAll(res.cookies())
            val location = res.header("Location")
            if (res.statusCode() !in 300..399 || location.isNullOrBlank()) return res
            url = toHttps(URL(URL(url), location).toString())
        }
        error("Too many redirects from $start")
    }

    private fun toHttps(url: String): String =
        if (url.startsWith("http://", ignoreCase = true)) "https://" + url.substring(7) else url

    private const val MAX_REDIRECTS = 6
    private const val MAX_PAGE_BYTES = 5 * 1024 * 1024

    const val TAG = "CachewShare"

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
