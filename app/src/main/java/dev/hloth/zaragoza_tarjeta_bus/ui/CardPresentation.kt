package dev.hloth.zaragoza_tarjeta_bus.ui

import androidx.annotation.StringRes
import dev.hloth.zaragoza_tarjeta_bus.R
import dev.hloth.zaragoza_tarjeta_bus.card.TransportCard
import dev.hloth.zaragoza_tarjeta_bus.card.TransportMode
import dev.hloth.zaragoza_tarjeta_bus.card.mode
import dev.hloth.zgztransport.Balance
import dev.hloth.zgztransport.CardType
import dev.hloth.zgztransport.Stop
import dev.hloth.zgztransport.Transaction
import dev.hloth.zgztransport.TransactionKind

private const val ADDED = "+"
private const val SPENT = "−"

data class Label(@param:StringRes val id: Int, val args: List<String> = emptyList())

data class ActivityRow(
    val badge: String,
    val title: Label,
    val place: Label?,
    val fare: Label?,
    val time: String,
    val amount: String?,
)

sealed interface CardScreen {
    data class Details(
        val cardType: Label,
        val balance: String,
        val cardId: String?,
        val notice: Label?,
        val activity: List<ActivityRow>,
    ) : CardScreen

    data class Unsupported(val cardType: Label) : CardScreen
}

fun TransportCard.screen(): CardScreen = when (cardType) {
    CardType.AVANZA_TOP_UP -> CardScreen.Details(
        cardType = cardType.label(),
        balance = balance.formatted(),
        cardId = id?.toString(),
        notice = if (warnings.isEmpty()) null else Label(R.string.card_partially_read),
        activity = transactions.sortedByDescending { it.createdAt() }.map { it.row() },
    )

    else -> CardScreen.Unsupported(cardType.label())
}

private fun CardType.label(): Label = Label(
    when (this) {
        CardType.AVANZA_TOP_UP -> R.string.card_type_top_up
        CardType.AVANZA_PERSONAL_UNLIMITED -> R.string.card_type_personal
        CardType.LAZO_TOP_UP -> R.string.card_type_lazo
    }
)

private fun Transaction.row(): ActivityRow {
    val topUp = kind() is TransactionKind.TopUp
    val mode = route().mode()
    return ActivityRow(
        badge = if (topUp) ADDED else route().toString(),
        title = if (topUp) Label(R.string.transaction_top_up) else journeyTitle(mode),
        place = place(),
        fare = if (topUp) null else fare(mode),
        time = createdAt().formatted(),
        amount = amount().takeIf { it > 0 }
            ?.let { (if (topUp) ADDED else SPENT) + Balance(it.toLong()).formatted() },
    )
}

private fun Transaction.journeyTitle(mode: TransportMode?): Label {
    val route = route().toString()
    return when (mode) {
        TransportMode.BUS -> Label(R.string.transaction_bus_ride_line, listOf(route))
        TransportMode.TRAM -> Label(R.string.transaction_tram_ride_line, listOf(route))
        TransportMode.CERCANIAS -> Label(R.string.transaction_cercanias_ride)
        TransportMode.OTHER_OPERATOR, null -> Label(R.string.transaction_journey_line, listOf(route))
    }
}

private fun Transaction.place(): Label? = (stop() as? Stop.Tram)
    ?.let { Label(R.string.transaction_tram_stop, listOf(it.number().toString())) }

private fun Transaction.fare(mode: TransportMode?): Label? = when {
    !isFree() -> null
    isTransfer() -> Label(R.string.transaction_transfer)
    mode == TransportMode.CERCANIAS && isCheckOut() -> Label(R.string.transaction_check_out)
    else -> Label(R.string.transaction_free)
}
