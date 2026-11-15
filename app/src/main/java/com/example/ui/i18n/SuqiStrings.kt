package com.example.ui.i18n

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryDto
import com.example.data.model.ProductDto
import com.example.ui.theme.SuqiEthiopicFontFamily
import com.example.ui.theme.SuqiTypography

enum class SuqiLanguage(
    val displayName: String,
    val nativeLabel: String,
    val code: String
) {
    EN("English", "English", "en"),
    AM("Amharic", "አማርኛ", "am"),
    OM("Afaan Oromo", "Afaan Oromoo", "om");

    companion object {
        fun fromSetting(raw: String?): SuqiLanguage {
            val normalized = raw.orEmpty().trim().lowercase()
            return when {
                normalized.contains("amharic") || normalized.contains("አማርኛ") || normalized == "am" -> AM
                normalized.contains("oromo") || normalized == "om" -> OM
                else -> EN
            }
        }
    }
}

data class SuqiStringsData(
    // Bottom Navigation
    val navHome: String,
    val navCategories: String,
    val navSearch: String,
    val navShops: String,
    val navAccount: String,

    // Home
    val searchPlaceholder: String,
    val allLabel: String,
    val featuredShops: String,
    val seeAllArrow: String,
    val hotDeals: String,
    val newArrivals: String,
    val popularNow: String,
    val recommendedForYou: String,
    val recentlyViewed: String,
    val browseAllCategories: String,
    val selectYourLocation: String,
    val setLocation: String,

    // Categories & Listings
    val categoriesTitle: String,
    val subCategoriesLabel: String,
    val filtersLabel: String,
    val sortLabel: String,
    val noProductsFound: String,

    // Search
    val recentSearches: String,
    val popularSearches: String,
    val clearAll: String,
    val productsTab: String,
    val shopsTab: String,

    // Product Details & Card
    val addToCart: String,
    val addedToCart: String,
    val buyNow: String,
    val inStock: String,
    val outOfStock: String,
    val specifications: String,
    val description: String,
    val customerReviews: String,
    val questionsAnswers: String,
    val sizeGuide: String,
    val similarProducts: String,
    val moreFromShop: String,
    val chatWithSeller: String,
    val visitShop: String,
    val shareProduct: String,
    val reportListing: String,

    // Cart & Checkout
    val cartTitle: String,
    val emptyCartTitle: String,
    val emptyCartSubtitle: String,
    val startShopping: String,
    val proceedToCheckout: String,
    val checkoutDeliveryTitle: String,
    val checkoutPaymentTitle: String,
    val orderSubmittedTitle: String,
    val subtotalLabel: String,
    val deliveryFeeLabel: String,
    val discountLabel: String,
    val totalLabel: String,
    val promoCodeHint: String,
    val applyLabel: String,
    val continueToPayment: String,
    val placeOrder: String,
    val uploadReceipt: String,

    // Orders
    val myOrders: String,
    val orderDetails: String,
    val reorder: String,
    val cancelOrder: String,
    val viewInvoice: String,
    val returnItem: String,
    val returnsTitle: String,

    // Account, Settings, Help, Auth
    val accountTitle: String,
    val editProfile: String,
    val savedAddresses: String,
    val wishlistTitle: String,
    val followingTitle: String,
    val notificationsTitle: String,
    val settingsTitle: String,
    val helpSupportTitle: String,
    val couponsTitle: String,
    val dealsTitle: String,
    val languageTitle: String,
    val appearanceTitle: String,
    val darkMode: String,
    val lightMode: String,
    val systemDefault: String,
    val sellerStudioLabel: String,
    val becomeSellerLabel: String,
    val signIn: String,
    val signOut: String,
    val welcomeToSuqi: String,
    val everyShopEveryProduct: String,
    val skipLabel: String,
    val continueLabel: String,

    // Common
    val saveLabel: String,
    val cancelLabel: String,
    val confirmLabel: String,
    val deleteLabel: String,
    val editLabel: String,
    val retryLabel: String,
    val verifiedLabel: String,
    val offlineBannerTitle: String
)

