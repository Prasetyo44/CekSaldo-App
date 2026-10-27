package id.prasetyo.ceksaldo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.prasetyo.ceksaldo.data.Category
import id.prasetyo.ceksaldo.data.Provider
import id.prasetyo.ceksaldo.data.SimChoice

private val PALETTE = listOf(
    0xFF0E7C66, 0xFF0060AF, 0xFF00529C, 0xFFF15A23, 0xFFE60012,
    0xFF4C3494, 0xFF00AA13, 0xFF108EE9, 0xFFF7A500, 0xFF2B2B2B,
)

/** Form tambah/ubah. [initial] null berarti entri baru. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProviderDialog(
    initial: Provider?,
    onDismiss: () -> Unit,
    onSave: (Provider) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: Category.BANK) }
    var color by remember { mutableLongStateOf(initial?.color ?: PALETTE.first()) }
    var ussd by remember { mutableStateOf(initial?.ussd ?: "") }
    var inline by remember { mutableStateOf(initial?.inlineUssd ?: false) }
    var smsNumber by remember { mutableStateOf(initial?.smsNumber ?: "") }
    var smsText by remember { mutableStateOf(initial?.smsText ?: "") }
    var appPackage by remember { mutableStateOf(initial?.appPackage ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }

    val ussdValid = ussd.isBlank() || Regex("""^[*#][0-9*#]+#$""").matches(ussd.trim())
    val canSave = name.isNotBlank() && ussdValid &&
        (ussd.isNotBlank() || smsNumber.isNotBlank() || appPackage.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah tujuan" else "Ubah ${initial.name}") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Nama") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Category.entries.forEach {
                        FilterChip(selected = category == it, onClick = { category = it }, label = { Text(it.label) })
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PALETTE.forEach { c ->
                        val selected = c == color
                        Spacer(
                            Modifier
                                .size(22.dp)
                                .background(Color(c), CircleShape)
                                .then(
                                    if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { color = c }
                        )
                    }
                }

                OutlinedTextField(
                    ussd, { ussd = it },
                    label = { Text("Kode USSD (opsional)") },
                    placeholder = { Text("*123#") },
                    isError = !ussdValid,
                    supportingText = { if (!ussdValid) Text("Format kode diawali * dan diakhiri #") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tampilkan balasan di aplikasi", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = inline, onCheckedChange = { inline = it })
                }
                Text(
                    "Cocok untuk kode satu langkah seperti cek pulsa. Menu bank bertingkat tetap dibuka lewat Telepon.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(smsNumber, { smsNumber = it }, label = { Text("Nomor SMS banking (opsional)") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(smsText, { smsText = it }, label = { Text("Isi SMS, tanpa PIN (opsional)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(appPackage, { appPackage = it }, label = { Text("Nama paket aplikasi (opsional)") },
                    placeholder = { Text("com.contoh.bank") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text("Catatan (opsional)") },
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        Provider(
                            id = initial?.id ?: "custom-${System.currentTimeMillis()}",
                            name = name.trim(),
                            category = category,
                            color = color,
                            ussd = ussd.trim().ifBlank { null },
                            smsNumber = smsNumber.trim().ifBlank { null },
                            smsText = smsText.trim().ifBlank { null },
                            appPackage = appPackage.trim().ifBlank { null },
                            note = note.trim().ifBlank { null },
                            favorite = initial?.favorite ?: false,
                            inlineUssd = inline,
                        )
                    )
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
fun SettingsDialog(
    sim: SimChoice,
    onSimChange: (SimChoice) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmReset by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pengaturan") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("SIM untuk USSD", style = MaterialTheme.typography.titleSmall)
                SimChoice.entries.forEach { choice ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = sim == choice, onClick = { onSimChange(choice) })
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = sim == choice, onClick = null)
                        Text(choice.label, Modifier.padding(start = 8.dp))
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                TextButton(onClick = { confirmReset = true }) { Text("Kembalikan daftar bawaan") }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Text("Privasi", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Cek Saldo hanya membuka kode USSD, SMS, atau aplikasi resmi bank dan e-wallet. " +
                        "Aplikasi ini tidak meminta, menyimpan, atau mengirim PIN, password, maupun data rekening. " +
                        "Tarif USSD/SMS mengikuti operator. Kode bisa berubah, cek situs resmi bank jika gagal.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
    )

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Kembalikan daftar bawaan?") },
            text = { Text("Entri yang kamu tambah atau ubah akan hilang.") },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; onReset() }) { Text("Kembalikan") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Batal") } },
        )
    }
}

/** Status cek saldo inline: menunggu, berhasil, atau gagal. */
sealed interface UssdState {
    data class Loading(val name: String) : UssdState
    data class Done(val name: String, val text: String) : UssdState
    data class Failed(val name: String, val message: String) : UssdState
}

@Composable
fun UssdResultDialog(state: UssdState, onDismiss: () -> Unit) {
    val title = when (state) {
        is UssdState.Loading -> state.name
        is UssdState.Done -> state.name
        is UssdState.Failed -> state.name
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            when (state) {
                is UssdState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Text("Menunggu balasan operator…", Modifier.padding(start = 16.dp))
                }
                is UssdState.Done -> Text(state.text)
                is UssdState.Failed -> Text(state.message, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
    )
}
