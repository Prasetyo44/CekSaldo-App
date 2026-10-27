package id.prasetyo.ceksaldo.nfc

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class TxKind(val label: String) { PAYMENT("Pembayaran"), TOP_UP("Top up") }

data class CardTransaction(
    /** Waktu transaksi (epoch millis). */
    val time: Long,
    val amount: Long,
    val kind: TxKind,
    /** Nama lokasi/gerbang jika kartu menyimpannya dalam bentuk teks. */
    val place: String? = null,
    val terminalId: String? = null,
)

data class EmoneyCard(
    val brand: String,
    /** UID chip NFC dalam hex, bukan nomor kartu yang tercetak. */
    val chipId: String,
    /** null jika saldo tidak berhasil dibaca. */
    val balance: Long?,
    val transactions: List<CardTransaction>,
    val readAt: Long = System.currentTimeMillis(),
)

object Format {
    val WIB: TimeZone = TimeZone.getTimeZone("Asia/Jakarta")
    private val ID = Locale.forLanguageTag("id-ID")

    fun rupiah(amount: Long): String =
        "Rp " + NumberFormat.getIntegerInstance(ID).format(amount)

    fun dateTime(millis: Long): String =
        SimpleDateFormat("dd MMM yyyy, HH:mm:ss", ID).apply { timeZone = WIB }.format(Date(millis)) + " WIB"

    fun fileStamp(millis: Long): String =
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).apply { timeZone = WIB }.format(Date(millis))

    fun maskedChipId(chipId: String): String =
        if (chipId.length <= 4) chipId else "•••• " + chipId.takeLast(4)
}
