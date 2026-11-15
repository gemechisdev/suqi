package com.example.ui.i18n

/**
 * Localized display names for Ethiopian regions, cities and Addis Ababa sub-cities.
 * Stored values stay in English (stable keys for the backend); only the label shown changes.
 */
object SuqiPlaces {
    // English key -> (Amharic, Afaan Oromo)
    private val names: Map<String, Pair<String, String>> = mapOf(
        // Regions / chartered cities
        "Addis Ababa" to ("አዲስ አበባ" to "Finfinnee"),
        "Oromia" to ("ኦሮሚያ" to "Oromiyaa"),
        "Amhara" to ("አማራ" to "Amaaraa"),
        "Sidama" to ("ሲዳማ" to "Sidaamaa"),
        "Dire Dawa" to ("ድሬዳዋ" to "Dirre Dhawaa"),
        "Tigray" to ("ትግራይ" to "Tigraay"),
        "Afar" to ("አፋር" to "Afar"),
        "Somali" to ("ሶማሌ" to "Somaalee"),
        "Harari" to ("ሐረሪ" to "Hararii"),
        "Gambela" to ("ጋምቤላ" to "Gaambeellaa"),
        "Benishangul-Gumuz" to ("ቤኒሻንጉል ጉሙዝ" to "Beenishaangul Gumuz"),
        "South Ethiopia" to ("ደቡብ ኢትዮጵያ" to "Kibba Itoophiyaa"),
        "Central Ethiopia" to ("ማዕከላዊ ኢትዮጵያ" to "Giddugaleessa Itoophiyaa"),
        "South West Ethiopia" to ("ደቡብ ምዕራብ ኢትዮጵያ" to "Kibba Dhihaa Itoophiyaa"),
        // Cities / hubs
        "Adama" to ("አዳማ" to "Adaamaa"),
        "Hawassa" to ("ሀዋሳ" to "Hawaasa"),
        "Bahir Dar" to ("ባሕር ዳር" to "Baahir Daar"),
        "Bishoftu" to ("ቢሾፍቱ" to "Bishooftuu"),
        "Mekelle" to ("መቀሌ" to "Mekelle"),
        "Gondar" to ("ጎንደር" to "Gondar"),
        "Jimma" to ("ጅማ" to "Jimmaa"),
        "Dessie" to ("ደሴ" to "Dessie"),
        "Harar" to ("ሐረር" to "Hararii"),
        // Addis Ababa sub-cities
        "Bole" to ("ቦሌ" to "Boolee"),
        "Kirkos" to ("ቂርቆስ" to "Qirqoos"),
        "Yeka" to ("የካ" to "Yeekkaa"),
        "Arada" to ("አራዳ" to "Araadaa"),
        "Lideta" to ("ልደታ" to "Liidataa"),
        "Nifas Silk" to ("ንፋስ ስልክ" to "Nifaas Silk"),
        "Nifas Silk Lafto" to ("ንፋስ ስልክ ላፍቶ" to "Nifaas Silk Laaftoo"),
        "Kolfe Keranio" to ("ኮልፌ ቀራኒዮ" to "Kolfee Qaraaniyoo"),
        "Gullele" to ("ጉለሌ" to "Guulallee"),
        "Akaky Kaliti" to ("አቃቂ ቃሊቲ" to "Akaakii Qaaliiti"),
        "Addis Ketema" to ("አዲስ ከተማ" to "Addis Ketamaa"),
        "Lemi Kura" to ("ለሚ ኩራ" to "Lammii Kuraa")
    )

    fun localize(name: String?, language: SuqiLanguage): String {
        val raw = name.orEmpty()
        if (raw.isBlank() || language == SuqiLanguage.EN) return raw
        // Handle comma separated composites such as "Bole, Addis Ababa".
        if (raw.contains(",")) {
            return raw.split(",").joinToString(", ") { localize(it.trim(), language) }
        }
        val pair = names[raw.trim()] ?: return raw
        return if (language == SuqiLanguage.AM) pair.first else pair.second
    }

    /** Keys that must exist for every supported language (used by the unit test). */
    fun allKeys(): Set<String> = names.keys
    fun isComplete(): Boolean = names.values.all { it.first.isNotBlank() && it.second.isNotBlank() }
}

fun String.localizedPlace(language: SuqiLanguage): String = SuqiPlaces.localize(this, language)

@androidx.compose.runtime.Composable
fun placeLabel(name: String?): String = SuqiPlaces.localize(name, LocalSuqiLanguage.current)
