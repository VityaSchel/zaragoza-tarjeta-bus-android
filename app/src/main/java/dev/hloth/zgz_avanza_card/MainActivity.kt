package dev.hloth.zaragoza_tarjeta_bus

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.MifareClassic
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hloth.zaragoza_tarjeta_bus.ui.theme.ZGZAvanzaCardTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private var nfcState by mutableStateOf(NfcState.READY)
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
                    loading = loading,
                    nfcState = nfcState,
                    errorMessage = errorMessage,
                    onErrorShown = { errorMessage = null })
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
                    null
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
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

enum class NfcState { READY, DISABLED, UNAVAILABLE }

@Composable
fun MainScreen(
    card: AvanzaCard? = null,
    loading: Boolean = false,
    nfcState: NfcState = NfcState.READY,
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
            when {
                loading -> LoadingState()
                card != null && card.type != CardType.TOP_UP -> UnsupportedCardScreen(card.type)
                card != null -> CardDetails(card)
                nfcState == NfcState.UNAVAILABLE -> InfoScreen(
                    title = "NFC not available",
                    subtitle = "This device doesn't have NFC, which is required to read your card.",
                )
                nfcState == NfcState.DISABLED -> InfoScreen(
                    title = "Turn on NFC",
                    subtitle = "Enable NFC in your device settings, then hold your Avanza card to the back of your phone.",
                )
                else -> ScanPrompt()
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text(
            text = "Reading your card…",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ScanPrompt(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val transition = rememberInfiniteTransition(label = "scan-pulse")
        val pulse by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "pulse",
        )

        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer {
                        val scale = 0.7f + pulse * 0.6f
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - pulse
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            )
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                ContactlessIcon(
                    modifier = Modifier.size(60.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text = "Scan your card",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Hold your Avanza card against the back of your phone to see its balance",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun UnsupportedCardScreen(type: CardType, modifier: Modifier = Modifier) {
    InfoScreen(
        title = "Card not supported yet",
        subtitle = "This looks like a ${type.label.lowercase()}. The app currently reads only " +
                "top-up (pay-per-ride) cards.",
        modifier = modifier,
    )
}

@Composable
private fun InfoScreen(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            ContactlessIcon(
                modifier = Modifier.size(56.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun CardDetails(card: AvanzaCard, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { BalanceCard(card) }
        item { CardIdRow(card.id) }

        if (card.transactions.isNotEmpty()) {
            item {
                Text(
                    text = "Recent activity",
                    modifier = Modifier.padding(top = 12.dp, start = 4.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            items(card.transactions) { transaction ->
                TransactionRow(transaction)
            }
        }
    }
}

@Composable
private fun BalanceCard(card: AvanzaCard) {
    Card(
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
                    text = card.type.label,
                    style = MaterialTheme.typography.labelLarge,
                )
                ContactlessIcon(
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Spacer(Modifier.height(40.dp))

            Text(
                text = "Balance",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "€${"%.2f".format(card.balance / 1000.0)}",
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
            text = "Card ID",
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

private val TRANSACTION_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy")
private val TRANSACTION_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

@Composable
private fun TransactionRow(transaction: AvanzaTransaction) {
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
                text = when (transaction.kind) {
                    TransactionKind.TOP_UP -> "+"
                    TransactionKind.RIDE -> transaction.line?.toString() ?: "·"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when (transaction.kind) {
                    TransactionKind.TOP_UP -> "Top-up"
                    TransactionKind.RIDE ->
                        if (transaction.line != null) "Bus ride · Line ${transaction.line}" else "Bus ride"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${transaction.dateTime.format(TRANSACTION_DATE_FORMAT)} · " +
                        transaction.dateTime.format(TRANSACTION_TIME_FORMAT),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ContactlessIcon(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.08f
        val centerX = size.width * 0.1f
        val centerY = size.height / 2f
        val outerRadius = size.width * 0.72f
        val arcs = 3
        for (i in 1..arcs) {
            val radius = outerRadius * i / arcs
            drawArc(
                color = color,
                startAngle = -52f,
                sweepAngle = 104f,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
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

private val previewCard = AvanzaCard(
    id = "BE123456",
    type = CardType.TOP_UP,
    balance = 1234,
    transactions = listOf(
        AvanzaTransaction(TransactionKind.RIDE, LocalDateTime.of(2026, 2, 14, 18, 45), line = 35, direction = 2),
        AvanzaTransaction(TransactionKind.TOP_UP, LocalDateTime.of(2026, 2, 12, 9, 10)),
        AvanzaTransaction(TransactionKind.RIDE, LocalDateTime.of(2026, 2, 11, 8, 32), line = 22, direction = 1),
    ),
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenCardDetailsPreview() {
    ZGZAvanzaCardTheme {
        MainScreen(card = previewCard)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenUnsupportedCardPreview() {
    ZGZAvanzaCardTheme {
        MainScreen(
            card = AvanzaCard(id = "BP987654", type = CardType.PERSONAL_UNLIMITED, balance = 0),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenNfcDisabledPreview() {
    ZGZAvanzaCardTheme {
        MainScreen(nfcState = NfcState.DISABLED)
    }
}