object SuqiStringCatalog {
    val english = SuqiStringsData(
        navHome = "Home",
        navCategories = "Categories",
        navSearch = "Search",
        navShops = "Shops",
        navAccount = "Account",
        searchPlaceholder = "Search products or shops...",
        allLabel = "All",
        featuredShops = "Featured Shops",
        seeAllArrow = "See all →",
        hotDeals = "Hot Deals",
        newArrivals = "New Arrivals",
        popularNow = "Popular Now",
        recommendedForYou = "Recommended For You",
        recentlyViewed = "Recently Viewed",
        browseAllCategories = "Browse All Categories",
        selectYourLocation = "Select Your Location",
        setLocation = "Set Location",
        categoriesTitle = "Categories",
        subCategoriesLabel = "Sub-categories",
        filtersLabel = "Filters",
        sortLabel = "Sort",
        noProductsFound = "No products match these filters",
        recentSearches = "Recent Searches",
        popularSearches = "Popular Searches",
        clearAll = "Clear all",
        productsTab = "Products",
        shopsTab = "Shops",
        addToCart = "Add to Cart",
        addedToCart = "Added to Cart!",
        buyNow = "Buy Now",
        inStock = "In Stock",
        outOfStock = "Out of Stock",
        specifications = "Specifications",
        description = "Description",
        customerReviews = "Customer Reviews",
        questionsAnswers = "Questions & Answers",
        sizeGuide = "Size Guide",
        similarProducts = "Similar Items",
        moreFromShop = "More from this shop",
        chatWithSeller = "Chat with Seller",
        visitShop = "Visit Shop",
        shareProduct = "Share",
        reportListing = "Report listing",
        cartTitle = "My Cart",
        emptyCartTitle = "Your cart is empty",
        emptyCartSubtitle = "Discover local shops across Addis Ababa and add items to get started.",
        startShopping = "Start Shopping",
        proceedToCheckout = "Proceed to Checkout",
        checkoutDeliveryTitle = "Delivery Details",
        checkoutPaymentTitle = "Payment & Proof",
        orderSubmittedTitle = "Order Submitted!",
        subtotalLabel = "Subtotal",
        deliveryFeeLabel = "Delivery Fee",
        discountLabel = "Discount",
        totalLabel = "Total",
        promoCodeHint = "Enter promo code",
        applyLabel = "Apply",
        continueToPayment = "Continue to Payment",
        placeOrder = "Submit Order",
        uploadReceipt = "Upload Payment Receipt",
        myOrders = "My Orders",
        orderDetails = "Order Details",
        reorder = "Reorder",
        cancelOrder = "Cancel Order",
        viewInvoice = "Receipt / Invoice",
        returnItem = "Return Item",
        returnsTitle = "Returns & Refunds",
        accountTitle = "Account",
        editProfile = "Edit Profile",
        savedAddresses = "Saved Addresses",
        wishlistTitle = "Wishlist",
        followingTitle = "Followed Shops",
        notificationsTitle = "Notifications",
        settingsTitle = "Settings",
        helpSupportTitle = "Help & Support",
        couponsTitle = "Coupons & Wallet",
        dealsTitle = "Hot Deals & Flash Sales",
        languageTitle = "Language",
        appearanceTitle = "Theme & Appearance",
        darkMode = "Dark",
        lightMode = "Light",
        systemDefault = "System",
        sellerStudioLabel = "Seller Studio",
        becomeSellerLabel = "Become a Seller",
        signIn = "Sign In",
        signOut = "Sign Out",
        welcomeToSuqi = "Welcome to Suqi!",
        everyShopEveryProduct = "Every Shop. Every Product.",
        skipLabel = "Skip",
        continueLabel = "Continue",
        saveLabel = "Save",
        cancelLabel = "Cancel",
        confirmLabel = "Confirm",
        deleteLabel = "Delete",
        editLabel = "Edit",
        retryLabel = "Retry",
        verifiedLabel = "Verified",
        offlineBannerTitle = "Offline mode — showing cached items"
    )

