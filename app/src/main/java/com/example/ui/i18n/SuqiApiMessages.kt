package com.example.ui.i18n

/**
 * Maps English service messages (from the builtin backend today, from the real API later)
 * to the user's language. Unknown messages pass through unchanged, so a future API can
 * keep sending free text; once it sends the same texts or codes, add rules here.
 */
object SuqiApiMessages {
    private class Rule(val pattern: Regex, val am: String, val om: String)

    private fun r(en: String, am: String, om: String) = Rule(Regex(en, RegexOption.DOT_MATCHES_ALL), am, om)

    // Regex group references are written as $1, $2 in the replacement templates.
    private val rules: List<Rule> = listOf(
        r("^Enter a valid Ethiopian phone number.*", "ትክክለኛ የኢትዮጵያ ስልክ ቁጥር ያስገቡ (+251 9xx xxx xxx)", "Lakkoofsa bilbilaa Itoophiyaa sirrii galchaa (+251 9xx xxx xxx)"),
        r("^Enter the 6-digit SMS verification code.*", "ባለ 6 አሃዝ የኤስኤምኤስ ማረጋገጫ ኮድ ያስገቡ", "Koodii mirkaneessaa SMS lakkoofsa 6 galchaa"),
        r("^Enter admin email/phone and at least a 4-digit PIN\\.?", "የአስተዳዳሪ ኢሜይል/ስልክ እና ቢያንስ ባለ 4 አሃዝ ፒን ያስገቡ።", "Imeelii/bilbila bulchaa fi yoo xiqqaate PIN lakkoofsa 4 galchaa."),
        r("^Invalid admin credentials\\..*", "የአስተዳዳሪ መረጃ ትክክል አይደለም።", "Ragaan bulchaa sirrii miti."),
        r("^Message cannot be empty\\.?", "መልዕክቱ ባዶ መሆን አይችልም።", "Ergaan duwwaa ta'uu hin danda'u."),
        r("^No active user session\\.?", "ንቁ የተጠቃሚ ክፍለ ጊዜ የለም።", "Yeroon fayyadamaa hojiirra jiru hin jiru."),
        r("^Staff accounts cannot be deleted here\\.?", "የሰራተኛ መለያዎች እዚህ መሰረዝ አይችሉም።", "Herregni hojjettootaa asitti haqamuu hin danda'u."),
        r("^Accounts that own a shop cannot be deleted.*", "ሱቅ ያላቸው መለያዎች ሱቁ ንቁ ሆኖ ሳለ መሰረዝ አይችሉም።", "Herregni dukaana qabu osoo dukaanni hojiirra jiruu haqamuu hin danda'u."),
        r("^No shop associated with (current )?account\\.?", "ከመለያው ጋር የተያያዘ ሱቅ የለም።", "Akaawuntii kanaan walqabatee suuqni hin jiru."),
        r("^No shop found for delivery option\\.?", "ለዚህ የማድረስ አማራጭ ሱቅ አልተገኘም።", "Filannoo geejjibaa kanaaf suuqni hin argamne."),
        r("^No shop found for payment method\\.?", "ለዚህ የክፍያ ዘዴ ሱቅ አልተገኘም።", "Mala kaffaltii kanaaf suuqni hin argamne."),
        r("^No shop found for promotion\\.?", "ለዚህ ማስተዋወቂያ ሱቅ አልተገኘም።", "Beeksisa kanaaf suuqni hin argamne."),
        r("^Order not found\\.?", "ትዕዛዙ አልተገኘም።", "Ajajni hin argamne."),
        r("^Product not found\\.?", "ምርቱ አልተገኘም።", "Oomishni hin argamne."),
        r("^Shop not found\\.?", "ሱቁ አልተገኘም።", "Suuqni hin argamne."),
        r("^Address not found\\.?", "አድራሻው አልተገኘም።", "Teessoon hin argamne."),
        r("^Cannot delete an address currently used by an active order\\.?", "አሁን በንቁ ትዕዛዝ ላይ ያለ አድራሻ መሰረዝ አይቻልም።", "Teessoo ajaja hojiirra jiruun fayyadamamu haquun hin danda'amu."),
        r("^Create a seller shop first before adding products\\.?", "ምርቶችን ከመጨመርዎ በፊት መጀመሪያ የሻጭ ሱቅ ይፍጠሩ።", "Oomishoota dabaluu dura suuqa gurgurtaa uumaa."),
        r("^Please sign in before opening a shop\\.?", "ሱቅ ከመክፈትዎ በፊት እባክዎ ይግቡ።", "Suuqa banuu dura maaloo seenaa."),
        r("^Sign in to save delivery addresses\\.?", "የማድረሻ አድራሻዎችን ለማስቀመጥ ይግቡ።", "Teessoo geejjibaa olkaa'uuf seenaa."),
        r("^Please enter at least 5 characters\\.?", "እባክዎ ቢያንስ 5 ቁምፊዎችን ያስገቡ።", "Maaloo yoo xiqqaate arfii 5 galchaa."),
        r("^Review comment cannot be empty\\.?", "የግምገማ አስተያየት ባዶ መሆን አይችልም።", "Yaadni ilaalcha duwwaa ta'uu hin danda'u."),
        r("^Transaction reference is required\\.?", "የግብይት ማጣቀሻ ያስፈልጋል።", "Lakkoofsi wabii daldalaa barbaachisaa dha."),
        r("^(.+) requires a payment receipt screenshot before submitting your order\\.?", "ትዕዛዝዎን ከማስገባትዎ በፊት $1 የክፍያ ደረሰኝ ፎቶ ይፈልጋል።", "Ajaja keessan galchuu dura $1 suuraa ragaa kaffaltii barbaada."),
        r("^Insufficient stock for (.+)! Only (\\d+) left\\.?", "ለ$1 በቂ ክምችት የለም! $2 ብቻ ቀርተዋል።", "$1 kuusaan gahaa miti! $2 qofatu hafe."),
        r("^Only (\\d+) items in stock!?", "በክምችት ውስጥ $1 እቃዎች ብቻ አሉ!", "Kuusaa keessa meeshaaleen $1 qofatu jira!"),
        r("^This transaction reference \\((.+)\\) is already registered on another order\\.?", "ይህ የግብይት ማጣቀሻ ($1) በሌላ ትዕዛዝ ላይ አስቀድሞ ተመዝግቧል።", "Lakkoofsi wabii daldalaa kun ($1) ajaja biraa irratti duraan galmeeffameera."),
        r("^This transaction reference \\((.+)\\) has already been submitted for another active order\\.?", "ይህ የግብይት ማጣቀሻ ($1) ለሌላ ንቁ ትዕዛዝ አስቀድሞ ገብቷል።", "Lakkoofsi wabii daldalaa kun ($1) ajaja hojiirra jiru biraaf duraan galfameera."),
        // Product validation (seller form)
        r("^Product name is required\\.?", "የምርት ስም ያስፈልጋል።", "Maqaan oomishaa barbaachisaa dha."),
        r("^A valid price is required\\.?", "ትክክለኛ ዋጋ ያስፈልጋል።", "Gatiin sirrii barbaachisaa dha."),
        r("^Price must be greater than 0\\.?", "ዋጋ ከ0 በላይ መሆን አለበት።", "Gatiin 0 ol ta'uu qaba."),
        r("^Discount must be between 0 and 90%\\.?", "ቅናሽ ከ0 እስከ 90% መሆን አለበት።", "Hir'inni 0 hanga 90% gidduu ta'uu qaba."),
        r("^Stock cannot be negative\\.?", "ክምችት አሉታዊ መሆን አይችልም።", "Kuusaan negatiivii ta'uu hin danda'u."),
        r("^You can add up to 12 media files\\.?", "እስከ 12 የሚዲያ ፋይሎችን ማከል ይችላሉ።", "Faayila miidiyaa hanga 12 dabaluu dandeessu."),
        r("^Bulk update applied\\.?", "የጅምላ ማሻሻያ ተተግብሯል።", "Haaromsi jumlaa hojiirra ooleera."),
        r("^Product duplicated\\.?", "ምርቱ ተቀድቷል።", "Oomishni garagalfameera."),
        r("^Select at least one product\\.?", "ቢያንስ አንድ ምርት ይምረጡ።", "Yoo xiqqaate oomisha tokko filadhaa."),
        r("^Return window expired.*", "የ7 ቀን የመመለሻ ጊዜ አልፏል።", "Yeroon deebisuu guyyaa 7 darbeera."),
        r("^A return request already exists.*", "ለዚህ ትዕዛዝ የመመለሻ ጥያቄ አስቀድሞ ገብቷል።", "Ajaja kanaaf gaaffiin deebisuu duraan galmeeffameera."),
        r("^Please select at least one item to return\\.?", "እባክዎ ለመመለስ ቢያንስ አንድ እቃ ይምረጡ።", "Maaloo deebisuuf yoo xiqqaate meeshaa tokko filadhaa."),
        r("^Minimum payout is ETB ([0-9,.]+)\\.?", "ዝቅተኛው የክፍያ መጠን ETB $1 ነው።", "Kaffaltiin xiqqaan ETB $1 dha."),
        r("^Insufficient available balance\\.?", "በቂ የሚወጣ ቀሪ ሂሳብ የለም።", "Hafteen baafamu gahaa miti."),
        r("^Save a payout account first\\.?", "መጀመሪያ የክፍያ መቀበያ መለያዎን ያስቀምጡ።", "Dura herrega kaffaltii olkaa'aa."),
        r("^Payout reference is required\\.?", "የክፍያ ማጣቀሻ ቁጥር ያስፈልጋል።", "Lakkoofsi wabii kaffaltii barbaachisaa dha."),
        r("^Promo code expired or inactive\\.?", "የማስተዋወቂያ ኮዱ ጊዜው አልፏል ወይም አይሰራም።", "Koodiin beeksisaa yeroon isaa darbeera ykn hin hojjetu."),
        r("^Promo usage limit reached\\.?", "የማስተዋወቂያ ኮዱ የአጠቃቀም ገደብ ላይ ደርሷል።", "Daangaan itti fayyadama koodii guutameera."),
        r("^Promo does not apply to items in cart\\.?", "ኮዱ በጋሪው ውስጥ ላሉ እቃዎች አይሰራም።", "Koodichi meeshaalee gaarii keessatti hin hojjetu."),
        r("^Sale price must be lower than original price\\.?", "የቅናሽ ዋጋ ከመጀመሪያው ዋጋ ያነሰ መሆን አለበት።", "Gatiin hir'ifame gatii duraa gadi ta'uu qaba."),
        r("^Upgrade request submitted for verification\\.?", "የዕቅድ ማሻሻያ ጥያቄ ለማረጋገጫ ቀርቧል።", "Gaaffiin olguddisuu karooraa mirkaneessaaf dhiyaateera."),
        r("^Admin permission required\\.?", "የአስተዳዳሪ ፈቃድ ያስፈልጋል።", "Hayyamni bulchaa barbaachisaa dha."),
        r("^Permission denied\\.?", "ፈቃድ ተከልክሏል።", "Hayyamni dhorkameera."),
        r("^You can only request returns for your own orders\\.?", "ለራስዎ ትዕዛዞች ብቻ የመመለሻ ጥያቄ ማቅረብ ይችላሉ።", "Ajaja keessan qofaaf gaaffii deebisuu dhiyeessuu dandeessu."),
        r("^Only the buyer or admin can mark a return shipped back\\.?", "እቃው መመለሱን ማረጋገጥ የሚችለው ገዢው ወይም አስተዳዳሪው ብቻ ነው።", "Meeshaan deebi'uu isaa kan mirkaneessuu danda'u bitaa ykn bulchaa qofa."),
        r("^Only the shop owner or admin can decide this return\\.?", "በዚህ የመመለሻ ጥያቄ ላይ መወሰን የሚችለው የሱቁ ባለቤት ወይም አስተዳዳሪው ብቻ ነው።", "Gaaffii deebisuu kana irratti kan murteessuu danda'u abbaa suuqaa ykn bulchaa qofa."),
        r("^Only the shop owner can reply to this review\\.?", "ለዚህ ግምገማ መልስ መስጠት የሚችለው የሱቁ ባለቤት ብቻ ነው።", "Ilaalcha kanaaf deebii kennuu kan danda'u abbaa suuqaa qofa."),
        r("^Only admins can add internal notes\\.?", "የውስጥ ማስታወሻዎችን ማከል የሚችሉት አስተዳዳሪዎች ብቻ ናቸው።", "Yaadannoo keessoo dabaluu kan danda'an bulchitoota qofa."),
        r("^Cannot (.+) return from status (.+)\\.?", "ከሁኔታ $2 ላይ $1 ማድረግ አይቻልም።", "Haala $2 irraa $1 gochuun hin danda'amu."),
        r("^Cannot (.+) payout from status (.+)\\.?", "ከሁኔታ $2 ላይ ክፍያ $1 ማድረግ አይቻልም።", "Haala $2 irraa kaffaltii $1 gochuun hin danda'amu."),
        r("^Upgrade request already (.+)\\.?", "የማሻሻያ ጥያቄው አስቀድሞ $1 ሆኗል።", "Gaaffiin olguddisuu duraan $1 ta'eera."),
        r("^(.+) is required\\.?", "$1 ያስፈልጋል።", "$1 barbaachisaa dha.")
    )

    fun localize(message: String?, language: SuqiLanguage): String {
        val raw = message.orEmpty()
        if (raw.isBlank() || language == SuqiLanguage.EN) return raw
        for (rule in rules) {
            val m = rule.pattern.find(raw.trim()) ?: continue
            var template = if (language == SuqiLanguage.AM) rule.am else rule.om
            for (i in m.groupValues.indices.drop(1).reversed()) {
                template = template.replace("$$i", m.groupValues[i])
            }
            return template
        }
        return raw
    }

    /** Used by the unit test to guarantee every rule has both translations. */
    fun isComplete(): Boolean = rules.all { it.am.isNotBlank() && it.om.isNotBlank() }
}
