package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.LocalSuqiLanguage
import com.example.ui.i18n.SuqiLanguage
import com.example.ui.i18n.placeLabel
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Total bytes of temporary files the app can safely drop (image cache, shared exports). */
fun cacheSizeBytes(context: android.content.Context): Long {
    fun size(f: File): Long = if (f.isDirectory) (f.listFiles()?.sumOf { size(it) } ?: 0L) else f.length()
    return size(context.cacheDir)
}

/** Deletes everything under cacheDir and the in-memory image cache. Never touches filesDir (user uploads). */
fun clearAppCache(context: android.content.Context) {
    context.cacheDir.listFiles()?.forEach { runCatching { it.deleteRecursively() } }
    runCatching { coil.Coil.imageLoader(context).memoryCache?.clear() }
}

fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
    else -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val w = strings.buyerWork
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isDarkPref by viewModel.isDarkMode.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val isDevUnlocked by viewModel.isDeveloperUnlocked.collectAsState()

    val curRegion by viewModel.selectedRegion.collectAsState()
    val curCity by viewModel.selectedCity.collectAsState()
    val curSubCity by viewModel.selectedSubCity.collectAsState()
    val curLang by viewModel.selectedLanguage.collectAsState()

    val store = viewModel.buyerPrefs
    var dataSaver by remember { mutableStateOf(store.loadDataSaver()) }
    var privacy by remember { mutableStateOf(store.loadPrivacyPrefs()) }
    var cacheBytes by remember { mutableStateOf(0L) }
    var showLocationSheet by remember { mutableStateOf(false) }
    var versionTapCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        cacheBytes = withContext(Dispatchers.IO) { cacheSizeBytes(context) }
    }

    SuqiScreen(
        title = strings.settingsTitle,
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Language + location
            item {
                SuqiSectionCard(padding = 16.dp) {
                    Text("${strings.languageTitle} · ${strings.buyer.regionLabel}", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(10.dp))
                    val langs = listOf("English" to "English", "Amharic" to "አማርኛ (Amharic)", "Afaan Oromo" to "Afaan Oromoo")
                    langs.forEach { (key, label) ->
                        val isSel = SuqiLanguage.fromSetting(curLang) == SuqiLanguage.fromSetting(key)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setSelectedLanguage(key)
                                    viewModel.showToast("${strings.languageTitle}: $label")
                                }
                                .defaultMinSize(minHeight = 48.dp)
                                .padding(vertical = 10.dp, horizontal = 6.dp)
                                .testTag("settings_lang_${key.lowercase().replace(" ", "_")}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, fontFamily = SuqiFontFamily, fontSize = 14.sp, color = colors.primaryText)
                            if (isSel) Icon(LucideIcons.Check, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                    HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 10.dp))
                    SuqiListRow(
                        icon = LucideIcons.MapPin,
                        title = "${placeLabel(curCity)}, ${placeLabel(curSubCity)}",
                        subtitle = "${strings.buyer.regionLabel}: ${placeLabel(curRegion)}",
                        badge = strings.common.changeLabel,
                        onClick = { showLocationSheet = true },
                        testTag = "settings_location_row"
                    )
                }
            }

            // Appearance: three preview tiles
            item {
                SuqiSectionCard(padding = 16.dp) {
                    Text(strings.appearanceTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf<Triple<String, Boolean?, ImageVector>>(
                            Triple(strings.systemDefault, null, LucideIcons.SunMoon),
                            Triple(strings.lightMode, false, LucideIcons.Sun),
                            Triple(strings.darkMode, true, LucideIcons.Moon)
                        ).forEach { (label, value, icon) ->
                            ThemeTile(
                                label = label,
                                icon = icon,
                                selected = isDarkPref == value,
                                onClick = {
                                    viewModel.setDarkMode(value)
                                    viewModel.showToast("${strings.appearanceTitle}: $label")
                                },
                                modifier = Modifier.weight(1f).testTag("settings_theme_${value?.toString() ?: "system"}")
                            )
                        }
                    }
                }
            }

            // Notifications link
            item {
                SuqiSectionCard(padding = 10.dp) {
                    SuqiListRow(
                        icon = LucideIcons.Bell,
                        title = w.settingsNotifLinkTitle,
                        subtitle = w.settingsNotifLinkSub,
                        onClick = { viewModel.navigate(ScreenDestination.NotificationPrefs) },
                        testTag = "settings_notif_prefs_row"
                    )
                }
            }

            // Data & storage
            item {
                SuqiSectionCard(padding = 10.dp) {
                    SuqiSwitchRow(
                        title = w.dataSaverTitle,
                        subtitle = w.dataSaverSub,
                        icon = LucideIcons.Video,
                        checked = dataSaver,
                        onCheckedChange = { dataSaver = it; store.saveDataSaver(it) },
                        testTag = "settings_data_saver_switch"
                    )
                    SuqiListRow(
                        icon = LucideIcons.Trash2,
                        title = w.clearCacheTitle,
                        subtitle = String.format(w.clearCacheSizeTemplate, formatBytes(cacheBytes)),
                        showChevron = false,
                        onClick = {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) { clearAppCache(context) }
                                viewModel.clearUserCaches()
                                cacheBytes = withContext(Dispatchers.IO) { cacheSizeBytes(context) }
                                viewModel.showToast(w.clearCacheDoneToast)
                            }
                        },
                        testTag = "settings_clear_cache_row"
                    )
                }
            }

            // Privacy (both toggles are enforced: see SuqiViewModel.recordProductView and SearchScreen)
            item {
                SuqiSectionCard(padding = 10.dp) {
                    Text(w.privacySectionTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText, modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 4.dp))
                    SuqiSwitchRow(
                        title = w.privacyRecentTitle,
                        subtitle = w.privacyRecentSub,
                        icon = LucideIcons.Eye,
                        checked = privacy.rememberRecentlyViewed,
                        onCheckedChange = {
                            privacy = privacy.copy(rememberRecentlyViewed = it)
                            store.savePrivacyPrefs(privacy)
                        },
                        testTag = "settings_privacy_recent_switch"
                    )
                    SuqiSwitchRow(
                        title = w.privacySearchTitle,
                        subtitle = w.privacySearchSub,
                        icon = LucideIcons.Search,
                        checked = privacy.saveSearchHistory,
                        onCheckedChange = {
                            privacy = privacy.copy(saveSearchHistory = it)
                            store.savePrivacyPrefs(privacy)
                        },
                        testTag = "settings_privacy_search_switch"
                    )
                    SuqiListRow(
                        icon = LucideIcons.History,
                        title = w.clearSearchHistoryBtn,
                        showChevron = false,
                        onClick = {
                            store.clearSearchHistory()
                            viewModel.showToast(w.searchHistoryClearedToast)
                        },
                        testTag = "settings_clear_search_row"
                    )
                }
            }

            // Account + staff tools
            item {
                SuqiSectionCard(padding = 16.dp) {
                    Text(strings.checkout.accountStaffToolsTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(8.dp))
                    SuqiListRow(
                        icon = LucideIcons.CircleUser,
                        title = strings.checkout.accountEmailTitle,
                        subtitle = currentUser?.email?.ifBlank { strings.checkout.notLinkedLabel } ?: strings.checkout.guestSessionLabel,
                        showChevron = false,
                        onClick = { viewModel.navigate(ScreenDestination.ProfileEdit) }
                    )
                    SuqiListRow(
                        icon = LucideIcons.Phone,
                        title = strings.buyer.phoneNumberLabel,
                        subtitle = currentUser?.phone?.ifBlank { strings.checkout.notLinkedLabel } ?: strings.checkout.notLinkedLabel,
                        showChevron = false,
                        onClick = { viewModel.navigate(ScreenDestination.ProfileEdit) }
                    )
                    HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 6.dp))
                    SuqiListRow(
                        icon = LucideIcons.ShieldCheck,
                        title = strings.admin.staffSignInTitle,
                        subtitle = strings.checkout.adminStaffSignInSub,
                        onClick = { viewModel.navigate(ScreenDestination.AdminSignIn) },
                        testTag = "settings_admin_signin_row"
                    )
                    // Simulator and diagnostics only after developer mode is unlocked (7 taps on the version)
                    if (isDevUnlocked) {
                        SuqiSwitchRow(
                            title = strings.buyer.offlineSimulatorTitle,
                            subtitle = strings.offlineBannerTitle,
                            icon = LucideIcons.WifiOff,
                            checked = isOffline,
                            onCheckedChange = { viewModel.setOfflineMode(it) },
                            testTag = "settings_offline_switch"
                        )
                        SuqiListRow(
                            icon = LucideIcons.Wrench,
                            title = strings.buyer.devToolsRowTitle,
                            subtitle = strings.adminOps.openDiagnosticsSub,
                            badge = strings.adminOps.activeStatus,
                            onClick = { viewModel.navigate(ScreenDestination.AdminDevTools) },
                            testTag = "settings_dev_tools_row"
                        )
                    }
                    HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 8.dp))
                    SuqiListRow(
                        icon = LucideIcons.Info,
                        title = strings.buyer.aboutSuqiTitle,
                        subtitle = w.aboutScreenSub,
                        onClick = { viewModel.navigate(ScreenDestination.AboutApp) },
                        testTag = "settings_about_row"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                versionTapCount++
                                if (versionTapCount >= 7 && !isDevUnlocked) {
                                    viewModel.unlockDeveloperTools()
                                    viewModel.showToast(strings.admin.devToolsTitle)
                                } else if (!isDevUnlocked && versionTapCount >= 4) {
                                    viewModel.showToast("${7 - versionTapCount}")
                                }
                            }
                            .defaultMinSize(minHeight = 48.dp)
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .testTag("settings_version_pill"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.checkout.appVersionLabel, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(colors.secondaryBackground).padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isDevUnlocked) "v${appVersionName(context)} (Dev)" else "v${appVersionName(context)}",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = if (isDevUnlocked) SuqiPrimary else colors.primaryText
                            )
                        }
                    }
                }
            }

            item {
                SuqiButton(
                    text = strings.signOut,
                    isPrimary = false,
                    icon = LucideIcons.LogOut,
                    onClick = {
                        coroutineScope.launch {
                            viewModel.signOut(ScreenDestination.Auth)
                            viewModel.showToast(strings.signOut)
                        }
                    },
                    testTag = "settings_sign_out_button",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showLocationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLocationSheet = false },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            LocationPickerContent(
                currentRegion = curRegion,
                currentCity = curCity,
                currentSubCity = curSubCity,
                onSelectLocation = { region, city, subCity ->
                    viewModel.selectedRegion.value = region
                    viewModel.selectedCity.value = city
                    viewModel.selectedSubCity.value = subCity
                    coroutineScope.launch {
                        viewModel.repository.updateUserProfile(mapOf("region" to region, "city" to city, "subCity" to subCity))
                    }
                    viewModel.showToast("${viewModel.localizePlace(city)}, ${viewModel.localizePlace(subCity)}")
                    showLocationSheet = false
                }
            )
        }
    }
}