    val amharic = SuqiStringsData(
        navHome = "መነሻ",
        navCategories = "ምድቦች",
        navSearch = "ፈልግ",
        navShops = "ሱቆች",
        navAccount = "መለያ",
        searchPlaceholder = "ምርቶችን ወይም ሱቆችን ይፈልጉ...",
        allLabel = "ሁሉም",
        featuredShops = "ተመራጭ ሱቆች",
        seeAllArrow = "ሁሉንም →",
        hotDeals = "ልዩ ቅናሾች",
        newArrivals = "አዲስ የገቡ",
        popularNow = "ተወዳጅ ምርቶች",
        recommendedForYou = "ለእርስዎ የተመረጡ",
        recentlyViewed = "በቅርቡ የታዩ",
        browseAllCategories = "ሁሉንም ምድቦች ይመልከቱ",
        selectYourLocation = "አካባቢዎን ይምረጡ",
        setLocation = "አካባቢ አረጋግጥ",
        categoriesTitle = "ምድቦች",
        subCategoriesLabel = "ንዑስ ምድቦች",
        filtersLabel = "ማጣሪያ",
        sortLabel = "ደርድር",
        noProductsFound = "በዚህ ማጣሪያ ምርት አልተገኘም",
        recentSearches = "የቅርብ ፍለጋዎች",
        popularSearches = "ተዘውታሪ ፍለጋዎች",
        clearAll = "ሁሉንም አጽዳ",
        productsTab = "ምርቶች",
        shopsTab = "ሱቆች",
        addToCart = "ወደ ጋሪ ጨምር",
        addedToCart = "ወደ ጋሪ ታክሏል!",
        buyNow = "አሁን ይግዙ",
        inStock = "በመጋዘን አለ",
        outOfStock = "አልቋል",
        specifications = "ዝርዝር መግለጫ",
        description = "ማብራሪያ",
        customerReviews = "የደንበኞች አስተያየት",
        questionsAnswers = "ጥያቄና መልስ",
        sizeGuide = "የመጠን መመሪያ",
        similarProducts = "ተመሳሳይ ምርቶች",
        moreFromShop = "ከዚህ ሱቅ ተጨማሪ",
        chatWithSeller = "ከሻጭ ጋር ይነጋገሩ",
        visitShop = "ሱቁን ይጎብኙ",
        shareProduct = "አጋራ",
        reportListing = "ቅሬታ አቅርብ",
        cartTitle = "የእኔ ጋሪ",
        emptyCartTitle = "ጋሪዎ ባዶ ነው",
        emptyCartSubtitle = "በአዲስ አበባ ያሉ ሱቆችን ይጎብኙና ምርቶችን ወደ ጋሪዎ ይጨምሩ።",
        startShopping = "መገበያየት ጀምር",
        proceedToCheckout = "ወደ ክፍያ ቀጥል",
        checkoutDeliveryTitle = "የማድረሻ ዝርዝር",
        checkoutPaymentTitle = "ክፍያ እና ደረሰኝ",
        orderSubmittedTitle = "ትዕዛዝ ተልኳል!",
        subtotalLabel = "ንዑስ ድምር",
        deliveryFeeLabel = "የማድረሻ ክፍያ",
        discountLabel = "ቅናሽ",
        totalLabel = "ጠቅላላ ድምር",
        promoCodeHint = "የቅናሽ ኮድ ያስገቡ",
        applyLabel = "ተግብር",
        continueToPayment = "ወደ ክፍያ ቀጥል",
        placeOrder = "ትዕዛዝ ላክ",
        uploadReceipt = "የክፍያ ደረሰኝ ይጫኑ",
        myOrders = "ትዕዛዞቼ",
        orderDetails = "የትዕዛዝ ዝርዝር",
        reorder = "እንደገና እዘዝ",
        cancelOrder = "ትዕዛዝ ሰርዝ",
        viewInvoice = "ደረሰኝ / ፋክቱር",
        returnItem = "ዕቃ መልስ",
        returnsTitle = "መመለሻ እና ተመላሽ ገንዘብ",
        accountTitle = "መለያ",
        editProfile = "መገለጫ አርትዕ",
        savedAddresses = "የተቀመጡ አድራሻዎች",
        wishlistTitle = "የምኞት ዝርዝር",
        followingTitle = "የሚከተሏቸው ሱቆች",
        notificationsTitle = "ማሳወቂያዎች",
        settingsTitle = "ቅንብሮች",
        helpSupportTitle = "እገዛ እና ድጋፍ",
        couponsTitle = "ኩፖኖች እና ቦርሳ",
        dealsTitle = "ልዩ ቅናሾች",
        languageTitle = "ቋንቋ",
        appearanceTitle = "ገጽታ እና ብርሃን",
        darkMode = "ጨለማ",
        lightMode = "ብሩህ",
        systemDefault = "የስርዓት",
        sellerStudioLabel = "የሻጭ ስቱዲዮ",
        becomeSellerLabel = "ሻጭ ይሁኑ",
        signIn = "ግባ",
        signOut = "ውጣ",
        welcomeToSuqi = "ወደ ሱቂ እንኳን በደህና መጡ!",
        everyShopEveryProduct = "ሁሉም ሱቅ። ሁሉም ምርት።",
        skipLabel = "ዝለል",
        continueLabel = "ቀጥል",
        saveLabel = "አስቀምጥ",
        cancelLabel = "ሰርዝ",
        confirmLabel = "አረጋግጥ",
        deleteLabel = "አጥፋ",
        editLabel = "አርትዕ",
        retryLabel = "እንደገና ሞክር",
        verifiedLabel = "የተረጋገጠ",
        offlineBannerTitle = "ከመስመር ውጪ — የተቀመጠ መረጃ እየታየ ነው"
    )

