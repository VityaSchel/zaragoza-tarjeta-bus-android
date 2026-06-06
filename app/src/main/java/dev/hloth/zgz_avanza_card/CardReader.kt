package dev.hloth.zaragoza_tarjeta_bus

import android.nfc.tech.MifareClassic
import java.time.LocalDateTime

private val KEY_A = hexToBytes("04000C0F0903")
private val KEY_B = hexToBytes("0B02070A0409")

enum class CardType(val label: String) {
    TOP_UP("Top-up card"),
    PERSONAL_UNLIMITED("Personal pass"),
}

enum class TransactionKind { RIDE, TOP_UP }

data class AvanzaTransaction(
    val kind: TransactionKind,
    val dateTime: LocalDateTime,
    val line: Int? = null,
    val direction: Int? = null,
)

data class AvanzaCard(
    val id: String,
    val type: CardType,
    val balance: Long,
    val transactions: List<AvanzaTransaction> = emptyList(),
) {
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
            val type = ZgzAvanza.decodeCardType(block1)

            val block2 = blocks[2] ?: throw AvanzaCardInvalidException("Missing block 2")
            val id = ZgzAvanza.decodeId(block2)

            val block8 = blocks[8] ?: throw AvanzaCardInvalidException("Missing block 8")
            val block9 = blocks[9] ?: throw AvanzaCardInvalidException("Missing block 9")
            if (!block8.contentEquals(block9)) {
                throw AvanzaCardInvalidException("Balance blocks 8 and 9 do not match")
            }
            val balance = ZgzAvanza.decodeBalance(block8)

            val transactions = listOf(5, 28, 29, 30, 32, 33)
                .mapNotNull { blocks[it] }
                .mapNotNull { ZgzAvanza.decodeTransaction(it) }
                .distinct()
                .sortedByDescending { it.dateTime }

            return AvanzaCard(id, type, balance, transactions)
        }
    }
}