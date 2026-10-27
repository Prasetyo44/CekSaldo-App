package id.prasetyo.ceksaldo

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Menjalankan alur utama aplikasi di emulator: daftar, cari, favorit, tambah entri, pengaturan. */
@RunWith(AndroidJUnit4::class)
class MainFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    // Daftar difilter lewat Flow di ViewModel, jadi tunggu sampai hasilnya muncul/hilang.
    private fun waitShown(text: String, substring: Boolean = false) = rule.waitUntil(10_000) {
        rule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
    }

    private fun waitGone(text: String) = rule.waitUntil(10_000) {
        rule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
    }

    private fun waitCount(description: String, count: Int) = rule.waitUntil(10_000) {
        rule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().size == count
    }

    private val searchField = hasTestTag("search")

    @Test
    fun mainFlow() {
        // Daftar bawaan tampil
        rule.onNodeWithText("Cek Saldo").assertIsDisplayed()
        rule.onNodeWithText("BRI").assertIsDisplayed()

        // Pencarian
        rule.onNode(searchField).performTextInput("dana")
        waitGone("BRI")
        waitShown("DANA")
        rule.onNodeWithText("DANA").assertIsDisplayed()
        rule.onNodeWithText("BRI").assertDoesNotExist()
        rule.onNodeWithContentDescription("Hapus").performClick()
        rule.onNode(searchField).performImeAction() // tutup keyboard
        waitShown("BRI")
        rule.onNodeWithText("BRI").assertIsDisplayed()

        // Favorit kosong, lalu tandai satu
        rule.onNodeWithText("Favorit").performClick()
        waitShown("Belum ada favorit", substring = true)
        rule.onNodeWithText("Belum ada favorit", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Semua").performClick()
        waitShown("BRI")
        rule.onAllNodesWithContentDescription("Tambah ke favorit").onFirst().performClick()
        rule.onNodeWithText("Favorit").performClick()
        waitGone("DANA")
        waitCount("Hapus dari favorit", 1)
        rule.onAllNodesWithContentDescription("Hapus dari favorit").assertCountEquals(1)
        rule.onNodeWithText("Semua").performClick()
        waitShown("BRI")

        // Tambah entri sendiri
        rule.onNodeWithText("Tambah", useUnmergedTree = true).performClick()
        rule.onNode(hasSetTextAction() and hasText("Nama")).performTextInput("Bank Uji")
        rule.onNode(hasSetTextAction() and hasText("Kode USSD (opsional)")).performTextInput("*999#")
        rule.onNodeWithText("Simpan").performClick()
        rule.onNode(searchField).performTextInput("Bank Uji")
        rule.onNode(searchField).performImeAction()
        waitShown("Bank Uji")
        rule.onNodeWithText("Bank Uji").assertIsDisplayed()
        rule.onNodeWithText("USSD").assertIsDisplayed()
        rule.onNodeWithContentDescription("Hapus").performClick()

        // Pengaturan SIM
        rule.onNodeWithContentDescription("Pengaturan").performClick()
        rule.onNodeWithText("SIM untuk USSD").assertIsDisplayed()
        rule.onNodeWithText("SIM 2").performClick()
        rule.onNodeWithText("Tutup").performClick()
        rule.onNodeWithText("SIM untuk USSD").assertDoesNotExist()
    }
}
