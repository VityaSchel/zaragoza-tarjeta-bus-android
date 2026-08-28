package dev.hloth.zaragoza_tarjeta_bus.ui

import dev.hloth.zaragoza_tarjeta_bus.R
import dev.hloth.zaragoza_tarjeta_bus.card.TransportCard
import dev.hloth.zgztransport.Balance
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Optional

private fun journey(
    route: Int,
    stop: Stop = Stop.Urban(500),
    amount: Int = 880,
    consecutivePayments: Int = 1,
    cardType: CardType = CardType.AVANZA_TOP_UP,
    at: CardDateTime = CardDateTime.of(2026, 2, 14, 18, 45, 0),
) = Transaction.builder()
    .cardType(cardType)
    .amount(amount)
    .consecutivePayments(consecutivePayments)
    .stop(stop)
    .route(Route(route))
    .kind(TransactionKind.Journey(Direction.ONE))
    .createdAt(at)
    .sequence(0)
    .build()

private fun topUp(at: CardDateTime = CardDateTime.of(2026, 2, 12, 9, 10, 0)) = Transaction.builder()
    .cardType(CardType.AVANZA_TOP_UP)
    .amount(10000)
    .stop(Stop.Other(7980))
    .route(Route(0))
    .kind(TransactionKind.TopUp())
    .createdAt(at)
    .sequence(1)
    .build()

private fun pass(sector: Int, validityDays: Int, starts: CardDate, ends: CardDate) = Product(
    sector,
    SubscriptionMetadata(6, 1, starts, 0, validityDays, 0),
    Subscription(starts, ends, 0, 0, Optional.empty()),
)

private fun summary(free: Boolean, route: Int, previous: Int? = null) = JourneySummary(
    Optional.ofNullable(previous?.let { JourneySummary.Leg(Route(it), Direction.ONE) }),
    JourneySummary.LastPaid(CardDate(2026, 2, 14), 18, 45),
    1,
    CardType.AVANZA_TOP_UP,
    free,
    Route(route),
    Direction.ONE,
    0x63,
)

private fun card(
    cardType: CardType = CardType.AVANZA_TOP_UP,
    transactions: List<Transaction> = emptyList(),
    warnings: List<String> = emptyList(),
    journeySummary: JourneySummary? = null,
    products: List<Product> = emptyList(),
) = TransportCard(
    cardType = cardType,
    balance = Balance(4450),
    uid = null,
    id = CardId.parse("BE123456"),
    transactions = transactions,
    journeySummary = journeySummary,
    products = products,
    warnings = warnings,
)

private val TODAY: LocalDate = LocalDate.of(2026, 2, 14)

private fun details(card: TransportCard) = card.screen(TODAY) as CardScreen.Details

class CardPresentationTest {

    @Test
    fun namesTheTramInsteadOfCallingItABus() {
        val row = details(card(transactions = listOf(journey(210, Stop.Tram(1300))))).activity.single()

        assertEquals(Label(R.string.transaction_tram_ride_line, listOf("L1")), row.title)
        assertEquals("L1", row.badge)
    }

    @Test
    fun namesAnUrbanBusAndLeavesItsStopUnnamed() {
        val row = details(card(transactions = listOf(journey(22)))).activity.single()

        assertEquals(Label(R.string.transaction_bus_ride_line, listOf("22")), row.title)
        assertNull(row.place)
    }

    @Test
    fun namesCercaniasAndNeverPrintsItsStopValue() {
        val checkOut = journey(169, Stop.Other(0x2303), amount = 0, consecutivePayments = 0)

        val row = details(card(transactions = listOf(checkOut))).activity.single()

        assertEquals(Label(R.string.transaction_cercanias_ride), row.title)
        assertNull(row.place)
        assertEquals(Label(R.string.transaction_check_out), row.fare)
        assertNull(row.amount)
    }

    @Test
    fun fallsBackToAPlainJourneyForAnUnidentifiedOperator() {
        val row = details(card(transactions = listOf(journey(152)))).activity.single()

        assertEquals(Label(R.string.transaction_journey_line, listOf("152")), row.title)
    }

    @Test
    fun showsTheTramStopNumberTheCardStoresTimesOneHundred() {
        val row = details(card(transactions = listOf(journey(210, Stop.Tram(1300))))).activity.single()

        assertEquals(Label(R.string.transaction_tram_stop, listOf("13")), row.place)
    }

    @Test
    fun marksAFreeRideThatCarriesAPaymentCounterAsATransfer() {
        val transfer = journey(31, amount = 0, consecutivePayments = 1)

        val row = details(card(transactions = listOf(transfer))).activity.single()

        assertEquals(Label(R.string.transaction_transfer), row.fare)
        assertNull(row.amount)
    }

    @Test
    fun marksAFreeRideThatIsNeitherTransferNorCheckOutAsFree() {
        val free = journey(31, amount = 0, consecutivePayments = 0)

        val row = details(card(transactions = listOf(free))).activity.single()

        assertEquals(Label(R.string.transaction_free), row.fare)
    }

    @Test
    fun leavesAPaidRideWithoutAFareLabel() {
        val row = details(card(transactions = listOf(journey(22)))).activity.single()

        assertNull(row.fare)
        assertTrue(row.amount, row.amount!!.startsWith("−"))
    }

