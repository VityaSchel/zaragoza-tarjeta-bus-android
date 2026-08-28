package dev.hloth.zaragoza_tarjeta_bus.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.hloth.zgztransport.Balance
import dev.hloth.zgztransport.CardDate
import dev.hloth.zgztransport.CardDateTime
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Currency

private const val DATE_PATTERN = "d MMM yyyy"
private const val TIME_PATTERN = "HH:mm"

@Composable
internal fun Label.text(): String = stringResource(id, *args.toTypedArray())

internal fun Balance.formatted(): String = NumberFormat.getCurrencyInstance()
    .apply { currency = Currency.getInstance("EUR") }
    .format(euros())

internal fun CardDate.formatted(): String {
    val calendarDate = toLocalDate().orElse(null) ?: return toString()
    return calendarDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))
}

internal fun CardDateTime.formatted(): String {
    val calendarDate = date().toLocalDate().orElse(null) ?: return toString()
    return "${calendarDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))} · " +
        time().format(DateTimeFormatter.ofPattern(TIME_PATTERN))
}
