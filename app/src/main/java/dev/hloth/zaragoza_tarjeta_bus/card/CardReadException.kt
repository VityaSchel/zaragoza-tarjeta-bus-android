package dev.hloth.zaragoza_tarjeta_bus.card

sealed class CardReadException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class UnknownCardType(val code: String) : CardReadException("unknown card type $code")

    class NotATransportCard(message: String) : CardReadException(message)

    class Locked(val sector: Int) : CardReadException("cannot authenticate sector $sector")

    class Unreadable(message: String, cause: Throwable? = null) : CardReadException(message, cause)
}
