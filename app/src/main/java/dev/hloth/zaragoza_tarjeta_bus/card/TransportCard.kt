package dev.hloth.zaragoza_tarjeta_bus.card

import dev.hloth.zgztransport.Balance
import dev.hloth.zgztransport.CardId
import dev.hloth.zgztransport.CardType
import dev.hloth.zgztransport.JourneySummary
import dev.hloth.zgztransport.Product
import dev.hloth.zgztransport.Transaction
import dev.hloth.zgztransport.Uid

data class TransportCard(
    val cardType: CardType,
    val balance: Balance,
    val uid: Uid?,
    val id: CardId?,
    val transactions: List<Transaction>,
    val journeySummary: JourneySummary?,
    val products: List<Product>,
    val warnings: List<String>,
)
