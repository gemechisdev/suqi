package com.example.util

import java.text.NumberFormat
import java.util.Locale

object EthiopianMarketplaceUtils {

    /**
     * Complete list of all 11 official Sub-Cities (Kifle Ketemas) of Addis Ababa,
     * including newly established administrative divisions (e.g. Lemi Kura).
     */
    val ADDIS_ABABA_SUB_CITIES = listOf(
        "Bole",
        "Kirkos",
        "Yeka",
        "Arada",
        "Nifas Silk Lafto",
        "Lideta",
        "Kolfe Keranio",
        "Gullele",
        "Akaky Kaliti",
        "Addis Ketema",
        "Lemi Kura"
    )

    /**
     * Key regional commercial hubs across Ethiopia connected with logistics lines.
     */
    val REGIONAL_COMMERCIAL_HUBS = listOf(
        "Adama",
        "Hawassa",
        "Bahir Dar",
        "Dire Dawa",
        "Bishoftu",
        "Mekelle",
        "Gondar",
        "Jimma"
    )

    /**
     * All official regions and chartered cities of Ethiopia.
     */
    val ETHIOPIA_REGIONS = listOf(
        "Addis Ababa",
        "Oromia",
        "Amhara",
        "Dire Dawa",
        "Sidama",
        "Tigray",
        "Somali",
        "South Ethiopia",
        "Central Ethiopia",
        "Afar",
        "Benishangul-Gumuz",
        "Gambela",
        "Harari"
    )

    /**
     * Validates Ethiopian mobile phone numbers.
     * Supports both Ethio Telecom (09... / +251 9...) and Safaricom Ethiopia (07... / +251 7...).
     */
    fun isValidEthiopianPhone(input: String): Boolean {
        val sanitized = input.replace(Regex("[\\s\\-\\(\\)]"), "")
        // Matches +251 followed by 9 or 7 and 8 digits (13 chars total)
        // OR 09 / 07 followed by 8 digits (10 chars total)
        // OR 9 / 7 followed by 8 digits (9 chars total)
        return sanitized.matches(Regex("^(\\+251[97]\\d{8}|0[97]\\d{8}|[97]\\d{8})$"))
    }

    /**
     * Normalizes an Ethiopian phone number to the standard international display format:
     * e.g. "+251 912 345 678" or "+251 712 345 678"
     */
    fun normalizeEthiopianPhone(input: String): String {
        val sanitized = input.replace(Regex("[\\s\\-\\(\\)]"), "")
        val digits = when {
            sanitized.startsWith("+251") -> sanitized.removePrefix("+251")
            sanitized.startsWith("251") -> sanitized.removePrefix("251")
            sanitized.startsWith("0") -> sanitized.removePrefix("0")
            else -> sanitized
        }

        if (digits.length == 9 && (digits.startsWith("9") || digits.startsWith("7"))) {
            return "+251 ${digits.substring(0, 3)} ${digits.substring(3, 6)} ${digits.substring(6)}"
        }
        return input.trim()
    }

    /**
     * Validates payment transaction references (Telebirr, CBE Birr, Awash, BOA, etc.).
     * Must be alphanumeric and at least 6 characters in length without suspicious wildcards.
     */
    fun isValidPaymentReference(reference: String): Boolean {
        val trimmed = reference.trim()
        if (trimmed.length < 6 || trimmed.length > 32) return false
        return trimmed.matches(Regex("^[a-zA-Z0-9\\-_]+$"))
    }

    /**
     * Formats an amount into standard Ethiopian Birr representation.
     */
    fun formatEtbPrice(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        return "ETB ${formatter.format(amount.toLong())}"
    }

    data class LegalDocument(
        val title: String,
        val subtitle: String,
        val effectiveDate: String,
        val sections: List<LegalSection>
    )

    data class LegalSection(
        val heading: String,
        val content: String
    )

