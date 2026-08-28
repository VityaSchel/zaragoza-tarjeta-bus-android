package dev.hloth.zaragoza_tarjeta_bus.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.hloth.zaragoza_tarjeta_bus.R
import dev.hloth.zaragoza_tarjeta_bus.card.TransportCard

enum class NfcState { READY, DISABLED, UNAVAILABLE }

@Composable
fun MainScreen(
    card: TransportCard? = null,
    loading: Boolean = false,
    nfcState: NfcState = NfcState.READY,
    errorMessage: String? = null,
    onErrorShown: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val screen = remember(card) { card?.screen() }

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
                    contentColor = MaterialTheme.colorScheme.onError,
                )
            }
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                loading -> LoadingScreen()
                screen is CardScreen.Details -> CardDetails(screen)
                screen is CardScreen.Unsupported -> InfoScreen(
                    title = stringResource(R.string.unsupported_card_title),
                    subtitle = stringResource(
                        R.string.unsupported_card_subtitle,
                        screen.cardType.text(),
                    ),
                )
                nfcState == NfcState.UNAVAILABLE -> InfoScreen(
                    title = stringResource(R.string.nfc_unavailable_title),
                    subtitle = stringResource(R.string.nfc_unavailable_subtitle),
                )
                nfcState == NfcState.DISABLED -> InfoScreen(
                    title = stringResource(R.string.nfc_disabled_title),
                    subtitle = stringResource(R.string.nfc_disabled_subtitle),
                )
                else -> ScanPrompt()
            }
        }
    }
}
