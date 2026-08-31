package dev.hloth.zaragoza_tarjeta_bus.ui

import dev.hloth.zaragoza_tarjeta_bus.card.TransportCard
import dev.hloth.zgztransport.Balance
import dev.hloth.zgztransport.CardDate
import dev.hloth.zgztransport.CardDateTime
import dev.hloth.zgztransport.CardId
import dev.hloth.zgztransport.CardType
import dev.hloth.zgztransport.Direction
import dev.hloth.zgztransport.JourneySummary
import dev.hloth.zgztransport.Product
import dev.hloth.zgztransport.Route
import dev.hloth.zgztransport.Stop
import dev.hloth.zgztransport.Subscription
import dev.hloth.zgztransport.SubscriptionMetadata
import dev.hloth.zgztransport.Transaction
import dev.hloth.zgztransport.TransactionKind
import dev.hloth.zgztransport.Uid
import java.util.Optional

internal fun samplePass(sector: Int, validityDays: Int, ends: CardDate) = Product(
    sector,
    SubscriptionMetadata(6, 1, CardDate(2025, 12, 3), 0, validityDays, 0),
    Subscription(CardDate(2025, 12, 3), ends, 0, 0, Optional.empty()),
)

internal fun sampleCard(
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

internal fun sampleTransaction(
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

internal val sampleAvanzaTopUpCard: TransportCard
    get() = sampleCard(
        CardType.AVANZA_TOP_UP, "BE123456", 1234,
        listOf(
            sampleTransaction(
                TransactionKind.Journey(Direction.ONE), 22, 880,
                CardDateTime.of(2026, 2, 11, 8, 32, 0), 0,
            ),
            sampleTransaction(
                TransactionKind.Journey(Direction.TWO), 169, 880,
                CardDateTime.of(2026, 2, 11, 14, 5, 0), 1,
            ),
            sampleTransaction(
                TransactionKind.Journey(Direction.ONE), 13, 880,
                CardDateTime.of(2026, 2, 11, 17, 20, 0), 2,
            ),
            sampleTransaction(
                TransactionKind.TopUp(), 0, 10000,
                CardDateTime.of(2026, 2, 12, 9, 10, 0), 3,
            ),
            sampleTransaction(
                TransactionKind.Journey(Direction.TWO), 210, 880,
                CardDateTime.of(2026, 2, 14, 18, 45, 0), 4,
                stop = Stop.Tram(1300),
            ),
            sampleTransaction(
                TransactionKind.Journey(Direction.ONE), 31, 0,
                CardDateTime.of(2026, 2, 14, 19, 12, 0), 5,
            ),
        ),
        journeySummary = JourneySummary(
            Optional.empty(),
            JourneySummary.LastPaid(CardDate(2026, 2, 14), 18, 45),
            1,
            CardType.AVANZA_TOP_UP,
            false,
            Route(210),
            Direction.TWO,
            0x63,
        ),
    )
