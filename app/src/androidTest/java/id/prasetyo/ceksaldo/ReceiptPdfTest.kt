package id.prasetyo.ceksaldo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.prasetyo.ceksaldo.nfc.CardTransaction
import id.prasetyo.ceksaldo.nfc.EmoneyCard
import id.prasetyo.ceksaldo.nfc.Receipt
import id.prasetyo.ceksaldo.nfc.TxKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Memastikan struk PDF benar-benar terbentuk di perangkat Android. */
@RunWith(AndroidJUnit4::class)
class ReceiptPdfTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val card = EmoneyCard(
        brand = "Mandiri e-money",
        chipId = "04A1B2C3D4",
        balance = 85_500,
        transactions = listOf(
            CardTransaction(1_761_550_205_000, 12_000, TxKind.PAYMENT, place = "GT CIKUPA", terminalId = "47542043494B"),
            CardTransaction(1_761_500_000_000, 100_000, TxKind.TOP_UP),
        ),
    )

    @Test
    fun createsSingleReceiptPdf() {
        val file = Receipt.writePdf(context, Receipt.forTransaction(card, card.transactions[0]), "tes-struk")
        assertTrue(file.length() > 500)
        assertEquals("%PDF", file.readBytes().copyOfRange(0, 4).decodeToString())
    }

    @Test
    fun createsStatementPdf() {
        val file = Receipt.writePdf(context, Receipt.forStatement(card, card.transactions), "tes-riwayat")
        assertTrue(file.length() > 500)
    }
}
