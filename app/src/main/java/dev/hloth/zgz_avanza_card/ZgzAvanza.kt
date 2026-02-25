package dev.hloth.zgz_avanza_card

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
    }
}