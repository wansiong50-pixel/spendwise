package com.spendwise.app.ui.botanical

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.spendwise.app.R
import com.spendwise.app.domain.AccountType
import com.spendwise.app.domain.Category
import java.util.Locale

/**
 * The sixteen category illustrations. [color] is the circle colour sampled
 * from the artwork; a category's bars and tracks take their colour from its
 * icon, so a category never clashes with its own picture.
 *
 * Categories store their pick in `iconName` as [key]. Rows written before the
 * redesign hold Material icon names ("restaurant", "receipt", …) and resolve
 * through [categoryArtFor] — no migration needed, and a backup restored into
 * an older build still round-trips the string untouched.
 */
enum class CategoryArt(
    val key: String,
    val label: String,
    @param:DrawableRes val drawable: Int,
    val color: Color
) {
    Food("art_food", "Food", R.drawable.category_food, Color(0xFFFBC120)),
    Shopping("art_shopping", "Shopping", R.drawable.category_shopping, Color(0xFFF56890)),
    Bills("art_bills", "Bills", R.drawable.category_bills, Color(0xFF9B8DF2)),
    Groceries("art_groceries", "Groceries", R.drawable.category_groceries, Color(0xFF89CF5A)),
    Transport("art_transport", "Transport", R.drawable.category_transport, Color(0xFF43A8F7)),
    Health("art_health", "Health", R.drawable.category_health, Color(0xFF4DCE98)),
    Salary("art_salary", "Salary", R.drawable.category_salary, Color(0xFF31ADB1)),
    Freelance("art_freelance", "Freelance", R.drawable.category_freelance, Color(0xFF5451B8)),
    Investment("art_investment", "Investment", R.drawable.category_investment, Color(0xFF79D1AB)),
    Petrol("art_petrol", "Petrol", R.drawable.category_petrol, Color(0xFFEC7835)),
    Toll("art_toll", "Toll and parking", R.drawable.category_toll, Color(0xFF74C5F0)),
    Rent("art_rent", "Rent and housing", R.drawable.category_rent, Color(0xFFB9553D)),
    Utilities("art_utilities", "Utilities", R.drawable.category_utilities, Color(0xFFE66C85)),
    Phone("art_phone", "Phone and internet", R.drawable.category_phone, Color(0xFF804DCB)),
    Family("art_family", "Family", R.drawable.category_family, Color(0xFFB17139)),
    Zakat("art_zakat", "Zakat and donations", R.drawable.category_zakat, Color(0xFF2AAC76));

    /** Stored `color` for a category using this art (ARGB long, as Room keeps it). */
    val storedColor: Long get() = color.toArgb().toLong() and 0xFFFFFFFFL

    companion object {
        fun fromKey(key: String?): CategoryArt? = entries.firstOrNull { it.key == key }
    }
}

// Keywords are matched against the words of a category name. A plain keyword
// matches any word that starts with it ("invest" → "Investments"); a keyword
// prefixed with '=' must equal a whole word, for short or ambiguous ones
// ("air" is water in Malay, but also the start of "airport"). Multi-word
// keywords match anywhere in the name. First matching art wins.
private val NAME_KEYWORDS: List<Pair<CategoryArt, List<String>>> = listOf(
    CategoryArt.Salary to listOf("salary", "gaji", "bonus", "payroll", "wage", "paycheck", "elaun", "allowance", "income"),
    CategoryArt.Freelance to listOf("freelanc", "=side", "=gig", "project", "client", "commission", "business", "bisnes"),
    CategoryArt.Investment to listOf("invest", "dividend", "interest", "saving", "=asb", "stock", "saham", "crypto", "unit trust", "=epf", "kwsp", "tabung"),
    CategoryArt.Petrol to listOf("petrol", "fuel", "minyak", "diesel", "=ron95", "=ron97"),
    CategoryArt.Toll to listOf("toll", "=tol", "parking", "parkir"),
    CategoryArt.Rent to listOf("rent", "sewa", "house", "housing", "mortgage", "home loan", "rumah", "condo", "apartment"),
    CategoryArt.Utilities to listOf("utilit", "electric", "elektrik", "=tnb", "water", "=air", "sewerage", "=gas", "=api"),
    CategoryArt.Phone to listOf("phone", "mobile", "internet", "wifi", "broadband", "=data", "unifi", "celcom", "maxis", "=digi", "telco", "prepaid", "postpaid", "=telefon"),
    CategoryArt.Family to listOf("family", "keluarga", "=kid", "=kids", "child", "=anak", "parent", "baby", "school", "sekolah", "tuition"),
    CategoryArt.Zakat to listOf("zakat", "donat", "charity", "sedekah", "derma", "infaq", "gift", "hadiah", "wakaf"),
    CategoryArt.Health to listOf("health", "medic", "clinic", "klinik", "doctor", "hospital", "pharma", "farmasi", "=ubat", "dental", "=gym", "fitness"),
    CategoryArt.Groceries to listOf("grocer", "market", "pasar", "=mart", "barang dapur", "=tesco", "=lotus", "=aeon", "=mydin"),
    CategoryArt.Food to listOf("food", "makan", "dining", "=eat", "restaurant", "cafe", "coffee", "=kopi", "lunch", "dinner", "breakfast", "snack", "drink", "mamak", "meal"),
    CategoryArt.Transport to listOf("transport", "=grab", "taxi", "=bus", "train", "=lrt", "=mrt", "=ktm", "=car", "kereta", "commute", "=ride", "flight", "travel"),
    CategoryArt.Shopping to listOf("shop", "=beli", "clothes", "fashion", "=baju", "shopee", "lazada", "online"),
    CategoryArt.Bills to listOf("bill", "=bil", "subscription", "langganan", "insurance", "=loan", "=fee", "=fees", "=tax", "cukai")
)

