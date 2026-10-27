package id.prasetyo.ceksaldo

import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import id.prasetyo.ceksaldo.nfc.NfcCardViewModel
import id.prasetyo.ceksaldo.ui.AppRoot
import id.prasetyo.ceksaldo.ui.CekSaldoTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private val nfcVm: NfcCardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CekSaldoTheme {
                AppRoot(vm, nfcVm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcVm.refreshNfc()
        // Reader mode: selama aplikasi terbuka, kartu yang ditempel langsung dibaca aplikasi ini.
        NfcAdapter.getDefaultAdapter(this)?.takeIf { it.isEnabled }?.enableReaderMode(
            this,
            { tag -> nfcVm.onTag(tag) },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            Bundle().apply { putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250) },
        )
    }

    override fun onPause() {
        super.onPause()
        NfcAdapter.getDefaultAdapter(this)?.disableReaderMode(this)
    }
}
