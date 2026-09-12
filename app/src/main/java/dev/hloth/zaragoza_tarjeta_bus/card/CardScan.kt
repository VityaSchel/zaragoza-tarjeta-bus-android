package dev.hloth.zaragoza_tarjeta_bus.card

import android.nfc.TagLostException
import android.nfc.tech.MifareClassic

private const val ATTEMPTS = 3
private const val HEADER_SECTOR = 0

sealed interface CardScan {
    data class Read(val card: TransportCard) : CardScan

    data class Unsupported(val code: String) : CardScan

    data class Failed(val cause: Exception) : CardScan

    data object NotATransportCard : CardScan

    data object Moved : CardScan
}

fun scanCard(mifare: MifareClassic): CardScan = scanWithRetries(
    read = { readTransportCard(MifareClassicBlocks.connect(mifare)) },
    reset = mifare::closeQuietly,
)

internal fun scanWithRetries(read: () -> TransportCard, reset: () -> Unit): CardScan {
    lateinit var lastFailure: Exception
    repeat(ATTEMPTS) {
        try {
            return CardScan.Read(read())
        } catch (moved: TagLostException) {
            return CardScan.Moved
        } catch (stale: SecurityException) {
            return CardScan.Moved
        } catch (unknown: CardReadException.UnknownCardType) {
            return CardScan.Unsupported(unknown.code)
        } catch (foreign: CardReadException.NotATransportCard) {
            return CardScan.NotATransportCard
        } catch (unsupported: UnsupportedOperationException) {
            return CardScan.NotATransportCard
        } catch (retryable: Exception) {
            lastFailure = retryable
            reset()
        }
    }
    val failure = lastFailure
    return if (failure is CardReadException.Locked && failure.sector == HEADER_SECTOR) {
        CardScan.NotATransportCard
    } else {
        CardScan.Failed(failure)
    }
}

fun MifareClassic.closeQuietly() {
    try {
        close()
    } catch (ignored: Exception) {
    }
}
