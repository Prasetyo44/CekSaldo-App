package id.prasetyo.ceksaldo.data

import org.json.JSONArray
import org.json.JSONObject

enum class Category(val label: String) {
    BANK("Bank"),
    EWALLET("E-Wallet"),
    OPERATOR("Operator"),
    OTHER("Lainnya"),
}

/**
 * Satu tujuan cek saldo: bank, e-wallet, atau operator seluler.
 * Aplikasi tidak pernah menyimpan PIN, password, atau nomor rekening.
 */
data class Provider(
    val id: String,
    val name: String,
    val category: Category,
    val color: Long,
    val ussd: String? = null,
    val smsNumber: String? = null,
    val smsText: String? = null,
    val appPackage: String? = null,
    val note: String? = null,
    val favorite: Boolean = false,
    /** Balasan USSD satu langkah (mis. cek pulsa) bisa ditampilkan langsung di aplikasi. */
    val inlineUssd: Boolean = false,
) {
    val initials: String
        get() = name.split(' ', '-', '\'')
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "?" }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("category", category.name)
        put("color", color)
        putOpt("ussd", ussd)
        putOpt("smsNumber", smsNumber)
        putOpt("smsText", smsText)
        putOpt("appPackage", appPackage)
        putOpt("note", note)
        put("favorite", favorite)
        put("inlineUssd", inlineUssd)
    }

    companion object {
        fun fromJson(o: JSONObject) = Provider(
            id = o.getString("id"),
            name = o.getString("name"),
            category = runCatching { Category.valueOf(o.getString("category")) }.getOrDefault(Category.OTHER),
            color = o.optLong("color", 0xFF0E7C66),
            ussd = o.optStringOrNull("ussd"),
            smsNumber = o.optStringOrNull("smsNumber"),
            smsText = o.optStringOrNull("smsText"),
            appPackage = o.optStringOrNull("appPackage"),
            note = o.optStringOrNull("note"),
            favorite = o.optBoolean("favorite", false),
            inlineUssd = o.optBoolean("inlineUssd", false),
        )

        fun listToJson(list: List<Provider>): String =
            JSONArray().apply { list.forEach { put(it.toJson()) } }.toString()

        fun listFromJson(json: String): List<Provider> {
            val arr = JSONArray(json)
            return (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        }

        private fun JSONObject.optStringOrNull(key: String): String? =
            if (has(key) && !isNull(key)) getString(key).takeIf { it.isNotBlank() } else null
    }
}
