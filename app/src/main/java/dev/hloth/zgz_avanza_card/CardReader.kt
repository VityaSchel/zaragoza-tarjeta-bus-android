package dev.hloth.zaragoza_tarjeta_bus

import android.nfc.tech.MifareClassic
import java.time.LocalDateTime

private val KEY_A = hexToBytes("04000C0F0903")
private val KEY_B = hexToBytes("0B02070A0409")

enum class CardType {
    TOP_UP,
    PERSONAL_UNLIMITED,
}

enum class TransactionKind { RIDE, TOP_UP }

data class TransportTransaction(
    val kind: TransactionKind,
    val dateTime: LocalDateTime,
    val line: Int? = null,
    val direction: Int? = null,
)

data class TransportCard(
    val id: String,
    val type: CardType,
    val balance: Long,
    val transactions: List<TransportTransaction> = emptyList(),
) {
    companion object {
        fun read(mifare: MifareClassic): TransportCard {
            mifare.connect()

            if (mifare.type != MifareClassic.TYPE_CLASSIC) {
                throw TransportCardInvalidException("Unsupported card type: ${mifare.type}, expected Mifare Classic")
            }
            if (mifare.size != MifareClassic.SIZE_1K) {
                throw TransportCardInvalidException("Unsupported card size: ${mifare.size}, expected 1K")
            }
            if (mifare.sectorCount < 9) {
                throw TransportCardInvalidException("Card has insufficient sectors: ${mifare.sectorCount}, expected at least 9")
            }

            val startSector = 0
            val endSector = 8

            val blocks = mutableMapOf<Int, ByteArray>()

            for (sector in startSector..endSector) {
                if (
                    !mifare.authenticateSectorWithKeyA(sector, KEY_A) &&
                    !mifare.authenticateSectorWithKeyB(sector, KEY_B)
                ) {
                    throw TransportCardInvalidException("Failed to authenticate sector $sector")
                }

                val blockCount = mifare.getBlockCountInSector(sector)
                if (blockCount != 4) {
                    throw TransportCardInvalidException("Unexpected block count in sector $sector: $blockCount, expected 4")
                }

                val firstBlock = mifare.sectorToBlock(sector)
                for (block in firstBlock until firstBlock + blockCount) {
                    blocks[block] = mifare.readBlock(block)
                }
            }

            mifare.close()

            val block1 = blocks[1] ?: throw TransportCardInvalidException("Missing block 1")
            val type = ZgzTransport.decodeCardType(block1)

            val block2 = blocks[2] ?: throw TransportCardInvalidException("Missing block 2")
            val id = ZgzTransport.decodeId(block2)

            val block8 = blocks[8] ?: throw TransportCardInvalidException("Missing block 8")
            val block9 = blocks[9] ?: throw TransportCardInvalidException("Missing block 9")
            if (!block8.contentEquals(block9)) {
                throw TransportCardInvalidException("Balance blocks 8 and 9 do not match")
            }
            val balance = ZgzTransport.decodeBalance(block8)

            val transactions = listOf(5, 28, 29, 30, 32, 33)
                .mapNotNull { blocks[it] }
                .mapNotNull { ZgzTransport.decodeTransaction(it) }
                .distinct()
                .sortedByDescending { it.dateTime }

            return TransportCard(id, type, balance, transactions)
        }
    }
}