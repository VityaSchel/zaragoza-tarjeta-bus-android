package dev.hloth.zaragoza_tarjeta_bus.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.hloth.zaragoza_tarjeta_bus.ui.theme.ZaragozaTarjetaBusTheme
import dev.hloth.zgztransport.Balance
import dev.hloth.zgztransport.Card
import dev.hloth.zgztransport.CardDateTime
import dev.hloth.zgztransport.CardId
import dev.hloth.zgztransport.CardType
import dev.hloth.zgztransport.Direction
import dev.hloth.zgztransport.Route
import dev.hloth.zgztransport.Stop
import dev.hloth.zgztransport.Transaction
import dev.hloth.zgztransport.TransactionKind
import dev.hloth.zgztransport.Uid
import java.util.Optional

private val PREVIEW_UID = Uid.of(byteArrayOf(0x1D, 0x68, 0xC3.toByte(), 0xA9.toByte()))

private fun previewCard(
    cardType: CardType,
    id: String,
    balance: Long,
    transactions: List<Transaction> = emptyList(),
) = Card(
    PREVIEW_UID,
    cardType,
    CardId.parse(id),
    Balance(balance),
    transactions,
    Optional.empty(),
    emptyList(),
)

private fun previewTransaction(
    kind: TransactionKind,
    route: Int,
    amount: Int,
    at: CardDateTime,
    sequence: Int,
) = Transaction.builder()
    .cardType(CardType.AVANZA_TOP_UP)
    .amount(amount)
    .stop(Stop.Urban(500))
    .route(Route(route))
    .kind(kind)
    .createdAt(at)
    .sequence(sequence)
    .build()

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
    ZaragozaTarjetaBusTheme {
        MainScreen(
            card = previewCard(
                CardType.AVANZA_TOP_UP, "BE123456", 1234,
                listOf(
                    previewTransaction(
                        TransactionKind.Journey(Direction.ONE), 22, 880,
                        CardDateTime.of(2026, 2, 11, 8, 32, 0), 0,
                    ),
                    previewTransaction(
                        TransactionKind.TopUp(), 0, 10000,
                        CardDateTime.of(2026, 2, 12, 9, 10, 0), 1,
                    ),
                    previewTransaction(
                        TransactionKind.Journey(Direction.TWO), 35, 880,
                        CardDateTime.of(2026, 2, 14, 18, 45, 0), 2,
                    ),
                ),
            ),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainScreenUnsupportedCardPreview() {
    ZaragozaTarjetaBusTheme {
        MainScreen(card = previewCard(CardType.AVANZA_PERSONAL_UNLIMITED, "BP987654", 0))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainScreenNfcDisabledPreview() {
    ZaragozaTarjetaBusTheme { MainScreen(nfcState = NfcState.DISABLED) }
}
