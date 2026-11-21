package app.cachew.share

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.jsoup.nodes.Document
import java.net.URI

/** What we could learn about a product. Any field may be missing. */
data class ProductInfo(
    val title: String? = null,
    val price: Double? = null,
    val imageUrl: String? = null,
    /** ISO code of [price] ("USD", "UAH"), when the page says. */
    val currency: String? = null,
)

/**
 * Turns a shared link + product page into name, price and photo.
 * Pure functions (no network, no Android), so they are unit-tested in ProductParserTest.
 *
 * Order of sources, from most to least reliable:
 *  1. JSON-LD "Product" (structured data most shops publish for Google)
 *  2. Open Graph meta tags: og:title, og:image, product:price:amount
 *  3. itemprop="price" and the page <title>
 *  4. a formatted price in the page's own data, e.g. Temu's "minOnSalePriceStr":"144.62₴"
 *  5. a price written in the shared text itself ("$64", "1 299 грн", "€12,50")
 */
object ProductParser {

    private val urlRegex = Regex("""https?://[^\s"'<>]+""")

    /** The first link in the shared text, without trailing punctuation. */
    fun extractUrl(text: String): String? =
        urlRegex.find(text)?.value?.trimEnd('.', ',', ')', ']', '!', '?', ';')

    /** A photo passed inside the link itself, e.g. Temu's "...goods.html?thumb_url=https%3A%2F%2Fimg..." */
    fun imageFromLink(url: String): String? {
        val query = runCatching { URI(url).rawQuery }.getOrNull() ?: return null
        return query.split('&').firstNotNullOfOrNull { part ->
            val (key, value) = part.split('=', limit = 2).takeIf { it.size == 2 } ?: return@firstNotNullOfOrNull null
            if (key in IMAGE_PARAMS) java.net.URLDecoder.decode(value, "UTF-8").takeIf { it.startsWith("http") } else null
        }
    }

    // Not Temu's "share_img": that's a promo card, not the product photo.
    private val IMAGE_PARAMS = setOf("thumb_url", "image", "img")

    /** Shared text minus the link: many shops put the product name there. */
    fun titleFromShareText(text: String): String? {
        val cleaned = urlRegex.replace(text, " ").replace(Regex("""\s+"""), " ").trim()
        return cleaned.takeIf { it.length in 3..120 }
    }

    fun parse(doc: Document): ProductInfo {
        val ld = fromJsonLd(doc)
        val og = ProductInfo(
            title = meta(doc, "og:title"),
            price = (meta(doc, "product:price:amount") ?: meta(doc, "og:price:amount"))?.let(::parseNumber),
            imageUrl = doc.selectFirst("meta[property=og:image], meta[name=og:image]")?.absUrl("content")?.ifBlank { null },
            currency = meta(doc, "product:price:currency") ?: meta(doc, "og:price:currency"),
        )
        val basic = ProductInfo(
            title = doc.title().ifBlank { null },
            price = doc.selectFirst("[itemprop=price]")?.let { el ->
                (el.attr("content").ifBlank { el.text() }).let(::parseNumber)
            },
            currency = doc.selectFirst("[itemprop=priceCurrency]")?.let { it.attr("content").ifBlank { it.text() } }?.ifBlank { null },
        )
        // Pick the price together with the currency of the same source.
        val priced = listOf(ld, og, basic).firstOrNull { it.price != null } ?: fromEmbeddedData(doc.html())
        return ProductInfo(
            title = (ld.title ?: og.title ?: basic.title)?.let { cleanTitle(it, doc.location()) },
            price = priced.price,
            imageUrl = (ld.imageUrl ?: og.imageUrl)?.let { resolve(doc.location(), it) },
            currency = priced.currency?.uppercase(),
        )
    }

    // ---- Price hidden in the page's script data (shops that render with JavaScript) ----

