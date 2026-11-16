package com.example.data.model

/** A reusable product type with a predefined (but fully editable) list of parameters. */
data class ItemType(
    val id: String,
    val label: String,
    val emoji: String,
    val categoryId: String,
    val group: String,
    val attributes: List<AttributeDef>
)

/** Named swatches used by COLOR attributes. Custom colours are stored as "#RRGGBB". */
object ColorSwatches {
    val named: LinkedHashMap<String, Long> = linkedMapOf(
        "Black" to 0xFF171717,
        "White" to 0xFFFFFFFF,
        "Grey" to 0xFF9CA3AF,
        "Silver" to 0xFFC7CCD1,
        "Space Gray" to 0xFF4B5563,
        "Gold" to 0xFFD4A537,
        "Beige" to 0xFFE8D8BF,
        "Brown" to 0xFF7A4B2A,
        "Red" to 0xFFE23B3B,
        "Orange" to 0xFFFF6500,
        "Yellow" to 0xFFF5C518,
        "Green" to 0xFF2E9E5B,
        "Blue" to 0xFF2F6FE4,
        "Navy" to 0xFF1B2A5C,
        "Purple" to 0xFF7845E8,
        "Pink" to 0xFFF472B6
    )

    /** ARGB for a swatch name or "#RRGGBB"; null when unknown. */
    fun argbOf(value: String): Long? {
        named[value]?.let { return it }
        val v = value.trim()
        if (v.length == 7 && v.startsWith("#")) {
            return v.substring(1).toLongOrNull(16)?.let { 0xFF000000 or it }
        }
        return null
    }
}

object ProductTemplates {

    // ───────── tiny DSL ─────────
    private fun a(
        key: String,
        label: String,
        type: String = AttrType.TEXT,
        group: String = "General",
        options: List<String> = emptyList(),
        unit: String = "",
        required: Boolean = false,
        selectable: Boolean = false
    ) = AttributeDef(
        key = key, label = label, type = type, options = options, unit = unit,
        required = required, enabled = true, selectable = selectable, group = group
    )

    private fun sel(key: String, label: String, options: List<String>, group: String = "General", selectable: Boolean = false, required: Boolean = false) =
        a(key, label, AttrType.SELECT, group, options, selectable = selectable, required = required)

    private fun multi(key: String, label: String, options: List<String>, group: String = "General") =
        a(key, label, AttrType.MULTI_SELECT, group, options)

    private fun meas(key: String, label: String, units: List<String>, group: String = "General", selectable: Boolean = false) =
        a(key, label, AttrType.MEASUREMENT, group, units, unit = units.first(), selectable = selectable)

    private fun num(key: String, label: String, group: String = "General", required: Boolean = false) =
        a(key, label, AttrType.NUMBER, group, required = required)

    private fun yesNo(key: String, label: String, group: String = "General") =
        a(key, label, AttrType.BOOLEAN, group)

    private fun date(key: String, label: String, group: String = "General", required: Boolean = false) =
        a(key, label, AttrType.DATE, group, required = required)

    private fun color(label: String = "Color", key: String = "color") =
        a(key, label, AttrType.COLOR, selectable = true)

    private val brand = a("brand", "Brand", required = true)
    private val model = a("model", "Model")
    private val condition = sel("condition", "Condition", listOf("New", "Like new", "Used", "Refurbished"))
    private val warranty = sel("warranty", "Warranty", listOf("None", "3 months", "6 months", "1 year", "2 years"))
    private val gender = sel("gender", "Gender", listOf("Men", "Women", "Unisex", "Boys", "Girls"))
    private val origin = a("origin", "Origin / Made in")

