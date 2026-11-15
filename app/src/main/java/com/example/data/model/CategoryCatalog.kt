package com.example.data.model

import com.example.ui.i18n.SuqiLanguage

data class CategorySpec(
    val id: String,
    val name: String,
    val nameAmharic: String,
    val nameAfaanOromo: String,
    val subtitle: String,
    val subtitleAmharic: String,
    val subtitleAfaanOromo: String,
    val icon: String,
    val imageUrl: String,
    val subCategories: List<String>,
    val sortOrder: Int
) {
    fun localizedSubtitle(language: SuqiLanguage): String = when (language) {
        SuqiLanguage.AM -> subtitleAmharic.ifBlank { subtitle }
        SuqiLanguage.OM -> subtitleAfaanOromo.ifBlank { subtitle }
        SuqiLanguage.EN -> subtitle
    }

    fun toDto() = CategoryDto(
        id = id,
        name = name,
        nameAmharic = nameAmharic,
        nameAfaanOromo = nameAfaanOromo,
        icon = icon,
        imageUrl = imageUrl,
        subCategories = subCategories,
        isActive = true,
        sortOrder = sortOrder
    )
}

object MarketplaceCategories {
    val all: List<CategorySpec> = listOf(
        CategorySpec(
            id = "cat_electronics",
            name = "Electronics",
            nameAmharic = "ኤሌክትሮኒክስ",
            nameAfaanOromo = "Elekitirooniksii",
            subtitle = "Phones, laptops & more",
            subtitleAmharic = "ስልኮች፣ ላፕቶፖች እና ሌሎች",
            subtitleAfaanOromo = "Bilbiloota, laaptooppotaa fi kkf",
            icon = "laptop",
            imageUrl = "https://cdn.jsdelivr.net/gh/gamistudios/myfiles@df4bb8bc78e45e81bff01b5182a2f2186af603fc/uploads/2026/11/df7d99d1-photo_2026-11-14_03-19-06_7708176369021664256.jpg",
            subCategories = listOf("Laptops", "Phones", "Audio", "Smart Watches", "Accessories"),
            sortOrder = 0
        ),
        CategorySpec(
            id = "cat_fashion",
            name = "Fashion",
            nameAmharic = "ፋሽንና ልብሶች",
            nameAfaanOromo = "Faashinii",
            subtitle = "Clothing, shoes & style",
            subtitleAmharic = "አልባሳት፣ ጫማዎች እና ስታይል",
            subtitleAfaanOromo = "Uffata, kophee fi miidhagina",
            icon = "tshirt",
            imageUrl = "https://cdn.jsdelivr.net/gh/gamistudios/myfiles@7b3b4d1a7b900b6e6ac5269bc50485a6c4c74118/uploads/2026/11/4e2457e1-photo_2026-11-14_03-08-14_7708177341413728256.jpg",
            subCategories = listOf("Men", "Women", "Shoes", "Watches", "Jewelry"),
            sortOrder = 1
        ),
        CategorySpec(
            id = "cat_home",
            name = "Home & Living",
            nameAmharic = "የቤት ዕቃዎች",
            nameAfaanOromo = "Mana & Jireenya",
            subtitle = "Kitchen, decor & bedding",
            subtitleAmharic = "ማዕድ ቤት፣ ዲኮር እና አልጋ ልብስ",
            subtitleAfaanOromo = "Kushinaa, faaya manaa fi uffata siree",
            icon = "home",
            imageUrl = "https://cdn.jsdelivr.net/gh/gamistudios/myfiles@da810fce145b521adf479e48291627c0fb8a5647/uploads/2026/11/feb76585-photo_2026-11-14_03-08-36_7708177394012563456.jpg",
            subCategories = listOf("Kitchen", "Living Room", "Bedding", "Home Decor", "Appliances"),
            sortOrder = 2
        ),
        CategorySpec(
            id = "cat_beauty",
            name = "Beauty & Care",
            nameAmharic = "ውበትና የግል እንክብካቤ",
            nameAfaanOromo = "Bareedina",
            subtitle = "Skincare & fragrance",
            subtitleAmharic = "የቆዳ እንክብካቤ እና ሽቶዎች",
            subtitleAfaanOromo = "Kunuunsa gogaa fi urgooftuu",
            icon = "sparkles",
            imageUrl = "https://cdn.jsdelivr.net/gh/gamistudios/myfiles@cf44b2242c16e039a3b288f59b19ffba0e3b5f82/uploads/2026/11/9fc181fa-photo_2026-11-14_03-09-04_7708177454134311936.jpg",
            subCategories = listOf("Skincare", "Fragrance", "Haircare", "Makeup", "Personal Care"),
            sortOrder = 3
        ),
        CategorySpec(
            id = "cat_sports",
            name = "Sports & Outdoors",
            nameAmharic = "ስፖርትና የውጪ እንቅስቃሴ",
            nameAfaanOromo = "Ispoortii",
            subtitle = "Gear, football & camping",
            subtitleAmharic = "የስፖርት፣ እግር ኳስ እና የካምፕ ዕቃዎች",
            subtitleAfaanOromo = "Meeshaalee ispoortii fi kubbaa miilaa",
            icon = "sports",
            imageUrl = "https://cdn.jsdelivr.net/gh/gamistudios/myfiles@36d75c68ed2218e8d1ca9432ec5153710ecb17ef/uploads/2026/11/91e934fc-photo_2026-11-14_03-27-17_7708177655334719488.jpg",
            subCategories = listOf("Football", "Running", "Outdoor Gear", "Sportswear", "Cycling"),
            sortOrder = 4
        ),
        CategorySpec(
            id = "cat_bags",
            name = "Bags & Accessories",
            nameAmharic = "ቦርሳዎችና መገልገያዎች",
            nameAfaanOromo = "Boorsaa fi Meeshaalee",
            subtitle = "Backpacks, wallets & belts",
            subtitleAmharic = "ቦርሳዎች፣ ዋሌቶች እና ቀበቶዎች",
            subtitleAfaanOromo = "Boorsaa dugdaa, boorsaa kiisii fi qabattoo",
            icon = "shopping-bag",
            imageUrl = "https://cdn.jsdelivr.net/gh/gamistudios/myfiles@5aa3cffee6a43d63e83583cdeb9ae6675e235c30/uploads/2026/11/4b1de6a1-photo_2026-11-14_03-27-59_7708177701975549952.jpg",
            subCategories = listOf("Backpacks", "Handbags", "Travel Luggage", "Wallets", "Sunglasses"),
            sortOrder = 5
        ),
        CategorySpec(
            id = "cat_furniture",
            name = "Furniture",
            nameAmharic = "የቤትና የቢሮ ፈርኒቸር",
            nameAfaanOromo = "Meeshaa Manaa",
            subtitle = "Sofas, beds & tables",
            subtitleAmharic = "ሶፋዎች፣ አልጋዎች እና ጠረጴዛዎች",
            subtitleAfaanOromo = "Soofaa, siree fi minjaala",
            icon = "armchair",
            imageUrl = "https://cdn.jsdelivr.net/gh/gamistudios/myfiles@aac5b05bbf44c6240279fce55dc16c1330362755/uploads/2026/11/5589ef28-photo_2026-11-14_03-28-54_7708177757574499328.jpg",
            subCategories = listOf("Sofas", "Beds", "Desks & Chairs", "Tables", "Storage"),
            sortOrder = 6
        ),
        CategorySpec(
            id = "cat_health",
            name = "Health & Fitness",
            nameAmharic = "ጤናና አካል ብቃት",
            nameAfaanOromo = "Fayyaa & Qaama",
            subtitle = "Wellness & gym gear",
            subtitleAmharic = "የጤና እና የጂም መገልገያዎች",
            subtitleAfaanOromo = "Fayyaa fi meeshaalee jiimii",
            icon = "heart-pulse",
            imageUrl = "https://cdn.jsdelivr.net/gh/gamistudios/myfiles@14ef88e4ce38a7529698cbb4f23da3c90de819ed/uploads/2026/11/faaef0a3-photo_2026-11-14_03-29-44_7708177831589006336.jpg",
            subCategories = listOf("Supplements", "Gym Equipment", "Wellness", "First Aid", "Personal Hygiene"),
            sortOrder = 7
        ),
        CategorySpec(
            id = "cat_kids",
            name = "Baby, Kids & Toys",
            nameAmharic = "የሕፃናትና የልጆች ዕቃዎች",
            nameAfaanOromo = "Daa'imman & Tapha",
            subtitle = "Toys, clothing & care",
            subtitleAmharic = "መጫወቻዎች፣ ልብሶች እና እንክብካቤ",
            subtitleAfaanOromo = "Meeshaalee taphaa, uffataa fi kunuunsa",
            icon = "baby",
            imageUrl = "https://cdn.jsdelivr.net/gh/gamistudios/myfiles@5187ee9012ef6efd274bd0cce83662058353a3b1/uploads/2026/11/8db7c8be-photo_2026-11-14_03-32-05_7708177903024851968.jpg",
            subCategories = listOf("Toys", "Baby Care", "Kids Apparel", "Educational", "Strollers"),
            sortOrder = 8
        ),
        CategorySpec(
            id = "cat_groceries",
            name = "Groceries & Food",
            nameAmharic = "ምግብና ሸቀጣ ሸቀጥ",
            nameAfaanOromo = "Nyaata & Gabaa",
            subtitle = "Coffee, spices & pantry",
            subtitleAmharic = "ቡና፣ ቅመማ ቅመም እና አስቤዛ",
            subtitleAfaanOromo = "Buna, mi'eessituu fi nyaata",
            icon = "groceries",
            imageUrl = "https://cdn.jsdelivr.net/gh/gamistudios/myfiles@940d98a0cc499d94ad3ddc7f4189bcc151b485bc/uploads/2026/11/8f9245f7-photo_2026-11-14_03-33-06_7708177933992143872.jpg",
            subCategories = listOf("Coffee & Tea", "Ethiopian Spices", "Fresh Produce", "Pantry", "Snacks"),
            sortOrder = 9
        ),
        CategorySpec(
            id = "cat_traditional",
            name = "Traditional Clothes",
            nameAmharic = "ባህላዊ አልባሳት",
            nameAfaanOromo = "Uffata Aadaa",
            subtitle = "Habesha Kemis & Netela",
            subtitleAmharic = "የሐበሻ ቀሚስ እና ነጠላ",
            subtitleAfaanOromo = "Qamisii Habashaa fi naxalaa",
            icon = "shirt",
            imageUrl = "https://cdn.jsdelivr.net/gh/gamistudios/myfiles@4c95ee863e07480bddc993304a5b86d61b07da75/uploads/2026/11/2f18ab23-photo_2026-11-14_03-33-35_7708177979212746752.jpg",
            subCategories = listOf("Habesha Kemis", "Netela & Gabi", "Men's Cultural", "Wedding Wear", "Tilet Fabrics"),
            sortOrder = 10
        ),
        CategorySpec(
            id = "cat_vehicles",
            name = "Automotive",
            nameAmharic = "ተሽከርካሪዎችና መለዋወጫ",
            nameAfaanOromo = "Konkolaataa",
            subtitle = "Cars, parts & accessories",
            subtitleAmharic = "መኪኖች፣ መለዋወጫዎች እና ዕቃዎች",
            subtitleAfaanOromo = "Konkolaataa, meeshaalee jijjiirraa",
            icon = "car",
            imageUrl = "https://cdn.jsdelivr.net/gh/gamistudios/myfiles@ca12234eed4facbfc0b4d169c42bc7036244caf2/uploads/2026/11/27399458-photo_2026-11-14_03-34-00_7708178013342907392.jpg",
            subCategories = listOf("Cars", "Motorbikes", "Auto Parts", "Car Care", "Tires & Batteries"),
            sortOrder = 11
        ),
        CategorySpec(
            id = "cat_books",
            name = "Books & Stationery",
            nameAmharic = "መጻሕፍትና የጽሕፈት መሣሪያ",
            nameAfaanOromo = "Kitaabilee & Barreeffama",
            subtitle = "Books, school & office",
            subtitleAmharic = "መጻሕፍት፣ የትምህርት እና የቢሮ ዕቃዎች",
            subtitleAfaanOromo = "Kitaabilee, meeshaalee barnootaa fi waajjiraa",
            icon = "book-open",
            imageUrl = "https://cdn.jsdelivr.net/gh/gamistudios/myfiles@1904801c800f240cc9c46f4e8eed636313fc146c/uploads/2026/11/39081249-photo_2026-11-14_03-36-18_7708178076177911808.jpg",
            subCategories = listOf("Amharic Books", "Textbooks", "Notebooks", "Office Supplies", "Art Pens"),
            sortOrder = 12
        ),
        CategorySpec(
            id = "cat_arts",
            name = "Arts & Crafts",
            nameAmharic = "ሥነ ጥበብና የእጅ ሥራ",
            nameAfaanOromo = "Aartii & Hojii Harkaa",
            subtitle = "Paintings, clay & weave",
            subtitleAmharic = "ሥዕሎች፣ ሸክላ እና የእጅ ሥራዎች",
            subtitleAfaanOromo = "Fakkiiwwan, suphee fi hojii harkaa",
            icon = "palette",
            imageUrl = "https://cdn.jsdelivr.net/gh/gamistudios/myfiles@df0042f7f3481fe86013c26015c3f8204fc8aa4f/uploads/2026/11/86c3882e-photo_2026-11-14_03-36-48_7708178106803549184.jpg",
            subCategories = listOf("Paintings", "Pottery & Jebena", "Woven Baskets", "Art Supplies", "Sculptures"),
            sortOrder = 13
        ),
        CategorySpec(
            id = "cat_garden",
            name = "Garden & Agriculture",
            nameAmharic = "አትክልትና ግብርና",
            nameAfaanOromo = "Biqiltuu & Qonna",
            subtitle = "Plants, seeds & tools",
            subtitleAmharic = "ተክሎች፣ ዘሮች እና መሣሪያዎች",
            subtitleAfaanOromo = "Biqiltuuwwan, sanyii fi meeshaalee",
            icon = "sprout",
            imageUrl = "https://cdn.jsdelivr.net/gh/gamistudios/myfiles@0a97edf8803c2f806123ff44bde1a30474f02d2f/uploads/2026/11/812d5e90-photo_2026-11-14_03-37-16_7708178130837350400.jpg",
            subCategories = listOf("Indoor Plants", "Seeds", "Garden Tools", "Pots & Planters", "Farm Inputs"),
            sortOrder = 14
        ),
        CategorySpec(
            id = "cat_gifts",
            name = "Gifts & Occasions",
            nameAmharic = "ስጦታዎችና በዓላት",
            nameAfaanOromo = "Kennaa & Ayyaana",
            subtitle = "Gift boxes & celebrations",
            subtitleAmharic = "የስጦታ ሳጥኖች እና የበዓላት ዕቃዎች",
            subtitleAfaanOromo = "Saanduqa kennaa fi ayyaanaa",
            icon = "gift",
            imageUrl = "https://cdn.jsdelivr.net/gh/gamistudios/myfiles@1dc3e23b02e61809302ceb0f7d0c97ef461a04d4/uploads/2026/11/d4bd12a3-photo_2026-11-14_03-38-01_7708178160934650880.jpg",
            subCategories = listOf("Gift Hampers", "Wedding", "Holiday", "Flowers", "Greeting Cards"),
            sortOrder = 15
        )
    )

    private val byIdMap: Map<String, CategorySpec> = all.associateBy { it.id }

    fun byId(id: String): CategorySpec? = byIdMap[id]

    fun nameForId(id: String): String =
        byIdMap[id]?.name ?: id.removePrefix("cat_").replaceFirstChar { it.uppercase() }

    fun imageUrlForId(id: String): String = byIdMap[id]?.imageUrl.orEmpty()

    fun subtitleForId(
        id: String,
        fallbackSubCategories: List<String> = emptyList(),
        language: SuqiLanguage = SuqiLanguage.EN
    ): String {
        val spec = byIdMap[id]
        if (spec != null) return spec.localizedSubtitle(language)
        if (fallbackSubCategories.isNotEmpty()) return fallbackSubCategories.take(3).joinToString(", ")
        return when (language) {
            SuqiLanguage.AM -> "ሁሉንም ይመልከቱ"
            SuqiLanguage.OM -> "Hunda ilaali"
            SuqiLanguage.EN -> "Browse all"
        }
    }

    fun subCategoriesForId(id: String): List<String> =
        byIdMap[id]?.subCategories ?: listOf("Popular", "Featured", "Deals", "New")
}