    val afaanOromo = SuqiStringsData(
        navHome = "Mana",
        navCategories = "Gosoota",
        navSearch = "Barbaadi",
        navShops = "Suuqiiwwan",
        navAccount = "Eenyummaa",
        searchPlaceholder = "Meeshaalee ykn suuqiiwwan barbaadi...",
        allLabel = "Hunda",
        featuredShops = "Suuqiiwwan Filatamoo",
        seeAllArrow = "Hunda →",
        hotDeals = "Hir'ina Ho'aa",
        newArrivals = "Haaraa Dhufe",
        popularNow = "Jaallatamoo",
        recommendedForYou = "Siif Filataman",
        recentlyViewed = "Dhiyeenya Ilaalaman",
        browseAllCategories = "Gosoota Hunda Ilaali",
        selectYourLocation = "Bakka Kee Filadhu",
        setLocation = "Bakka Mirkaneessi",
        categoriesTitle = "Gosoota",
        subCategoriesLabel = "Gosoota Xixiqqaa",
        filtersLabel = "Calaltuu",
        sortLabel = "Tartiiba",
        noProductsFound = "Meeshaan calaltuu kanaan argamu hin jiru",
        recentSearches = "Barbaacha Dhiyeenyaa",
        popularSearches = "Barbaacha Beekamoo",
        clearAll = "Hunda haqi",
        productsTab = "Meeshaalee",
        shopsTab = "Suuqiiwwan",
        addToCart = "Gara Gaarii Dabali",
        addedToCart = "Gara gaariitti dabalameera!",
        buyNow = "Amma Biti",
        inStock = "Kuusaa keessa jira",
        outOfStock = "Dhumateera",
        specifications = "Ibsa Bal'aa",
        description = "Ibsa",
        customerReviews = "Yaada Maamiltootaa",
        questionsAnswers = "Gaaffii fi Deebii",
        sizeGuide = "Qajeelfama Hanga",
        similarProducts = "Meeshaalee Walfakkaatan",
        moreFromShop = "Suuqii kanarraa dabalata",
        chatWithSeller = "Gurguraa Waliin Haasa'i",
        visitShop = "Suuqii Daawwadhu",
        shareProduct = "Qoodi",
        reportListing = "Gabaasi",
        cartTitle = "Gaarii Koo",
        emptyCartTitle = "Gaariin kee duwwaadha",
        emptyCartSubtitle = "Suuqiiwwan Finfinnee keessa jiran sakatta'iitii meeshaalee dabali.",
        startShopping = "Bittaa Jalqabi",
        proceedToCheckout = "Gara Kaffaltii Itti Fufi",
        checkoutDeliveryTitle = "Odeeffannoo Geejjibaa",
        checkoutPaymentTitle = "Kaffaltii fi Nagahee",
        orderSubmittedTitle = "Ajajni Ergameera!",
        subtotalLabel = "Ida'ama Gidduugaleessaa",
        deliveryFeeLabel = "Kaffaltii Geejjibaa",
        discountLabel = "Hir'ina",
        totalLabel = "Ida'ama Waliigalaa",
        promoCodeHint = "Koodii hir'inaa galchi",
        applyLabel = "Fayyadami",
        continueToPayment = "Gara Kaffaltii Itti Fufi",
        placeOrder = "Ajaja Ergi",
        uploadReceipt = "Nagahee Kaffaltii Ol-kaasi",
        myOrders = "Ajajoota Koo",
        orderDetails = "Bal'ina Ajajaa",
        reorder = "Irra Deebi'ii Ajaji",
        cancelOrder = "Ajaja Haqi",
        viewInvoice = "Nagahee / Invooyisii",
        returnItem = "Meeshaa Deebisi",
        returnsTitle = "Deebii fi Maallaqa Deebisuu",
        accountTitle = "Eenyummaa",
        editProfile = "Piroofaayila Gulaali",
        savedAddresses = "Teessoo Ol-kaa'aman",
        wishlistTitle = "Tarree Hawwii",
        followingTitle = "Suuqiiwwan Hordoftu",
        notificationsTitle = "Beeksisoota",
        settingsTitle = "Qindaa'ina",
        helpSupportTitle = "Gargaarsa fi Deeggarsa",
        couponsTitle = "Kuuppoonii fi Boorsaa",
        dealsTitle = "Hir'ina Yeroo Gabaabaa",
        languageTitle = "Afaan",
        appearanceTitle = "Bifa fi Ifa",
        darkMode = "Dukkana",
        lightMode = "Ifa",
        systemDefault = "Sirna",
        sellerStudioLabel = "Istudiyoo Gurguraa",
        becomeSellerLabel = "Gurguraa Ta'i",
        signIn = "Seeni",
        signOut = "Ba'i",
        welcomeToSuqi = "Baga Gara Suqi Nagaan Dhuftan!",
        everyShopEveryProduct = "Suuqii Hunda. Meeshaa Hunda.",
        skipLabel = "Darbi",
        continueLabel = "Itti fufi",
        saveLabel = "Ol-kaa'i",
        cancelLabel = "Dhiisi",
        confirmLabel = "Mirkaneessi",
        deleteLabel = "Balleessi",
        editLabel = "Gulaali",
        retryLabel = "Irra deebi'ii yaali",
        verifiedLabel = "Mirkanaa'e",
        offlineBannerTitle = "Sarara ala — odeeffannoo kuufame agarsiisaa jira"
    )

