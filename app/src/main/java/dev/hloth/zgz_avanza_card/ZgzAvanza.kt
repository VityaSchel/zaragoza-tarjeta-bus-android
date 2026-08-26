package dev.hloth.zaragoza_tarjeta_bus

import java.time.LocalDate

// Reference, notes, JS implementation: https://git.hloth.dev/hloth/zgz-transport
// TODO: migrate to a separate Java/Kotlin library

class ZgzTransport {
    companion object {
        fun decodeId(block: ByteArray): String {
            val cardIdPrefixAscii = block.copyOfRange(0, 2)
            val cardIdPrefix = String(cardIdPrefixAscii)
            var cardIdNumber = block.copyOfRange(2, 15)
            val cardIdChecksumA = block[15]
            val cardIdChecksumB = cardIdPrefixAscii.fold(0) { acc, b -> acc xor b.toInt() } xor
                    cardIdNumber.fold(0) { acc, b -> acc xor b.toInt() }
            if (cardIdChecksumA.toInt() != cardIdChecksumB) {
                throw TransportCardInvalidException("Card ID checksum mismatch: expected $cardIdChecksumB, got $cardIdChecksumA")
            }
            while (cardIdNumber.last().toInt() == 0 && cardIdNumber.size > 3) {
                cardIdNumber = cardIdNumber.copyOfRange(0, cardIdNumber.size - 1)
            }

            return cardIdPrefix + bytesToHex(cardIdNumber)
        }

        fun decodeBalance(block: ByteArray): Long {
            if (block.size != 16) {
                throw TransportCardInvalidException("Invalid balance block length: expected 16 bytes, got ${block.size}")
            }

            val le = block.copyOfRange(0, 4)
            val aComplement = block.copyOfRange(4, 8)

            for (i in 0 until 4) {
                if (le[i] != block[8 + i]) {
                    throw TransportCardInvalidException("Invalid balance block: bytes 0-3 and 8-11 do not match")
                }
            }

            var balance = 0L
            for (i in 0 until 4) {
                balance += (le[i].toLong() and 0xFFL) shl (i * 8)
            }

            for (i in 0 until 4) {
                val expectedComplement = (le[i].toInt() xor 0xFF).toByte()
                if (aComplement[i] != expectedComplement) {
                    throw TransportCardInvalidException("Invalid balance block: bytes 4-7 are not the complement of bytes 0-3")
                }
            }

            return balance
        }

        fun decodeCardType(block: ByteArray): CardType {
            if (block.size != 16) {
                throw TransportCardInvalidException("Invalid card type block length: expected 16 bytes, got ${block.size}")
            }

            val checksum = block.copyOfRange(0, 15).fold(0) { acc, b -> acc xor (b.toInt() and 0xFF) }
            if (checksum != (block[15].toInt() and 0xFF)) {
                throw TransportCardInvalidException("Card type checksum mismatch")
            }

            return when (val type = bytesToHex(block.copyOfRange(0, 3))) {
                "02699F" -> CardType.TOP_UP
                "0A9775" -> CardType.PERSONAL_UNLIMITED
                else -> throw TransportCardInvalidException("Unknown card type: $type")
            }
        }

        fun decodeDate(high: Byte, low: Byte): LocalDate {
            val value = ((high.toInt() and 0xFF) shl 8) or (low.toInt() and 0xFF)
            val year = 2000 + ((value ushr 9) and 0x7F)
            val month = (value ushr 5) and 0x0F
            val day = value and 0x1F
            if (month !in 1..12 || day !in 1..31) {
                throw TransportCardInvalidException("Invalid packed date: $year-$month-$day")
            }
            return LocalDate.of(year, month, day)
        }

        fun decodeTransaction(block: ByteArray): TransportTransaction? {
            if (block.size != 16 || block.all { it.toInt() == 0 }) {
                return null
            }

            val marker = bytesToHex(block.copyOfRange(0, 3))
            val firstByte = block[0].toInt() and 0xFF
            val kind = when {
                marker == "020013" -> TransactionKind.TOP_UP
                firstByte == 0x02 || firstByte == 0x0A -> TransactionKind.RIDE
                else -> return null
            }

            val date = try {
                decodeDate(block[10], block[11])
            } catch (e: TransportCardInvalidException) {
                return null
            }
            val hour = block[12].toInt() and 0xFF
            val minute = block[13].toInt() and 0xFF
            val second = block[14].toInt() and 0xFF
            if (hour > 23 || minute > 59 || second > 59) {
                return null
            }
            val dateTime = date.atTime(hour, minute, second)

            return when (kind) {
                TransactionKind.RIDE -> TransportTransaction(
                    kind = kind,
                    dateTime = dateTime,
                    line = block[7].toInt() and 0xFF,
                    direction = block[8].toInt() and 0xFF,
                )

                TransactionKind.TOP_UP -> TransportTransaction(kind = kind, dateTime = dateTime)
            }
        }
    }
}

data class ZgzTransportTransaction(
    val header: String,
    val cardTypeConst: String,
    val unknownVar1: String,
    val lineNumber: Int,
    ) {}