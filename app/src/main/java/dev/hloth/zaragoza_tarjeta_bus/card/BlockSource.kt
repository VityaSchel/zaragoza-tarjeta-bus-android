package dev.hloth.zaragoza_tarjeta_bus.card

import dev.hloth.zgztransport.SectorKeys

interface BlockSource {
    fun sectorOf(block: Int): Int

    fun authenticate(sector: Int, keys: SectorKeys): Boolean

    fun read(block: Int): ByteArray
}