    fun forLanguage(lang: SuqiLanguage): SuqiStringsData = when (lang) {
        SuqiLanguage.EN -> english
        SuqiLanguage.AM -> amharic
        SuqiLanguage.OM -> afaanOromo
    }

    /** Returns list of any empty translation fields across all 3 supported languages. */
    fun findMissingTranslations(): List<String> {
        val issues = mutableListOf<String>()
        listOf("EN" to english, "AM" to amharic, "OM" to afaanOromo).forEach { (code, data) ->
            SuqiStringsData::class.java.declaredFields
                .filter { !it.isSynthetic && it.type == String::class.java }
                .forEach { f ->
                    f.isAccessible = true
                    val v = f.get(data) as? String
                    if (v.isNullOrBlank()) {
                        issues.add("$code:${f.name}")
                    }
                }
        }
        return issues
    }
}

val LocalSuqiLanguage = staticCompositionLocalOf { SuqiLanguage.EN }
val LocalSuqiStrings = staticCompositionLocalOf { SuqiStringCatalog.english }

fun CategoryDto.localizedName(language: SuqiLanguage): String = when (language) {
    SuqiLanguage.AM -> nameAmharic.ifBlank { name }
    SuqiLanguage.OM -> nameAfaanOromo.ifBlank { name }
    SuqiLanguage.EN -> name
}

