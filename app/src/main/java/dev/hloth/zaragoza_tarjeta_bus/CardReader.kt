package dev.hloth.zaragoza_tarjeta_bus

import android.nfc.tech.MifareClassic
import dev.hloth.zgztransport.Card
import dev.hloth.zgztransport.CardFormatException
import dev.hloth.zgztransport.CardType
import dev.hloth.zgztransport.Dump
import dev.hloth.zgztransport.SectorKeys
import dev.hloth.zgztransport.TransactionLog

private const val UID_BLOCK = 0
private const val CARD_TYPE_BLOCK = 1
private const val CARD_ID_BLOCK = 2
private const val BALANCE_BLOCK = 8
private const val BALANCE_COPY_BLOCK = 9
private const val JOURNEY_SUMMARY_BLOCK = 10
private const val BLOCKS_PER_SECTOR = 4

private val HEADER_BLOCKS = listOf(UID_BLOCK, CARD_TYPE_BLOCK, CARD_ID_BLOCK)
private val HEADER_SECTOR_KEYS =
    CardType.values().mapNotNull { it.keys(0).orElse(null) }.distinct()

fun MifareClassic.readTransportCard(): Card {
    connect()
    if (type != MifareClassic.TYPE_CLASSIC) {
        throw CardFormatException("expected a MIFARE Classic card, got type $type")
    }

    val header = readBlocks(HEADER_BLOCKS) { HEADER_SECTOR_KEYS }
    val cardType = CardType.decode(header.getValue(CARD_TYPE_BLOCK))
    val payload = readBlocks(cardType.blocksToRead()) { sector ->
        listOfNotNull(cardType.keys(sector).orElse(null))
    }

    val dump = Dump.builder(cardType.chip())
    for ((index, bytes) in header + payload) {
        dump.block(index, bytes)
    }
    return dump.build().card()
}

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

private fun MifareClassic.readBlocks(
    blocks: List<Int>,
    keysFor: (sector: Int) -> List<SectorKeys>,
): Map<Int, ByteArray> = buildMap {
    for ((sector, sectorBlocks) in blocks.groupBy { blockToSector(it) }) {
        if (keysFor(sector).none { authenticateSector(sector, it) }) {
            throw CardFormatException("cannot authenticate sector $sector")
        }
        for (block in sectorBlocks) {
            put(block, readBlock(block))
        }
    }
}

private fun MifareClassic.authenticateSector(sector: Int, keys: SectorKeys): Boolean {
    val keyA = keys.a().orElse(null)
    if (keyA != null && authenticateSectorWithKeyA(sector, keyA.bytes())) return true
    val keyB = keys.b().orElse(null)
    return keyB != null && authenticateSectorWithKeyB(sector, keyB.bytes())
}
