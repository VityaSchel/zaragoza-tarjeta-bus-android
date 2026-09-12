package dev.hloth.zaragoza_tarjeta_bus.card

import dev.hloth.zgztransport.Balance
import dev.hloth.zgztransport.CardFormatException
import dev.hloth.zgztransport.CardId
import dev.hloth.zgztransport.CardType
import dev.hloth.zgztransport.JourneySummary
import dev.hloth.zgztransport.Product
import dev.hloth.zgztransport.SectorKeys
import dev.hloth.zgztransport.Subscription
import dev.hloth.zgztransport.SubscriptionMetadata
import dev.hloth.zgztransport.Transaction
import dev.hloth.zgztransport.TransactionLog
import dev.hloth.zgztransport.Uid

private const val UID_BLOCK = 0
private const val CARD_TYPE_BLOCK = 1
private const val CARD_ID_BLOCK = 2
private const val BALANCE_BLOCK = 8
private const val BALANCE_COPY_BLOCK = 9
private const val JOURNEY_SUMMARY_BLOCK = 10
private const val BLOCKS_PER_SECTOR = 4
private const val BLOCK_SIZE = 16

private val HEADER_BLOCKS = listOf(UID_BLOCK, CARD_TYPE_BLOCK, CARD_ID_BLOCK)
private val HEADER_SECTOR_KEYS =
    CardType.values().mapNotNull { it.keys(0).orElse(null) }.distinct()

fun readTransportCard(blocks: BlockSource): TransportCard {
    val header = blocks.readAll(HEADER_BLOCKS) { HEADER_SECTOR_KEYS }
    val cardType = decodeCardType(header.getValue(CARD_TYPE_BLOCK))
    val read = header + blocks.readAll(cardType.blocksToRead()) { sector ->
        listOfNotNull(cardType.keys(sector).orElse(null))
    }

    val warnings = mutableListOf<String>()
    fun <T> optional(what: String, decode: () -> T): T? = try {
        decode()
    } catch (malformed: IllegalArgumentException) {
        warnings += what
        null
    }

    val balance = optional("balance block $BALANCE_BLOCK") { Balance.decode(read.getValue(BALANCE_BLOCK)) }
    val balanceCopy = optional("balance block $BALANCE_COPY_BLOCK") { Balance.decode(read.getValue(BALANCE_COPY_BLOCK)) }
    if (balance != null && balanceCopy != null && balance != balanceCopy) {
        warnings += "balance blocks disagree"
    }

    val transactions = TransactionLog.BLOCKS
        .filter { read.getValue(it)[0].toInt() != 0 }
        .mapNotNull { block -> optional("transaction block $block") { Transaction.decode(read.getValue(block)) } }
        .sortedBy { it.createdAt }

    val journeySummary = read[JOURNEY_SUMMARY_BLOCK]
        ?.takeUnless { it.isBlank() }
        ?.let { block -> optional("journey summary") { JourneySummary.decode(block) } }

    val products = cardType.productSectors().mapNotNull { sector ->
        val metadata = read.getValue(sector * BLOCKS_PER_SECTOR)
        if (metadata.isBlank()) return@mapNotNull null
        optional("product in sector $sector") {
            Product(
                sector,
                SubscriptionMetadata.decode(metadata),
                Subscription.decode(read.getValue(sector * BLOCKS_PER_SECTOR + 1)),
            )
        }
    }

    return TransportCard(
        cardType = cardType,
        balance = balance ?: balanceCopy ?: throw CardReadException.Unreadable("neither balance block decodes"),
        uid = optional("uid") { Uid.decode(read.getValue(UID_BLOCK)) },
        id = optional("card id") { CardId.decode(read.getValue(CARD_ID_BLOCK)) },
        transactions = transactions,
        journeySummary = journeySummary,
        products = products,
        warnings = warnings.toList(),
    )
}

private fun decodeCardType(block: ByteArray): CardType = try {
    CardType.decode(block)
} catch (malformed: CardFormatException) {
    val code = block.take(3).fold(0) { value, byte -> (value shl 8) or (byte.toInt() and 0xff) }
    if (block.isIntactTypeBlock() && code != 0) {
        throw CardReadException.UnknownCardType("%06X".format(code))
    }
    throw CardReadException.Unreadable("card type block is corrupt", malformed)
}

private fun ByteArray.isIntactTypeBlock(): Boolean =
    size == BLOCK_SIZE &&
        copyOfRange(3, BLOCK_SIZE - 1).isBlank() &&
        take(BLOCK_SIZE - 1).fold(0) { sum, byte -> sum xor (byte.toInt() and 0xff) } == (last().toInt() and 0xff)

private fun ByteArray.isBlank(): Boolean = all { it.toInt() == 0 }

private fun CardType.blocksToRead(): List<Int> = buildList {
    add(BALANCE_BLOCK)
    add(BALANCE_COPY_BLOCK)
    if (recordsJourneySummary()) add(JOURNEY_SUMMARY_BLOCK)
    addAll(TransactionLog.BLOCKS)
    for (sector in productSectors()) {
        val metadataBlock = sector * BLOCKS_PER_SECTOR
        val subscriptionBlock = metadataBlock + 1
        add(metadataBlock)
        add(subscriptionBlock)
    }
}

private fun BlockSource.readAll(
    blocks: List<Int>,
    keysFor: (sector: Int) -> List<SectorKeys>,
): Map<Int, ByteArray> = buildMap {
    for ((sector, sectorBlocks) in blocks.sorted().groupBy { sectorOf(it) }) {
        if (keysFor(sector).none { authenticate(sector, it) }) {
            throw CardReadException.Locked(sector)
        }
        for (block in sectorBlocks) {
            put(block, read(block))
        }
    }
}
