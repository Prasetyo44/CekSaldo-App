package id.prasetyo.ceksaldo.nfc

import java.util.Calendar

/**
 * Pembaca kartu uang elektronik Indonesia. Semua perintah hanya MEMBACA (SELECT, READ RECORD,
 * READ BINARY, GET VALUE); aplikasi tidak pernah menulis atau mengubah saldo kartu.
 *
 * Urutan perintah dan format data mengacu pada riset terbuka proyek KUE Reader SDK
 * (github.com/muhakmal/osp-kue-reader, Apache 2.0).
 */
internal interface CardReader {
    /** null jika kartu bukan jenis ini. */
    fun read(apdu: Apdu, chipId: String, log: StringBuilder): ReadOutcome?
}

sealed interface ReadOutcome {
    val log: String

    data class Success(val card: EmoneyCard, override val log: String = "") : ReadOutcome

    /** Kartu dikenali tapi isinya terkunci (mis. Flazz generasi 1). */
    data class Locked(val brand: String, val reason: String, override val log: String = "") : ReadOutcome

    data class NotRecognized(override val log: String = "") : ReadOutcome

    data class Failed(val message: String, override val log: String = "") : ReadOutcome
}

private fun StringBuilder.trace(label: String, cmd: ByteArray, resp: ByteArray) {
    append(label).append(": ").append(cmd.hex()).append(" -> ").append(resp.hex()).append('\n')
}

internal object MandiriReader : CardReader {
    private val AIDS = listOf(
        bytes(0xA0, 0x00, 0x00, 0x00, 0x18, 0x30, 0x00, 0x01),
        bytes(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01),
    )
    private const val RECORD = 24

    override fun read(apdu: Apdu, chipId: String, log: StringBuilder): ReadOutcome? {
        if (AIDS.none { apdu.select(it) }) return null
        log.append("Terdeteksi: Mandiri e-money\n")
        val card = EmoneyCard("Mandiri e-money", chipId, balance(apdu, log), history(apdu, log))
        return ReadOutcome.Success(card)
    }

    private fun balance(apdu: Apdu, log: StringBuilder): Long? {
        val rec = bytes(0x00, 0xB2, 0x01, 0x04, 0x00)
        val r1 = apdu.send(rec).also { log.trace("saldo", rec, it) }
        if (r1.ok() && r1.data().size >= 16) return r1.uintBE(12, 16)

        val direct = bytes(0x00, 0xB5, 0x00, 0x00, 0x0A)
        val r2 = apdu.send(direct).also { log.trace("saldo-2", direct, it) }
        if (r2.ok() && r2.data().size >= 4) return r2.uintLE(0, 4)
        return null
    }

    private fun history(apdu: Apdu, log: StringBuilder): List<CardTransaction> {
        val raw = mutableListOf<Byte>()
        for (i in 0 until 32) {
            val resp = runCatching { apdu.send(bytes(0x00, 0xD1, i, 0x00, 0x00)) }.getOrNull() ?: break
            if (!resp.ok()) break
            val data = resp.data()
            if (data.all { it == 0.toByte() }) break
            raw += data.toList()
        }
        val newFormat = raw.toByteArray().toList().chunked(RECORD)
            .filter { it.size == RECORD }
            .mapNotNull { decodeNew(it.toByteArray()) }
        if (newFormat.isNotEmpty()) return newFormat.distinctBy { it.time to it.amount }

        // Kartu lama: catatan transaksi lewat READ RECORD dengan tanggal BCD.
        log.append("Riwayat format baru kosong, mencoba format lama\n")
        val old = (0..9).mapNotNull { i ->
            val resp = runCatching { apdu.send(bytes(0x00, 0xB2, i, 0x00, 0x00)) }.getOrNull()
            if (resp == null || !resp.ok() || resp.size < 32) null else decodeOld(resp.copyOfRange(0, 30))
        }
        return old.distinctBy { it.time to it.amount }
    }

    /** Tanggal DDMMYY dan jam HHMMSS dalam BCD, terminal di byte 7..13, jenis 14..15, nominal LE 16..19. */
    internal fun decodeNew(r: ByteArray): CardTransaction? {
        val d = (0..5).map { r[it].bcd() ?: return null }
        val time = wibTime(2000 + d[2], d[1], d[0], d[3], d[4], d[5]) ?: return null
        val terminal = r.copyOfRange(7, 14)
        val topUp = r[14] == 0x01.toByte() && r[15] == 0x00.toByte()
        return CardTransaction(
            time = time,
            amount = r.uintLE(16, 20),
            kind = if (topUp) TxKind.TOP_UP else TxKind.PAYMENT,
            place = printableAscii(terminal),
            terminalId = terminal.hex(),
        )
    }

