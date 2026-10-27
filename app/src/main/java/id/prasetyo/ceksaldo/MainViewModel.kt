package id.prasetyo.ceksaldo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.prasetyo.ceksaldo.data.Category
import id.prasetyo.ceksaldo.data.Provider
import id.prasetyo.ceksaldo.data.ProviderRepository
import id.prasetyo.ceksaldo.data.SimChoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Filter chip di atas daftar. null kategori = semua. */
data class Filter(val label: String, val category: Category? = null, val favoritesOnly: Boolean = false)

val FILTERS = listOf(Filter("Semua"), Filter("Favorit", favoritesOnly = true)) +
    Category.entries.map { Filter(it.label, category = it) }

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ProviderRepository(app)

    val query = MutableStateFlow("")
    val filter = MutableStateFlow(FILTERS.first())
    val sim: StateFlow<SimChoice> = repo.sim

    val visible: StateFlow<List<Provider>> =
        combine(repo.providers, query, filter) { list, q, f ->
            list.asSequence()
                .filter { !f.favoritesOnly || it.favorite }
                .filter { f.category == null || it.category == f.category }
                .filter { q.isBlank() || it.name.contains(q.trim(), ignoreCase = true) }
                .sortedWith(compareByDescending<Provider> { it.favorite }.thenBy { it.category.ordinal })
                .toList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleFavorite(id: String) = repo.toggleFavorite(id)
    fun save(provider: Provider) = repo.upsert(provider)
    fun delete(id: String) = repo.delete(id)
    fun resetToDefaults() = repo.resetToDefaults()
    fun setSim(choice: SimChoice) = repo.setSim(choice)
}
