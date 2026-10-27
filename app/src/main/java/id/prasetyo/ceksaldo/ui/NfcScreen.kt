package id.prasetyo.ceksaldo.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.prasetyo.ceksaldo.nfc.CardScreenState
import id.prasetyo.ceksaldo.nfc.CardTransaction
import id.prasetyo.ceksaldo.nfc.EmoneyCard
import id.prasetyo.ceksaldo.nfc.Format
import id.prasetyo.ceksaldo.nfc.NfcCardViewModel
import id.prasetyo.ceksaldo.nfc.NfcStatus
import id.prasetyo.ceksaldo.nfc.ReadOutcome
import id.prasetyo.ceksaldo.nfc.Receipt
import id.prasetyo.ceksaldo.nfc.TxKind

private val SUPPORTED = listOf("Mandiri e-money", "BCA Flazz (baru)", "BNI TapCash", "BRI Brizzi")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcScreen(vm: NfcCardViewModel) {
    val nfc by vm.nfc.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kartu e-Money") },
                actions = {
                    if (state is CardScreenState.Result) {
                        IconButton(onClick = vm::reset) { Icon(Icons.Outlined.Refresh, "Baca kartu lain") }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                nfc == NfcStatus.UNSUPPORTED -> Message(
                    Icons.Outlined.ErrorOutline,
                    "HP ini tidak punya NFC",
                    "Cek saldo kartu e-money butuh HP dengan fitur NFC.",
                )
                nfc == NfcStatus.DISABLED -> {
                    val context = LocalContext.current
                    Message(Icons.Outlined.Contactless, "NFC sedang mati", "Nyalakan NFC untuk membaca kartu e-money.") {
                        Button(onClick = { openNfcSettings(context) }) { Text("Buka pengaturan NFC") }
                    }
                }
                else -> when (val s = state) {
                    CardScreenState.Waiting -> Message(
                        Icons.Outlined.Contactless,
                        "Tempelkan kartu e-money",
                        "Letakkan kartu di bagian belakang HP (dekat kamera) dan tahan 2-3 detik.\n\nDidukung: ${SUPPORTED.joinToString()}.",
                    )
                    CardScreenState.Reading -> Message(null, "Membaca kartu…", "Jangan lepas kartu dulu.") {
                        CircularProgressIndicator()
                    }
                    is CardScreenState.Result -> ResultView(s.outcome, onRetry = vm::reset)
                }
            }
        }
    }
}

@Composable
private fun ResultView(outcome: ReadOutcome, onRetry: () -> Unit) {
    val context = LocalContext.current
    when (outcome) {
        is ReadOutcome.Success -> CardResult(outcome.card)
        is ReadOutcome.Locked -> Message(Icons.Outlined.Lock, outcome.brand, outcome.reason) {
            OutlinedButton(onClick = onRetry) { Text("Coba kartu lain") }
        }
        is ReadOutcome.NotRecognized -> Message(
            Icons.Outlined.ErrorOutline,
            "Kartu tidak dikenali",
            "Kartu ini bukan kartu e-money yang didukung, atau kartu terlepas terlalu cepat. Coba tempel lagi.",
        ) {
            OutlinedButton(onClick = onRetry) { Text("Coba lagi") }
            TextButton(onClick = { shareLog(context, outcome.log) }) { Text("Kirim log diagnostik") }
        }
        is ReadOutcome.Failed -> Message(Icons.Outlined.ErrorOutline, "Gagal membaca kartu", outcome.message) {
            OutlinedButton(onClick = onRetry) { Text("Coba lagi") }
            TextButton(onClick = { shareLog(context, outcome.log) }) { Text("Kirim log diagnostik") }
        }
    }
}

private enum class TxFilter(val label: String) { ALL("Semua"), PAYMENT("Pembayaran"), TOP_UP("Top up") }

