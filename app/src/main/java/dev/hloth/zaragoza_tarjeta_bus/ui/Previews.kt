package dev.hloth.zaragoza_tarjeta_bus.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.hloth.zaragoza_tarjeta_bus.ui.theme.ZaragozaTarjetaBusTheme
import dev.hloth.zgztransport.Balance
import dev.hloth.zaragoza_tarjeta_bus.card.TransportCard
import dev.hloth.zgztransport.CardDateTime
import dev.hloth.zgztransport.CardId
import dev.hloth.zgztransport.CardType
import dev.hloth.zgztransport.CardDate
import dev.hloth.zgztransport.Direction
import dev.hloth.zgztransport.JourneySummary
import dev.hloth.zgztransport.Product
import dev.hloth.zgztransport.Subscription
import dev.hloth.zgztransport.SubscriptionMetadata
import dev.hloth.zgztransport.Route
import dev.hloth.zgztransport.Stop
import dev.hloth.zgztransport.Transaction
import dev.hloth.zgztransport.TransactionKind
import dev.hloth.zgztransport.Uid

private fun previewPass(sector: Int, validityDays: Int, ends: CardDate) = Product(
    sector,
    SubscriptionMetadata(6, 1, CardDate(2025, 12, 3), 0, validityDays, 0),
    Subscription(CardDate(2025, 12, 3), ends, 0, 0, java.util.Optional.empty()),
)

private fun previewCard(
    cardType: CardType,
    id: String,
    balance: Long,
    transactions: List<Transaction> = emptyList(),
    journeySummary: JourneySummary? = null,
    products: List<Product> = emptyList(),
) = TransportCard(
    cardType = cardType,
    balance = Balance(balance),
    uid = Uid.of(byteArrayOf(0x1D, 0x68, 0xC3.toByte(), 0xA9.toByte())),
    id = CardId.parse(id),
    transactions = transactions,
    journeySummary = journeySummary,
    products = products,
    warnings = emptyList(),
)

private fun previewTransaction(
    kind: TransactionKind,
    route: Int,
    amount: Int,
    at: CardDateTime,
    sequence: Int,
    stop: Stop = Stop.Urban(500),
    consecutivePayments: Int = 1,
) = Transaction.builder()
    .cardType(CardType.AVANZA_TOP_UP)
    .amount(amount)
    .consecutivePayments(consecutivePayments)
    .stop(stop)
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
                        TransactionKind.Journey(Direction.TWO), 210, 880,
                        CardDateTime.of(2026, 2, 14, 18, 45, 0), 2,
                        stop = Stop.Tram(1300),
                    ),
                    previewTransaction(
                        TransactionKind.Journey(Direction.ONE), 31, 0,
                        CardDateTime.of(2026, 2, 14, 19, 12, 0), 3,
                    ),
                ),
                journeySummary = JourneySummary(
                    java.util.Optional.empty(),
                    JourneySummary.LastPaid(CardDate(2026, 2, 14), 18, 45),
                    1,
                    CardType.AVANZA_TOP_UP,
                    false,
                    Route(210),
                    Direction.TWO,
                    0x63,
                ),
            ),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainScreenPersonalPassPreview() {
    ZaragozaTarjetaBusTheme {
        MainScreen(
            card = previewCard(
                CardType.AVANZA_PERSONAL_UNLIMITED, "BP987654", 0,
                transactions = listOf(
                    previewTransaction(
                        TransactionKind.Journey(Direction.ONE), 22, 0,
                        CardDateTime.of(2026, 2, 14, 18, 45, 0), 0,
                    ),
                ),
                products = listOf(
                    previewPass(3, 2, CardDate(2025, 12, 5)),
                    previewPass(4, 365, CardDate(2026, 12, 3)),
                ),
            ),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainScreenLazoCardPreview() {
    ZaragozaTarjetaBusTheme {
        MainScreen(card = previewCard(CardType.LAZO_TOP_UP, "CT123456", 600))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainScreenNfcDisabledPreview() {
    ZaragozaTarjetaBusTheme { MainScreen(nfcState = NfcState.DISABLED) }
}