// Material icon names the pre-redesign pickers stored.
private val LEGACY_ICONS: Map<String, CategoryArt> = mapOf(
    "restaurant" to CategoryArt.Food,
    "food" to CategoryArt.Food,
    "shopping_bag" to CategoryArt.Shopping,
    "box" to CategoryArt.Shopping,
    "cart" to CategoryArt.Groceries,
    "grocery" to CategoryArt.Groceries,
    "receipt" to CategoryArt.Bills,
    "bills" to CategoryArt.Bills,
    "card" to CategoryArt.Bills,
    "movie" to CategoryArt.Bills,
    "film" to CategoryArt.Bills,
    "directions_car" to CategoryArt.Transport,
    "car" to CategoryArt.Transport,
    "transport" to CategoryArt.Transport,
    "local_hospital" to CategoryArt.Health,
    "health" to CategoryArt.Health,
    "favorite" to CategoryArt.Health,
    "heart" to CategoryArt.Health,
    "account_balance_wallet" to CategoryArt.Salary,
    "wallet" to CategoryArt.Salary,
    "cash" to CategoryArt.Salary,
    "spark" to CategoryArt.Salary,
    "salary" to CategoryArt.Salary,
    "bonus" to CategoryArt.Salary,
    "bank" to CategoryArt.Investment,
    "house" to CategoryArt.Rent,
    "home" to CategoryArt.Rent,
    "rent" to CategoryArt.Rent,
    "phone" to CategoryArt.Phone,
    "gift" to CategoryArt.Zakat
)

/** Art matched from a category's name alone, or null when nothing fits. */
fun categoryArtFromName(name: String): CategoryArt? {
    val normalized = name.lowercase(Locale.ROOT)
    val words = normalized.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
    val joined = words.joinToString(" ")
    for ((art, keywords) in NAME_KEYWORDS) {
        val hit = keywords.any { keyword ->
            when {
                keyword.startsWith("=") -> words.any { it == keyword.substring(1) }
                ' ' in keyword -> joined.contains(keyword)
                else -> words.any { it.startsWith(keyword) }
            }
        }
        if (hit) return art
    }
    return null
}

/**
 * The illustration for a category: an explicit pick wins, then the name
 * (more specific than the old generic icons — "Petrol" stored with a car
 * icon still gets the fuel pump), then the legacy icon, then a neutral
 * default per kind.
 */
fun categoryArtFor(name: String, iconName: String?, isIncome: Boolean): CategoryArt =
    // Every ledger row asks this on each render; the keyword scan is cheap but
    // not free, and a ledger only ever has a few dozen distinct categories.
    artCache.getOrPut(ArtQuery(name, iconName, isIncome)) {
        CategoryArt.fromKey(iconName)
            ?: categoryArtFromName(name)
            ?: LEGACY_ICONS[iconName]
            ?: if (isIncome) CategoryArt.Salary else CategoryArt.Bills
    }

private data class ArtQuery(val name: String, val iconName: String?, val isIncome: Boolean)

private val artCache = java.util.concurrent.ConcurrentHashMap<ArtQuery, CategoryArt>()

val Category.art: CategoryArt get() = categoryArtFor(name, iconName, isIncomeAdjustment)

/** The four landscapes behind account cards. Stored in `iconName` as [key]. */
enum class AccountScene(val key: String, @param:DrawableRes val drawable: Int) {
    Coast("scene_coast", R.drawable.account_scene_coast),
    Falls("scene_falls", R.drawable.account_scene_falls),
    Canyon("scene_canyon", R.drawable.account_scene_canyon),
    Alpine("scene_alpine", R.drawable.account_scene_alpine);

    companion object {
        fun fromKey(key: String?): AccountScene? = entries.firstOrNull { it.key == key }

        /** First scene for a type — matches the prototype's sample accounts. */
        fun defaultFor(type: AccountType): AccountScene = when (type) {
            AccountType.Bank -> Coast
            AccountType.EWallet -> Falls
            AccountType.Cash -> Canyon
            AccountType.Credit -> Alpine
        }
    }
}

fun accountSceneFor(iconName: String?, type: AccountType): AccountScene =
    AccountScene.fromKey(iconName) ?: AccountScene.defaultFor(type)

/** Round illustrated icon for an account, by type. */
@DrawableRes
fun accountIconFor(type: AccountType): Int = when (type) {
    AccountType.Bank -> R.drawable.account_icon_bank
    AccountType.EWallet -> R.drawable.account_icon_ewallet
    AccountType.Cash -> R.drawable.account_icon_cash
    AccountType.Credit -> R.drawable.account_icon_credit
}

/** Account types in the order the form offers them. */
val AccountTypeOrder = listOf(AccountType.Bank, AccountType.EWallet, AccountType.Cash, AccountType.Credit)
