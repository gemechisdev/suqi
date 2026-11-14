package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val coroutineScope = rememberCoroutineScope()

    var phoneInput by remember { mutableStateOf("") }
    var fullNameInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var resendSeconds by remember { mutableIntStateOf(0) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var showSocialSheet by remember { mutableStateOf<String?>(null) } // "TELEGRAM" or "GOOGLE"
    var socialName by remember { mutableStateOf("") }
    var socialHandle by remember { mutableStateOf("") }

    LaunchedEffect(otpSent, resendSeconds) {
        if (otpSent && resendSeconds > 0) {
            delay(1000)
            resendSeconds -= 1
        }
    }

    Scaffold(
        topBar = {
            SuqiHeader(
                title = "Welcome to Suqi",
                onBackClick = {
                    if (!viewModel.goBack()) viewModel.popToRoot(ScreenDestination.Home)
                }
            )
        },
        containerColor = colors.surfaceBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.suqi_logo),
                        contentDescription = "Suqi Logo",
                        modifier = Modifier
                            .size(72.dp)
                            .suqiWarmShadow(RoundedCornerShape(20.dp), elevation = 10.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Every Shop. Every Product.",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sign in with your Ethiopian phone number or preferred account",
                        fontFamily = SuqiFontFamily,
                        fontSize = 13.sp,
                        color = colors.secondaryText,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // B-01 Phone + OTP Card
            item {
                SuqiSectionCard(padding = 18.dp) {
                    Text(
                        text = if (!otpSent) "Sign in with Phone" else "Enter 6-Digit SMS Code",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (!otpSent) "We'll send a one-time verification SMS to your +251 number."
                        else "Sent to +251 ${phoneInput.removePrefix("0").removePrefix("+251").trim()} (Use demo code 123456)",
                        fontFamily = SuqiFontFamily,
                        fontSize = 12.sp,
                        color = colors.secondaryText
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (!otpSent) {
                        SuqiTextField(
                            value = fullNameInput,
                            onValueChange = { fullNameInput = it },
                            label = "Your Name (for new accounts)",
                            placeholder = "e.g. Dawit Bekele",
                            leadingIcon = LucideIcons.User,
                            testTag = "auth_name_input"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.primaryTint)
                                    .border(1.dp, SuqiPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp)
                            ) {
                                Text(
                                    text = "+251",
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = SuqiPrimary
                                )
                            }
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = {
                                    phoneInput = it.filter { ch -> ch.isDigit() || ch == ' ' }.take(12)
                                    errorMsg = null
                                },
                                placeholder = { Text("912 345 678", color = colors.mutedText) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = colors.secondaryBackground,
                                    unfocusedContainerColor = colors.secondaryBackground,
                                    focusedBorderColor = SuqiPrimary,
                                    unfocusedBorderColor = colors.border,
                                    focusedTextColor = colors.primaryText,
                                    unfocusedTextColor = colors.primaryText
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("auth_phone_input")
                            )
                        }
                        if (!errorMsg.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMsg!!,
                                fontFamily = SuqiFontFamily,
                                fontSize = 12.sp,
                                color = SuqiError
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        SuqiButton(
                            text = "Send Verification Code",
                            onClick = {
                                val digits = phoneInput.filter { it.isDigit() }
                                if (digits.length < 9) {
                                    errorMsg = "Enter a valid 9-digit Ethiopian mobile number"
                                } else {
                                    otpSent = true
                                    resendSeconds = 30
                                    errorMsg = null
                                    viewModel.showToast("SMS code sent (demo: 123456)")
                                }
                            },
                            isPill = true,
                            icon = LucideIcons.Phone,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "auth_send_otp"
                        )
                    } else {
                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = {
                                otpInput = it.filter { ch -> ch.isDigit() }.take(6)
                                errorMsg = null
                            },
                            label = { Text("6-digit code") },
                            placeholder = { Text("123456") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_otp_input")
                        )
                        if (!errorMsg.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMsg!!,
                                fontFamily = SuqiFontFamily,
                                fontSize = 12.sp,
                                color = SuqiError
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { otpSent = false; otpInput = "" }) {
                                Text("Change number", color = colors.secondaryText)
                            }
                            TextButton(
                                enabled = resendSeconds == 0,
                                onClick = {
                                    resendSeconds = 30
                                    viewModel.showToast("Code resent (123456)")
                                }
                            ) {
                                Text(
                                    text = if (resendSeconds > 0) "Resend in ${resendSeconds}s" else "Resend code",
                                    color = if (resendSeconds > 0) colors.mutedText else SuqiPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SuqiButton(
                            text = "Verify & Sign In",
                            onClick = {
                                coroutineScope.launch {
                                    val digits = phoneInput.filter { it.isDigit() }.removePrefix("0")
                                    val res = viewModel.repository.loginWithPhoneOtp("+251 $digits", otpInput, fullNameInput)
                                    if (res.success) {
                                        viewModel.refreshSession()
                                        viewModel.showToast("Welcome ${res.data?.name ?: "to Suqi"}!")
                                        viewModel.popToRoot(ScreenDestination.Home)
                                    } else {
                                        errorMsg = res.message ?: "Verification failed"
                                    }
                                }
                            },
                            isPill = true,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "auth_verify_otp"
                        )
                    }
                }
            }

            // Other providers + Guest
            item {
                SuqiSectionCard(padding = 16.dp) {
                    Text(
                        text = "Quick Sign-In Options",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SuqiButton(
                        text = "Continue with Telegram",
                        onClick = {
                            socialName = ""
                            socialHandle = ""
                            showSocialSheet = "TELEGRAM"
                        },
                        isPill = true,
                        icon = LucideIcons.Send,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "auth_telegram"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SuqiButton(
                        text = "Continue with Google",
                        onClick = {
                            socialName = ""
                            socialHandle = ""
                            showSocialSheet = "GOOGLE"
                        },
                        isPrimary = false,
                        isPill = true,
                        icon = LucideIcons.CircleUser,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "auth_google"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.repository.loginAsGuest()
                                viewModel.refreshSession()
                                viewModel.showToast("Browsing as Guest")
                                viewModel.popToRoot(ScreenDestination.Home)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("auth_guest")
                    ) {
                        Text(
                            text = "Browse as Guest",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = colors.secondaryText
                        )
                    }
                }
            }

            item {
                Text(
                    text = "By continuing, you agree to Suqi's Terms of Service & Privacy Policy.",
                    fontFamily = SuqiFontFamily,
                    fontSize = 12.sp,
                    color = colors.mutedText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    val provider = showSocialSheet
    if (provider != null) {
        val isTg = provider == "TELEGRAM"
        SuqiBottomSheet(
            onDismissRequest = { showSocialSheet = null },
            title = if (isTg) "Connect Telegram Account" else "Connect Google Account"
        ) {
            SuqiTextField(
                value = socialName,
                onValueChange = { socialName = it },
                label = "Display Name",
                placeholder = "e.g. Meron Tadesse"
            )
            Spacer(modifier = Modifier.height(12.dp))
            SuqiTextField(
                value = socialHandle,
                onValueChange = { socialHandle = it },
                label = if (isTg) "Phone Number (+251)" else "Email Address",
                placeholder = if (isTg) "+251 911 234 567" else "name@gmail.com"
            )
            Spacer(modifier = Modifier.height(16.dp))
            SuqiButton(
                text = if (isTg) "Authorize & Sign In" else "Sign In with Google",
                onClick = {
                    coroutineScope.launch {
                        val finalName = socialName.trim().ifBlank { if (isTg) "Telegram User" else "Google Shopper" }
                        val res = if (isTg) {
                            viewModel.repository.loginWithTelegram(finalName, socialHandle.trim().ifBlank { "+251 911 234 567" })
                        } else {
                            viewModel.repository.loginWithGoogle(finalName, socialHandle.trim().ifBlank { "shopper@gmail.com" })
                        }
                        showSocialSheet = null
                        if (res.success) {
                            viewModel.refreshSession()
                            viewModel.showToast("Signed in as $finalName!")
                            viewModel.popToRoot(ScreenDestination.Home)
                        }
                    }
                },
                isPill = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun OnboardingScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val coroutineScope = rememberCoroutineScope()
    var selectedLanguage by remember { mutableStateOf(viewModel.selectedLanguage.value) }
    var selectedRegion by remember { mutableStateOf(viewModel.selectedRegion.value) }
    var selectedCity by remember { mutableStateOf(viewModel.selectedCity.value) }
    var selectedSubCity by remember { mutableStateOf(viewModel.selectedSubCity.value) }

    val languages = listOf("English", "Amharic", "Afaan Oromo")
    val regions = listOf("Addis Ababa", "Oromia", "Amhara", "Dire Dawa", "Sidama")
    val subCities = listOf("Bole", "Kirkos", "Yeka", "Arada", "Lideta", "Nifas Silk")

    Scaffold(
        containerColor = colors.surfaceBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Skip",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = colors.secondaryText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { viewModel.popToRoot(ScreenDestination.Home) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                    Text(
                        text = selectedLanguage,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SuqiPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Welcome to Suqi!",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose your preferred language and delivery sub-city for accurate local shipping estimates.",
                    fontFamily = SuqiFontFamily,
                    fontSize = 14.sp,
                    color = colors.secondaryText
                )
            }

            item {
                SuqiSectionCard(padding = 16.dp) {
                    Text(
                        text = "Language",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        languages.forEach { lang ->
                            val isSel = lang == selectedLanguage
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (isSel) SuqiPrimary else colors.secondaryBackground)
                                    .clickable {
                                        selectedLanguage = lang
                                        viewModel.selectedLanguage.value = lang
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lang,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = if (isSel) Color.White else colors.primaryText
                                )
                            }
                        }
                    }
                }
            }

            item {
                SuqiSectionCard(padding = 16.dp) {
                    Text(
                        text = "Region",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(regions) { reg ->
                            val isSel = reg == selectedRegion
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (isSel) SuqiPrimary else colors.secondaryBackground)
                                    .clickable {
                                        selectedRegion = reg
                                        selectedCity = reg
                                        viewModel.selectedRegion.value = reg
                                        viewModel.selectedCity.value = reg
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = reg,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isSel) Color.White else colors.primaryText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Sub-City",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(subCities) { sc ->
                            val isSel = sc == selectedSubCity
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (isSel) colors.primaryTint else colors.secondaryBackground)
                                    .border(1.dp, if (isSel) SuqiPrimary else colors.border, RoundedCornerShape(999.dp))
                                    .clickable {
                                        selectedSubCity = sc
                                        viewModel.selectedSubCity.value = sc
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = sc,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isSel) SuqiPrimary else colors.primaryText
                                )
                            }
                        }
                    }
                }
            }

            item {
                SuqiButton(
                    text = "Save & Explore Suqi",
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.updateUserProfile(
                                mapOf(
                                    "preferredLanguage" to selectedLanguage,
                                    "region" to selectedRegion,
                                    "city" to selectedCity,
                                    "subCity" to selectedSubCity
                                )
                            )
                            viewModel.refreshSession()
                            viewModel.popToRoot(ScreenDestination.Home)
                        }
                    },
                    isPill = true,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "onboarding_continue"
                )
            }
        }
    }
}

