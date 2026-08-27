package dev.hloth.zaragoza_tarjeta_bus.ui

import dev.hloth.zgztransport.CardDate
import dev.hloth.zgztransport.CardDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class CardFormatTest {

    @Test
    fun formatsATimestampTheCalendarAccepts() {
        val formatted = CardDateTime.of(2026, 2, 14, 18, 45, 30).formatted()

        assertTrue(formatted, formatted.contains("2026"))
        assertTrue(formatted, formatted.contains("18:45"))
        assertTrue(formatted, formatted.contains(" · "))
    }

    @Test
    fun fallsBackToThePrintedFormWhenTheMonthHasNoSuchDay() {
        val february30 = CardDateTime(CardDate(2026, 2, 30), LocalTime.of(8, 32, 0))

        assertEquals("2026-02-30 08:32:00", february30.formatted())
    }
}