fun ProductDto.localizedName(language: SuqiLanguage): String = when (language) {
    SuqiLanguage.AM -> nameAmharic.ifBlank { name }
    SuqiLanguage.OM -> nameAfaanOromo.ifBlank { name }
    SuqiLanguage.EN -> name
}

fun ProductDto.localizedDescription(language: SuqiLanguage): String = when (language) {
    SuqiLanguage.AM -> descriptionAmharic.ifBlank { description }
    SuqiLanguage.OM -> descriptionAfaanOromo.ifBlank { description }
    SuqiLanguage.EN -> description
}

fun suqiLineHeight(baseFontSize: TextUnit, language: SuqiLanguage): TextUnit =
    if (language == SuqiLanguage.AM) (baseFontSize.value + 6f).sp else (baseFontSize.value + 4f).sp

fun TextStyle.forSuqiLanguage(language: SuqiLanguage): TextStyle {
    if (language != SuqiLanguage.AM) return this
    val nextLineHeight = if (lineHeight != TextUnit.Unspecified) (lineHeight.value + 2f).sp else (fontSize.value + 6f).sp
    return copy(
        fontFamily = SuqiEthiopicFontFamily,
        lineHeight = nextLineHeight,
        letterSpacing = 0.sp
    )
}

fun suqiTypographyForLanguage(language: SuqiLanguage): Typography {
    if (language != SuqiLanguage.AM) return SuqiTypography
    return SuqiTypography.copy(
        displayLarge = SuqiTypography.displayLarge.forSuqiLanguage(language),
        displayMedium = SuqiTypography.displayMedium.forSuqiLanguage(language),
        headlineLarge = SuqiTypography.headlineLarge.forSuqiLanguage(language),
        headlineMedium = SuqiTypography.headlineMedium.forSuqiLanguage(language),
        headlineSmall = SuqiTypography.headlineSmall.forSuqiLanguage(language),
        titleLarge = SuqiTypography.titleLarge.forSuqiLanguage(language),
        titleMedium = SuqiTypography.titleMedium.forSuqiLanguage(language),
        titleSmall = SuqiTypography.titleSmall.forSuqiLanguage(language),
        bodyLarge = SuqiTypography.bodyLarge.forSuqiLanguage(language),
        bodyMedium = SuqiTypography.bodyMedium.forSuqiLanguage(language),
        bodySmall = SuqiTypography.bodySmall.forSuqiLanguage(language),
        labelLarge = SuqiTypography.labelLarge.forSuqiLanguage(language),
        labelMedium = SuqiTypography.labelMedium.forSuqiLanguage(language),
        labelSmall = SuqiTypography.labelSmall.forSuqiLanguage(language)
    )
}

@Composable
fun suqiStrings(): SuqiStringsData = LocalSuqiStrings.current