fun appVersionName(context: android.content.Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull().orEmpty().ifBlank { "1.0.0" }

@Composable
private fun ThemeTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) colors.primaryTint else colors.secondaryBackground)
            .border(if (selected) 2.dp else 1.dp, if (selected) SuqiPrimary else colors.border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 48.dp)
            .padding(vertical = 14.dp, horizontal = 6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) SuqiPrimary else colors.secondaryText, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontFamily = SuqiFontFamily,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            color = if (selected) SuqiPrimary else colors.primaryText
        )
    }
}

/** B-18 About screen: version, tagline, open-source licences and legal links. */
@Composable
fun AboutAppScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val w = strings.buyerWork
    val context = LocalContext.current
    val isOffline by viewModel.isOffline.collectAsState()
    val version = remember { appVersionName(context) }

    SuqiScreen(
        title = strings.buyer.aboutSuqiTitle,
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SuqiSectionCard(containerColor = colors.primaryTint, padding = 24.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.size(72.dp).clip(CircleShape).background(SuqiPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(LucideIcons.Store, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(34.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Suqi", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = colors.primaryText)
                        Text(strings.everyShopEveryProduct, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(10.dp))
                        SuqiStatusBadge("${w.aboutVersionLabel} $version", StatusType.PRIMARY, modifier = Modifier.testTag("about_version"))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(String.format(w.aboutTaglineTemplate, "2026"), fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.mutedText)
                    }
                }
            }
            item {
                SuqiSectionCard(padding = 16.dp) {
                    Text(w.aboutLicensesTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(w.aboutLicensesBody, fontFamily = SuqiFontFamily, fontSize = 13.sp, lineHeight = 20.sp, color = colors.secondaryText)
                }
            }
            item {
                SuqiSectionCard(padding = 10.dp) {
                    SuqiListRow(
                        icon = LucideIcons.FileText,
                        title = strings.buyer.termsTitle,
                        onClick = { viewModel.navigate(ScreenDestination.HelpLegal) },
                        testTag = "about_terms_row"
                    )
                    SuqiListRow(
                        icon = LucideIcons.Shield,
                        title = strings.buyer.privacyPolicyTitle,
                        onClick = { viewModel.navigate(ScreenDestination.HelpLegal) },
                        testTag = "about_privacy_row"
                    )
                }
            }
        }
    }
}
