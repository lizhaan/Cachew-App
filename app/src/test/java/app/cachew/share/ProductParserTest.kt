package app.cachew.share

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductParserTest {

    @Test fun extractsUrlFromShareText() {
        assertEquals(
            "https://shop.example/p/123?ref=app",
            ProductParser.extractUrl("Look at this! https://shop.example/p/123?ref=app."),
        )
        assertNull(ProductParser.extractUrl("no link here"))
    }

    @Test fun titleFromShareTextDropsLink() {
        assertEquals("Chunky knit sweater", ProductParser.titleFromShareText("Chunky knit sweater https://x.y/z"))
    }

    @Test fun numbers() {
        assertEquals(64.0, ProductParser.parseNumber("64.00")!!, 0.001)
        assertEquals(1299.0, ProductParser.parseNumber("1 299")!!, 0.001)
        assertEquals(1299.99, ProductParser.parseNumber("1,299.99")!!, 0.001)
        assertEquals(12.5, ProductParser.parseNumber("12,50")!!, 0.001)
        assertEquals(1299.0, ProductParser.parseNumber("1.299")!!, 0.001)
        assertEquals(2499.99, ProductParser.parseNumber("2 499,99")!!, 0.001)
    }

    @Test fun priceFromText() {
        assertEquals(64.0, ProductParser.priceFromText("Sweater for $64!")!!, 0.001)
        assertEquals(1299.0, ProductParser.priceFromText("Ціна 1 299 грн, доставка")!!, 0.001)
        assertEquals(12.5, ProductParser.priceFromText("€12,50 only")!!, 0.001)
        assertEquals(2499.99, ProductParser.priceFromText("₴2 499,99")!!, 0.001)
        assertNull(ProductParser.priceFromText("no price"))
    }

    @Test fun jsonLdProductWins() {
        val html = """
            <html><head><title>Shop page</title>
            <meta property="og:title" content="OG title">
            <script type="application/ld+json">
              {"@context":"https://schema.org","@graph":[
                {"@type":"BreadcrumbList"},
                {"@type":"Product","name":"Chunky knit sweater","image":["/img/sweater.jpg"],
                 "offers":{"@type":"Offer","price":"64.00","priceCurrency":"USD"}}
              ]}
            </script></head><body></body></html>
        """.trimIndent()
        val info = ProductParser.parse(Jsoup.parse(html, "https://shop.example/p/1"))
        assertEquals("Chunky knit sweater", info.title)
        assertEquals(64.0, info.price!!, 0.001)
        assertEquals("https://shop.example/img/sweater.jpg", info.imageUrl)
    }

    @Test fun openGraphFallback() {
        val html = """
            <html><head><title>Page title</title>
            <meta property="og:title" content="Linen shirt">
            <meta property="og:image" content="https://cdn.example/shirt.jpg">
            <meta property="product:price:amount" content="29.90">
            </head></html>
        """.trimIndent()
        val info = ProductParser.parse(Jsoup.parse(html, "https://shop.example/p/2"))
        assertEquals("Linen shirt", info.title)
        assertEquals(29.9, info.price!!, 0.001)
        assertEquals("https://cdn.example/shirt.jpg", info.imageUrl)
    }

    @Test fun itempropAndTitleFallback() {
        val html = """<html><head><title>Mug | Shop</title></head>
            <body><span itemprop="price" content="1 299">1 299 грн</span></body></html>"""
        val info = ProductParser.parse(Jsoup.parse(html, "https://shop.example/p/3"))
        assertEquals("Mug | Shop", info.title)
        assertEquals(1299.0, info.price!!, 0.001)
        assertNull(info.imageUrl)
    }
}
