package dev.hloth.zaragoza_tarjeta_bus.card

import dev.hloth.zgztransport.Route

enum class TransportMode { BUS, TRAM, CERCANIAS, OTHER_OPERATOR }

private const val OFF_BOARD_ROUTE = 0
private val CERCANIAS_ROUTES = setOf(169)
private val OTHER_OPERATOR_ROUTES = setOf(152, 251)

fun Route.mode(): TransportMode? = when (id()) {
    OFF_BOARD_ROUTE -> null
    Route.TRAM.id() -> TransportMode.TRAM
    in CERCANIAS_ROUTES -> TransportMode.CERCANIAS
    in OTHER_OPERATOR_ROUTES -> TransportMode.OTHER_OPERATOR
    else -> TransportMode.BUS
}
