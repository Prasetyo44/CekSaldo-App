package id.prasetyo.ceksaldo.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Pilihan SIM untuk menjalankan USSD. 0 = ikuti pengaturan ponsel. */
enum class SimChoice(val label: String) {
    SYSTEM("Ikuti pengaturan ponsel"),
    SIM1("SIM 1"),
    SIM2("SIM 2"),
}

class ProviderRepository(context: Context) {
    private val prefs = context.getSharedPreferences("ceksaldo", Context.MODE_PRIVATE)

    private val _providers = MutableStateFlow(load())
    val providers: StateFlow<List<Provider>> = _providers.asStateFlow()

    private val _sim = MutableStateFlow(
        runCatching { SimChoice.valueOf(prefs.getString(KEY_SIM, null) ?: "") }.getOrDefault(SimChoice.SYSTEM)
    )
    val sim: StateFlow<SimChoice> = _sim.asStateFlow()

    private fun load(): List<Provider> {
        val json = prefs.getString(KEY_PROVIDERS, null) ?: return DefaultProviders.all
        return runCatching { Provider.listFromJson(json) }.getOrDefault(DefaultProviders.all)
    }

    private fun save(list: List<Provider>) {
        prefs.edit().putString(KEY_PROVIDERS, Provider.listToJson(list)).apply()
    }

    private fun mutate(block: (List<Provider>) -> List<Provider>) {
        _providers.update { block(it).also(::save) }
    }

    fun toggleFavorite(id: String) = mutate { list ->
        list.map { if (it.id == id) it.copy(favorite = !it.favorite) else it }
    }

    fun upsert(provider: Provider) = mutate { list ->
        if (list.any { it.id == provider.id }) list.map { if (it.id == provider.id) provider else it }
        else list + provider
    }

    fun delete(id: String) = mutate { list -> list.filterNot { it.id == id } }

    fun resetToDefaults() = mutate { DefaultProviders.all }

    fun setSim(choice: SimChoice) {
        prefs.edit().putString(KEY_SIM, choice.name).apply()
        _sim.value = choice
    }

    private companion object {
        const val KEY_PROVIDERS = "providers_v1"
        const val KEY_SIM = "sim_choice"
    }
}
