package app.cachew.data

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

/** Formats prices the way the mockups do: "$64", "$64.50", "₴1,299". */
object Money {

    /** Currency for new items: US dollars (US market), whatever the phone's region. */
    const val DEFAULT_CURRENCY = "USD"

    // Locale.US so dollars show as "$" (not "US$") on phones set to other regions.
    fun symbol(currencyCode: String, locale: Locale = Locale.US): String =
        runCatching { Currency.getInstance(currencyCode).getSymbol(locale) }.getOrDefault(currencyCode)

    fun format(amount: Double, currencyCode: String, locale: Locale = Locale.US): String {
        val pattern = if (amount % 1.0 == 0.0) "#,##0" else "#,##0.00"
        val number = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US)).format(amount)
        return symbol(currencyCode, locale) + number
    }

    /**
     * A shop price in another currency → dollars, at the fixed rates in [Rules.USD_PER_UNIT],
     * rounded to cents (₴144.62 → $3.48). Null if we don't know the currency.
     * Approximate on purpose: there's no backend for live rates, and the app is for the US.
     */
    fun toUsd(amount: Double, currencyCode: String): Double? {
        val rate = Rules.USD_PER_UNIT[currencyCode.uppercase()] ?: return null
        return Math.round(amount * rate * 100) / 100.0
    }

    /** Parses what the user typed: "64", "64.5", "64,50", "1 299". Null if it isn't a positive number. */
    fun parse(text: String): Double? =
        text.replace(" ", "").replace(' '.toString(), "").replace(',', '.')
            .toDoubleOrNull()?.takeIf { it > 0 }
}
