package dev.hloth.zaragoza_tarjeta_bus.card

import dev.hloth.zgztransport.Balance
import dev.hloth.zgztransport.CardDateTime
import dev.hloth.zgztransport.CardFormatException
import dev.hloth.zgztransport.CardId
import dev.hloth.zgztransport.CardType
import dev.hloth.zgztransport.Chip
import dev.hloth.zgztransport.Direction
import dev.hloth.zgztransport.Dump
import dev.hloth.zgztransport.JourneySummary
import dev.hloth.zgztransport.Route
import dev.hloth.zgztransport.Stop
import dev.hloth.zgztransport.Transaction
import dev.hloth.zgztransport.TransactionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private const val AVANZA_BLOCK_0 = "1D68C3A9BF880400C8000020000000AB"
private const val LAZO_BLOCK_0 = "0468C3A9BF12341802008100000023AA"

private fun hex(text: String) = ByteArray(text.length / 2) {
    text.substring(it * 2, it * 2 + 2).toInt(16).toByte()
}

private fun ride(route: Int, at: CardDateTime, sequence: Int, cardType: CardType) =
    Transaction.builder()
        .cardType(cardType)
        .amount(880)
        .consecutivePayments(1)
        .stop(Stop.Urban(500))
        .route(Route(route))
        .kind(TransactionKind.Journey(Direction.ONE))
        .dutyTrip(7)
        .createdAt(at)
        .sequence(sequence)
        .build()

private fun avanzaTopUp(): Dump = Dump.builder(Chip.CLASSIC_1K)
    .block(0, hex(AVANZA_BLOCK_0))
    .block(1, CardType.AVANZA_TOP_UP)
    .block(2, CardId.parse("BE123456"))
    .block(5, ride(22, CardDateTime.of(2026, 2, 14, 18, 45, 0), 2, CardType.AVANZA_TOP_UP))
    .block(8, Balance(4450))
    .block(9, Balance(4450))
    .block(28, ride(35, CardDateTime.of(2026, 2, 11, 8, 32, 0), 0, CardType.AVANZA_TOP_UP))
    .build()

private fun avanzaPersonal(): Dump = Dump.builder(Chip.CLASSIC_1K)
    .block(0, hex(AVANZA_BLOCK_0))
    .block(1, CardType.AVANZA_PERSONAL_UNLIMITED)
    .block(2, CardId.parse("BP987654"))
    .block(8, Balance(0))
    .block(9, Balance(0))
    .block(10, JourneySummary.personalBlock())
    .build()

private fun lazoTopUp(): Dump = Dump.builder(Chip.CLASSIC_4K)
    .block(0, hex(LAZO_BLOCK_0))
    .block(1, CardType.LAZO_TOP_UP)
    .block(2, CardId.parse("CT123456"))
    .block(8, Balance(600))
    .block(9, Balance(600))
    .build()

class CardReaderTest {

    @Test
    fun readsOnlyTheSectorsATopUpCardFills() {
        val blocks = FakeBlocks(avanzaTopUp(), CardType.AVANZA_TOP_UP)

        readTransportCard(blocks)

        assertEquals(listOf(0, 1, 2, 7, 8), blocks.sectorsAuthenticated)
        assertEquals(listOf(0, 1, 2, 5, 8, 9, 10, 28, 29, 30, 32, 33), blocks.readBlocks)
    }

    @Test
    fun authenticatesEachSectorOnceHoweverManyBlocksItHolds() {
        val blocks = FakeBlocks(avanzaTopUp(), CardType.AVANZA_TOP_UP)

        readTransportCard(blocks)

        assertEquals(blocks.sectorsAuthenticated.size, blocks.authenticated.size)
        assertEquals(listOf(8, 9, 10), blocks.readBlocks.filter { blocks.sectorOf(it) == 2 })
    }

    @Test
    fun readsProductSectorsAndSkipsTheSummaryOnAPersonalCard() {
        val blocks = FakeBlocks(avanzaPersonal(), CardType.AVANZA_PERSONAL_UNLIMITED)

        readTransportCard(blocks)

        assertTrue(blocks.readBlocks.containsAll(listOf(12, 13, 16, 17)))
        assertFalse(blocks.readBlocks.contains(10))
        assertEquals(listOf(0, 1, 2, 3, 4, 7, 8), blocks.sectorsAuthenticated)
    }

    @Test
    fun triesTheAvanzaSectorZeroKeyBeforeTheLazoOne() {
        val blocks = FakeBlocks(lazoTopUp(), CardType.LAZO_TOP_UP)

        readTransportCard(blocks)

        val sectorZeroAttempts = blocks.authenticated.filter { it.first == 0 }.map { it.second }
        assertEquals(2, sectorZeroAttempts.size)
        assertEquals(CardType.AVANZA_TOP_UP.keys(0).get(), sectorZeroAttempts[0])
        assertEquals(CardType.LAZO_TOP_UP.keys(0).get(), sectorZeroAttempts[1])
    }

    @Test
    fun probesSectorZeroWithOneCandidatePerDistinctKeySet() {
        val blocks = FakeBlocks(avanzaTopUp(), CardType.AVANZA_TOP_UP)

        readTransportCard(blocks)

        assertEquals(1, blocks.authenticated.count { it.first == 0 })
    }

    @Test
    fun failsWhenASectorDoesNotAuthenticate() {
        val blocks = FakeBlocks(avanzaTopUp(), CardType.AVANZA_TOP_UP)
        blocks.unauthenticatableSector = 7

        val failure = assertThrows(CardFormatException::class.java) { readTransportCard(blocks) }

        assertTrue(failure.message!!.contains("sector 7"))
    }

    @Test
    fun decodesTheCardTheBlocksHold() {
        val card = readTransportCard(FakeBlocks(avanzaTopUp(), CardType.AVANZA_TOP_UP))

        assertEquals(CardType.AVANZA_TOP_UP, card.cardType)
        assertEquals("BE123456", card.id.toString())
        assertEquals(4450L, card.balance.units())
        assertEquals("1D68C3A9", card.uid.toString())
        assertEquals(listOf(35, 22), card.transactions.map { it.route.id() })
    }

    @Test
    fun readsALazoCardOffItsOwnKeyTable() {
        val card = readTransportCard(FakeBlocks(lazoTopUp(), CardType.LAZO_TOP_UP))

        assertEquals(CardType.LAZO_TOP_UP, card.cardType)
        assertEquals(600L, card.balance.units())
        assertEquals(Chip.CLASSIC_4K, card.uid.chip())
    }
}
