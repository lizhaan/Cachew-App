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

    /** The page once it contains product data (og:title / JSON-LD), or null on timeout. */
    suspend fun load(context: Context, url: String): Page? = withTimeoutOrNull(TIMEOUT_MS) {
        suspendCancellableCoroutine { cont ->
            val main = Handler(Looper.getMainLooper())
            val web = createWebView(context)
            var done = false

            fun finish(page: Page?) {
                if (done) return
                done = true
                main.removeCallbacksAndMessages(null)
                web.stopLoading()
                web.destroy()
                if (cont.isActive) cont.resume(page)
            }

            // Check the page every second: the bot check reloads it, so "page finished" fires
            // more than once and only the last one has the product.
            val poll = object : Runnable {
                override fun run() {
                    if (done) return
                    web.evaluateJavascript("document.documentElement.outerHTML") { raw ->
                        val html = runCatching { JSONTokener(raw).nextValue() as? String }.getOrNull()
                        if (html != null && hasProductData(html)) {
                            if (BuildConfig.DEBUG) Log.d(ProductFetcher.TAG, "webview got page, chars=${html.length}")
                            finish(Page(html, web.url ?: url))
                        } else {
                            if (BuildConfig.DEBUG) Log.d(ProductFetcher.TAG, "webview waiting: url=${web.url?.take(60)} title='${web.title}' chars=${html?.length}")
                            main.postDelayed(this, POLL_MS)
                        }
                    }
                }
            }
            web.loadUrl(url)
            main.postDelayed(poll, POLL_MS)
            cont.invokeOnCancellation { main.post { finish(null) } }
        }
    }

    private fun hasProductData(html: String): Boolean =
        "og:title" in html || "application/ld+json" in html

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
