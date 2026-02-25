package dev.hloth.zgz_avanza_card

import android.nfc.tech.MifareClassic

private val KEY_A = hexToBytes("04000C0F0903")
private val KEY_B = hexToBytes("0B02070A0409")

data class AvanzaCard(val id: String) {
    companion object {
        fun read(mifare: MifareClassic): AvanzaCard {
            mifare.connect()

            if (mifare.type != MifareClassic.TYPE_CLASSIC) {
                throw AvanzaCardInvalidException("Unsupported card type: ${mifare.type}, expected Mifare Classic")
            }
            if (mifare.size != MifareClassic.SIZE_1K) {
                throw AvanzaCardInvalidException("Unsupported card size: ${mifare.size}, expected 1K")
            }
            if (mifare.sectorCount < 9) {
                throw AvanzaCardInvalidException("Card has insufficient sectors: ${mifare.sectorCount}, expected at least 9")
            }

            val startSector = 0
            val endSector = 8

            val blocks = mutableMapOf<Int, ByteArray>()

            for (sector in startSector..endSector) {
                if (
                    !mifare.authenticateSectorWithKeyA(sector, KEY_A) &&
                    !mifare.authenticateSectorWithKeyB(sector, KEY_B)
                ) {
                    throw AvanzaCardInvalidException("Failed to authenticate sector $sector")
                }

                val blockCount = mifare.getBlockCountInSector(sector)
                if (blockCount != 4) {
                    throw AvanzaCardInvalidException("Unexpected block count in sector $sector: $blockCount, expected 4")
                }

                val firstBlock = mifare.sectorToBlock(sector)
                for (block in firstBlock until firstBlock + blockCount) {
                    blocks[block] = mifare.readBlock(block)
                }
            }

            mifare.close()

            val block1 = blocks[1] ?: throw AvanzaCardInvalidException("Missing block 1")
            if (!block1.contentEquals(hexToBytes("02699F000000000000000000000000F4"))) {
                throw AvanzaCardInvalidException("Unexpected value in block 1")
            }

            val block2 = blocks[2] ?: throw AvanzaCardInvalidException("Missing block 2")
            val id = ZgzAvanza.decodeId(block2)

            return AvanzaCard(id)
        }
    }
}