package id.prasetyo.ceksaldo

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import id.prasetyo.ceksaldo.data.Category
import id.prasetyo.ceksaldo.data.Provider
import id.prasetyo.ceksaldo.data.ProviderRepository
import id.prasetyo.ceksaldo.data.SimChoice
import kotlinx.coroutines.flow.StateFlow

/** Filter chip di atas daftar. null kategori = semua. */
data class Filter(val label: String, val category: Category? = null, val favoritesOnly: Boolean = false)

val FILTERS = listOf(Filter("Semua"), Filter("Favorit", favoritesOnly = true)) +
    Category.entries.map { Filter(it.label, category = it) }

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ProviderRepository(app)

    // State Compose biasa (bukan Flow) agar TextField pencarian tidak tertinggal satu frame.
    var query by mutableStateOf("")
    var filter by mutableStateOf(FILTERS.first())
    val sim: StateFlow<SimChoice> = repo.sim

    val providers: StateFlow<List<Provider>> = repo.providers

    fun toggleFavorite(id: String) = repo.toggleFavorite(id)
    fun save(provider: Provider) = repo.upsert(provider)
    fun delete(id: String) = repo.delete(id)
    fun resetToDefaults() = repo.resetToDefaults()
    fun setSim(choice: SimChoice) = repo.setSim(choice)
}

/** Dihitung langsung saat composition agar hasil filter dan posisi scroll berubah di frame yang sama. */
fun filterProviders(list: List<Provider>, query: String, filter: Filter): List<Provider> =
    list.asSequence()
        .filter { !filter.favoritesOnly || it.favorite }
        .filter { filter.category == null || it.category == filter.category }
        .filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
        .sortedWith(compareByDescending<Provider> { it.favorite }.thenBy { it.category.ordinal })
        .toList()