    /** Format lama: YYMMDDhhmmss BCD, nominal BE di byte 10..13. */
    internal fun decodeOld(r: ByteArray): CardTransaction? {
        val p = (0..5).map { r[it].bcd() ?: return null }
        val time = wibTime(2000 + p[0], p[1], p[2], p[3], p[4], p[5]) ?: return null
        val amount = r.uintBE(10, 14)
        if (amount <= 0) return null
        val topUp = (18 until 30).any { r[it] == 0x64.toByte() }
        return CardTransaction(time, amount, if (topUp) TxKind.TOP_UP else TxKind.PAYMENT)
    }
}

internal object TapCashReader : CardReader {
    private val AID = bytes(0x11, 0x22, 0x33, 0x44, 0x55, 0x66)

    override fun read(apdu: Apdu, chipId: String, log: StringBuilder): ReadOutcome? {
        if (!apdu.select(AID)) return null
        log.append("Terdeteksi: BNI TapCash\n")

        val balCmd = bytes(0x90, 0x32, 0x03, 0x00, 0x00)
        val bal = apdu.send(balCmd).also { log.trace("saldo", balCmd, it) }
        val balance = if (bal.ok() && bal.size >= 5) bal.uintBE(2, 5) else null

        val history = (1..10).asSequence()
            .map { i -> runCatching { apdu.send(bytes(0x90, 0x32, 0x01, 0x00, 0x01, i, 0x00)) }.getOrNull() }
            .takeWhile { it != null && it.ok() && it.size >= 18 }
            .mapNotNull { decode(it!!.copyOfRange(0, 16)) }
            .distinctBy { it.time to it.amount }
            .toList()

        return ReadOutcome.Success(EmoneyCard("BNI TapCash", chipId, balance, history))
    }

    /** Jenis di byte 0, nominal BE 1..3, detik sejak 1 Jan 1995 WIB di 4..7, terminal 8..13. */
    internal fun decode(r: ByteArray): CardTransaction? {
        val kind = r[0].toInt() and 0xFF
        val amount = r.uintBE(1, 4)
        if (amount <= 0) return null
        val epoch1995 = Calendar.getInstance(Format.WIB).apply { clear(); set(1995, 0, 1, 0, 0, 0) }.timeInMillis
        return CardTransaction(
            time = epoch1995 + r.uintBE(4, 8) * 1000,
            amount = amount,
            kind = if (kind == 0x03 || kind == 0x04) TxKind.TOP_UP else TxKind.PAYMENT,
            terminalId = r.copyOfRange(8, 14).hex(),
        )
    }
}

internal object BrizziReader : CardReader {
    private const val RECORD = 32

