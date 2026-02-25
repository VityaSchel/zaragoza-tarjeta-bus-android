package dev.hloth.zgz_avanza_card

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.MifareClassic
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hloth.zgz_avanza_card.ui.theme.ZGZAvanzaCardTheme

class MainActivity : ComponentActivity() {
    private lateinit var nfcAdapter: NfcAdapter
    private var loading by mutableStateOf(false)
    private var avanzaCard by mutableStateOf<AvanzaCard?>(null)
    private var errorMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        enableEdgeToEdge()
        setContent {
            ZGZAvanzaCardTheme {
                MainScreen(
                    card = avanzaCard,
                    loading,
                    errorMessage,
                    onErrorShown = { errorMessage = null })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter.enableReaderMode(
            this,
            { tag -> onTagDetected(tag) },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter.disableReaderMode(this)
    }

    fun onTagDetected(tag: Tag) {
        val mifare = MifareClassic.get(tag) ?: return
        runOnUiThread {
            avanzaCard = null
            loading = true
        }

        try {
            val card = AvanzaCard.read(mifare)
            runOnUiThread { avanzaCard = card }
        } catch (e: TagLostException) {
            Log.i(LOG_TAG, "Tag was removed before reading could complete: ${e.message}")
        } catch (e: AvanzaCardInvalidException) {
            runOnUiThread { errorMessage = "Unsupported or invalid card" }
            Log.e(LOG_TAG, "Card is invalid: ${e.message}")
        } catch (e: Exception) {
            runOnUiThread { errorMessage = "Error reading card" }
            Log.e(LOG_TAG, "Error while reading MifareClassic: ${e.message}", e)
        } finally {
            runOnUiThread {
                loading = false
            }
            try {
                mifare.close()
            } catch (e: Exception) {
                Log.w(LOG_TAG, "Error closing MifareClassic: ${e.message}")
            }
        }
    }
}

@Composable
fun MainScreen(
    card: AvanzaCard? = null,
    loading: Boolean = false,
    errorMessage: String? = null,
    onErrorShown: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onErrorShown()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (loading) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )

                    Text(
                        text = "Reading...",
                        textAlign = TextAlign.Center
                    )
                }
            } else if (card == null) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Scan your card to see the details",
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                CardDetails(card)
            }
        }
    }
}

@Composable
fun CardDetails(card: AvanzaCard) {
    val balanceEur = "%.2f".format(card.balance / 1000.0)
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Card ID: ${card.id}")
        Text(text = "Balance: €${balanceEur}")
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenIdlePreview() {
    ZGZAvanzaCardTheme {
        MainScreen()
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenLoadingPreview() {
    ZGZAvanzaCardTheme {
        MainScreen(loading = true)
    }
}