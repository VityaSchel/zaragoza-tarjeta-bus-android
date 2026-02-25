package dev.hloth.zgz_avanza_card

// Reference, notes, JS implementation: https://git.hloth.dev/hloth/zgz-avanza
class ZgzAvanza {
    companion object {
        fun decodeId(block: ByteArray): String {
            val cardIdPrefixAscii = block.copyOfRange(0, 2)
            val cardIdPrefix = String(cardIdPrefixAscii)
            var cardIdNumber = block.copyOfRange(2, 15)
            val cardIdChecksumA = block[15]
            val cardIdChecksumB = cardIdPrefixAscii.fold(0) { acc, b -> acc xor b.toInt() } xor
                    cardIdNumber.fold(0) { acc, b -> acc xor b.toInt() }
            if (cardIdChecksumA.toInt() != cardIdChecksumB) {
                throw AvanzaCardInvalidException("Card ID checksum mismatch: expected $cardIdChecksumB, got $cardIdChecksumA")
            }
            while (cardIdNumber.last().toInt() == 0 && cardIdNumber.size > 3) {
                cardIdNumber = cardIdNumber.copyOfRange(0, cardIdNumber.size - 1)
            }

            return cardIdPrefix + bytesToHex(cardIdNumber)
        }

        fun decodeBalance(block: ByteArray): Long {
            if (block.size != 16) {
                throw AvanzaCardInvalidException("Invalid balance block length: expected 16 bytes, got ${block.size}")
            }

            val le = block.copyOfRange(0, 4)
            val aComplement = block.copyOfRange(4, 8)

            for (i in 0 until 4) {
                if (le[i] != block[8 + i]) {
                    throw AvanzaCardInvalidException("Invalid balance block: bytes 0-3 and 8-11 do not match")
                }
            }

            var balance = 0L
            for (i in 0 until 4) {
                balance += (le[i].toLong() and 0xFFL) shl (i * 8)
            }

            for (i in 0 until 4) {
                val expectedComplement = (le[i].toInt() xor 0xFF).toByte()
                if (aComplement[i] != expectedComplement) {
                    throw AvanzaCardInvalidException("Invalid balance block: bytes 4-7 are not the complement of bytes 0-3")
                }
            }

            return balance
        }
    }
}