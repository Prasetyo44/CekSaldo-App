package id.prasetyo.ceksaldo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.prasetyo.ceksaldo.MainViewModel
import id.prasetyo.ceksaldo.nfc.CardScreenState
import id.prasetyo.ceksaldo.nfc.NfcCardViewModel

@Composable
fun AppRoot(main: MainViewModel, nfc: NfcCardViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val cardState by nfc.state.collectAsStateWithLifecycle()

    // Kartu ditempel saat sedang di tab lain: langsung pindah ke tab kartu.
    LaunchedEffect(cardState) {
        if (cardState is CardScreenState.Reading) tab = 1
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Outlined.AccountBalanceWallet, null) },
                    label = { Text("Saldo") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Outlined.Contactless, null) },
                    label = { Text("Kartu NFC") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
            if (tab == 0) HomeScreen(main) else NfcScreen(nfc)
        }
    }
}
