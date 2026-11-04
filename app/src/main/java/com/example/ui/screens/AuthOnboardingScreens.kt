package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Suqi Logo Bag Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SuqiPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = LucideIcons.ShoppingBag,
                        contentDescription = "Suqi Logo",
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Suqi",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = SuqiPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Every Shop. Every Product.\nOne Marketplace.",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = SuqiSecondaryText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Continue with Telegram (Primary orange button)
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.loginWithTelegram("Telegram User", "+251 912 345 678")
                            viewModel.showToast("Signed in with Telegram!")
                            viewModel.popToRoot(ScreenDestination.Home)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("auth_telegram")
                ) {
                    Icon(LucideIcons.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Continue with Telegram",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Continue with Google (Clean white bordered button)
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.loginWithGoogle("Fitsum Gebre", "fitsum.gebre@gmail.com")
                            viewModel.showToast("Signed in with Google!")
                            viewModel.popToRoot(ScreenDestination.Home)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SuqiBorder),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("auth_google")
                ) {
                    Icon(LucideIcons.CircleUser, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Continue with Google",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = SuqiPrimaryText
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Continue as Guest
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.loginAsGuest()
                            viewModel.popToRoot(ScreenDestination.Home)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiSecondaryBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("auth_guest")
                ) {
                    Text(
                        text = "Continue as Guest",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = SuqiPrimaryText
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "By continuing, you agree to our\nTerms of Service and Privacy Policy",
                    fontFamily = SuqiFontFamily,
                    fontSize = 12.sp,
                    color = SuqiMutedText,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    var selectedLanguage by remember { mutableStateOf(viewModel.selectedLanguage.value) }
    var selectedRegion by remember { mutableStateOf(viewModel.selectedRegion.value) }
    var selectedCity by remember { mutableStateOf(viewModel.selectedCity.value) }
    var selectedSubCity by remember { mutableStateOf(viewModel.selectedSubCity.value) }

    val languages = listOf("English", "Amharic", "Afaan Oromo")
    val regions = listOf("Addis Ababa", "Oromia", "Amhara", "Dire Dawa", "Sidama")
    val subCities = listOf("Bole", "Kirkos", "Yeka", "Arada", "Lideta")

    Scaffold(
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Skip",
                        fontFamily = SuqiFontFamily,
                        fontSize = 14.sp,
                        color = SuqiSecondaryText,
                        modifier = Modifier.clickable { viewModel.popToRoot(ScreenDestination.Home) }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SuqiPrimary))
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SuqiBorder))
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SuqiBorder))
                    }

                    Text(
                        text = selectedLanguage,
                        fontFamily = SuqiFontFamily,
                        fontSize = 14.sp,
                        color = SuqiPrimary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Welcome to Suqi!",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = SuqiPrimaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Let's set up your location to show\nyou the best deals near you.",
                    fontFamily = SuqiFontFamily,
                    fontSize = 14.sp,
                    color = SuqiSecondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Choose Language
                Text(
                    text = "Choose Language",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
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
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isSel) SuqiPrimary else SuqiSecondaryBackground)
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
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = if (isSel) Color.White else SuqiPrimaryText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Select Region
                Text(
                    text = "Select Your Region",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(regions) { reg ->
                        val isSel = reg == selectedRegion
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSel) SuqiPrimaryLight else SuqiSecondaryBackground)
                                .border(1.dp, if (isSel) SuqiPrimary else SuqiBorder, RoundedCornerShape(14.dp))
                                .clickable {
                                    selectedRegion = reg
                                    viewModel.selectedRegion.value = reg
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = reg,
                                fontFamily = SuqiFontFamily,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = if (isSel) SuqiPrimary else SuqiPrimaryText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Select Sub-City
                Text(
                    text = "Select Sub-City",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(subCities) { sc ->
                        val isSel = sc == selectedSubCity
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSel) SuqiPrimaryLight else SuqiSecondaryBackground)
                                .border(1.dp, if (isSel) SuqiPrimary else SuqiBorder, RoundedCornerShape(14.dp))
                                .clickable {
                                    selectedSubCity = sc
                                    viewModel.selectedSubCity.value = sc
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = sc,
                                fontFamily = SuqiFontFamily,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = if (isSel) SuqiPrimary else SuqiPrimaryText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                SuqiButton(
                    text = "Continue",
                    onClick = { viewModel.popToRoot(ScreenDestination.Home) },
                    modifier = Modifier.fillMaxWidth()
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
    val coroutineScope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(1) } // 1: Shop Info, 2: Details, 3: Contact

    var shopName by remember { mutableStateOf("Tech World") }
    var shopType by remember { mutableStateOf("Retail Shop") }
    var category by remember { mutableStateOf("Electronics") }
    var description by remember { mutableStateOf("Your trusted electronics store in Ethiopia...") }
    var address by remember { mutableStateOf("Bole, Addis Ababa") }
    var phone by remember { mutableStateOf("+251 912 345 678") }
    var telegram by remember { mutableStateOf("https://t.me/techworld") }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (step > 1) step -= 1 else viewModel.goBack()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SuqiSecondaryBackground)
                ) {
                    Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Become a Seller",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = SuqiPrimaryText
                )
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp)
        ) {
            item {
                Text(
                    text = "Join thousands of successful sellers\non Suqi Marketplace.",
                    fontFamily = SuqiFontFamily,
                    fontSize = 14.sp,
                    color = SuqiSecondaryText
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 3-Step Guided Progress Indicator (Mockup 3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GuidedStepItem(stepNum = 1, label = "Shop Info", isActive = step >= 1)
                    Box(modifier = Modifier.weight(1f).height(2.dp).background(if (step >= 2) SuqiPrimary else SuqiBorder))
                    GuidedStepItem(stepNum = 2, label = "Details", isActive = step >= 2)
                    Box(modifier = Modifier.weight(1f).height(2.dp).background(if (step >= 3) SuqiPrimary else SuqiBorder))
                    GuidedStepItem(stepNum = 3, label = "Contact", isActive = step >= 3)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            if (step == 1) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SuqiTextField(
                            value = shopName,
                            onValueChange = { shopName = it },
                            label = "Shop Name *"
                        )
                        SuqiTextField(
                            value = shopType,
                            onValueChange = { shopType = it },
                            label = "Shop Type *"
                        )
                        SuqiTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = "Business Category *"
                        )
                        SuqiTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = "Short Description *"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SuqiButton(
                            text = "Continue",
                            onClick = { step = 2 },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else if (step == 2) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SuqiTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = "Street Address / Landmark *"
                        )
                        SuqiTextField(
                            value = "Addis Ababa",
                            onValueChange = {},
                            label = "City *"
                        )
                        SuqiTextField(
                            value = "Bole",
                            onValueChange = {},
                            label = "Sub-City *"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SuqiButton(
                            text = "Continue",
                            onClick = { step = 3 },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SuqiTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = "Contact Phone (+251) *"
                        )
                        SuqiTextField(
                            value = telegram,
                            onValueChange = { telegram = it },
                            label = "Telegram Link (Optional)"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SuqiButton(
                            text = "Create Shop & Open Studio",
                            onClick = {
                                coroutineScope.launch {
                                    val payload = mapOf(
                                        "shopName" to shopName,
                                        "shopType" to shopType,
                                        "category" to category,
                                        "description" to description,
                                        "address" to address,
                                        "phone" to phone,
                                        "telegram" to telegram
                                    )
                                    val res = viewModel.repository.upgradeToSeller(payload)
                                    if (res.success) {
                                        viewModel.showToast("Shop created! Welcome to Seller Studio.")
                                        viewModel.navigate(ScreenDestination.SellerStudio)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "btn_create_shop"
                        )
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isActive) SuqiPrimary else SuqiSecondaryBackground)
                .border(1.dp, if (isActive) SuqiPrimary else SuqiBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNum.toString(),
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isActive) Color.White else SuqiSecondaryText
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontFamily = SuqiFontFamily,
            fontSize = 11.sp,
            color = if (isActive) SuqiPrimaryText else SuqiMutedText
        )
    }
}
