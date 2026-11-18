package app.cachew.data

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

/** Formats prices the way the mockups do: "$64", "$64.50", "₴1,299". */
object Money {

    /** Currency for new items until Me → settings exist: the phone's region, else US dollars. */
    fun defaultCurrency(locale: Locale = Locale.getDefault()): String =
        runCatching { Currency.getInstance(locale).currencyCode }.getOrNull() ?: "USD"

    fun symbol(currencyCode: String, locale: Locale = Locale.getDefault()): String =
        runCatching { Currency.getInstance(currencyCode).getSymbol(locale) }.getOrDefault(currencyCode)

    fun format(amount: Double, currencyCode: String, locale: Locale = Locale.getDefault()): String {
        val pattern = if (amount % 1.0 == 0.0) "#,##0" else "#,##0.00"
        val number = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US)).format(amount)
        return symbol(currencyCode, locale) + number
    }

    /** Parses what the user typed: "64", "64.5", "64,50", "1 299". Null if it isn't a positive number. */
    fun parse(text: String): Double? =
        text.replace(" ", "").replace(' '.toString(), "").replace(',', '.')
            .toDoubleOrNull()?.takeIf { it > 0 }
}
