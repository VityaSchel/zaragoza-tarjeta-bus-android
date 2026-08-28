package dev.hloth.zaragoza_tarjeta_bus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.hloth.zaragoza_tarjeta_bus.R
import dev.hloth.zaragoza_tarjeta_bus.card.TransportCard
import dev.hloth.zgztransport.Transaction
import dev.hloth.zgztransport.TransactionKind
import androidx.compose.material3.Card as MaterialCard

@Composable
internal fun CardDetails(card: TransportCard, modifier: Modifier = Modifier) {
    val newestFirst = remember(card) { card.transactions.reversed() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { BalanceCard(card) }
        card.id?.let { id -> item { CardIdRow(id.toString()) } }

        if (newestFirst.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.recent_activity_label),
                    modifier = Modifier.padding(top = 12.dp, start = 4.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            items(newestFirst) { transaction -> TransactionRow(transaction) }
        }
    }
}

@Composable
private fun BalanceCard(card: TransportCard) {
    MaterialCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(28.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = card.cardType.label(),
                    style = MaterialTheme.typography.labelLarge,
                )
                ContactlessIcon(
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Spacer(Modifier.height(40.dp))

            Text(
                text = stringResource(R.string.balance_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = card.balance.formatted(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CardIdRow(id: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.card_id_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = id,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TransactionRow(transaction: Transaction) {
    val topUp = transaction.kind is TransactionKind.TopUp
    val route = transaction.route.toString()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (topUp) "+" else route,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (topUp) {
                    stringResource(R.string.transaction_top_up)
                } else {
                    stringResource(R.string.transaction_bus_ride_line, route)
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = transaction.createdAt.formatted(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
