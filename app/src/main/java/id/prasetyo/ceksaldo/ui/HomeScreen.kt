package id.prasetyo.ceksaldo.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.prasetyo.ceksaldo.FILTERS
import id.prasetyo.ceksaldo.MainViewModel
import id.prasetyo.ceksaldo.data.Provider
import id.prasetyo.ceksaldo.util.BalanceActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val providers by vm.visible.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val sim by vm.sim.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<Provider?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Provider?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var ussdState by remember { mutableStateOf<UssdState?>(null) }

    // Dinaikkan setiap kembali ke aplikasi, agar status "Aplikasi terpasang" ikut diperbarui.
    var resumeTick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumeTick++ }

    // Aksi yang menunggu hasil permintaan izin telepon.
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        pending?.invoke()
        pending = null
    }
    fun withPhonePermission(action: () -> Unit) {
        val needed = buildList {
            if (!BalanceActions.hasPermission(context, Manifest.permission.CALL_PHONE)) add(Manifest.permission.CALL_PHONE)
            if (!BalanceActions.hasPermission(context, Manifest.permission.READ_PHONE_STATE)) add(Manifest.permission.READ_PHONE_STATE)
        }
        if (needed.isEmpty()) action() else {
            pending = action
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        // Ikut memperhitungkan keyboard agar daftar dan tombol Tambah tidak tertutup.
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Cek Saldo") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Outlined.Settings, "Pengaturan")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Tambah") },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.query.value = it },
                placeholder = { Text("Cari bank, e-wallet, operator") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.query.value = "" }) { Icon(Icons.Outlined.Clear, "Hapus") }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(FILTERS) { f ->
                    FilterChip(
                        selected = f == filter,
                        onClick = { vm.filter.value = f },
                        label = { Text(f.label) },
                    )
                }
            }

            if (providers.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (filter.favoritesOnly && query.isBlank())
                            "Belum ada favorit. Ketuk bintang pada bank atau e-wallet yang sering kamu cek."
                        else "Tidak ada yang cocok.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(providers, key = { it.id }) { p ->
                        val installed = remember(p.appPackage, resumeTick) {
                            p.appPackage?.let { BalanceActions.isInstalled(context, it) } ?: false
                        }
                        ProviderCard(
                            provider = p,
                            installed = installed,
                            onFavorite = { vm.toggleFavorite(p.id) },
                            onUssd = {
                                p.ussd?.let { code ->
                                    withPhonePermission { BalanceActions.dialUssd(context, code, sim) }
                                }
                            },
                            onUssdInline = {
                                val code = p.ussd
                                if (code != null) withPhonePermission {
                                    if (!BalanceActions.hasPermission(context, Manifest.permission.CALL_PHONE)) {
                                        ussdState = UssdState.Failed(p.name, "Izin telepon dibutuhkan untuk membaca balasan USSD.")
                                        return@withPhonePermission
                                    }
                                    ussdState = UssdState.Loading(p.name)
                                    BalanceActions.runUssdInline(
                                        context, code, sim,
                                        onResult = { ussdState = UssdState.Done(p.name, it) },
                                        onError = { ussdState = UssdState.Failed(p.name, it) },
                                    )
                                }
                            },
                            onSms = { p.smsNumber?.let { BalanceActions.openSms(context, it, p.smsText) } },
                            onApp = { p.appPackage?.let { BalanceActions.openApp(context, it) } },
                            onEdit = { editing = p },
                            onDelete = { deleting = p },
                        )
                    }
                }
            }
        }
    }

    if (adding || editing != null) {
        EditProviderDialog(
            initial = editing,
            onDismiss = { adding = false; editing = null },
            onSave = {
                vm.save(it)
                adding = false
                editing = null
            },
        )
    }

    deleting?.let { p ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Hapus ${p.name}?") },
            text = { Text("Kamu bisa menambahkannya lagi nanti atau mengembalikan daftar bawaan di Pengaturan.") },
            confirmButton = { TextButton(onClick = { vm.delete(p.id); deleting = null }) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Batal") } },
        )
    }

    if (showSettings) {
        SettingsDialog(
            sim = sim,
            onSimChange = vm::setSim,
            onReset = vm::resetToDefaults,
            onDismiss = { showSettings = false },
        )
    }

    ussdState?.let { UssdResultDialog(it, onDismiss = { ussdState = null }) }
}
