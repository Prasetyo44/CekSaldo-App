package id.prasetyo.ceksaldo.nfc

import java.util.Calendar

/** Saluran kirim-terima APDU. Dipisah dari IsoDep agar parser bisa dites tanpa kartu. */
fun interface Apdu {
    fun send(command: ByteArray): ByteArray
}

internal fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

internal fun ByteArray.hex(): String = joinToString("") { "%02X".format(it) }

internal fun ByteArray.sw(sw1: Int, sw2: Int): Boolean =
    size >= 2 && this[size - 2] == sw1.toByte() && this[size - 1] == sw2.toByte()

/** ISO 7816 sukses (90 00). */
internal fun ByteArray.ok(): Boolean = sw(0x90, 0x00)

/** Perintah DESFire native terbungkus sukses (91 00). */
internal fun ByteArray.desfireOk(): Boolean = sw(0x91, 0x00)

internal fun ByteArray.data(): ByteArray = if (size >= 2) copyOfRange(0, size - 2) else ByteArray(0)

/** Angka unsigned big-endian dari [from] sampai [toExclusive]. */
internal fun ByteArray.uintBE(from: Int, toExclusive: Int): Long {
    var v = 0L
    for (i in from until toExclusive) v = (v shl 8) or (this[i].toLong() and 0xFF)
    return v
}

internal fun ByteArray.uintLE(from: Int, toExclusive: Int): Long {
    var v = 0L
    for (i in toExclusive - 1 downTo from) v = (v shl 8) or (this[i].toLong() and 0xFF)
    return v
}

/** Byte BCD (mis. 0x25 -> 25), atau null jika bukan BCD. */
internal fun Byte.bcd(): Int? {
    val hi = (toInt() shr 4) and 0x0F
    val lo = toInt() and 0x0F
    return if (hi > 9 || lo > 9) null else hi * 10 + lo
}

internal fun Apdu.select(aid: ByteArray): Boolean = try {
    send(bytes(0x00, 0xA4, 0x04, 0x00, aid.size) + aid + bytes(0x00)).ok()
} catch (e: Exception) {
    false
}

/** Teks ASCII yang bisa dicetak dari [raw], atau null jika terlalu pendek untuk berarti. */
internal fun printableAscii(raw: ByteArray, minLength: Int = 3): String? =
    raw.map { it.toInt() and 0xFF }
        .filter { it in 0x20..0x7E }
        .joinToString("") { it.toChar().toString() }
        .trim()
        .takeIf { it.length >= minLength }

/** Waktu WIB dari komponen tanggal; null jika di luar rentang. */
internal fun wibTime(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long? {
    if (month !in 1..12 || day !in 1..31 || hour !in 0..23 || minute !in 0..59 || second !in 0..59) return null
    return Calendar.getInstance(Format.WIB).apply {
        clear()
        set(year, month - 1, day, hour, minute, second)
    }.timeInMillis
}
