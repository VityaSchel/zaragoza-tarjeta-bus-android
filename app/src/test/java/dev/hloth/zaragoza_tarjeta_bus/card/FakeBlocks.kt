package dev.hloth.zaragoza_tarjeta_bus.card

import dev.hloth.zgztransport.CardType
import dev.hloth.zgztransport.Chip
import dev.hloth.zgztransport.Dump
import dev.hloth.zgztransport.SectorKeys

private const val BLOCKS_PER_SECTOR = 4

class FakeBlocks(private val dump: Dump, private val cardType: CardType) : BlockSource {
    val authenticated = mutableListOf<Pair<Int, SectorKeys>>()
    val readBlocks = mutableListOf<Int>()
    var unauthenticatableSector: Int? = null

    val sectorsAuthenticated: List<Int> get() = authenticated.map { it.first }.distinct()

    override fun sectorOf(block: Int): Int = when {
        cardType.chip() == Chip.CLASSIC_4K && block >= 128 -> 32 + (block - 128) / 16
        else -> block / BLOCKS_PER_SECTOR
    }

    override fun authenticate(sector: Int, keys: SectorKeys): Boolean {
        authenticated += sector to keys
        if (sector == unauthenticatableSector) return false
        return cardType.keys(sector).orElse(null) == keys
    }

    override fun read(block: Int): ByteArray {
        readBlocks += block
        return dump.block(block)
    }
}
