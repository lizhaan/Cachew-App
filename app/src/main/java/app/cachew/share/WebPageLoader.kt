package app.cachew.share

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import app.cachew.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONTokener
import kotlin.coroutines.resume

/**
 * Plan B for shops that hide product data from simple requests (Temu answers with a JavaScript
 * bot check). Loads the page in an invisible WebView — Android's built-in browser engine, so the
 * check runs like in Chrome — and returns the finished page's HTML for ProductParser.
 * Nothing is shown to the user; the WebView is destroyed afterwards.
 */
object WebPageLoader {

    private const val TIMEOUT_MS = 12_000L
    private const val POLL_MS = 1_000L

    /** The finished page: its HTML and the address it ended up on (after redirects). */
    class Page(val html: String, val url: String)

    /**
     * The page once it has a name *and* a price, or — when the time runs out — the best page seen
     * (name and photo but no price). Null if nothing useful loaded at all.
     */
    suspend fun load(context: Context, url: String): Page? = withTimeoutOrNull(TIMEOUT_MS + 3_000L) {
        suspendCancellableCoroutine { cont ->
            val main = Handler(Looper.getMainLooper())
            val web = createWebView(context)
            var done = false
            var best: Page? = null // has a name, still waiting for the price

            fun finish(page: Page?) {
                if (done) return
                done = true
                main.removeCallbacksAndMessages(null)
                web.stopLoading()
                web.destroy()
                if (BuildConfig.DEBUG && page != null) {
                    Log.d(ProductFetcher.TAG, "webview done, chars=${page.html.length} price=${hasPrice(page.html)}")
                    // Debug only: keep the page to tune the parser (adb shell run-as app.cachew cat cache/last_page.html).
                    runCatching { java.io.File(context.cacheDir, "last_page.html").writeText(page.html) }
                }
                if (cont.isActive) cont.resume(page)
            }

            // Check the page every second. Temu first sends a page with only the name and photo;
            // the price data arrives a moment later from JavaScript, so we wait for it.
            val poll = object : Runnable {
                override fun run() {
                    if (done) return
                    web.evaluateJavascript("document.documentElement.outerHTML") { raw ->
                        val html = runCatching { JSONTokener(raw).nextValue() as? String }.getOrNull()
                        when {
                            html != null && hasName(html) && hasPrice(html) -> finish(Page(html, web.url ?: url))
                            else -> {
                                if (html != null && hasName(html)) best = Page(html, web.url ?: url)
                                main.postDelayed(this, POLL_MS)
                            }
                        }
                    }
                }
            }
            web.loadUrl(url)
            main.postDelayed(poll, POLL_MS)
            main.postDelayed({ finish(best) }, TIMEOUT_MS) // out of time: keep what we have
            cont.invokeOnCancellation { main.post { finish(null) } }
        }
    }

    private fun hasName(html: String): Boolean =
        "og:title" in html || "application/ld+json" in html

    /** Any of the places ProductParser finds a price in. */
    private fun hasPrice(html: String): Boolean =
        PRICE_MARKERS.any { it in html }

    private val PRICE_MARKERS = listOf(
        "minOnSalePriceStr", "salePriceStr", "priceStr", "formattedPrice",
        "product:price:amount", "og:price:amount", "itemprop=\"price\"", "\"offers\"",
    )

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(context: Context) = WebView(context).apply {
        settings.javaScriptEnabled = true // the bot check is JavaScript
        settings.domStorageEnabled = true
        settings.blockNetworkImage = true // we only need the HTML
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                when (request.url.scheme) {
                    "https" -> false
                    // Temu redirects through plain http, which Android blocks: go to the https address instead.
                    "http" -> {
                        view.loadUrl(request.url.buildUpon().scheme("https").build().toString())
                        true
                    }
                    // Stay on web pages: the shop may try to jump into its app (temu://...).
                    else -> true
                }
        }
    }
}
