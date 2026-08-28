package dev.hloth.zaragoza_tarjeta_bus.card

import dev.hloth.zgztransport.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransportModeTest {

    @Test
    fun readsTheModeOfTheRoutesTheSpecNames() {
        assertEquals(TransportMode.TRAM, Route(210).mode())
        assertEquals(TransportMode.CERCANIAS, Route(169).mode())
        assertEquals(TransportMode.OTHER_OPERATOR, Route(152).mode())
        assertEquals(TransportMode.OTHER_OPERATOR, Route(251).mode())
    }

    @Test
    fun treatsEveryOtherRouteAsAnUrbanBus() {
        val named = setOf(0, 152, 169, 210, 251)
        val misread = (0..255).filterNot { it in named }.filter { Route(it).mode() != TransportMode.BUS }

        assertEquals(emptyList<Int>(), misread)
    }

    @Test
    fun leavesTheOffBoardTopUpRouteWithoutAMode() {
        assertNull(Route(0).mode())
    }
}
