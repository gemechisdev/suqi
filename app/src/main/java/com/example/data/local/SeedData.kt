package com.example.data.local

import com.example.data.model.MarketplaceCategories
import com.example.data.model.ProductDetails
import com.example.data.model.ProductTemplates
import com.example.data.model.ProductVariant
import com.example.data.model.legacyProductDetails
import com.example.data.model.toJson

object SeedData {
    private fun daysAgoTimestamp(daysAgo: Int): String {
        val millis = System.currentTimeMillis() - daysAgo.toLong() * 86_400_000L
        return java.text.SimpleDateFormat("MMM d, yyyy • hh:mm a", java.util.Locale.US).format(java.util.Date(millis))
    }

    suspend fun seedDatabase(db: SuqiDatabase) {
        // Admin Settings
        db.adminSettingsDao().insert(
            AdminSettingsEntity(
                id = 1,
                isChatEnabled = true,
                isExternalContactEnabled = true,
                isSubscriptionModeEnabled = false,
                sellerReviewsEnabled = true,
                showFeaturedShops = true,
                activePlanName = "Pro Seller Plan",
                activePlanPrice = 299.0
            )
        )

        // Users (default active session = Fitsum Gebre seller+buyer account; also seed Admin & Guest)
        val defaultUser = UserEntity(
            id = "user_fitsum",
            name = "Fitsum Gebre",
            phone = "+251 912 345 678",
            email = "fitsum.gebre@gmail.com",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
            role = "SELLER",
            preferredLanguage = "English",
            region = "Addis Ababa",
            city = "Addis Ababa",
            subCity = "Bole",
            shopId = "shop_techhub",
            defaultAddressId = "addr_1",
            adminTier = "NONE",
            activeRole = "SELLER",
            isActiveSession = true,
            createdAt = daysAgoTimestamp(25)
        )
        val adminUser = UserEntity(
            id = "user_admin",
            name = "Abebe Kebede",
            phone = "+251 911 000 111",
            email = "admin@suqi.et",
            avatarUrl = "",
            role = "ADMIN",
            preferredLanguage = "English",
            region = "Addis Ababa",
            city = "Addis Ababa",
            subCity = "Kirkos",
            shopId = null,
            defaultAddressId = null,
            adminTier = "OWNER",
            activeRole = "ADMIN",
            isActiveSession = false,
            createdAt = daysAgoTimestamp(60)
        )
        db.userDao().insertUser(defaultUser)
        db.userDao().insertUser(adminUser)

        // Saved Addresses
        db.savedAddressDao().insertAll(
            listOf(
                SavedAddressEntity(
                    id = "addr_1",
                    userId = "user_fitsum",
                    fullName = "Fitsum Gebre",
                    phone = "+251 912 345 678",
                    region = "Addis Ababa",
                    city = "Addis Ababa",
                    subCity = "Bole",
                    streetOrLandmark = "Behind Edna Mall, House 412",
                    deliveryInstructions = "Call upon arrival at the gate",
                    isDefault = true
                ),
                SavedAddressEntity(
                    id = "addr_2",
                    userId = "user_fitsum",
                    fullName = "Fitsum Gebre (Office)",
                    phone = "+251 912 345 678",
                    region = "Addis Ababa",
                    city = "Addis Ababa",
                    subCity = "Kirkos",
                    streetOrLandmark = "Kazanchis, Sunshine Bldg 3rd Fl",
                    deliveryInstructions = "Leave at reception desk",
                    isDefault = false
                )
            )
        )

        // Categories (16 canonical marketplace categories)
        val categories = MarketplaceCategories.all.map { c ->
            CategoryEntity(
                id = c.id,
                name = c.name,
                nameAmharic = c.nameAmharic,
                nameAfaanOromo = c.nameAfaanOromo,
                icon = c.icon,
                imageUrl = c.imageUrl,
                subCategoriesCsv = c.subCategories.joinToString(","),
                isActive = true,
                sortOrder = c.sortOrder
            )
        }
        db.categoryDao().insertAll(categories)

        // Shops
        val shops = listOf(
            ShopEntity(
                id = "shop_techhub",
                ownerUserId = "user_fitsum",
                name = "Tech Hub",
                slug = "tech-hub",
                logoUrl = "https://images.unsplash.com/photo-1550009158-9ebf69173e03?w=150",
                coverUrl = "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=800",
                category = "Electronics",
                shopType = "Retail Shop",
                description = "Your trusted electronics and gadgets store in Ethiopia. 100% genuine products with full warranty.",
                rating = 4.8,
                reviewCount = 2640,
                isVerified = true,
                verificationStatus = "VERIFIED",
                phone = "+251 912 345 678",
                telegramLink = "https://t.me/techhub",
                whatsappLink = "https://wa.me/251912345678",
                address = "Bole Medhanialem, Addis Ababa",
                region = "Addis Ababa",
                city = "Addis Ababa",
                subCity = "Bole",
                followersCount = 8900,
                productCount = 24,
                isVacationMode = false,
                acceptsOrders = true,
                isFollowing = true,
                createdAt = daysAgoTimestamp(60)
            ),
            ShopEntity(
                id = "shop_fashionworld",
                ownerUserId = "user_fashion",
                name = "Fashion World",
                slug = "fashion-world",
                logoUrl = "https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=150",
                coverUrl = "https://images.unsplash.com/photo-1441984904996-e0b6ba687e04?w=800",
                category = "Fashion",
                shopType = "Boutique",
                description = "Trendy men's and women's apparel, shoes, and Ethiopian designer fashion.",
                rating = 4.7,
                reviewCount = 1920,
                isVerified = true,
                verificationStatus = "VERIFIED",
                phone = "+251 911 223 344",
                telegramLink = "https://t.me/fashionworldet",
                whatsappLink = "",
                address = "Piassa, Addis Ababa",
                region = "Addis Ababa",
                city = "Addis Ababa",
                subCity = "Arada",
                followersCount = 12400,
                productCount = 42,
                isVacationMode = false,
                acceptsOrders = true,
                isFollowing = false,
                createdAt = daysAgoTimestamp(22)
            ),
            ShopEntity(
                id = "shop_homeplus",
                ownerUserId = "user_home",
                name = "Home Plus",
                slug = "home-plus",
                logoUrl = "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=150",
                coverUrl = "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=800",
                category = "Home & Living",
                shopType = "Furniture & Decor",
                description = "Modern contemporary furniture, comfortable sofas, and Ethiopian handcrafted home decor.",
                rating = 4.6,
                reviewCount = 1210,
                isVerified = true,
                verificationStatus = "VERIFIED",
                phone = "+251 922 445 566",
                telegramLink = "https://t.me/homepluset",
                whatsappLink = "",
                address = "Kirkos, Addis Ababa",
                region = "Addis Ababa",
                city = "Addis Ababa",
                subCity = "Kirkos",
                followersCount = 5600,
                productCount = 18,
                isVacationMode = false,
                acceptsOrders = true,
                isFollowing = false,
                createdAt = daysAgoTimestamp(6)
            ),
            ShopEntity(
                id = "shop_royalstore",
                ownerUserId = "user_royal",
                name = "Royal Store",
                slug = "royal-store",
                logoUrl = "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=150",
                coverUrl = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800",
                category = "Electronics",
                shopType = "Retail Store",
                description = "Premium lifestyle tech, audio gear and luxury smart devices.",
                rating = 4.8,
                reviewCount = 2600,
                isVerified = true,
                verificationStatus = "VERIFIED",
                phone = "+251 933 556 677",
                telegramLink = "https://t.me/royalstoreet",
                whatsappLink = "",
                address = "Bole Atlas, Addis Ababa",
                region = "Addis Ababa",
                city = "Addis Ababa",
                subCity = "Bole",
                followersCount = 9400,
                productCount = 35,
                isVacationMode = false,
                acceptsOrders = true,
                isFollowing = false,
                createdAt = daysAgoTimestamp(2)
            )
        )
        db.shopDao().insertAll(shops)
        db.shopFollowDao().insertAll(
            listOf(
                ShopFollowEntity(userId = "user_fitsum", shopId = "shop_techhub")
            )
        )

        // Products
        val products = listOf(
            ProductEntity(
                id = "prod_laptop_hp",
                shopId = "shop_techhub",
                shopName = "Tech Hub",
                shopVerified = true,
                name = "HP Laptop 15.6\" Core i5",
                nameAmharic = "ኤች ፒ ላፕቶፕ 15.6 ኢንች ኮር አይ 5",
                nameAfaanOromo = "Laaptooppii HP 15.6 Core i5",
                description = "Powerful Intel Core i5 12th Gen processor, 8GB DDR4 RAM, 512GB NVMe SSD, vibrant 15.6\" FHD Micro-Edge display. Perfect for work, study and multitasking.",
                descriptionAmharic = "የ12ኛ ትውልድ ኢንቴል ኮር አይ 5 ፕሮሰሰር፣ 8 ጊጋባይት ራም፣ 512 ጊጋባይት ኤስኤስዲ። ለስራ እና ለትምህርት ተመራጭ።",
                descriptionAfaanOromo = "Piroosesarii Core i5, RAM 8GB fi SSD 512GB qaba.",
                primaryImageUrl = "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=600",
                extraImagesCsv = "https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?w=600,https://images.unsplash.com/photo-1541807084-5c52b6b3adef?w=600,https://media.giphy.com/media/3o7aCTPPm4OHfRLSH6/giphy.gif,https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                categoryId = "cat_electronics",
                subCategory = "Laptops",
                price = 24999.0,
                originalPrice = 29999.0,
                discountPercent = 17,
                stockQuantity = 3, // Low stock - only 3 left!
                stockHoldEnabled = true,
                rating = 4.8,
                reviewCount = 856,
                soldCount = "1.2k sold",
                colorsCsv = "Silver,Space Gray,Black",
                specsCsv = "8GB RAM / 512GB SSD,16GB RAM / 512GB SSD",
                status = "ACTIVE",
                isWishlisted = true
            ),
            ProductEntity(
                id = "prod_laptop_lenovo",
                shopId = "shop_techhub",
                shopName = "Tech Hub",
                shopVerified = true,
                name = "Lenovo IdeaPad 3",
                nameAmharic = "ሌኖቮ አይዲያፓድ 3",
                nameAfaanOromo = "Lenovo IdeaPad 3",
                description = "Slim and lightweight laptop featuring AMD Ryzen 5, 8GB RAM, 256GB SSD, and anti-glare screen.",
                primaryImageUrl = "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=600",
                categoryId = "cat_electronics",
                subCategory = "Laptops",
                price = 26999.0,
                originalPrice = 32000.0,
                discountPercent = 16,
                stockQuantity = 12,
                stockHoldEnabled = true,
                rating = 4.7,
                reviewCount = 642,
                soldCount = "890 sold",
                colorsCsv = "Abyss Blue,Platinum Grey",
                specsCsv = "8GB RAM / 256GB SSD",
                status = "ACTIVE",
                isWishlisted = false
            ),
            ProductEntity(
                id = "prod_laptop_dell",
                shopId = "shop_techhub",
                shopName = "Tech Hub",
                shopVerified = true,
                name = "Dell Inspiron 15",
                nameAmharic = "ዴል ኢንስፒሮን 15",
                nameAfaanOromo = "Dell Inspiron 15",
                description = "Reliable everyday performance with 11th Gen Intel i5, 120Hz display, 16GB RAM, 512GB SSD.",
                primaryImageUrl = "https://images.unsplash.com/photo-1593642632823-8f785ba67e45?w=600",
                categoryId = "cat_electronics",
                subCategory = "Laptops",
                price = 28999.0,
                originalPrice = 34999.0,
                discountPercent = 17,
                stockQuantity = 8,
                stockHoldEnabled = true,
                rating = 4.9,
                reviewCount = 721,
                soldCount = "650 sold",
                colorsCsv = "Carbon Black,Platinum Silver",
                specsCsv = "16GB RAM / 512GB SSD",
                status = "ACTIVE",
                isWishlisted = false
            ),
            ProductEntity(
                id = "prod_headphones_sony",
                shopId = "shop_techhub",
                shopName = "Tech Hub",
                shopVerified = true,
                name = "Sony Wireless Headphones",
                nameAmharic = "የሶኒ ገመድ አልባ የጆሮ ማዳመጫ",
                nameAfaanOromo = "Headphones Sony",
                description = "Industry-leading noise cancellation, 30-hour battery life, touch sensor controls and supreme sound clarity.",
                primaryImageUrl = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600",
                categoryId = "cat_electronics",
                subCategory = "Audio",
                price = 8999.0,
                originalPrice = 10999.0,
                discountPercent = 18,
                stockQuantity = 45,
                stockHoldEnabled = true,
                rating = 4.7,
                reviewCount = 342,
                soldCount = "420 sold",
                colorsCsv = "Black,Silver,Midnight Blue",
                specsCsv = "Standard Edition",
                status = "ACTIVE",
                isWishlisted = true
            ),
            ProductEntity(
                id = "prod_airpods_pro",
                shopId = "shop_techhub",
                shopName = "Tech Hub",
                shopVerified = true,
                name = "Wireless Earbuds Pro",
                nameAmharic = "ዋየርለስ ኢርበድስ ፕሮ",
                nameAfaanOromo = "Wireless Earbuds Pro",
                description = "Active Noise Cancellation, Transparency mode, Adaptive Audio, and wireless charging case.",
                primaryImageUrl = "https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=600",
                categoryId = "cat_electronics",
                subCategory = "Audio",
                price = 2800.0,
                originalPrice = 3500.0,
                discountPercent = 15,
                stockQuantity = 25,
                stockHoldEnabled = true,
                rating = 4.6,
                reviewCount = 420,
                soldCount = "510 sold",
                colorsCsv = "White",
                specsCsv = "USB-C MagSafe",
                status = "ACTIVE",
                isWishlisted = false
            ),
            ProductEntity(
                id = "prod_phone_samsung",
                shopId = "shop_techhub",
                shopName = "Tech Hub",
                shopVerified = true,
                name = "Samsung Galaxy A15",
                nameAmharic = "ሳምሰንግ ጋላክሲ ኤ 15",
                nameAfaanOromo = "Samsung Galaxy A15",
                description = "6.5-inch Super AMOLED 90Hz display, 50MP triple camera, 5000mAh battery with 25W fast charging.",
                primaryImageUrl = "https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?w=600",
                categoryId = "cat_electronics",
                subCategory = "Phones",
                price = 13499.0,
                originalPrice = 14999.0,
                discountPercent = 10,
                stockQuantity = 18,
                stockHoldEnabled = true,
                rating = 4.7,
                reviewCount = 721,
                soldCount = "920 sold",
                colorsCsv = "Blue Black,Light Blue,Yellow",
                specsCsv = "128GB / 6GB RAM,256GB / 8GB RAM",
                status = "ACTIVE",
                isWishlisted = false
            ),
            ProductEntity(
                id = "prod_tshirt_mens",
                shopId = "shop_fashionworld",
                shopName = "Fashion World",
                shopVerified = true,
                name = "Men's Premium T-Shirt",
                nameAmharic = "የወንዶች ቲሸርት",
                nameAfaanOromo = "T-Shirt Dhiiraa",
                description = "100% breathable combed organic cotton. Premium stitching, vibrant colour retention and tailored fit.",
                primaryImageUrl = "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=600",
                categoryId = "cat_fashion",
                subCategory = "Men",
                price = 799.0,
                originalPrice = 999.0,
                discountPercent = 20,
                stockQuantity = 60,
                stockHoldEnabled = true,
                rating = 4.8,
                reviewCount = 310,
                soldCount = "1.5k sold",
                colorsCsv = "Orange,White,Black,Navy Blue",
                specsCsv = "M,L,XL,XXL",
                status = "ACTIVE",
                isWishlisted = false
            ),
            ProductEntity(
                id = "prod_nike_af1",
                shopId = "shop_fashionworld",
                shopName = "Fashion World",
                shopVerified = true,
                name = "Nike Air Force 1",
                nameAmharic = "ናይክ ኤር ፎርስ 1 ጫማ",
                nameAfaanOromo = "Kophee Nike Air Force 1",
                description = "Legendary street style sneaker with stitched overlays, crisp leather and lightweight Nike Air cushioning.",
                primaryImageUrl = "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?w=600",
                categoryId = "cat_fashion",
                subCategory = "Shoes",
                price = 12499.0,
                originalPrice = 14500.0,
                discountPercent = 14,
                stockQuantity = 15,
                stockHoldEnabled = true,
                rating = 4.8,
                reviewCount = 520,
                soldCount = "780 sold",
                colorsCsv = "Triple White,White/Black",
                specsCsv = "EU 40,EU 41,EU 42,EU 43,EU 44",
                status = "ACTIVE",
                isWishlisted = true
            ),
            ProductEntity(
                id = "prod_smart_watch",
                shopId = "shop_techhub",
                shopName = "Tech Hub",
                shopVerified = true,
                name = "Smart Watch Series 9",
                nameAmharic = "ስማርት ሰዓት ተከታታይ 9",
                nameAfaanOromo = "Sa'aatii Ammayyaa",
                description = "Always-on Retina display, ECG and blood oxygen apps, sleep tracking, and crash detection.",
                primaryImageUrl = "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600",
                categoryId = "cat_electronics",
                subCategory = "Smart Watches",
                price = 15999.0,
                originalPrice = 18000.0,
                discountPercent = 11,
                stockQuantity = 12,
                stockHoldEnabled = true,
                rating = 4.6,
                reviewCount = 180,
                soldCount = "340 sold",
                colorsCsv = "Midnight,Starlight,Silver",
                specsCsv = "41mm,45mm",
                status = "PAUSED",
                isWishlisted = false
            ),
            ProductEntity(
                id = "prod_backpack",
                shopId = "shop_fashionworld",
                shopName = "Fashion World",
                shopVerified = true,
                name = "Travel Backpack Pro",
                nameAmharic = "የጉዞ ጀርባ ቦርሳ",
                nameAfaanOromo = "Boorsaa Imalaa",
                description = "Waterproof nylon, dedicated 16\" laptop compartment, USB charging port, anti-theft hidden pocket.",
                primaryImageUrl = "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=600",
                categoryId = "cat_bags",
                subCategory = "Backpacks",
                price = 9999.0,
                originalPrice = 11999.0,
                discountPercent = 16,
                stockQuantity = 20,
                stockHoldEnabled = true,
                rating = 4.7,
                reviewCount = 95,
                soldCount = "210 sold",
                colorsCsv = "Black,Grey,Army Green",
                specsCsv = "30L Capacity",
                status = "ACTIVE",
                isWishlisted = false
            ),
            ProductEntity(
                id = "prod_living_sofa",
                shopId = "shop_homeplus",
                shopName = "Home Plus",
                shopVerified = true,
                name = "Modern Living Room Sofa",
                nameAmharic = "ዘመናዊ የሳሎን ሶፋ",
                nameAfaanOromo = "Soofaa Ammayyaa",
                description = "Comfortable 3-seater contemporary sofa with soft linen fabric, solid wood frame, and high-density foam.",
                primaryImageUrl = "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=600",
                categoryId = "cat_furniture",
                subCategory = "Sofas",
                price = 48000.0,
                originalPrice = 55000.0,
                discountPercent = 12,
                stockQuantity = 4,
                stockHoldEnabled = true,
                rating = 4.9,
                reviewCount = 78,
                soldCount = "45 sold",
                colorsCsv = "Warm Grey,Emerald,Navy",
                specsCsv = "3-Seater (220cm)",
                status = "ACTIVE",
                isWishlisted = false
            )
        )
        db.productDao().insertAll(products.map { withDemoDetails(it) })
        db.wishlistDao().insertAll(
            listOf(
                WishlistItemEntity(userId = "user_fitsum", productId = "prod_laptop_hp"),
                WishlistItemEntity(userId = "user_fitsum", productId = "prod_headphones_sony"),
                WishlistItemEntity(userId = "user_fitsum", productId = "prod_nike_af1")
            )
        )

        // Shop Payment Methods
        val paymentMethods = listOf(
            ShopPaymentMethodEntity(
                id = "pm_th_bank",
                shopId = "shop_techhub",
                providerType = "BANK_TRANSFER",
                bankName = "Commercial Bank of Ethiopia",
                accountName = "Tech Hub Trading PLC",
                accountNumber = "100013456789",
                instructions = "1. Transfer the exact amount.\n2. Use Order Reference in the transfer memo.\n3. Enter the transaction reference below.",
                isRequired = true,
                isEnabled = true
            ),
            ShopPaymentMethodEntity(
                id = "pm_th_telebirr",
                shopId = "shop_techhub",
                providerType = "TELEBIRR",
                bankName = "Telebirr",
                accountName = "Tech Hub Store",
                accountNumber = "+251 912 345 678",
                instructions = "Send payment to Telebirr merchant phone +251 912 345 678 and enter the received transaction reference number.",
                isRequired = false,
                isEnabled = true
            ),
            ShopPaymentMethodEntity(
                id = "pm_th_cbebirr",
                shopId = "shop_techhub",
                providerType = "CBE_BIRR",
                bankName = "CBE Birr",
                accountName = "Tech Hub CBE Birr",
                accountNumber = "889922",
                instructions = "Use CBE Birr mobile app to pay till number 889922 and enter the SMS reference ID.",
                isRequired = false,
                isEnabled = true
            ),
            // Fashion World payment methods
            ShopPaymentMethodEntity(
                id = "pm_fw_telebirr",
                shopId = "shop_fashionworld",
                providerType = "TELEBIRR",
                bankName = "Telebirr",
                accountName = "Fashion World",
                accountNumber = "+251 911 223 344",
                instructions = "Pay through Telebirr and submit reference.",
                isRequired = true,
                isEnabled = true
            ),
            ShopPaymentMethodEntity(
                id = "pm_fw_bank",
                shopId = "shop_fashionworld",
                providerType = "BANK_TRANSFER",
                bankName = "Awash Bank",
                accountName = "Fashion World Apparel",
                accountNumber = "01320495867300",
                instructions = "Transfer to Awash Bank and submit reference code.",
                isRequired = false,
                isEnabled = true
            )
        )
        db.shopPaymentMethodDao().insertAll(paymentMethods)

        // Shop Delivery Options
        val deliveryOptions = listOf(
            ShopDeliveryOptionEntity(
                id = "del_th_std",
                shopId = "shop_techhub",
                optionType = "STANDARD",
                title = "Standard Delivery",
                fee = 50.0,
                estimatedTime = "1-3 days",
                isEnabled = true
            ),
            ShopDeliveryOptionEntity(
                id = "del_th_exp",
                shopId = "shop_techhub",
                optionType = "EXPRESS",
                title = "Express Delivery",
                fee = 100.0,
                estimatedTime = "Same day",
                isEnabled = true
            ),
            ShopDeliveryOptionEntity(
                id = "del_th_pickup",
                shopId = "shop_techhub",
                optionType = "PICKUP",
                title = "Store Pickup",
                fee = 0.0,
                estimatedTime = "Pick up at our store (Bole Medhanialem)",
                isEnabled = true
            ),
            ShopDeliveryOptionEntity(
                id = "del_fw_std",
                shopId = "shop_fashionworld",
                optionType = "STANDARD",
                title = "Standard Delivery",
                fee = 50.0,
                estimatedTime = "1-2 days",
                isEnabled = true
            )
        )
        db.shopDeliveryOptionDao().insertAll(deliveryOptions)

        // Promo Codes
        db.promoCodeDao().insertAll(
            listOf(
                PromoCodeEntity(
                    id = "promo_1",
                    shopId = "shop_techhub",
                    code = "SUMMER25",
                    discountType = "PERCENTAGE",
                    discountValue = 25.0,
                    minOrderAmount = 1000.0,
                    validUntil = "Jun 30, 2026",
                    isActive = true
                ),
                PromoCodeEntity(
                    id = "promo_2",
                    shopId = "shop_techhub",
                    code = "TECH10",
                    discountType = "PERCENTAGE",
                    discountValue = 10.0,
                    minOrderAmount = 5000.0,
                    validUntil = "Dec 31, 2026",
                    isActive = true
                )
            )
        )

        // Pre-loaded Cart items matching UI Mockup (Cart with Tech Hub + Fashion World)
        db.cartDao().insertCartItem(
            CartItemEntity(
                userId = "user_fitsum",
                productId = "prod_laptop_hp",
                shopId = "shop_techhub",
                shopName = "Tech Hub",
                shopVerified = true,
                productName = "HP Laptop 15.6\"",
                productImageUrl = "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=600",
                unitPrice = 24999.0,
                quantity = 2,
                selectedVariation = "Silver • 8GB RAM",
                stockAvailable = 3
            )
        )
        db.cartDao().insertCartItem(
            CartItemEntity(
                userId = "user_fitsum",
                productId = "prod_tshirt_mens",
                shopId = "shop_fashionworld",
                shopName = "Fashion World",
                shopVerified = true,
                productName = "Men's T-Shirt",
                productImageUrl = "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=600",
                unitPrice = 799.0,
                quantity = 1,
                selectedVariation = "Orange • L",
                stockAvailable = 60
            )
        )

        // Sample Orders
        val orders = listOf(
            OrderEntity(
                id = "ord_1",
                orderReference = "SUQ-2025-001234",
                customerUserId = "user_fitsum",
                customerName = "Fitsum Gebre",
                customerPhone = "+251 912 345 678",
                deliveryAddress = "Bole, Addis Ababa, Ethiopia",
                shopId = "shop_techhub",
                shopName = "Tech Hub",
                itemsJson = "[{\"productId\":\"prod_laptop_hp\",\"productName\":\"HP Laptop 15.6\\\" Core i5\",\"productImageUrl\":\"https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=600\",\"variation\":\"Silver • 8GB RAM\",\"unitPrice\":24999.0,\"quantity\":1,\"totalPrice\":24999.0}]",
                subtotal = 24999.0,
                deliveryFee = 50.0,
                discountAmount = 0.0,
                totalAmount = 25049.0,
                deliveryOptionTitle = "Standard Delivery",
                deliveryOptionType = "STANDARD",
                deliveryEstimatedDays = "1-3 days",
                paymentMethodTitle = "Telebirr",
                paymentMethodType = "TELEBIRR",
                paymentMethodAccount = "+251 912 345 678",
                transactionReference = "TXN-TB-9823145",
                paymentStatus = "PENDING_VERIFICATION",
                orderStatus = "Awaiting Payment Confirmation",
                createdAt = daysAgoTimestamp(1),
                transferredAmount = 25049.0
            ),
            OrderEntity(
                id = "ord_2",
                orderReference = "SUQ-2025-001233",
                customerUserId = "user_fitsum",
                customerName = "Fitsum Gebre",
                customerPhone = "+251 912 345 678",
                deliveryAddress = "Bole, Addis Ababa, Ethiopia",
                shopId = "shop_fashionworld",
                shopName = "Fashion World",
                itemsJson = "[{\"productId\":\"prod_nike_af1\",\"productName\":\"Nike Air Force 1\",\"productImageUrl\":\"https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?w=600\",\"variation\":\"EU 42 • White\",\"unitPrice\":12499.0,\"quantity\":1,\"totalPrice\":12499.0}]",
                subtotal = 12499.0,
                deliveryFee = 50.0,
                discountAmount = 0.0,
                totalAmount = 12549.0,
                deliveryOptionTitle = "Standard Delivery",
                deliveryOptionType = "STANDARD",
                deliveryEstimatedDays = "1-2 days",
                paymentMethodTitle = "Telebirr",
                paymentMethodType = "TELEBIRR",
                paymentMethodAccount = "+251 911 223 344",
                transactionReference = "TXN-TB-8841203",
                paymentStatus = "VERIFIED",
                orderStatus = "Confirmed",
                createdAt = daysAgoTimestamp(10),
                transferredAmount = 12549.0
            ),
            OrderEntity(
                id = "ord_3",
                orderReference = "SUQ-2025-001232",
                customerUserId = "user_fitsum",
                customerName = "Fitsum Gebre",
                customerPhone = "+251 912 345 678",
                deliveryAddress = "Kazanchis, Sunshine Bldg, Kirkos",
                shopId = "shop_homeplus",
                shopName = "Home Plus",
                itemsJson = "[{\"productId\":\"prod_backpack\",\"productName\":\"Travel Backpack\",\"productImageUrl\":\"https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=600\",\"variation\":\"Black\",\"unitPrice\":9999.0,\"quantity\":1,\"totalPrice\":9999.0}]",
                subtotal = 9999.0,
                deliveryFee = 0.0,
                discountAmount = 0.0,
                totalAmount = 9999.0,
                deliveryOptionTitle = "Store Pickup",
                deliveryOptionType = "PICKUP",
                deliveryEstimatedDays = "Ready for pickup",
                paymentMethodTitle = "Bank Transfer",
                paymentMethodType = "BANK_TRANSFER",
                paymentMethodAccount = "Commercial Bank of Ethiopia",
                transactionReference = "FT26117A718290",
                paymentStatus = "VERIFIED",
                orderStatus = "Delivered",
                createdAt = daysAgoTimestamp(45),
                transferredAmount = 9999.0
            )
        )
        db.orderDao().insertAll(orders)

        // Reviews
        db.reviewDao().insertAll(
            listOf(
                ReviewEntity(
                    id = "rev_1",
                    userId = "user_dawit",
                    userName = "Dawit Bekele",
                    userAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100",
                    targetType = "PRODUCT",
                    targetId = "prod_laptop_hp",
                    rating = 5.0,
                    comment = "Excellent laptop! Genuine HP with warranty card. Booted up super fast and battery lasts 8 hours. Highly recommend Tech Hub.",
                    isVerifiedPurchase = true,
                    date = "2 days ago"
                ),
                ReviewEntity(
                    id = "rev_2",
                    userId = "user_selam",
                    userName = "Selamawit Tadesse",
                    userAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100",
                    targetType = "PRODUCT",
                    targetId = "prod_laptop_hp",
                    rating = 4.5,
                    comment = "Very good display and keyboard feels nice. Delivery to Bole was fast within 2 days.",
                    isVerifiedPurchase = true,
                    date = "1 week ago"
                ),
                ReviewEntity(
                    id = "rev_3",
                    userId = "user_natnael",
                    userName = "Natnael Yohannes",
                    userAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=100",
                    targetType = "SHOP",
                    targetId = "shop_techhub",
                    rating = 5.0,
                    comment = "One of the best verified tech shops in Addis Ababa. Responsive on chat and verified my Telebirr payment in 15 minutes.",
                    isVerifiedPurchase = true,
                    date = "Apr 20, 2026"
                )
            )
        )

        // Chat messages with Tech Hub (matches reference image conversation)
        db.chatDao().insertAll(
            listOf(
                ChatMessageEntity(
                    id = "msg_1",
                    conversationId = "conv_techhub_user_fitsum",
                    shopId = "shop_techhub",
                    shopName = "TechAddis",
                    senderType = "SHOP",
                    message = "Hello! 👋\nThanks for contacting us.\nHow can we help you today?",
                    timestamp = "9:20 AM",
                    isRead = true,
                    customerUserId = "user_fitsum"
                ),
                ChatMessageEntity(
                    id = "msg_2",
                    conversationId = "conv_techhub_user_fitsum",
                    shopId = "shop_techhub",
                    shopName = "TechAddis",
                    senderType = "CUSTOMER",
                    message = "Is this product available in Addis Ababa?",
                    timestamp = "9:22 AM",
                    isRead = true,
                    customerUserId = "user_fitsum"
                ),
                ChatMessageEntity(
                    id = "msg_3",
                    conversationId = "conv_techhub_user_fitsum",
                    shopId = "shop_techhub",
                    shopName = "TechAddis",
                    senderType = "SHOP",
                    message = "Yes, it is available.\nWe can deliver within 1–3 days. 🚚\nYou can also check our other electronics products.",
                    timestamp = "9:24 AM",
                    productContextName = "Wireless Earbuds Pro",
                    productContextPrice = "ETB 2,800",
                    productContextImage = "https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=600",
                    isRead = true,
                    customerUserId = "user_fitsum"
                ),
                ChatMessageEntity(
                    id = "msg_4",
                    conversationId = "conv_techhub_user_fitsum",
                    shopId = "shop_techhub",
                    shopName = "Tech Hub",
                    senderType = "CUSTOMER",
                    message = "Hi! I just submitted my Telebirr transfer for order SUQ-2025-001234. Can you dispatch today?",
                    timestamp = "10:15 AM",
                    isRead = false,
                    orderContextRef = "SUQ-2025-001234",
                    orderContextStatus = "Awaiting Payment Confirmation",
                    orderContextTotal = "ETB 25,049",
                    threadCategory = "ORDER",
                    customerUserId = "user_fitsum"
                ),
                ChatMessageEntity(
                    id = "msg_fw_1",
                    conversationId = "conv_fashionworld_user_fitsum",
                    shopId = "shop_fashionworld",
                    shopName = "Fashion World",
                    senderType = "CUSTOMER",
                    message = "Do you have Nike Air Force 1 in EU 43 ready in Bole?",
                    timestamp = "Yesterday",
                    isRead = true,
                    threadCategory = "QUESTION",
                    customerUserId = "user_fitsum"
                )
            )
        )

        // Notifications
        db.notificationDao().insertAll(
            listOf(
                NotificationEntity(
                    id = "notif_1",
                    userId = "user_fitsum",
                    type = "PAYMENT",
                    title = "Payment Pending Verification",
                    body = "Your payment reference TXN-TB-9823145 for order #SUQ-2025-001234 has been submitted to Tech Hub.",
                    timestamp = "10 min ago",
                    isRead = false
                ),
                NotificationEntity(
                    id = "notif_2",
                    userId = "user_fitsum",
                    type = "ORDER",
                    title = "Order Confirmed",
                    body = "Fashion World has verified your payment and confirmed Order #SUQ-2025-001233.",
                    timestamp = "Yesterday",
                    isRead = true
                ),
                NotificationEntity(
                    id = "notif_3",
                    userId = "user_fitsum",
                    type = "CHAT",
                    title = "New message from Tech Hub",
                    body = "Thank you! Your order is on the way.",
                    timestamp = "2 hours ago",
                    isRead = true
                )
            )
        )

        ensureSecondaryTablesSeeded(db)
    }

