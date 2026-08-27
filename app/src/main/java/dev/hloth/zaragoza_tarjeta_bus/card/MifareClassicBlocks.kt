package dev.hloth.zaragoza_tarjeta_bus.card

import android.nfc.tech.MifareClassic
import dev.hloth.zgztransport.CardFormatException
import dev.hloth.zgztransport.SectorKeys

class MifareClassicBlocks private constructor(private val mifare: MifareClassic) : BlockSource {

    companion object {
        fun connect(mifare: MifareClassic): MifareClassicBlocks {
            mifare.connect()
            if (mifare.type != MifareClassic.TYPE_CLASSIC) {
                throw CardFormatException("expected a MIFARE Classic card, got type ${mifare.type}")
            }
            return MifareClassicBlocks(mifare)
        }
    }

    override fun sectorOf(block: Int): Int = mifare.blockToSector(block)

    override fun authenticate(sector: Int, keys: SectorKeys): Boolean {
        val keyA = keys.a().orElse(null)
        if (keyA != null && mifare.authenticateSectorWithKeyA(sector, keyA.bytes())) return true
        val keyB = keys.b().orElse(null)
        return keyB != null && mifare.authenticateSectorWithKeyB(sector, keyB.bytes())
    }

    override fun read(block: Int): ByteArray = mifare.readBlock(block)
}
