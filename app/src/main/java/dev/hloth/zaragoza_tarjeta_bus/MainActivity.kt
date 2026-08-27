package dev.hloth.zaragoza_tarjeta_bus

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.MifareClassic
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.hloth.zaragoza_tarjeta_bus.card.MifareClassicBlocks
import dev.hloth.zaragoza_tarjeta_bus.card.readTransportCard
import dev.hloth.zaragoza_tarjeta_bus.ui.MainScreen
import dev.hloth.zaragoza_tarjeta_bus.ui.NfcState
import dev.hloth.zaragoza_tarjeta_bus.ui.theme.ZaragozaTarjetaBusTheme
import dev.hloth.zgztransport.Card

private const val LOG_TAG = "ZaragozaTarjetaBus"

class MainActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private var nfcState by mutableStateOf(NfcState.READY)
    private var loading by mutableStateOf(false)
    private var card by mutableStateOf<Card?>(null)
    private var errorMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        enableEdgeToEdge()
        setContent {
            ZaragozaTarjetaBusTheme {
                MainScreen(
                    card = card,
                    loading = loading,
                    nfcState = nfcState,
                    errorMessage = errorMessage,
                    onErrorShown = { errorMessage = null },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val adapter = nfcAdapter
        when {
            adapter == null -> nfcState = NfcState.UNAVAILABLE
            !adapter.isEnabled -> nfcState = NfcState.DISABLED
            else -> {
                nfcState = NfcState.READY
                adapter.enableReaderMode(
                    this,
                    { tag -> onTagDetected(tag) },
                    NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                    null,
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    private fun onTagDetected(tag: Tag) {
        val mifare = MifareClassic.get(tag) ?: return
        runOnUiThread {
            card = null
            loading = true
        }

        try {
            val read = readTransportCard(MifareClassicBlocks.connect(mifare))
            runOnUiThread { card = read }
        } catch (e: TagLostException) {
            Log.i(LOG_TAG, "Tag was removed before reading could complete: ${e.message}")
        } catch (e: IllegalArgumentException) {
            val message = getString(R.string.error_invalid_card)
            runOnUiThread { errorMessage = message }
            Log.e(LOG_TAG, "Card is invalid: ${e.message}")
        } catch (e: Exception) {
            val message = getString(R.string.error_reading_card)
            runOnUiThread { errorMessage = message }
            Log.e(LOG_TAG, "Error while reading MifareClassic: ${e.message}", e)
        } finally {
            runOnUiThread { loading = false }
            try {
                mifare.close()
            } catch (e: Exception) {
                Log.w(LOG_TAG, "Error closing MifareClassic: ${e.message}")
            }
        }
    }
}