    @Test
    fun showsAnOffBoardTopUpWithoutARouteOrAStop() {
        val row = details(card(transactions = listOf(topUp()))).activity.single()

        assertEquals("+", row.badge)
        assertEquals(Label(R.string.transaction_top_up), row.title)
        assertNull(row.place)
        assertNull(row.fare)
        assertTrue(row.amount, row.amount!!.startsWith("+"))
    }

    @Test
    fun ordersTheActivityNewestFirst() {
        val activity = details(
            card(
                transactions = listOf(
                    journey(22, at = CardDateTime.of(2026, 2, 11, 8, 32, 0)),
                    topUp(),
                    journey(35, at = CardDateTime.of(2026, 2, 14, 18, 45, 0)),
                ),
            ),
        ).activity

        assertEquals(listOf("35", "+", "22"), activity.map { it.badge })
    }

    @Test
    fun keepsTheRingOrderWhenTwoRecordsShareASecond() {
        val sameSecond = CardDateTime.of(2026, 2, 14, 18, 45, 0)
        val activity = details(
            card(transactions = listOf(journey(22, at = sameSecond), journey(35, at = sameSecond))),
        ).activity

        assertEquals(listOf("22", "35"), activity.map { it.badge })
    }

    @Test
    fun showsALazoCardLikeAnyOtherBalanceCard() {
        val screen = details(card(cardType = CardType.LAZO_TOP_UP))

        assertEquals(Label(R.string.card_type_lazo), screen.cardType)
        assertEquals(Label(R.string.balance_label), screen.headline!!.label)
        assertEquals(emptyList<PassRow>(), screen.passes)
    }

    @Test
    fun headlinesAPersonalCardWithTheLatestDateItsPassesRunTo() {
        val screen = details(
            card(
                cardType = CardType.AVANZA_PERSONAL_UNLIMITED,
                products = listOf(
                    pass(3, 2, CardDate(2025, 12, 3), CardDate(2025, 12, 5)),
                    pass(4, 365, CardDate(2025, 12, 3), CardDate(2026, 12, 3)),
                ),
            ),
        )

        assertEquals(Label(R.string.pass_valid_until), screen.headline!!.label)
        assertEquals("3 Dec 2026", screen.headline.value)
    }

    @Test
    fun listsTheLongestRunningPassFirstAndCountsItsDaysLeft() {
        val screen = details(
            card(
                cardType = CardType.AVANZA_PERSONAL_UNLIMITED,
                products = listOf(
                    pass(3, 2, CardDate(2025, 12, 3), CardDate(2025, 12, 5)),
                    pass(4, 365, CardDate(2025, 12, 3), CardDate(2026, 12, 3)),
                ),
            ),
        )

        assertEquals(
            listOf(Label(R.string.pass_days, listOf("365")), Label(R.string.pass_days, listOf("2"))),
            screen.passes.map { it.kind },
        )
        assertEquals(Label(R.string.pass_days_left, listOf("292")), screen.passes[0].remaining)
        assertEquals("3 Dec 2025 – 3 Dec 2026", screen.passes[0].window)
    }

    @Test
    fun marksAPassThatHasRunOutAsExpired() {
        val screen = details(
            card(
                cardType = CardType.AVANZA_PERSONAL_UNLIMITED,
                products = listOf(pass(3, 2, CardDate(2025, 12, 3), CardDate(2025, 12, 5))),
            ),
        )

        assertEquals(Label(R.string.pass_expired), screen.passes.single().remaining)
    }

    @Test
    fun leavesEveryRideOfASubscriptionCardWithoutAFareLabelOrAnAmount() {
        val row = details(
            card(
                cardType = CardType.AVANZA_PERSONAL_UNLIMITED,
                transactions = listOf(
                    journey(22, amount = 0, consecutivePayments = 0, cardType = CardType.AVANZA_PERSONAL_UNLIMITED),
                ),
            ),
        ).activity.single()

        assertNull(row.fare)
        assertNull(row.amount)
    }

    @Test
    fun readsTheLastPaidRideOffTheCurrentJourneyWhenItWasPaid() {
        val screen = details(card(journeySummary = summary(free = false, route = 22)))

        assertEquals("14 Feb 2026 · 18:45 · 22", screen.lastPaid)
    }

    @Test
    fun readsTheLastPaidRideOffThePreviousLegWhenTheCurrentOneWasFree() {
        val screen = details(card(journeySummary = summary(free = true, route = 210, previous = 31)))

        assertEquals("14 Feb 2026 · 18:45 · 31", screen.lastPaid)
    }

    @Test
    fun leavesTheLastPaidRideWithoutARouteWhenTheCardRemembersNoPreviousLeg() {
        val screen = details(card(journeySummary = summary(free = true, route = 210)))

        assertEquals("14 Feb 2026 · 18:45", screen.lastPaid)
    }

    @Test
    fun showsNoLastPaidRideWhenTheCardHasNoSummary() {
        assertNull(details(card()).lastPaid)
    }

    @Test
    fun raisesANoticeOnlyWhenPartsOfTheCardWereSkipped() {
        assertNull(details(card()).notice)
        assertEquals(
            Label(R.string.card_partially_read),
            details(card(warnings = listOf("transaction block 28"))).notice,
        )
    }

    @Test
    fun dropsTheCardIdRowWhenTheIdDidNotDecode() {
        val unreadable = card().copy(id = null)

        assertNull(details(unreadable).cardId)
    }
}