@Composable
fun BecomeSellerScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()
    var step by remember { mutableIntStateOf(1) }

    var shopName by remember { mutableStateOf("") }
    var shopType by remember { mutableStateOf("Retail Shop") }
    var category by remember { mutableStateOf("Electronics") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("Bole, Addis Ababa") }
    var city by remember { mutableStateOf("Addis Ababa") }
    var subCity by remember { mutableStateOf("Bole") }
    var phone by remember { mutableStateOf(currentUser?.phone?.ifBlank { "+251 9" } ?: "+251 9") }
    var telegram by remember { mutableStateOf("") }

    SuqiScreen(
        title = "Become a Seller",
        subtitle = "Step $step of 3",
        onBackClick = { if (step > 1) step -= 1 else viewModel.goBack() },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SuqiSectionCard(padding = 16.dp) {
                    Text(
                        text = "Launch your Ethiopian storefront on Suqi in minutes.",
                        fontFamily = SuqiFontFamily,
                        fontSize = 13.sp,
                        color = colors.secondaryText
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GuidedStepItem(stepNum = 1, label = "Shop Info", isActive = step >= 1)
                        Box(modifier = Modifier.weight(1f).height(2.dp).background(if (step >= 2) SuqiPrimary else colors.border))
                        GuidedStepItem(stepNum = 2, label = "Location", isActive = step >= 2)
                        Box(modifier = Modifier.weight(1f).height(2.dp).background(if (step >= 3) SuqiPrimary else colors.border))
                        GuidedStepItem(stepNum = 3, label = "Contact", isActive = step >= 3)
                    }
                }
            }

            if (step == 1) {
                item {
                    SuqiSectionCard(padding = 18.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SuqiTextField(
                                value = shopName,
                                onValueChange = { shopName = it },
                                label = "Shop Name *",
                                placeholder = "e.g. Addis Gadgets Hub"
                            )
                            SuqiTextField(
                                value = shopType,
                                onValueChange = { shopType = it },
                                label = "Shop Type *",
                                placeholder = "Retail Shop, Wholesaler, Boutique"
                            )
                            SuqiTextField(
                                value = category,
                                onValueChange = { category = it },
                                label = "Primary Category *",
                                placeholder = "Electronics, Fashion, Home & Living"
                            )
                            SuqiTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = "Short Bio *",
                                placeholder = "Tell buyers what you specialize in...",
                                singleLine = false
                            )
                            SuqiButton(
                                text = "Continue",
                                onClick = {
                                    if (shopName.isBlank()) {
                                        viewModel.showToast("Please enter a shop name")
                                    } else {
                                        step = 2
                                    }
                                },
                                isPill = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else if (step == 2) {
                item {
                    SuqiSectionCard(padding = 18.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SuqiTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = "Street Address / Building *",
                                placeholder = "e.g. Edna Mall Ground Floor #12"
                            )
                            SuqiTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = "City *"
                            )
                            SuqiTextField(
                                value = subCity,
                                onValueChange = { subCity = it },
                                label = "Sub-City *"
                            )
                            SuqiButton(
                                text = "Continue",
                                onClick = { step = 3 },
                                isPill = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                item {
                    SuqiSectionCard(padding = 18.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SuqiTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = "Business Phone (+251) *"
                            )
                            SuqiTextField(
                                value = telegram,
                                onValueChange = { telegram = it },
                                label = "Telegram Link (Optional)",
                                placeholder = "https://t.me/yourshop"
                            )
                            SuqiButton(
                                text = "Create Shop & Open Studio",
                                onClick = {
                                    coroutineScope.launch {
                                        val payload = mapOf(
                                            "shopName" to shopName.trim(),
                                            "shopType" to shopType.trim(),
                                            "category" to category.trim(),
                                            "description" to description.trim().ifBlank { "Welcome to ${shopName.trim()} on Suqi!" },
                                            "address" to address.trim(),
                                            "city" to city.trim(),
                                            "subCity" to subCity.trim(),
                                            "phone" to phone.trim(),
                                            "telegram" to telegram.trim()
                                        )
                                        val res = viewModel.repository.upgradeToSeller(payload)
                                        if (res.success) {
                                            viewModel.refreshSession()
                                            viewModel.showToast("Shop created! Welcome to Seller Studio.")
                                            viewModel.navigate(ScreenDestination.SellerStudio)
                                        } else {
                                            viewModel.showToast(res.message ?: "Unable to create shop")
                                        }
                                    }
                                },
                                isPill = true,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "btn_create_shop"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GuidedStepItem(
    stepNum: Int,
    label: String,
    isActive: Boolean
) {
    val colors = LocalSuqiColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isActive) SuqiPrimary else colors.secondaryBackground)
                .border(1.dp, if (isActive) SuqiPrimary else colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNum.toString(),
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isActive) Color.White else colors.secondaryText
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontFamily = SuqiFontFamily,
            fontSize = 11.sp,
            color = if (isActive) colors.primaryText else colors.mutedText
        )
    }
}
