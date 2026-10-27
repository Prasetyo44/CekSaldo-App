package id.prasetyo.ceksaldo.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class EmoneyReaderTest {

    /** Kartu palsu: menjawab perintah yang dikenal, selain itu "file tidak ditemukan" (6A82). */
    private fun fakeCard(responses: Map<String, String>) = Apdu { cmd ->
        val hex = responses[cmd.hex()] ?: "6A82"
        ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }

    private fun wib(y: Int, m: Int, d: Int, h: Int, min: Int, s: Int) =
        Calendar.getInstance(Format.WIB).apply { clear(); set(y, m - 1, d, h, min, s) }.timeInMillis

    @Test
    fun readsTapCashBalanceAndHistory() {
        val card = fakeCard(
            mapOf(
                "00A404000611223344556600" to "9000",
                "9032030000" to "00000186A09000",
                // Pembayaran Rp 12.000, 3600 detik setelah 1 Jan 1995 WIB
                "90320100010100" to "01002EE000000E10AABBCCDDEEFF00009000",
            )
        )
        val result = EmoneyReader.read(card, "04A1B2C3") as ReadOutcome.Success
        assertEquals("BNI TapCash", result.card.brand)
        assertEquals(100_000L, result.card.balance)
        val tx = result.card.transactions.single()
        assertEquals(12_000L, tx.amount)
        assertEquals(TxKind.PAYMENT, tx.kind)
        assertEquals(wib(1995, 1, 1, 1, 0, 0), tx.time)
        assertEquals("AABBCCDDEEFF", tx.terminalId)
    }

    @Test
    fun decodesMandiriTollRecord() {
        val record = byteArrayOf(
            0x27, 0x10, 0x26, // 27-10-26
            0x14, 0x30, 0x05, // 14:30:05
            0x00,
            'G'.code.toByte(), 'T'.code.toByte(), ' '.code.toByte(), 'C'.code.toByte(),
            'K'.code.toByte(), 'P'.code.toByte(), 'G'.code.toByte(),
            0x00, 0x00, // pembayaran
            0xE0.toByte(), 0x2E, 0x00, 0x00, // Rp 12.000 (LE)
            0x00, 0x00, 0x00, 0x00,
        )
        val tx = MandiriReader.decodeNew(record)!!
        assertEquals(12_000L, tx.amount)
        assertEquals(TxKind.PAYMENT, tx.kind)
        assertEquals("GT CKPG", tx.place)
        assertEquals(wib(2026, 10, 27, 14, 30, 5), tx.time)
    }

    @Test
    fun oldFlazzIsReportedAsLocked() {
        val card = fakeCard(mapOf("00A4040009A00000000386980701" + "00" to "9000"))
        val result = EmoneyReader.read(card, "01020304")
        assertTrue(result is ReadOutcome.Locked)
    }

    @Test
    fun unknownCardIsNotRecognized() {
        assertTrue(EmoneyReader.read(fakeCard(emptyMap()), "01") is ReadOutcome.NotRecognized)
    }

    @Test
    fun formatsRupiah() {
        assertEquals("Rp 1.250.000", Format.rupiah(1_250_000))
    }
}
