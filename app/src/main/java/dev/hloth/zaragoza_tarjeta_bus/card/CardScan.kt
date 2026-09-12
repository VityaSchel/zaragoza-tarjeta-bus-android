package dev.hloth.zaragoza_tarjeta_bus.card

import android.nfc.TagLostException
import android.nfc.tech.MifareClassic

private const val HEADER_SECTOR = 0

sealed interface CardScan {
    data class Read(val card: TransportCard) : CardScan

    data class Unsupported(val code: String) : CardScan

    data class Failed(val cause: Exception) : CardScan

    data object NotATransportCard : CardScan

    data object Moved : CardScan
}

fun scanCard(mifare: MifareClassic): CardScan = try {
    CardScan.Read(readTransportCard(MifareClassicBlocks.connect(mifare)))
} catch (moved: TagLostException) {
    CardScan.Moved
} catch (unknown: CardReadException.UnknownCardType) {
    CardScan.Unsupported(unknown.code)
} catch (foreign: CardReadException.NotATransportCard) {
    CardScan.NotATransportCard
} catch (locked: CardReadException.Locked) {
    if (locked.sector == HEADER_SECTOR) CardScan.NotATransportCard else CardScan.Failed(locked)
} catch (failure: Exception) {
    CardScan.Failed(failure)
}

fun MifareClassic.closeQuietly() {
    try {
        close()
    } catch (ignored: Exception) {
    }
}