@Composable
private fun CardResult(card: EmoneyCard) {
    val context = LocalContext.current
    var filter by remember { mutableStateOf(TxFilter.ALL) }
    var selected by remember { mutableStateOf<CardTransaction?>(null) }
    val shown = card.transactions.filter {
        when (filter) {
            TxFilter.ALL -> true
            TxFilter.PAYMENT -> it.kind == TxKind.PAYMENT
            TxFilter.TOP_UP -> it.kind == TxKind.TOP_UP
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(20.dp),
                    )
                    .padding(20.dp),
            ) {
                Text(card.brand, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    "ID chip ${Format.maskedChipId(card.chipId)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(12.dp))
                Text("Saldo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    card.balance?.let(Format::rupiah) ?: "Tidak terbaca",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Dibaca ${Format.dateTime(card.readAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Riwayat di kartu", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (shown.isNotEmpty()) {
                    FilledTonalButton(onClick = {
                        val file = Receipt.writePdf(
                            context, Receipt.forStatement(card, shown),
                            "riwayat-${Format.fileStamp(card.readAt)}",
                        )
                        Receipt.share(context, file)
                    }) {
                        Icon(Icons.Outlined.Receipt, null, Modifier.size(18.dp))
                        Text("Struk semua", Modifier.padding(start = 6.dp))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TxFilter.entries.forEach {
                    FilterChip(selected = filter == it, onClick = { filter = it }, label = { Text(it.label) })
                }
            }
        }

        if (shown.isEmpty()) {
            item {
                Text(
                    if (card.transactions.isEmpty())
                        "Kartu ini tidak menyimpan riwayat transaksi yang bisa dibaca."
                    else "Tidak ada transaksi untuk filter ini.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }

        items(shown) { tx ->
            TxRow(tx) { selected = tx }
            HorizontalDivider()
        }

        item {
            Text(
                "Kartu hanya menyimpan beberapa transaksi terakhir. Ketuk transaksi untuk membuat struknya, " +
                    "misalnya untuk bukti bayar tol (gerbang tol Jasa Marga, Astra Infra, dan lainnya memakai kartu yang sama).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    selected?.let { tx -> ReceiptDialog(card, tx, onDismiss = { selected = null }) }
}

@Composable
private fun TxRow(tx: CardTransaction, onClick: () -> Unit) {
    val topUp = tx.kind == TxKind.TOP_UP
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(tx.place ?: tx.kind.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                Format.dateTime(tx.time) + if (tx.place != null) " · ${tx.kind.label}" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            (if (topUp) "+" else "-") + Format.rupiah(tx.amount),
            fontWeight = FontWeight.SemiBold,
            color = if (topUp) Color(0xFF1B8A4B) else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ReceiptDialog(card: EmoneyCard, tx: CardTransaction, onDismiss: () -> Unit) {
    val context = LocalContext.current
    fun file() = Receipt.writePdf(context, Receipt.forTransaction(card, tx), "struk-${Format.fileStamp(tx.time)}")

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Receipt, null) },
        title = { Text("Struk ${tx.kind.label.lowercase()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(Format.rupiah(tx.amount), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(Format.dateTime(tx.time))
                tx.place?.let { Text("Lokasi: $it") }
                tx.terminalId?.let { Text("Terminal: $it", style = MaterialTheme.typography.bodySmall) }
                Text("Kartu: ${card.brand}", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { Receipt.share(context, file()); onDismiss() }) {
                Icon(Icons.Outlined.Share, null, Modifier.size(18.dp))
                Text("Bagikan PDF", Modifier.padding(start = 6.dp))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                val ok = Receipt.saveToDownloads(context, file())
                Toast.makeText(
                    context,
                    if (ok) "Struk disimpan di Download/CekSaldo" else "Gagal menyimpan, gunakan Bagikan PDF",
                    Toast.LENGTH_SHORT,
                ).show()
                onDismiss()
            }) {
                Icon(Icons.Outlined.Download, null, Modifier.size(18.dp))
                Text("Simpan", Modifier.padding(start = 6.dp))
            }
        },
    )
}

@Composable
private fun Message(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    title: String,
    body: String,
    actions: @Composable () -> Unit = {},
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon?.let { Icon(it, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        actions()
    }
}

private fun openNfcSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun shareLog(context: Context, log: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, "Log baca kartu Cek Saldo")
        .putExtra(Intent.EXTRA_TEXT, log.ifBlank { "(log kosong)" })
    context.startActivity(Intent.createChooser(send, "Kirim log").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