    private val apparelSizes = listOf("XS", "S", "M", "L", "XL", "XXL", "XXXL", "Free size")
    private val shoeSizes = (36..47).map { it.toString() }
    private val ramOptions = listOf("2 GB", "4 GB", "6 GB", "8 GB", "12 GB", "16 GB", "32 GB", "64 GB")
    private val storageOptions = listOf("32 GB", "64 GB", "128 GB", "256 GB", "512 GB", "1 TB", "2 TB")
    private val weightUnits = listOf("g", "kg")
    private val volumeUnits = listOf("ml", "L", "g", "oz")
    private val lengthUnits = listOf("cm", "m", "inch", "mm")

    val custom = ItemType("custom", "Custom / Other", "✨", "", "Other", emptyList())

    @Volatile
    private var syncedTypes: List<ItemType>? = null

    val all: List<ItemType>
        get() = syncedTypes ?: defaultAll

    fun syncFromPersisted(types: List<ItemType>) {
        syncedTypes = if (types.isEmpty()) null else types
    }

    val defaultAll: List<ItemType> = listOf(
        // ───────── Electronics ─────────
        ItemType("laptop", "Laptop", "💻", "cat_electronics", "Electronics", listOf(
            brand, model,
            sel("cpu", "Processor (CPU)", listOf("Intel Core i3", "Intel Core i5", "Intel Core i7", "Intel Core i9", "AMD Ryzen 3", "AMD Ryzen 5", "AMD Ryzen 7", "Apple M-series", "Other"), "Performance"),
            sel("ram", "RAM", ramOptions, "Performance"),
            sel("storage", "Storage", storageOptions, "Performance"),
            sel("storage_type", "Storage type", listOf("SSD", "HDD", "SSD + HDD"), "Performance"),
            a("gpu", "Graphics (GPU)", group = "Performance"),
            meas("screen_size", "Screen size", listOf("inch", "cm"), "Display"),
            sel("resolution", "Resolution", listOf("HD", "Full HD", "2K", "4K"), "Display"),
            sel("os", "Operating system", listOf("Windows 11", "Windows 10", "macOS", "Linux", "ChromeOS", "No OS"), "Software"),
            meas("battery_life", "Battery life", listOf("hours"), "Power"),
            color(), condition, warranty
        )),
        ItemType("smartphone", "Smartphone", "📱", "cat_electronics", "Electronics", listOf(
            brand, model,
            sel("ram", "RAM", ramOptions, "Performance"),
            sel("storage", "Storage", storageOptions, "Performance", selectable = true),
            meas("screen_size", "Screen size", listOf("inch", "cm"), "Display"),
            meas("battery", "Battery", listOf("mAh"), "Power"),
            a("camera", "Main camera", group = "Camera"),
            multi("network", "Network", listOf("4G", "5G", "Dual SIM", "eSIM", "NFC"), "Connectivity"),
            sel("os", "Operating system", listOf("Android", "iOS", "HarmonyOS"), "Software"),
            color(), condition, warranty
        )),
        ItemType("tablet", "Tablet", "📲", "cat_electronics", "Electronics", listOf(
            brand, model,
            sel("ram", "RAM", ramOptions, "Performance"),
            sel("storage", "Storage", storageOptions, "Performance", selectable = true),
            meas("screen_size", "Screen size", listOf("inch", "cm"), "Display"),
            meas("battery", "Battery", listOf("mAh"), "Power"),
            multi("network", "Connectivity", listOf("Wi-Fi", "4G", "5G", "Stylus support")),
            sel("os", "Operating system", listOf("Android", "iPadOS", "Windows"), "Software"),
            color(), condition, warranty
        )),
        ItemType("tv", "TV & Display", "📺", "cat_electronics", "Electronics", listOf(
            brand, model,
            meas("screen_size", "Screen size", listOf("inch", "cm"), "Display"),
            sel("resolution", "Resolution", listOf("HD", "Full HD", "4K", "8K"), "Display"),
            sel("panel", "Panel type", listOf("LED", "OLED", "QLED", "LCD"), "Display"),
            yesNo("smart", "Smart TV", "Features"),
            num("hdmi", "HDMI ports", "Connectivity"),
            meas("power", "Power", listOf("W"), "Power"),
            condition, warranty
        )),
        ItemType("audio", "Audio (Headphones & Speakers)", "🎧", "cat_electronics", "Electronics", listOf(
            brand, model,
            sel("audio_type", "Type", listOf("Headphones", "Earbuds", "Speaker", "Soundbar", "Microphone")),
            multi("connectivity", "Connectivity", listOf("Bluetooth", "Wired", "Wi-Fi", "USB-C"), "Connectivity"),
            meas("battery_life", "Battery life", listOf("hours"), "Power"),
            yesNo("anc", "Noise cancelling", "Features"),
            color(), condition, warranty
        )),
        ItemType("camera", "Camera", "📷", "cat_electronics", "Electronics", listOf(
            brand, model,
            sel("camera_type", "Type", listOf("DSLR", "Mirrorless", "Compact", "Action camera", "CCTV / Security", "Drone")),
            meas("megapixels", "Resolution", listOf("MP"), "Sensor"),
            sel("video", "Video quality", listOf("1080p", "2.7K", "4K", "8K"), "Sensor"),
            a("lens", "Lens / Mount", group = "Sensor"),
            condition, warranty
        )),
        ItemType("wearable", "Smartwatch & Wearable", "⌚", "cat_electronics", "Electronics", listOf(
            brand, model,
            meas("screen_size", "Screen size", listOf("inch", "mm"), "Display"),
            meas("battery_life", "Battery life", listOf("days", "hours"), "Power"),
            yesNo("water_resistant", "Water resistant", "Features"),
            multi("compat", "Compatible with", listOf("Android", "iOS"), "Connectivity"),
            color("Strap color", "strap_color"),
            sel("strap_size", "Strap size", listOf("S", "M", "L"), selectable = true),
            condition, warranty
        )),
        ItemType("accessory", "Electronics Accessory", "🔌", "cat_electronics", "Electronics", listOf(
            brand,
            sel("acc_type", "Type", listOf("Charger", "Cable", "Case / Cover", "Power bank", "Mouse", "Keyboard", "Memory card", "Adapter")),
            a("compatible", "Compatible with"),
            sel("connector", "Connector", listOf("USB-C", "Lightning", "Micro USB", "HDMI", "3.5mm")),
            meas("power", "Power", listOf("W", "mAh")),
            meas("length", "Length", lengthUnits),
            color(), warranty
        )),

        // ───────── Fashion ─────────
        ItemType("clothing", "Clothing", "👕", "cat_fashion", "Fashion", listOf(
            brand, gender,
            sel("size", "Size", apparelSizes, selectable = true, required = true),
            color(),
            sel("material", "Material", listOf("Cotton", "Polyester", "Linen", "Wool", "Denim", "Silk", "Leather", "Blend")),
            sel("fit", "Fit", listOf("Regular", "Slim", "Loose", "Oversized")),
            multi("occasion", "Occasion", listOf("Casual", "Formal", "Sport", "Party", "Work")),
            a("care", "Care instructions"),
            condition
        )),
        ItemType("shoes", "Shoes", "👟", "cat_fashion", "Fashion", listOf(
            brand, gender,
            sel("shoe_type", "Shoe type", listOf("Sneakers", "Boots", "Sandals", "Formal", "Sports", "Slippers")),
            sel("size", "Size (EU)", shoeSizes, selectable = true, required = true),
            color(),
            sel("material", "Material", listOf("Leather", "Synthetic", "Canvas", "Mesh", "Rubber")),
            condition
        )),
        ItemType("bags", "Bags & Luggage", "👜", "cat_bags", "Bags & Accessories", listOf(
            brand,
            sel("bag_type", "Type", listOf("Backpack", "Handbag", "Laptop bag", "Travel bag", "Wallet", "School bag")),
            sel("size", "Size", listOf("Small", "Medium", "Large"), selectable = true),
            color(),
            sel("material", "Material", listOf("Leather", "Canvas", "Nylon", "Polyester", "Synthetic")),
            meas("capacity", "Capacity", listOf("L")),
            yesNo("waterproof", "Waterproof")
        )),
        ItemType("jewelry", "Jewelry & Watches", "💍", "cat_fashion", "Fashion", listOf(
            brand,
            sel("jewelry_type", "Type", listOf("Watch", "Necklace", "Ring", "Bracelet", "Earrings")),
            gender,
            sel("material", "Material", listOf("Gold", "Silver", "Stainless steel", "Leather", "Plastic", "Beads")),
            color(),
            a("ring_size", "Size", selectable = true),
            yesNo("water_resistant", "Water resistant")
        )),
        ItemType("traditional", "Traditional Wear", "👗", "cat_traditional", "Traditional Clothes", listOf(
            sel("garment", "Garment", listOf("Habesha Kemis", "Netela / Gabi", "Kuta", "Shuruba", "Men's suit", "Cultural dress")),
            gender,
            sel("size", "Size", apparelSizes, selectable = true),
            color(),
            sel("fabric", "Fabric", listOf("Cotton", "Shemma", "Silk", "Linen")),
            a("embroidery", "Embroidery / Tilet style"),
            multi("occasion", "Occasion", listOf("Wedding", "Holiday", "Church", "Daily wear")),
            yesNo("handmade", "Handmade")
        )),

        // ───────── Furniture ─────────
        ItemType("furniture", "Furniture", "🛋️", "cat_furniture", "Furniture", listOf(
            sel("furniture_type", "Type", listOf("Sofa", "Bed", "Table", "Chair", "Wardrobe", "Desk", "Shelf", "Cabinet")),
            sel("material", "Material", listOf("Wood", "Metal", "Fabric", "Leather", "Glass", "Plastic")),
            meas("length", "Length", lengthUnits, "Dimensions"),
            meas("width", "Width", lengthUnits, "Dimensions"),
            meas("height", "Height", lengthUnits, "Dimensions"),
            meas("weight", "Weight", weightUnits, "Dimensions"),
            color(),
            sel("assembly", "Assembly", listOf("Fully assembled", "Self-assembly required")),
            condition, warranty
        )),
        ItemType("appliance", "Home Appliance", "🧺", "cat_home", "Home & Living", listOf(
            brand, model,
            sel("appliance_type", "Type", listOf("Refrigerator", "Washing machine", "Microwave", "Oven / Stove", "Fan / Heater", "Vacuum cleaner", "Water dispenser")),
            meas("power", "Power", listOf("W", "kW"), "Power"),
            meas("capacity", "Capacity", listOf("L", "kg")),
            sel("voltage", "Voltage", listOf("110V", "220V", "110-240V"), "Power"),
            color(), condition, warranty
        )),
        ItemType("kitchenware", "Kitchenware", "🍳", "cat_home", "Home & Living", listOf(
            brand,
            sel("kitchen_type", "Type", listOf("Cookware", "Dinnerware", "Utensils", "Storage", "Flask / Bottle", "Coffee set (Jebena)")),
            sel("material", "Material", listOf("Stainless steel", "Aluminium", "Clay", "Glass", "Plastic", "Ceramic")),
            meas("capacity", "Capacity", listOf("L", "ml")),
            num("pieces", "Number of pieces"),
            yesNo("dishwasher", "Dishwasher safe"),
            color()
        )),
        ItemType("bedding", "Bedding & Decor", "🛏️", "cat_home", "Home & Living", listOf(
            sel("decor_type", "Type", listOf("Bedsheet", "Blanket", "Curtain", "Rug / Carpet", "Pillow", "Wall art")),
            sel("material", "Material", listOf("Cotton", "Wool", "Polyester", "Microfiber", "Silk")),
            sel("size", "Size", listOf("Single", "Double", "Queen", "King"), selectable = true),
            color(),
            num("pieces", "Number of pieces")
        )),
        ItemType("tools", "Tools & Building Materials", "🛠️", "cat_home", "Home & Living", listOf(
            brand,
            a("tool_type", "Item type"),
            sel("power_source", "Power source", listOf("Manual", "Electric", "Battery", "Petrol")),
            a("material", "Material"),
            a("dimensions", "Dimensions / Size"),
            meas("weight", "Weight", weightUnits),
            condition, warranty
        )),

        // ───────── Beauty & Care ─────────
        ItemType("cosmetics", "Cosmetics & Skincare", "💄", "cat_beauty", "Beauty & Care", listOf(
            brand,
            a("cosmetic_type", "Product type"),
            multi("skin_type", "Skin type", listOf("Oily", "Dry", "Combination", "Sensitive", "All skin types")),
            meas("volume", "Volume", volumeUnits, selectable = true),
            color("Shade", "shade"),
            a("ingredients", "Ingredients"),
            date("expiry", "Expiry date", required = true),
            yesNo("cruelty_free", "Cruelty-free"),
            origin
        )),
        ItemType("perfume", "Perfume & Fragrance", "🌸", "cat_beauty", "Beauty & Care", listOf(
            brand,
            sel("family", "Fragrance family", listOf("Floral", "Woody", "Citrus", "Oriental", "Fresh", "Oud / Arabian")),
            gender,
            sel("concentration", "Concentration", listOf("Eau de Toilette", "Eau de Parfum", "Parfum", "Body mist")),
            meas("volume", "Volume", listOf("ml"), selectable = true),
            a("notes", "Scent notes"),
            date("expiry", "Expiry date")
        )),
        ItemType("haircare", "Hair & Personal Care", "💇", "cat_beauty", "Beauty & Care", listOf(
            brand,
            a("care_type", "Product type"),
            multi("hair_type", "Hair type", listOf("Straight", "Curly", "Coily", "Wavy", "All hair types")),
            meas("volume", "Volume", volumeUnits, selectable = true),
            a("scent", "Scent"),
            a("ingredients", "Ingredients"),
            date("expiry", "Expiry date")
        )),

        // ───────── Groceries & Food ─────────
        ItemType("food", "Food & Groceries", "🛒", "cat_groceries", "Groceries & Food", listOf(
            brand,
            meas("weight", "Net weight", weightUnits, selectable = true),
            meas("volume", "Volume", listOf("ml", "L")),
            a("ingredients", "Ingredients"),
            date("expiry", "Expiry date", required = true),
            origin,
            yesNo("organic", "Organic"),
            sel("storage", "Storage", listOf("Room temperature", "Refrigerate", "Frozen")),
            sel("packaging", "Packaging", listOf("Bag", "Box", "Bottle", "Can", "Jar", "Loose"))
        )),
        ItemType("coffee", "Coffee & Tea", "☕", "cat_groceries", "Groceries & Food", listOf(
            sel("coffee_type", "Type", listOf("Coffee beans", "Ground coffee", "Green beans", "Tea leaves", "Tea bags")),
            sel("region", "Origin region", listOf("Yirgacheffe", "Sidama", "Harar", "Guji", "Jimma", "Limu", "Kaffa", "Other")),
            sel("roast", "Roast level", listOf("Light", "Medium", "Dark")),
            sel("process", "Process", listOf("Washed", "Natural", "Honey")),
            meas("weight", "Net weight", weightUnits, selectable = true),
            sel("grind", "Grind", listOf("Whole bean", "Espresso", "Filter", "Jebena"), selectable = true),
            date("harvest", "Harvest / roast date"),
            yesNo("organic", "Organic")
        )),
        ItemType("spices", "Spices & Grains", "🌶️", "cat_groceries", "Groceries & Food", listOf(
            sel("spice_type", "Type", listOf("Berbere", "Mitmita", "Teff", "Shiro", "Honey", "Spice blend", "Cereal / Pulse", "Oil / Butter")),
            meas("weight", "Net weight", weightUnits, selectable = true),
            a("ingredients", "Ingredients"),
            origin,
            date("expiry", "Expiry date"),
            yesNo("organic", "Organic")
        )),

        // ───────── Sports & Outdoors ─────────
        ItemType("sports", "Sports & Outdoors", "⚽", "cat_sports", "Sports & Outdoors", listOf(
            brand,
            sel("sport", "Sport", listOf("Football", "Basketball", "Running", "Gym & Fitness", "Cycling", "Camping", "Other")),
            a("item_type", "Item type"),
            sel("size", "Size", listOf("S", "M", "L", "XL", "One size"), selectable = true),
            color(),
            a("material", "Material"),
            meas("weight", "Weight", weightUnits),
            condition
        )),

        // ───────── Health & Fitness ─────────
        ItemType("health_fitness", "Health & Fitness", "💪", "cat_health", "Health & Fitness", listOf(
            brand,
            sel("health_type", "Type", listOf("Supplements", "Gym Equipment", "Yoga & Mobility", "Blood Pressure / Monitor", "First Aid")),
            meas("weight", "Weight / Volume", listOf("g", "kg", "ml", "caps")),
            date("expiry", "Expiry date"),
            origin,
            condition
        )),

        // ───────── Automotive ─────────
        ItemType("car", "Car", "🚗", "cat_vehicles", "Automotive", listOf(
            a("make", "Make", required = true), a("model", "Model", required = true),
            num("year", "Year", required = true),
            meas("mileage", "Mileage", listOf("km", "mi")),
            sel("fuel", "Fuel type", listOf("Petrol", "Diesel", "Hybrid", "Electric")),
            sel("transmission", "Transmission", listOf("Manual", "Automatic")),
            sel("body", "Body type", listOf("Sedan", "Hatchback", "SUV", "Pickup", "Van", "Minibus")),
            meas("engine", "Engine size", listOf("cc", "L")),
            num("seats", "Seats"),
            color(), 
            sel("condition", "Condition", listOf("New", "Used"))
        ).map { if (it.key == "color") it.copy(selectable = false) else it }),
        ItemType("motorcycle", "Motorbike & Bajaj", "🏍️", "cat_vehicles", "Automotive", listOf(
            a("make", "Make", required = true), a("model", "Model"),
            sel("moto_type", "Type", listOf("Motorbike", "Scooter", "Bajaj (3-wheeler)")),
            num("year", "Year"),
            meas("mileage", "Mileage", listOf("km", "mi")),
            meas("engine", "Engine size", listOf("cc")),
            sel("fuel", "Fuel type", listOf("Petrol", "Electric")),
            color(),
            sel("condition", "Condition", listOf("New", "Used"))
        ).map { if (it.key == "color") it.copy(selectable = false) else it }),
        ItemType("autoparts", "Auto Parts", "🔧", "cat_vehicles", "Automotive", listOf(
            a("part_type", "Part type", required = true),
            a("compatible_make", "Compatible make"),
            a("compatible_model", "Compatible model"),
            a("year_range", "Year range"),
            brand,
            a("part_number", "Part number"),
            condition, warranty
        )),

        // ───────── Baby, Kids & Toys ─────────
        ItemType("toys", "Toys & Games", "🧸", "cat_kids", "Baby, Kids & Toys", listOf(
            brand,
            sel("toy_type", "Type", listOf("Educational", "Dolls", "Cars", "Building blocks", "Board game", "Outdoor", "Puzzle")),
            sel("age", "Age range", listOf("0-2 years", "3-5 years", "6-8 years", "9-12 years", "13+ years")),
            a("material", "Material"),
            yesNo("battery", "Needs batteries"),
            num("pieces", "Number of pieces"),
            color()
        )),
        ItemType("baby", "Baby Products", "🍼", "cat_kids", "Baby, Kids & Toys", listOf(
            brand,
            sel("baby_type", "Type", listOf("Diapers", "Clothing", "Feeding", "Stroller / Car seat", "Bath & Care", "Nursery")),
            sel("age", "Age range", listOf("Newborn", "0-6 months", "6-12 months", "1-2 years", "2-4 years")),
            sel("size", "Size", listOf("NB", "S", "M", "L", "XL"), selectable = true),
            a("material", "Material"),
            color()
        )),

        // ───────── Books & Stationery ─────────
        ItemType("books", "Books", "📚", "cat_books", "Books & Stationery", listOf(
            a("author", "Author", required = true),
            a("publisher", "Publisher"),
            sel("language", "Language", listOf("Amharic", "Afaan Oromoo", "Tigrinya", "English", "Other")),
            sel("format", "Format", listOf("Paperback", "Hardcover", "E-book"), selectable = true),
            num("pages", "Pages"),
            num("year", "Publication year"),
            a("isbn", "ISBN"),
            a("genre", "Genre"),
            condition
        )),
        ItemType("stationery", "Stationery & Office", "✏️", "cat_books", "Books & Stationery", listOf(
            brand,
            sel("stationery_type", "Type", listOf("Notebooks", "Pens & Markers", "School Backpack Set", "Art Paper", "Office Supplies")),
            color(),
            num("pieces", "Pack count")
        )),

        // ───────── Arts & Crafts ─────────
        ItemType("arts_crafts", "Arts & Crafts", "🎨", "cat_arts", "Arts & Crafts", listOf(
            sel("craft_type", "Type", listOf("Painting", "Pottery / Clay", "Woven Basket (Mesob)", "Wood Carving", "Craft Supplies")),
            a("artist", "Artist / Maker"),
            a("material", "Material"),
            meas("dimensions", "Dimensions", lengthUnits),
            yesNo("handmade", "Handmade"),
            origin
        )),

        // ───────── Garden & Agriculture ─────────
        ItemType("garden_agri", "Garden & Agriculture", "🌱", "cat_garden", "Garden & Agriculture", listOf(
            sel("garden_type", "Type", listOf("Indoor Plant", "Seeds & Seedlings", "Planter / Pot", "Garden Tools", "Fertilizer & Soil")),
            a("species", "Plant / Variety"),
            sel("sunlight", "Sunlight", listOf("Full sun", "Partial shade", "Indoor")),
            meas("weight", "Weight / Size", weightUnits),
            origin
        )),

        // ───────── Gifts & Occasions ─────────
        ItemType("gifts", "Gifts & Occasions", "🎁", "cat_gifts", "Gifts & Occasions", listOf(
            sel("occasion", "Occasion", listOf("Birthday", "Wedding", "Holiday", "Graduation", "Anniversary", "Thank You")),
            sel("gift_type", "Type", listOf("Gift Box / Hamper", "Flowers", "Personalized Gift", "Chocolate & Treats")),
            yesNo("gift_wrap", "Gift wrapping included"),
            color()
        ))
    )

    /** Look up an item type by id (the blank custom type when unknown). */
    fun byId(id: String): ItemType = all.firstOrNull { it.id == id } ?: custom

    /** Types shown in the picker, with the blank custom type last. */
    val pickerTypes: List<ItemType> get() = all + custom

    /** Every parameter we know about, de-duplicated by key, for the "Add parameter" picker. */
    val library: List<AttributeDef>
        get() = (defaultAll + all)
            .flatMap { it.attributes }
            .distinctBy { it.key }
            .sortedBy { it.label.lowercase() }

    /** Categories referenced by item types (used to create missing category rows). */
    val extraCategories = listOf(
        listOf("cat_vehicles", "Vehicles & Parts", "ተሽከርካሪዎች", "Konkolaataa", "sparkles", "Cars,Motorbikes,Bajaj,Auto Parts"),
        listOf("cat_kids", "Kids & Toys", "የልጆች ዕቃዎች", "Waan Daa'immanii", "sparkles", "Toys,Baby"),
        listOf("cat_books", "Books & Media", "መጻሕፍትና ሙዚቃ", "Kitaabilee", "sparkles", "Books,Music")
    )
}