    private val embeddedPrice = Regex(""""(?:minOnSalePriceStr|salePriceStr|normalPriceStr|priceStr|formattedPrice)"\s*:\s*"([^"]{1,30})"""")
    private val embeddedCurrency = Regex(""""(?:currencyCode|priceCurrency|currency)"\s*:\s*"([A-Z]{3})"""")

    /** Temu-style pages: "minOnSalePriceStr":"144.62₴" and "currencyCode":"UAH" inside a script. */
    fun fromEmbeddedData(html: String): ProductInfo {
        val text = embeddedPrice.find(html)?.groupValues?.get(1) ?: return ProductInfo()
        val price = priceFromText(text) ?: parseNumber(text.filter { it.isDigit() || it == '.' || it == ',' })
        return ProductInfo(price = price, currency = embeddedCurrency.find(html)?.groupValues?.get(1) ?: currencyFromSymbol(text))
    }

    private fun currencyFromSymbol(text: String): String? = when {
        '$' in text -> "USD"
        '€' in text -> "EUR"
        '₴' in text || "грн" in text -> "UAH"
        else -> null
    }

    // ---- Title ---------------------------------------------------------------------------

    /**
     * "  сумка   led нічник - Temu Ukraine" → "сумка led нічник".
     * Squeezes spaces and drops a " - Shop name" / " | Shop name" tail when it names the website.
     */
    fun cleanTitle(raw: String, pageUrl: String): String {
        var t = raw.replace(Regex("""\s+"""), " ").trim()
        val brand = runCatching { URI(pageUrl).host }.getOrNull()
            ?.removePrefix("www.")?.split('.')?.firstOrNull { it.length > 2 && it != "com" }
        val tail = Regex("""\s+[-|–—:]\s+([^-|–—]+)$""").find(t)
        if (brand != null && tail != null && tail.groupValues[1].contains(brand, ignoreCase = true)) {
            t = t.substring(0, tail.range.first).trim()
        }
        return t
    }

    // ---- JSON-LD ------------------------------------------------------------------------

    private fun fromJsonLd(doc: Document): ProductInfo {
        for (script in doc.select("script[type=application/ld+json]")) {
            val root = runCatching { Json.parseToJsonElement(script.data()) }.getOrNull() ?: continue
            val product = findProduct(root) ?: continue
            return ProductInfo(
                title = product.str("name"),
                price = priceFromOffers(product["offers"]),
                imageUrl = imageFrom(product["image"]),
                currency = currencyFromOffers(product["offers"]),
            )
        }
        return ProductInfo()
    }

    /** Products can sit at the top, inside an array, or inside "@graph". */
    private fun findProduct(el: JsonElement): JsonObject? = when (el) {
        is JsonArray -> el.firstNotNullOfOrNull { findProduct(it) }
        is JsonObject -> when {
            isProductType(el["@type"]) -> el
            el["@graph"] != null -> findProduct(el["@graph"]!!)
            else -> null
        }
        else -> null
    }

    private fun isProductType(type: JsonElement?): Boolean = when (type) {
        is JsonPrimitive -> type.contentOrNull.equals("Product", ignoreCase = true)
        is JsonArray -> type.any { isProductType(it) }
        else -> false
    }

    private fun priceFromOffers(offers: JsonElement?): Double? = when (offers) {
        is JsonArray -> offers.firstNotNullOfOrNull { priceFromOffers(it) }
        is JsonObject -> (offers.str("price") ?: offers.str("lowPrice"))?.let(::parseNumber)
            ?: priceFromOffers(offers["priceSpecification"])
        else -> null
    }

    private fun currencyFromOffers(offers: JsonElement?): String? = when (offers) {
        is JsonArray -> offers.firstNotNullOfOrNull { currencyFromOffers(it) }
        is JsonObject -> offers.str("priceCurrency") ?: currencyFromOffers(offers["priceSpecification"])
        else -> null
    }

    private fun imageFrom(image: JsonElement?): String? = when (image) {
        is JsonPrimitive -> image.contentOrNull
        is JsonArray -> image.firstNotNullOfOrNull { imageFrom(it) }
        is JsonObject -> image.str("url") ?: image.str("contentUrl")
        else -> null
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun meta(doc: Document, name: String): String? =
        doc.selectFirst("meta[property=$name], meta[name=$name]")?.attr("content")?.ifBlank { null }

    private fun resolve(base: String, url: String): String =
        runCatching { if (base.isBlank()) url else URI(base).resolve(url).toString() }.getOrDefault(url)

    // ---- Prices --------------------------------------------------------------------------

    private const val NUM = """\d[\d\s  .,]*"""
    private val priceBefore = Regex("""(?:\$|€|₴|USD|EUR|UAH)\s?($NUM)""")
    private val priceAfter = Regex("""($NUM)\s?(?:грн|₴|€|\$|USD|EUR|UAH)""")

    /** Finds a price written in text: "$64", "1 299 грн", "€12,50", "₴2 499,99". */
    fun priceFromText(text: String): Double? =
        (priceBefore.find(text) ?: priceAfter.find(text))?.groupValues?.get(1)?.let(::parseNumber)

    /**
     * "1 299" → 1299, "1,299.99" → 1299.99, "12,50" → 12.5, "1.299" → 1299, "64.00" → 64.
     * Spaces are thousands separators; with both '.' and ',' the last one is the decimal point;
     * a single separator followed by exactly 3 digits is a thousands separator.
     */
    fun parseNumber(raw: String): Double? {
        var s = raw.trim().replace(Regex("""[\s  ]"""), "").trimEnd('.', ',')
        if (s.isEmpty()) return null
        val lastDot = s.lastIndexOf('.')
        val lastComma = s.lastIndexOf(',')
        s = when {
            lastDot >= 0 && lastComma >= 0 -> {
                val decimal = if (lastDot > lastComma) '.' else ','
                val thousands = if (decimal == '.') ',' else '.'
                s.replace(thousands.toString(), "").replace(decimal, '.')
            }
            lastDot >= 0 || lastComma >= 0 -> {
                val sep = if (lastDot >= 0) '.' else ','
                val after = s.length - s.lastIndexOf(sep) - 1
                val count = s.count { it == sep }
                if (after == 3 || count > 1) s.replace(sep.toString(), "") else s.replace(sep, '.')
            }
            else -> s
        }
        return s.toDoubleOrNull()?.takeIf { it > 0 }
    }
}