    suspend fun ensureSecondaryTablesSeeded(db: SuqiDatabase) {
        if (db.userDao().getUserById("user_admin") == null) {
            db.userDao().insertUser(
                UserEntity(
                    id = "user_admin",
                    name = "Abebe Kebede",
                    phone = "+251 911 000 111",
                    email = "admin@suqi.et",
                    avatarUrl = "",
                    role = "ADMIN",
                    preferredLanguage = "English",
                    region = "Addis Ababa",
                    city = "Addis Ababa",
                    subCity = "Kirkos",
                    adminTier = "OWNER",
                    activeRole = "ADMIN",
                    isActiveSession = false,
                    createdAt = daysAgoTimestamp(60)
                )
            )
        }
        if (db.userDao().getActiveSessionUser() == null) {
            db.userDao().getFirstUser()?.let { db.userDao().setActiveSession(it.id) }
        }
        val linkedByCat = ProductTemplates.defaultAll
            .filter { it.categoryId.isNotBlank() }
            .groupBy { it.categoryId }
            .mapValues { (_, list) -> list.joinToString(",") { it.id } }

        val existingCategories = db.categoryDao().getAllCategories()
        if (existingCategories.isEmpty()) {
            db.categoryDao().insertAll(
                MarketplaceCategories.all.map { c ->
                    CategoryEntity(
                        id = c.id,
                        name = c.name,
                        nameAmharic = c.nameAmharic,
                        nameAfaanOromo = c.nameAfaanOromo,
                        icon = c.icon,
                        imageUrl = c.imageUrl,
                        subCategoriesCsv = c.subCategories.joinToString(","),
                        isActive = true,
                        sortOrder = c.sortOrder,
                        linkedItemTypesCsv = linkedByCat[c.id].orEmpty()
                    )
                }
            )
        } else if (existingCategories.all { it.linkedItemTypesCsv.isBlank() }) {
            db.categoryDao().insertAll(
                existingCategories.map { cat ->
                    cat.copy(linkedItemTypesCsv = linkedByCat[cat.id].orEmpty())
                }
            )
        }

        if (db.itemTypeDao().getAllItemTypes().isEmpty()) {
            db.itemTypeDao().insertAll(
                ProductTemplates.defaultAll.mapIndexed { idx, t ->
                    ItemTypeEntity(
                        id = t.id,
                        label = t.label,
                        emoji = t.emoji,
                        categoryId = t.categoryId,
                        groupName = t.group,
                        attributesJson = com.example.data.model.attributesToJson(t.attributes),
                        sortOrder = idx
                    )
                }
            )
        }

        if (db.promoBannerDao().getAllBanners().isEmpty()) {
            db.promoBannerDao().insertAll(
                listOf(
                    PromoBannerEntity(
                        id = "banner_1",
                        badge = "Hot Deal",
                        title = "Up to 50% Off",
                        highlight = "50%",
                        subtitle = "On Selected Items",
                        buttonText = "Shop Now",
                        imageUrl = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600",
                        targetCategory = "cat_electronics",
                        targetName = "Electronics",
                        isActive = true,
                        sortOrder = 0,
                        badgeIcon = "flame",
                        destinationType = "CATEGORY",
                        targetId = "cat_electronics"
                    ),
                    PromoBannerEntity(
                        id = "banner_2",
                        badge = "New Season",
                        title = "Ethiopian Fashion",
                        highlight = "Fashion",
                        subtitle = "Handcrafted & modern",
                        buttonText = "Explore Style",
                        imageUrl = "https://images.unsplash.com/photo-1523381294911-8d3cead13475?w=600",
                        targetCategory = "cat_fashion",
                        targetName = "Fashion",
                        isActive = true,
                        sortOrder = 1,
                        badgeIcon = "sparkles",
                        destinationType = "CATEGORY",
                        targetId = "cat_fashion"
                    ),
                    PromoBannerEntity(
                        id = "banner_3",
                        badge = "Home & Living",
                        title = "Transform Your Home",
                        highlight = "Home",
                        subtitle = "Furniture & decor",
                        buttonText = "View Decor",
                        imageUrl = "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=600",
                        targetCategory = "cat_home",
                        targetName = "Home & Living",
                        isActive = true,
                        sortOrder = 2,
                        badgeIcon = "house",
                        destinationType = "CATEGORY",
                        targetId = "cat_home"
                    )
                )
            )
        }
        if (db.subscriptionPlanDao().getAllPlans().isEmpty()) {
            db.subscriptionPlanDao().insertAll(
                listOf(
                    SubscriptionPlanEntity(
                        id = "plan_starter",
                        name = "Starter",
                        monthlyPrice = 0.0,
                        yearlyPrice = 0.0,
                        maxProducts = 25,
                        featuresCsv = "Up to 25 products,Standard listing,Telebirr & CBE checkout",
                        isPopular = false,
                        isActive = true
                    ),
                    SubscriptionPlanEntity(
                        id = "plan_pro",
                        name = "Pro Seller Plan",
                        monthlyPrice = 299.0,
                        yearlyPrice = 2990.0,
                        maxProducts = 500,
                        featuresCsv = "Up to 500 products,Verified shop badge boost,Promo codes & vouchers,Priority seller analytics",
                        isPopular = true,
                        isActive = true
                    ),
                    SubscriptionPlanEntity(
                        id = "plan_enterprise",
                        name = "Enterprise",
                        monthlyPrice = 899.0,
                        yearlyPrice = 8990.0,
                        maxProducts = 5000,
                        featuresCsv = "Unlimited listings,Multi-staff verification,Featured homepage placement,Dedicated Addis support",
                        isPopular = false,
                        isActive = true
                    )
                )
            )
        }
        if (db.flaggedReviewDao().getAllFlagged().isEmpty()) {
            db.flaggedReviewDao().insertAll(
                listOf(
                    FlaggedReviewEntity(
                        id = "flag_1",
                        authorName = "Kaleb T.",
                        reason = "Contains external contact link",
                        reviewText = "Message me on WhatsApp 0911xxx to get this item cheaper direct!",
                        status = "REPORTED",
                        createdAt = "Today"
                    ),
                    FlaggedReviewEntity(
                        id = "flag_2",
                        authorName = "Meron A.",
                        reason = "Suspicious promotional spam",
                        reviewText = "Visit bit.ly/free-etb-promo for 1000 ETB cashback coupon now!",
                        status = "REPORTED",
                        createdAt = "Yesterday"
                    )
                )
            )
        }
        if (db.productQuestionDao().getQuestionsForProduct("prod_laptop_hp").isEmpty()) {
            db.productQuestionDao().insertAll(
                listOf(
                    ProductQuestionEntity(
                        id = "pq_1",
                        productId = "prod_laptop_hp",
                        shopId = "shop_techhub",
                        askerName = "Robel M.",
                        question = "Does it come with an Ethiopian power adapter and laptop bag?",
                        answer = "Yes! Every HP 15 unit includes the official EU/ET 2-pin charger and a complimentary sleeve.",
                        answeredByShop = true,
                        createdAt = "3 days ago"
                    ),
                    ProductQuestionEntity(
                        id = "pq_2",
                        productId = "prod_earbuds_pro",
                        shopId = "shop_techhub",
                        askerName = "Hanna D.",
                        question = "How long does the charging case hold power?",
                        answer = "Up to 24 hours combined playback with ANC on.",
                        answeredByShop = true,
                        createdAt = "5 days ago"
                    )
                )
            )
        }
    }

