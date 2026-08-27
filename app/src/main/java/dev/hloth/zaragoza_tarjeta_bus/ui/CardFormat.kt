package dev.hloth.zaragoza_tarjeta_bus.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.hloth.zaragoza_tarjeta_bus.R
import dev.hloth.zgztransport.Balance
import dev.hloth.zgztransport.CardDateTime
import dev.hloth.zgztransport.CardType
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Currency

private const val DATE_PATTERN = "d MMM yyyy"
private const val TIME_PATTERN = "HH:mm"

@Composable
internal fun CardType.label(): String = stringResource(
    when (this) {
        CardType.AVANZA_TOP_UP -> R.string.card_type_top_up
        CardType.AVANZA_PERSONAL_UNLIMITED -> R.string.card_type_personal
        CardType.LAZO_TOP_UP -> R.string.card_type_lazo
    }
)

internal fun Balance.formatted(): String = NumberFormat.getCurrencyInstance()
    .apply { currency = Currency.getInstance("EUR") }
    .format(euros())

internal fun CardDateTime.formatted(): String {
    val calendarDate = date().toLocalDate().orElse(null) ?: return toString()
    return "${calendarDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))} · " +
        time().format(DateTimeFormatter.ofPattern(TIME_PATTERN))
}
