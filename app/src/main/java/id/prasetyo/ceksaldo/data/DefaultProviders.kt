package id.prasetyo.ceksaldo.data

/**
 * Daftar bawaan. Kode USSD dan nama paket aplikasi bisa berubah sewaktu-waktu oleh
 * bank/operator, jadi semuanya bisa diubah pengguna dari dalam aplikasi.
 */
object DefaultProviders {
    val all: List<Provider> = listOf(
        // Bank
        Provider("bca", "BCA", Category.BANK, 0xFF0060AF, appPackage = "com.bca",
            note = "m-BCA juga tersedia di menu SIM Toolkit"),
        Provider("mybca", "myBCA", Category.BANK, 0xFF0060AF, appPackage = "com.bca.mybca.omni"),
        Provider("bri", "BRI", Category.BANK, 0xFF00529C, ussd = "*141*575#", appPackage = "id.co.bri.brimo"),
        Provider("mandiri", "Bank Mandiri", Category.BANK, 0xFF003D79, ussd = "*141*6#", appPackage = "id.bmri.livin"),
        Provider("bni", "BNI", Category.BANK, 0xFFF15A23, ussd = "*141*46#", appPackage = "id.bni.wondr"),
        Provider("bsi", "BSI", Category.BANK, 0xFF00A39D, ussd = "*14014#", appPackage = "id.co.bankbsi.superapp"),
        Provider("btn", "BTN", Category.BANK, 0xFF1A4C9C, appPackage = "id.co.btn.mobilebanking.android"),
        Provider("cimb", "CIMB Niaga", Category.BANK, 0xFF7A1315, appPackage = "id.co.cimbniaga.mobile.android"),
        Provider("permata", "PermataBank", Category.BANK, 0xFF0B8A3E, appPackage = "net.myinfosys.PermataMobileX"),
        Provider("danamon", "Danamon", Category.BANK, 0xFFF39200, appPackage = "com.danamon.dbank"),
        Provider("jenius", "Jenius", Category.BANK, 0xFF00A8E0, appPackage = "com.btpn.dc"),
        Provider("jago", "Bank Jago", Category.BANK, 0xFFF7A500, appPackage = "com.jago.digitalBanking"),
        Provider("seabank", "SeaBank", Category.BANK, 0xFFEE4D2D, appPackage = "id.co.bankbkemum.mobile"),
        Provider("blu", "blu by BCA", Category.BANK, 0xFF00B7F1, appPackage = "com.bcadigital.blu"),

        // E-wallet
        Provider("gopay", "GoPay", Category.EWALLET, 0xFF00AA13, appPackage = "com.gojek.gopay"),
        Provider("ovo", "OVO", Category.EWALLET, 0xFF4C3494, appPackage = "ovo.id"),
        Provider("dana", "DANA", Category.EWALLET, 0xFF108EE9, appPackage = "id.dana"),
        Provider("shopeepay", "ShopeePay", Category.EWALLET, 0xFFEE4D2D, appPackage = "com.shopee.id"),
        Provider("linkaja", "LinkAja", Category.EWALLET, 0xFFE82529, appPackage = "com.telkom.mwallet"),

        // Operator seluler (cek pulsa/kuota)
        Provider("telkomsel", "Telkomsel", Category.OPERATOR, 0xFFE60012, ussd = "*888#",
            appPackage = "com.telkomsel.telkomselcm", inlineUssd = true),
        Provider("indosat", "IM3 / Indosat", Category.OPERATOR, 0xFFFFC20E, ussd = "*123#",
            appPackage = "com.pure.indosat.care", inlineUssd = true),
        Provider("xl", "XL", Category.OPERATOR, 0xFF0B4DA2, ussd = "*123#",
            appPackage = "com.apps.MyXL", inlineUssd = true),
        Provider("axis", "AXIS", Category.OPERATOR, 0xFF6D2C91, ussd = "*888#",
            appPackage = "com.axis.net", inlineUssd = true),
        Provider("tri", "Tri (3)", Category.OPERATOR, 0xFF2B2B2B, ussd = "*111#",
            appPackage = "com.hutchison3g.bima", inlineUssd = true),
        Provider("smartfren", "Smartfren", Category.OPERATOR, 0xFFE4007F, ussd = "*995#",
            appPackage = "com.smartfren", inlineUssd = true),
    )
}
