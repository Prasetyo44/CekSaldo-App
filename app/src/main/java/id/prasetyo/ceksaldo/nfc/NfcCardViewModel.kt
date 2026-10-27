package id.prasetyo.ceksaldo.nfc

import android.app.Application
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NfcStatus { READY, DISABLED, UNSUPPORTED }

sealed interface CardScreenState {
    data object Waiting : CardScreenState
    data object Reading : CardScreenState
    data class Result(val outcome: ReadOutcome) : CardScreenState
}

class NfcCardViewModel(app: Application) : AndroidViewModel(app) {
    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(app)

    private val _nfc = MutableStateFlow(currentStatus())
    val nfc: StateFlow<NfcStatus> = _nfc.asStateFlow()

    private val _state = MutableStateFlow<CardScreenState>(CardScreenState.Waiting)
    val state: StateFlow<CardScreenState> = _state.asStateFlow()

    private fun currentStatus() = when {
        adapter == null -> NfcStatus.UNSUPPORTED
        !adapter.isEnabled -> NfcStatus.DISABLED
        else -> NfcStatus.READY
    }

    fun refreshNfc() {
        _nfc.value = currentStatus()
    }

    fun reset() {
        _state.value = CardScreenState.Waiting
    }

    /** Dipanggil dari thread NFC (reader mode), jadi boleh memblokir selama kartu dibaca. */
    fun onTag(tag: Tag) {
        _state.value = CardScreenState.Reading
        val chipId = tag.id.hex()
        val isoDep = IsoDep.get(tag)
        val outcome = if (isoDep == null) {
            ReadOutcome.NotRecognized("Teknologi: ${tag.techList.joinToString { it.substringAfterLast('.') }}\n")
        } else {
            try {
                isoDep.connect()
                isoDep.timeout = 5000
                EmoneyReader.read({ isoDep.transceive(it) }, chipId)
            } catch (e: Exception) {
                ReadOutcome.Failed("Kartu terlepas sebelum selesai dibaca. Tempelkan lagi dan tahan 2-3 detik.", e.toString())
            } finally {
                runCatching { isoDep.close() }
            }
        }
        _state.value = CardScreenState.Result(outcome)
    }
}
