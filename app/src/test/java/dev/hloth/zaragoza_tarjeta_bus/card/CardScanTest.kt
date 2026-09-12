package dev.hloth.zaragoza_tarjeta_bus.card

import dev.hloth.zgztransport.Balance
import dev.hloth.zgztransport.CardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

private val card = TransportCard(
    cardType = CardType.AVANZA_TOP_UP,
    balance = Balance(4450),
    uid = null,
    id = null,
    transactions = emptyList(),
    journeySummary = null,
    products = emptyList(),
    warnings = emptyList(),
)

private class Attempts(private vararg val outcomes: () -> TransportCard) {
    var reads = 0
    var resets = 0

    fun read(): TransportCard = outcomes[minOf(reads++, outcomes.size - 1)]()

    fun scan(): CardScan = scanWithRetries(read = ::read, reset = { resets++ })
}

class CardScanTest {

    @Test
    fun retriesAnUnreadableCardUntilItReads() {
        val attempts = Attempts({ throw CardReadException.Unreadable("noise") }, { card })

        assertEquals(CardScan.Read(card), attempts.scan())
        assertEquals(2, attempts.reads)
        assertEquals(1, attempts.resets)
    }

    @Test
    fun givesUpAfterThreeFailedAttempts() {
        val noise = CardReadException.Unreadable("noise")
        val attempts = Attempts({ throw noise })

        val scan = attempts.scan()

        assertTrue(scan is CardScan.Failed)
        assertSame(noise, (scan as CardScan.Failed).cause)
        assertEquals(3, attempts.reads)
    }

    @Test
    fun reportsACardNoKnownKeyOpensAsNotATransportCard() {
        val attempts = Attempts({ throw CardReadException.Locked(0) })

        assertEquals(CardScan.NotATransportCard, attempts.scan())
        assertEquals(3, attempts.reads)
    }

    @Test
    fun reportsALockedLaterSectorAsAFailedRead() {
        val attempts = Attempts({ throw CardReadException.Locked(7) })

        assertTrue(attempts.scan() is CardScan.Failed)
    }

    @Test
    fun doesNotRetryAnUnknownCardTypeOrAStaleTag() {
        val unknown = Attempts({ throw CardReadException.UnknownCardType("ABCDEF") })
        val stale = Attempts({ throw SecurityException("tag is out of date") })

        assertEquals(CardScan.Unsupported("ABCDEF"), unknown.scan())
        assertEquals(CardScan.Moved, stale.scan())
        assertEquals(1, unknown.reads)
        assertEquals(1, stale.reads)
    }
}
