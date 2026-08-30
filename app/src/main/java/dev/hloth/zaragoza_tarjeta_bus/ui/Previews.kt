package dev.hloth.zaragoza_tarjeta_bus.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.hloth.zaragoza_tarjeta_bus.ui.theme.ZaragozaTarjetaBusTheme
import dev.hloth.zgztransport.CardDate
import dev.hloth.zgztransport.CardDateTime
import dev.hloth.zgztransport.CardType
import dev.hloth.zgztransport.Direction
import dev.hloth.zgztransport.TransactionKind

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainScreenIdlePreview() {
    ZaragozaTarjetaBusTheme { MainScreen() }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainScreenLoadingPreview() {
    ZaragozaTarjetaBusTheme { MainScreen(loading = true) }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainScreenCardDetailsPreview() {
    ZaragozaTarjetaBusTheme { MainScreen(card = sampleAvanzaTopUpCard) }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainScreenPersonalPassPreview() {
    ZaragozaTarjetaBusTheme {
        MainScreen(
            card = sampleCard(
                CardType.AVANZA_PERSONAL_UNLIMITED, "BP987654", 0,
                transactions = listOf(
                    sampleTransaction(
                        TransactionKind.Journey(Direction.ONE), 22, 0,
                        CardDateTime.of(2026, 2, 14, 18, 45, 0), 0,
                    ),
                ),
                products = listOf(
                    samplePass(3, 2, CardDate(2025, 12, 5)),
                    samplePass(4, 365, CardDate(2026, 12, 3)),
                ),
            ),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainScreenLazoCardPreview() {
    ZaragozaTarjetaBusTheme {
        MainScreen(card = sampleCard(CardType.LAZO_TOP_UP, "CT123456", 600))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainScreenNfcDisabledPreview() {
    ZaragozaTarjetaBusTheme { MainScreen(nfcState = NfcState.DISABLED) }
}