    /**
     * Showcases the dynamic product system on a few seeded products (laptop with configurable
     * storage/color variants, sneakers, t-shirt). Other products use the legacy mapping.
     */
    private fun withDemoDetails(p: ProductEntity): ProductEntity {
        val extra = p.extraImagesCsv.split(",").filter { it.isNotBlank() }
        val colors = p.colorsCsv.split(",").filter { it.isNotBlank() }
        val base = legacyProductDetails(p.primaryImageUrl, extra, colors, emptyList())
        return when (p.id) {
            "prod_laptop_hp" -> {
                val attrs = ProductTemplates.byId("laptop").attributes.map {
                    if (it.key == "storage") it.copy(selectable = true) else it
                }
                val values = mapOf(
                    "brand" to listOf("HP"), "model" to listOf("15-fd0000"),
                    "cpu" to listOf("Intel Core i5"), "ram" to listOf("8 GB"),
                    "storage" to listOf("512 GB", "1 TB"), "storage_type" to listOf("SSD"),
                    "screen_size" to listOf("15.6 inch"), "resolution" to listOf("Full HD"),
                    "os" to listOf("Windows 11"), "color" to colors,
                    "condition" to listOf("New"), "warranty" to listOf("1 year")
                )
                val variants = listOf("512 GB", "1 TB").flatMap { storage ->
                    colors.map { color ->
                        ProductVariant(
                            id = "$storage • $color",
                            options = linkedMapOf("storage" to storage, "color" to color),
                            price = if (storage == "1 TB") 28999.0 else null,
                            stock = if (storage == "1 TB") 2 else null
                        )
                    }
                }
                p.copy(detailsJson = ProductDetails("laptop", base.media, attrs, values, variants).toJson())
            }
            "prod_nike_af1" -> {
                val values = mapOf(
                    "brand" to listOf("Nike"), "gender" to listOf("Men"), "shoe_type" to listOf("Sneakers"),
                    "size" to listOf("40", "41", "42", "43", "44"), "color" to colors.ifEmpty { listOf("White", "Black") },
                    "material" to listOf("Leather"), "condition" to listOf("New")
                )
                p.copy(detailsJson = ProductDetails("shoes", base.media, ProductTemplates.byId("shoes").attributes, values).toJson())
            }
            "prod_tshirt_mens" -> {
                val values = mapOf(
                    "brand" to listOf("Habesha Cotton Co."), "gender" to listOf("Men"),
                    "size" to listOf("S", "M", "L", "XL"), "color" to colors.ifEmpty { listOf("White", "Black") },
                    "material" to listOf("Cotton"), "fit" to listOf("Regular"), "condition" to listOf("New")
                )
                p.copy(detailsJson = ProductDetails("clothing", base.media, ProductTemplates.byId("clothing").attributes, values).toJson())
            }
            else -> p
        }
    }
}
