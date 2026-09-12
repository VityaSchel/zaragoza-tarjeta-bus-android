package dev.hloth.zaragoza_tarjeta_bus

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.hloth.zaragoza_tarjeta_bus.card.CardScan
import dev.hloth.zaragoza_tarjeta_bus.card.closeQuietly
import dev.hloth.zaragoza_tarjeta_bus.card.scanCard
import dev.hloth.zaragoza_tarjeta_bus.ui.MainScreen
import dev.hloth.zaragoza_tarjeta_bus.ui.NfcState
import dev.hloth.zaragoza_tarjeta_bus.ui.theme.ZaragozaTarjetaBusTheme

private const val LOG_TAG = "ZaragozaTarjetaBus"

class MainActivity : ComponentActivity() {
    private val viewModel: CardViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null
    private var nfcState by mutableStateOf(NfcState.READY)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        enableEdgeToEdge()
        setContent {
            ZaragozaTarjetaBusTheme {
                MainScreen(
                    card = viewModel.card,
                    unsupportedCardType = viewModel.unsupportedCardType,
                    loading = viewModel.loading,
                    nfcState = nfcState,
                    errorMessage = viewModel.errorMessage,
                    onErrorShown = { viewModel.errorMessage = null },
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
        val mifare = mifareClassicOf(tag)
        if (mifare == null) {
            onMainThread { apply(CardScan.NotATransportCard) }
            return
        }

        onMainThread { viewModel.loading = true }
        val scan = scanCard(mifare)
        mifare.closeQuietly()
        onMainThread { apply(scan) }
    }

    private fun mifareClassicOf(tag: Tag): MifareClassic? = try {
        MifareClassic.get(tag).also {
            if (it == null) Log.w(LOG_TAG, "This device cannot read the tapped tag as MIFARE Classic")
        }
    } catch (unsupported: RuntimeException) {
        Log.w(LOG_TAG, "Tapped tag is not a MIFARE Classic card this device can read", unsupported)
        null
    }

    private fun apply(scan: CardScan) {
        viewModel.loading = false
        viewModel.unsupportedCardType = null
        if (scan !is CardScan.Read) {
            viewModel.card = null
        }
        when (scan) {
            is CardScan.Read -> {
                if (scan.card.warnings.isNotEmpty()) {
                    Log.w(LOG_TAG, "Card read with parts skipped: ${scan.card.warnings.joinToString()}")
                }
                viewModel.errorMessage = null
                viewModel.card = scan.card
            }

            is CardScan.Unsupported -> {
                Log.w(LOG_TAG, "Card type ${scan.code} is not supported yet")
                viewModel.unsupportedCardType = scan.code
            }

            is CardScan.Failed -> {
                Log.e(LOG_TAG, "Card could not be read", scan.cause)
                viewModel.errorMessage = getString(R.string.error_read_failed)
            }

            CardScan.NotATransportCard -> {
                viewModel.errorMessage = getString(R.string.error_not_transport_card)
            }

            CardScan.Moved -> {
                viewModel.errorMessage = getString(R.string.error_card_moved)
            }
        }
    }

    private fun onMainThread(update: () -> Unit) = runOnUiThread(update)
}
