package id.prasetyo.ceksaldo.nfc

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

/**
 * Struk PDF bergaya kertas thermal 80 mm dari data yang dibaca langsung dari chip kartu.
 * Ini catatan pribadi, bukan bukti transaksi resmi dari bank atau operator tol.
 */
object Receipt {
    private const val WIDTH = 226 // 80 mm dalam point
    private const val MARGIN = 14f
    private const val LINE = 13f

    /** Satu baris struk. */
    sealed interface Row
    data class Center(val text: String, val bold: Boolean = false, val size: Float = 9f) : Row
    data class KeyValue(val left: String, val right: String, val bold: Boolean = false) : Row
    data class Wrap(val text: String) : Row
    data object Rule : Row
    data object Gap : Row

    fun forTransaction(card: EmoneyCard, tx: CardTransaction): List<Row> = buildList<Row> {
        header(card)
        add(KeyValue("Waktu", Format.dateTime(tx.time)))
        add(KeyValue("Jenis", tx.kind.label))
        tx.place?.let { add(KeyValue("Lokasi", it)) }
        tx.terminalId?.let { add(KeyValue("Terminal", it)) }
        add(Rule)
        add(KeyValue(if (tx.kind == TxKind.TOP_UP) "ISI SALDO" else "BAYAR", Format.rupiah(tx.amount), bold = true))
        add(Rule)
        footer(card)
    }

    fun forStatement(card: EmoneyCard, txs: List<CardTransaction>): List<Row> = buildList<Row> {
        header(card)
        add(Center("RIWAYAT TRANSAKSI", bold = true))
        add(Gap)
        txs.forEach { tx ->
            add(KeyValue(Format.dateTime(tx.time).substringBefore(" WIB"), ""))
            val sign = if (tx.kind == TxKind.TOP_UP) "+" else "-"
            add(KeyValue("  ${tx.kind.label}${tx.place?.let { " · $it" } ?: ""}", sign + Format.rupiah(tx.amount)))
        }
        add(Rule)
        add(KeyValue("Total bayar", Format.rupiah(txs.filter { it.kind == TxKind.PAYMENT }.sumOf { it.amount }), bold = true))
        add(KeyValue("Total isi saldo", Format.rupiah(txs.filter { it.kind == TxKind.TOP_UP }.sumOf { it.amount })))
        add(Rule)
        footer(card)
    }

    private fun MutableList<Row>.header(card: EmoneyCard) {
        add(Center("CEK SALDO", bold = true, size = 12f))
        add(Center("Struk Kartu Uang Elektronik"))
        add(Rule)
        add(KeyValue("Kartu", card.brand))
        add(KeyValue("ID chip", card.chipId))
        add(Rule)
    }

    private fun MutableList<Row>.footer(card: EmoneyCard) {
        add(KeyValue("Saldo kartu", card.balance?.let(Format::rupiah) ?: "-", bold = true))
        add(KeyValue("Dibaca", Format.dateTime(card.readAt)))
        add(Gap)
        add(Wrap("Dibaca dari chip kartu melalui NFC. Struk ini catatan pribadi, bukan bukti resmi dari bank penerbit atau operator tol."))
    }

    /** Membuat file PDF di cache dan mengembalikannya. [rows] berasal dari [forTransaction] / [forStatement]. */
    fun writePdf(context: Context, rows: List<Row>, name: String): File {
        val items = rows
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; typeface = Typeface.MONOSPACE; textSize = 7.5f }
        val bold = Paint(text).apply { typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) }
        val usable = WIDTH - 2 * MARGIN

        // Pecah teks panjang dulu agar tinggi halaman bisa dihitung.
        val lines = items.flatMap { row ->
            if (row is Wrap) wrap(row.text, text, usable).map { Center(it, size = 7f) } else listOf(row)
        }
        val height = (MARGIN * 2 + lines.sumOf { (if (it is Gap) LINE / 2 else LINE).toDouble() }).toInt() + 10

        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(WIDTH, height, 1).create())
        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)
        var y = MARGIN + 8f
        for (row in lines) {
            when (row) {
                is Center -> {
                    val p = Paint(if (row.bold) bold else text).apply { textSize = row.size; textAlign = Paint.Align.CENTER }
                    canvas.drawText(row.text, WIDTH / 2f, y, p)
                }
                is KeyValue -> {
                    val p = if (row.bold) bold else text
                    val right = Paint(p).apply { textAlign = Paint.Align.RIGHT }
                    val maxLeft = usable - right.measureText(row.right) - 6
                    canvas.drawText(ellipsize(row.left, p, maxLeft), MARGIN, y, p)
                    canvas.drawText(row.right, WIDTH - MARGIN, y, right)
                }
                Rule -> {
                    val dash = Paint(text).apply { strokeWidth = 0.6f; pathEffect = android.graphics.DashPathEffect(floatArrayOf(2f, 2f), 0f) }
                    canvas.drawLine(MARGIN, y - 3, WIDTH - MARGIN, y - 3, dash)
                }
                Gap, is Wrap -> Unit
            }
            y += if (row is Gap) LINE / 2 else LINE
        }
        doc.finishPage(page)

        val dir = File(context.cacheDir, "struk").apply { mkdirs() }
        val file = File(dir, "$name.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "Bagikan struk").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Menyimpan ke folder Download (Android 10+). Mengembalikan false jika tidak didukung/gagal. */
    fun saveToDownloads(context: Context, file: File): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CekSaldo")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } } ?: return false
            true
        }.getOrDefault(false)
    }

    private fun wrap(text: String, paint: Paint, width: Float): List<String> {
        val out = mutableListOf<String>()
        var line = ""
        for (word in text.split(' ')) {
            val next = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(next) > width && line.isNotEmpty()) {
                out += line
                line = word
            } else line = next
        }
        if (line.isNotEmpty()) out += line
        return out
    }

    private fun ellipsize(text: String, paint: Paint, width: Float): String {
        if (paint.measureText(text) <= width) return text
        var t = text
        while (t.isNotEmpty() && paint.measureText("$t…") > width) t = t.dropLast(1)
        return "$t…"
    }
}