    val TERMS_OF_SERVICE = LegalDocument(
        title = "Terms of Service",
        subtitle = "Suqi Ethiopian Multi-Vendor Marketplace Agreement",
        effectiveDate = "November 2026",
        sections = listOf(
            LegalSection(
                heading = "1. Introduction & Ethiopian Legal Framework",
                content = "Suqi operates as a multi-vendor digital commerce marketplace in accordance with Ethiopian Proclamation No. 1205/2020 on Electronic Transactions. By accessing or using Suqi, customers, vendors, and partners agree to abide by these Terms."
            ),
            LegalSection(
                heading = "2. Vendor Independence & Responsibility",
                content = "Each vendor operates an autonomous shop on Suqi. Vendors are directly responsible for the accuracy of their product descriptions, stock quantities, pricing in Ethiopian Birr (ETB), VAT compliance, and warranty commitments."
            ),
            LegalSection(
                heading = "3. Payment Verification & Order Fulfillment",
                content = "Orders placed using manual bank transfer, CBE Birr, Telebirr, or Awash Pay require valid transaction references. Suqi reserves the right to cancel orders with falsified or duplicated transaction receipts."
            ),
            LegalSection(
                heading = "4. Delivery & Inspection",
                content = "Buyers have the right to inspect delivered merchandise at the time of handover. In cases of damaged or defective items, claims must be filed within 48 hours with order reference and photo documentation."
            )
        )
    )

    val PRIVACY_POLICY = LegalDocument(
        title = "Privacy & Data Protection Policy",
        subtitle = "Protection of Consumer and Vendor Data in Ethiopia",
        effectiveDate = "November 2026",
        sections = listOf(
            LegalSection(
                heading = "1. Data Collection",
                content = "Suqi collects customer name, phone number, delivery address (Region, City, Sub-City), and order history solely to facilitate order processing, delivery logistics, and fraud prevention."
            ),
            LegalSection(
                heading = "2. Financial Information",
                content = "Suqi does not store bank account PINs or Telebirr credentials. Only public transaction references submitted by users for verification are retained on our secure database."
            ),
            LegalSection(
                heading = "3. Vendor Communication",
                content = "Contact between buyers and sellers is recorded within the in-app chat to protect both parties from scam attempts, coordinate pickup locations, and resolve product inquiries."
            )
        )
    )

    val SELLER_AGREEMENT = LegalDocument(
        title = "Vendor Marketplace Agreement",
        subtitle = "Rules of Engagement for Suqi Merchants",
        effectiveDate = "November 2026",
        sections = listOf(
            LegalSection(
                heading = "1. Merchant Onboarding & Verification",
                content = "All merchants must provide valid Ethiopian identification (Kebele ID, National ID/Fayda, or Commercial Registration TIN). Verification is subject to administrator review."
            ),
            LegalSection(
                heading = "2. Counterfeit & Prohibited Goods",
                content = "Selling counterfeit goods, expired pharmaceuticals, uncertified hazardous chemicals, or contraband items is strictly prohibited and results in immediate account termination."
            ),
            LegalSection(
                heading = "3. Payment Verification SLA",
                content = "Merchants must verify or reject customer payment receipts within 4 business hours of order placement to guarantee fast dispatch for customers."
            )
        )
    )

    val FAQS = listOf(
        "How do I pay on Suqi?" to "Select your preferred payment method during checkout (Telebirr, CBE Birr, Awash Bank, or Bank of Abyssinia). Complete the transfer using the shop's displayed account number, then copy and paste your Transaction Reference ID into Suqi.",
        "How does delivery work?" to "Each shop provides delivery options including Standard Delivery, Express Delivery, or In-Store Pickup in major hubs like Addis Ababa, Adama, Hawassa, and Dire Dawa.",
        "What if my payment is rejected?" to "If your transaction reference was mistyped or could not be found, the seller will specify the reason. You can immediately resubmit the correct reference on your Order Details screen.",
        "Can I open my own shop?" to "Yes! Go to the Account tab and tap 'Become a Seller'. Set up your shop name, contact details, payment accounts, and start listing products immediately.",
        "Are reviews verified?" to "Only customers who have confirmed or received orders for a product receive the 'Verified Buyer' badge on their reviews to ensure genuine feedback."
    )
}
