package com.example.ui.screens

import com.example.ui.i18n.placeLabel
import androidx.activity.compose.BackHandler
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
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val coroutineScope = rememberCoroutineScope()

    var phoneInput by remember { mutableStateOf("") }
    var fullNameInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var resendSeconds by remember { mutableIntStateOf(0) }
    var failedAttempts by remember { mutableIntStateOf(0) }
    var lockoutSeconds by remember { mutableIntStateOf(0) }
    var otpHasError by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { androidx.compose.animation.core.Animatable(0f) }

    var showSocialSheet by remember { mutableStateOf<String?>(null) } // "TELEGRAM" or "GOOGLE"
    var socialName by remember { mutableStateOf("") }
    var socialHandle by remember { mutableStateOf("") }

    LaunchedEffect(otpSent, resendSeconds) {
        if (otpSent && resendSeconds > 0) {
            delay(1000)
            resendSeconds -= 1
        }
    }

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000)
            lockoutSeconds -= 1
            if (lockoutSeconds == 0) {
                failedAttempts = 0
                errorMsg = null
            }
        }
    }

    BackHandler(enabled = otpSent) {
        otpSent = false
        otpInput = ""
        otpHasError = false
        errorMsg = null
    }

    Scaffold(
        topBar = {
            SuqiHeader(
                title = strings.welcomeToSuqi,
                onBackClick = {
                    if (otpSent) {
                        otpSent = false
                        otpInput = ""
                        otpHasError = false
                        errorMsg = null
                    } else if (!viewModel.goBack()) {
                        viewModel.popToRoot(ScreenDestination.Home)
                    }
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
                        contentDescription = strings.welcomeToSuqi,
                        modifier = Modifier
                            .size(72.dp)
                            .suqiWarmShadow(RoundedCornerShape(20.dp), elevation = 10.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = strings.everyShopEveryProduct,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.checkout.signInPhoneSub,
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
                        text = if (!otpSent) strings.signIn else strings.buyer.enterOtpPrompt,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val demoHint = if (com.example.data.api.BackendConfig.DemoMode) " (${strings.checkout.demoOtpHint})" else ""
                    Text(
                        text = if (!otpSent) strings.buyer.enterPhonePrompt
                        else "+251 ${phoneInput.removePrefix("0").removePrefix("+251").trim()}$demoHint",
                        fontFamily = SuqiFontFamily,
                        fontSize = 12.sp,
                        color = colors.secondaryText
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (!otpSent) {
                        SuqiTextField(
                            value = fullNameInput,
                            onValueChange = { fullNameInput = it },
                            label = strings.buyer.fullNameLabel,
                            placeholder = strings.extras.placeholderFullNameDawit,
                            leadingIcon = LucideIcons.User,
                            testTag = "auth_name_input"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val phoneError = !errorMsg.isNullOrBlank()
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
                                    .background(if (phoneError) colors.errorBg else colors.primaryTint)
                                    .border(
                                        1.dp,
                                        if (phoneError) SuqiError else SuqiPrimary.copy(alpha = 0.4f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 14.dp)
                            ) {
                                Text(
                                    text = "+251",
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (phoneError) SuqiError else SuqiPrimary
                                )
                            }
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = {
                                    phoneInput = it.filter { ch -> ch.isDigit() || ch == ' ' }.take(12)
                                    errorMsg = null
                                },
                                isError = phoneError,
                                placeholder = { Text("912 345 678", color = colors.mutedText) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = colors.secondaryBackground,
                                    unfocusedContainerColor = colors.secondaryBackground,
                                    focusedBorderColor = if (phoneError) SuqiError else SuqiPrimary,
                                    unfocusedBorderColor = if (phoneError) SuqiError else colors.border,
                                    errorBorderColor = SuqiError,
                                    focusedTextColor = colors.primaryText,
                                    unfocusedTextColor = colors.primaryText
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("auth_phone_input")
                            )
                        }
                        if (phoneError) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMsg!!,
                                fontFamily = SuqiFontFamily,
                                fontSize = 12.sp,
                                color = SuqiError,
                                modifier = Modifier.testTag("auth_phone_error")
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        SuqiButton(
                            text = strings.buyer.sendOtpLabel,
                            onClick = {
                                val rawDigits = phoneInput.filter { it.isDigit() }
                                val normalizedDigits = rawDigits.removePrefix("251").removePrefix("0")
                                val isValid = com.example.util.EthiopianMarketplaceUtils.isValidEthiopianPhone("+251$normalizedDigits")
                                if (!isValid) {
                                    errorMsg = strings.buyer.enterPhonePrompt
                                } else {
                                    otpSent = true
                                    otpInput = ""
                                    otpHasError = false
                                    failedAttempts = 0
                                    resendSeconds = 42
                                    errorMsg = null
                                    if (com.example.data.api.BackendConfig.DemoMode) {
                                        viewModel.showToast(strings.checkout.demoOtpHint)
                                    } else {
                                        viewModel.showToast(strings.buyer.sendOtpLabel)
                                    }
                                }
                            },
                            isPill = true,
                            icon = LucideIcons.Phone,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "auth_send_otp"
                        )
                    } else {
                        // 6 separate 52dp OTP digit boxes with paste support and wrong-code shake animation
                        androidx.compose.foundation.text.BasicTextField(
                            value = otpInput,
                            onValueChange = { incoming ->
                                if (lockoutSeconds == 0) {
                                    otpInput = incoming.filter { ch -> ch.isDigit() }.take(6)
                                    otpHasError = false
                                    errorMsg = null
                                }
                            },
                            enabled = lockoutSeconds == 0,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { androidx.compose.ui.unit.IntOffset(shakeOffset.value.toInt(), 0) }
                                .testTag("auth_otp_input"),
                            decorationBox = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    for (idx in 0 until 6) {
                                        val ch = otpInput.getOrNull(idx)?.toString() ?: ""
                                        val isActive = lockoutSeconds == 0 && (otpInput.length == idx || (idx == 5 && otpInput.length == 6))
                                        val borderColor = when {
                                            otpHasError || lockoutSeconds > 0 -> SuqiError
                                            isActive -> SuqiPrimary
                                            ch.isNotEmpty() -> SuqiPrimary.copy(alpha = 0.55f)
                                            else -> colors.border
                                        }
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(width = 46.dp, height = 52.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    when {
                                                        otpHasError -> colors.errorBg
                                                        isActive -> colors.primaryTint
                                                        else -> colors.secondaryBackground
                                                    }
                                                )
                                                .border(
                                                    width = if (isActive || otpHasError) 2.dp else 1.dp,
                                                    color = borderColor,
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .testTag("auth_otp_box_$idx")
                                        ) {
                                            Text(
                                                text = ch,
                                                fontFamily = SuqiFontFamily,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 20.sp,
                                                color = if (otpHasError) SuqiError else colors.primaryText
                                            )
                                        }
                                    }
                                }
                            }
                        )

                        if (lockoutSeconds > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            SuqiSectionCard(containerColor = colors.warningBg, padding = 12.dp) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.testTag("auth_lockout_banner")
                                ) {
                                    Icon(LucideIcons.TriangleAlert, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(20.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = strings.extras.tooManyAttempts,
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = colors.primaryText
                                        )
                                        Text(
                                            text = String.format(strings.extras.tryAgainInTemplate, "0:${lockoutSeconds.toString().padStart(2, '0')}"),
                                            fontFamily = SuqiFontFamily,
                                            fontSize = 12.sp,
                                            color = colors.secondaryText
                                        )
                                    }
                                }
                            }
                        } else if (!errorMsg.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMsg!!,
                                fontFamily = SuqiFontFamily,
                                fontSize = 12.sp,
                                color = SuqiError,
                                modifier = Modifier.testTag("auth_otp_error")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = {
                                    otpSent = false
                                    otpInput = ""
                                    otpHasError = false
                                    failedAttempts = 0
                                    lockoutSeconds = 0
                                    errorMsg = null
                                },
                                modifier = Modifier.testTag("auth_change_number")
                            ) {
                                Text(strings.buyer.changeNumberLabel, color = colors.secondaryText)
                            }
                            TextButton(
                                enabled = resendSeconds == 0 && lockoutSeconds == 0,
                                onClick = {
                                    resendSeconds = 42
                                    if (com.example.data.api.BackendConfig.DemoMode) {
                                        viewModel.showToast(strings.checkout.demoOtpHint)
                                    } else {
                                        viewModel.showToast(strings.buyer.resendOtpLabel)
                                    }
                                },
                                modifier = Modifier.testTag("auth_resend_otp")
                            ) {
                                Text(
                                    text = if (resendSeconds > 0) "0:${resendSeconds.toString().padStart(2, '0')}" else strings.buyer.resendOtpLabel,
                                    color = if (resendSeconds > 0 || lockoutSeconds > 0) colors.mutedText else SuqiPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SuqiButton(
                            text = strings.checkout.verifySignInBtn,
                            enabled = lockoutSeconds == 0 && otpInput.length == 6,
                            onClick = {
                                coroutineScope.launch {
                                    val digits = phoneInput.filter { it.isDigit() }.removePrefix("251").removePrefix("0")
                                    val res = viewModel.repository.loginWithPhoneOtp("+251 $digits", otpInput, fullNameInput)
                                    if (res.success) {
                                        failedAttempts = 0
                                        otpHasError = false
                                        viewModel.refreshSession()
                                        viewModel.showToast(String.format(strings.extras.welcomeToastTemplate, res.data?.name ?: "Suqi"))
                                        viewModel.completeAuth()
                                    } else {
                                        otpHasError = true
                                        failedAttempts += 1
                                        if (failedAttempts >= 5) {
                                            lockoutSeconds = 30
                                            errorMsg = strings.extras.authCodeMismatchLocked
                                        } else {
                                            errorMsg = String.format(strings.extras.authCodeMismatchTriesTemplate, 5 - failedAttempts)
                                        }
                                        // Horizontal shake animation
                                        listOf(-18f, 18f, -12f, 12f, -6f, 0f).forEach { target ->
                                            shakeOffset.animateTo(
                                                targetValue = target,
                                                animationSpec = androidx.compose.animation.core.tween(durationMillis = 42)
                                            )
                                        }
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
                        text = strings.checkout.orContinueWith,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SuqiButton(
                        text = strings.buyer.continueTelegram,
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
                        text = strings.buyer.continueGoogle,
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
                                viewModel.showToast(strings.checkout.guestSessionLabel)
                                viewModel.completeAuth()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("auth_guest")
                    ) {
                        Text(
                            text = strings.common.continueAsGuest,
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
                    text = "${strings.buyer.termsTitle} · ${strings.buyer.privacyPolicyTitle}",
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
            title = if (isTg) strings.buyer.continueTelegram else strings.buyer.continueGoogle
        ) {
            SuqiTextField(
                value = socialName,
                onValueChange = { socialName = it },
                label = strings.buyer.fullNameLabel,
                placeholder = strings.extras.placeholderFullNameMeron
            )
            Spacer(modifier = Modifier.height(12.dp))
            SuqiTextField(
                value = socialHandle,
                onValueChange = { socialHandle = it },
                label = if (isTg) strings.buyer.phoneNumberLabel else strings.buyer.emailOptionalLabel,
                placeholder = if (isTg) "+251 911 234 567" else "name@gmail.com"
            )
            Spacer(modifier = Modifier.height(16.dp))
            SuqiButton(
                text = if (isTg) strings.buyer.continueTelegram else strings.buyer.continueGoogle,
                onClick = {
                    coroutineScope.launch {
                        val finalName = socialName.trim().ifBlank { if (isTg) strings.extras.telegramUserName else strings.extras.googleShopperName }
                        val res = if (isTg) {
                            viewModel.repository.loginWithTelegram(finalName, socialHandle.trim().ifBlank { "+251 911 234 567" })
                        } else {
                            viewModel.repository.loginWithGoogle(finalName, socialHandle.trim().ifBlank { "shopper@gmail.com" })
                        }
                        showSocialSheet = null
                        if (res.success) {
                            viewModel.refreshSession()
                            viewModel.showToast("${strings.welcomeToSuqi} $finalName")
                            viewModel.completeAuth()
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
    val strings = com.example.ui.i18n.suqiStrings()
    val coroutineScope = rememberCoroutineScope()
    var selectedLanguage by remember { mutableStateOf(viewModel.selectedLanguage.value) }
    var selectedRegion by remember { mutableStateOf(viewModel.selectedRegion.value) }
    var selectedCity by remember { mutableStateOf(viewModel.selectedCity.value) }
    var selectedSubCity by remember { mutableStateOf(viewModel.selectedSubCity.value) }

    val languages = listOf(
        "English" to "English",
        "Amharic" to "አማርኛ",
        "Afaan Oromo" to "Afaan Oromoo"
    )
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
                        text = strings.skipLabel,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = colors.secondaryText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { if (!viewModel.goBack()) viewModel.popToRoot(ScreenDestination.Home) }
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
                    text = strings.welcomeToSuqi,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.buyer.locationSheetSubtitle,
                    fontFamily = SuqiFontFamily,
                    fontSize = 14.sp,
                    color = colors.secondaryText
                )
            }

            item {
                SuqiSectionCard(padding = 16.dp) {
                    Text(
                        text = strings.languageTitle,
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
                        languages.forEach { (key, label) ->
                            val isSel = key == selectedLanguage
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (isSel) SuqiPrimary else colors.secondaryBackground)
                                    .clickable {
                                        selectedLanguage = key
                                        viewModel.setSelectedLanguage(key)
                                    }
                                    .padding(vertical = 10.dp, horizontal = 6.dp)
                                    .testTag("onboarding_lang_${key.lowercase().replace(" ", "_")}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = if (isSel) Color.White else colors.primaryText,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            item {
                SuqiSectionCard(padding = 16.dp) {
                    Text(
                        text = strings.buyer.regionLabel,
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
                                    text = placeLabel(reg),
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
                        text = strings.checkout.businessSubCityLabel.removeSuffix(" *").trim(),
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
                                    text = placeLabel(sc),
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
                    text = strings.buyer.getStartedLabel,
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
                            if (!viewModel.goBack()) viewModel.popToRoot(ScreenDestination.Home)
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
    val strings = suqiStrings()
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

    BackHandler(enabled = step > 1) {
        step -= 1
    }

    SuqiScreen(
        title = strings.becomeSellerLabel,
        subtitle = "$step / 3",
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
                        text = strings.checkout.sellOnSuqiSub,
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
                        GuidedStepItem(stepNum = 1, label = strings.checkout.becomeSellerStepShopInfo, isActive = step >= 1)
                        Box(modifier = Modifier.weight(1f).height(2.dp).background(if (step >= 2) SuqiPrimary else colors.border))
                        GuidedStepItem(stepNum = 2, label = strings.checkout.becomeSellerStepLocation, isActive = step >= 2)
                        Box(modifier = Modifier.weight(1f).height(2.dp).background(if (step >= 3) SuqiPrimary else colors.border))
                        GuidedStepItem(stepNum = 3, label = strings.checkout.becomeSellerStepContact, isActive = step >= 3)
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
                                label = strings.checkout.shopNameRequiredLabel,
                                placeholder = strings.extras.placeholderShopName
                            )
                            SuqiTextField(
                                value = shopType,
                                onValueChange = { shopType = it },
                                label = strings.checkout.shopTypeLabel,
                                placeholder = strings.extras.placeholderShopType
                            )
                            SuqiTextField(
                                value = category,
                                onValueChange = { category = it },
                                label = strings.buyer.categoryLabel,
                                placeholder = strings.extras.placeholderCategories
                            )
                            SuqiTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = strings.checkout.shopDescriptionLabel,
                                placeholder = "...",
                                singleLine = false
                            )
                            SuqiButton(
                                text = strings.continueLabel,
                                onClick = {
                                    if (shopName.isBlank()) {
                                        viewModel.showToast(strings.checkout.shopNameRequiredLabel)
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
                                label = strings.checkout.businessAddressLabel,
                                placeholder = strings.extras.placeholderLandmark
                            )
                            SuqiTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = strings.checkout.businessCityLabel
                            )
                            SuqiTextField(
                                value = subCity,
                                onValueChange = { subCity = it },
                                label = strings.checkout.businessSubCityLabel
                            )
                            SuqiButton(
                                text = strings.continueLabel,
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
                                label = strings.checkout.businessPhoneRequiredLabel
                            )
                            SuqiTextField(
                                value = telegram,
                                onValueChange = { telegram = it },
                                label = strings.checkout.telegramOptionalLabel,
                                placeholder = "https://t.me/yourshop"
                            )
                            SuqiButton(
                                text = strings.checkout.createShopOpenStudioBtn,
                                onClick = {
                                    coroutineScope.launch {
                                        val payload = mapOf(
                                            "shopName" to shopName.trim(),
                                            "shopType" to shopType.trim(),
                                            "category" to category.trim(),
                                            "description" to description.trim().ifBlank { String.format(strings.extras.defaultShopDescriptionTemplate, shopName.trim()) },
                                            "address" to address.trim(),
                                            "city" to city.trim(),
                                            "subCity" to subCity.trim(),
                                            "phone" to phone.trim(),
                                            "telegram" to telegram.trim()
                                        )
                                        val res = viewModel.repository.upgradeToSeller(payload)
                                        if (res.success) {
                                            viewModel.refreshSession()
                                            viewModel.showToast(strings.seller.studioTitle)
                                            viewModel.navigate(ScreenDestination.SellerStudio)
                                        } else {
                                            viewModel.showToast(res.message ?: strings.common.failedToAddToast)
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
