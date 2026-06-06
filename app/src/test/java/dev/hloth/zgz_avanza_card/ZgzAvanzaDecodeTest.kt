package dev.hloth.zgz_avanza_card

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ZgzAvanzaDecodeTest {
    @Test
    fun decodesTopUpCardType() {
        val block = hexToBytes("02699F000000000000000000000000F4")
        assertEquals(CardType.TOP_UP, ZgzAvanza.decodeCardType(block))
    }

    @Test
    fun decodesPersonalUnlimitedCardType() {
        // 0A ^ 97 ^ 75 = E8
        val block = hexToBytes("0A9775000000000000000000000000E8")
        assertEquals(CardType.PERSONAL_UNLIMITED, ZgzAvanza.decodeCardType(block))
    }

    @Test
    fun rejectsCardTypeWithBadChecksum() {
        val block = hexToBytes("02699F00000000000000000000000000")
        assertThrows(AvanzaCardInvalidException::class.java) {
            ZgzAvanza.decodeCardType(block)
        }
    }

    @Test
    fun rejectsUnknownCardType() {
        // DE AD BE, checksum DE ^ AD ^ BE = CD
        val block = hexToBytes("DEADBE000000000000000000000000CD")
        assertThrows(AvanzaCardInvalidException::class.java) {
            ZgzAvanza.decodeCardType(block)
        }
    }

    @Test
    fun decodesDateFromSpecExample() {
        assertEquals(LocalDate.of(2026, 2, 14), ZgzAvanza.decodeDate(0x34, 0x4E))
    }

    @Test
    fun decodesDateFrom2021Dump() {
        assertEquals(LocalDate.of(2021, 6, 28), ZgzAvanza.decodeDate(0x2A.toByte(), 0xDC.toByte()))
    }

    @Test
    fun rejectsImpossibleDate() {
        assertThrows(AvanzaCardInvalidException::class.java) {
            ZgzAvanza.decodeDate(0x00, 0x00)
        }
    }

    @Test
    fun decodesRideFromSpecBlock5() {
        val block = hexToBytes("020002260180CE16010D3454102D2403")
        val tx = ZgzAvanza.decodeTransaction(block)!!
        assertEquals(TransactionKind.RIDE, tx.kind)
        assertEquals(22, tx.line)
        assertEquals(1, tx.direction)
        assertEquals(LocalDateTime.of(2026, 2, 20, 16, 45, 36), tx.dateTime)
    }

    @Test
    fun decodesTopUpTransaction() {
        val block = hexToBytes("02001388001F2C0008003454102D2400")
        val tx = ZgzAvanza.decodeTransaction(block)!!
        assertEquals(TransactionKind.TOP_UP, tx.kind)
        assertNull(tx.line)
        assertNull(tx.direction)
        assertEquals(LocalDateTime.of(2026, 2, 20, 16, 45, 36), tx.dateTime)
    }

    @Test
    fun ignoresEmptyTransactionBlock() {
        assertNull(ZgzAvanza.decodeTransaction(hexToBytes("00000000000000000000000000000000")))
    }

    @Test
    fun ignoresBlockWithInvalidTime() {
        val block = hexToBytes("020002260180CE16010D3454992D2400")
        assertNull(ZgzAvanza.decodeTransaction(block))
    }
}