    override fun read(apdu: Apdu, chipId: String, log: StringBuilder): ReadOutcome? {
        val selectApp = bytes(0x90, 0x5A, 0x00, 0x00, 0x03, 0x01, 0x00, 0x00, 0x00)
        val sel = runCatching { apdu.send(selectApp) }.getOrNull() ?: return null
        if (!sel.desfireOk() && !sel.ok()) return null

        val valueCmd = bytes(0x90, 0x6C, 0x00, 0x00, 0x01, 0x00, 0x00)
        val value = apdu.send(valueCmd).also { log.trace("saldo", valueCmd, it) }
        if (!value.desfireOk() || value.data().size < 4) return null
        log.append("Terdeteksi: BRI Brizzi\n")

        val raw = mutableListOf<Byte>()
        runCatching {
            var resp = apdu.send(bytes(0x90, 0xBB, 0x00, 0x00, 0x07, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
            var guard = 0
            while ((resp.desfireOk() || resp.sw(0x91, 0xAF)) && guard++ < 32) {
                raw += resp.data().toList()
                if (!resp.sw(0x91, 0xAF)) break
                resp = apdu.send(bytes(0x90, 0xAF, 0x00, 0x00, 0x00))
            }
        }.onFailure { log.append("riwayat: ${it.message}\n") }

        val history = raw.chunked(RECORD)
            .filter { it.size == RECORD }
            .mapNotNull { decode(it.toByteArray()) }
            .distinctBy { it.time to it.amount }

        return ReadOutcome.Success(EmoneyCard("BRI Brizzi", chipId, value.uintLE(0, 4), history))
    }

    /** Jam (h,m,s) di byte 16..18, tanggal (d,m,y) 19..21, jenis 22, nominal LE 23..25. */
    internal fun decode(r: ByteArray): CardTransaction? {
        fun b(i: Int) = r[i].toInt() and 0xFF
        val time = wibTime(2000 + b(21), b(20), b(19), b(16), b(17), b(18)) ?: return null
        val amount = r.uintLE(23, 26)
        if (amount <= 0) return null
        val topUp = b(22) == 0xEC || b(22) == 0xED
        return CardTransaction(time, amount, if (topUp) TxKind.TOP_UP else TxKind.PAYMENT)
    }
}

internal object FlazzReader : CardReader {
    private val GEN2_AID = bytes(0xA0, 0x00, 0x00, 0x00, 0x18, 0x0F, 0x00, 0x00, 0x01, 0x80, 0x01)
    private val GEN1_AID = bytes(0xA0, 0x00, 0x00, 0x00, 0x03, 0x86, 0x98, 0x07, 0x01)

    override fun read(apdu: Apdu, chipId: String, log: StringBuilder): ReadOutcome? {
        if (apdu.select(GEN2_AID)) {
            log.append("Terdeteksi: BCA Flazz generasi 2\n")
            readGen2(apdu, chipId, log)?.let { return it }
        }
        if (apdu.select(GEN1_AID)) {
            log.append("Terdeteksi: BCA Flazz generasi 1\n")
            return ReadOutcome.Locked(
                "BCA Flazz (generasi 1)",
                "Saldo kartu Flazz generasi lama dienkripsi dan hanya bisa dibaca oleh mesin/aplikasi resmi BCA. " +
                    "Cek lewat aplikasi Flazz BCA, atau tukar ke kartu Flazz terbaru di kantor cabang BCA.",
            )
        }
        return null
    }

    private fun readGen2(apdu: Apdu, chipId: String, log: StringBuilder): ReadOutcome? {
        val selFile = bytes(0x00, 0xA4, 0x01, 0x00, 0x02, 0x02, 0x00)
        val f = apdu.send(selFile).also { log.trace("file", selFile, it) }
        if (!f.ok()) return null
        val info = bytes(0x00, 0xB0, 0x81, 0x00, 0x8E)
        val i = apdu.send(info).also { log.trace("info", info, it) }
        if (!i.ok()) return null
        val balCmd = bytes(0x80, 0x32, 0x00, 0x03, 0x04, 0x00, 0x00, 0x00, 0x00)
        val bal = apdu.send(balCmd).also { log.trace("saldo", balCmd, it) }
        if (bal.size < 4) return null
        val balance = if (bal.size >= 5) bal.uintBE(1, 4) else null

        val history = mutableListOf<CardTransaction>()
        runCatching {
            apdu.send(bytes(0x00, 0x00, 0x40, 0x0B, 0x00, 0x00, 0x04, 0x02, 0x00, 0x00))
            for (sfi in listOf(0x84, 0x85)) {
                for (n in 0 until 16) {
                    val resp = apdu.send(bytes(0x00, 0xB0, sfi, n * 15, 0x3C))
                    if (!resp.ok() || resp.size < 62) break
                    decode(resp.data())?.let { history += it }
                }
            }
        }.onFailure { log.append("riwayat: ${it.message}\n") }

        val card = EmoneyCard("BCA Flazz", chipId, balance, history.distinctBy { it.time to it.amount })
        return ReadOutcome.Success(card)
    }

    /** Jenis BE 0..1 (0x0400 = pembayaran), nominal BE 6..8, lokasi 30..37, waktu Unix BE 38..41. */
    internal fun decode(r: ByteArray): CardTransaction? {
        if (r.size < 60) return null
        val amount = r.uintBE(6, 9)
        if (amount <= 0) return null
        val location = r.copyOfRange(30, 38)
        return CardTransaction(
            time = r.uintBE(38, 42) * 1000,
            amount = amount,
            kind = if (r.uintBE(0, 2) == 0x0400L) TxKind.PAYMENT else TxKind.TOP_UP,
            place = printableAscii(location, minLength = 2),
            terminalId = location.hex(),
        )
    }
}

/** Mencoba semua pembaca secara berurutan pada satu koneksi kartu. */
object EmoneyReader {
    private val readers = listOf(MandiriReader, TapCashReader, BrizziReader, FlazzReader)

    fun read(apdu: Apdu, chipId: String): ReadOutcome {
        val log = StringBuilder()
        for (reader in readers) {
            val result = try {
                reader.read(apdu, chipId, log)
            } catch (e: Exception) {
                log.append("${reader::class.simpleName}: ${e.message}\n")
                null
            }
            if (result != null) return withLog(result, log.toString())
        }
        return ReadOutcome.NotRecognized(log.toString())
    }

    private fun withLog(result: ReadOutcome, log: String): ReadOutcome = when (result) {
        is ReadOutcome.Success -> result.copy(
            card = result.card.copy(transactions = result.card.transactions.sortedByDescending { it.time }),
            log = log,
        )
        is ReadOutcome.Locked -> result.copy(log = log)
        is ReadOutcome.NotRecognized -> result.copy(log = log)
        is ReadOutcome.Failed -> result.copy(log = log)
    }
}
