package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContentEntryDto
import com.example.data.model.LocationAreaDto
import com.example.data.model.PaymentProviderDto
import com.example.data.model.PlatformSwitchDto
import com.example.data.model.SuqiLanguageCode
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.StatusType
import com.example.ui.components.SuqiBottomSheet
import com.example.ui.components.SuqiButton
import com.example.ui.components.SuqiEmptyState
import com.example.ui.components.SuqiFilterChips
import com.example.ui.components.SuqiScreen
import com.example.ui.components.SuqiSectionCard
import com.example.ui.components.SuqiStatusBadge
import com.example.ui.components.SuqiSwitchRow
import com.example.ui.components.SuqiTextField
import com.example.ui.components.formatEtb
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.LocalSuqiColors
import com.example.ui.theme.LucideIcons
import com.example.ui.theme.SuqiError
import com.example.ui.theme.SuqiFontFamily
import com.example.ui.theme.SuqiPrimary
import com.example.ui.theme.SuqiSuccess
import com.example.ui.theme.SuqiWarning
import kotlinx.coroutines.launch

// ==========================================
// A-17 (P3): Locations manager
// ==========================================
@Composable
fun AdminLocationsManagerScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p6 = strings.admin6
    val isOffline by viewModel.isOffline.collectAsState()

    var areas by remember { mutableStateOf(emptyList<LocationAreaDto>()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadAreas() {
        coroutineScope.launch {
            val res = viewModel.repository.getLocationAreas()
            if (res.success && res.data != null) {
                areas = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadAreas() }

    SuqiScreen(
        title = strings.admin.locationsTitle,
        subtitle = "${areas.size} areas",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SuqiSectionCard(padding = 16.dp) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuqiButton(
                        text = p6.addRegionBtn,
                        onClick = {},
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                    )
                    SuqiButton(
                        text = p6.addCityBtn,
                        onClick = {},
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                        isPrimary = false
                    )
                    SuqiButton(
                        text = p6.addSubCityBtn,
                        onClick = {},
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                        isPrimary = false
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (isLoading) {
                com.example.ui.components.SuqiSkeletonList(count = 3, modifier = Modifier.fillMaxWidth())
            } else if (areas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    com.example.ui.components.SuqiEmptyState(
                        icon = LucideIcons.MapPin,
                        title = strings.admin.locationsTitle,
                        subtitle = "No regions defined yet",
                        testTag = "locations_empty"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(areas, key = { it.id }) { area ->
                        SuqiSectionCard(
                            padding = 14.dp,
                            containerColor = if (area.isDefaultZone) colors.primaryTint.copy(alpha = 0.08f) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = area.name,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = colors.primaryText
                                    )
                                    Text(
                                        text = "${area.level} ${if (area.parentId.isNotBlank()) "parent: ${area.parentId}" else ""} • sort ${area.sortOrder}",
                                        fontSize = 12.sp,
                                        color = colors.secondaryText
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SuqiSwitchRow(
                                        title = p6.defaultZoneLabel,
                                        checked = area.isDefaultZone,
                                        onCheckedChange = {},
                                        modifier = Modifier.width(150.dp),
                                        testTag = "default_zone_${area.id}"
                                    )
                                    SuqiButton(
                                        text = p6.disableAreaLabel,
                                        onClick = {},
                                        modifier = Modifier.heightIn(min = 36.dp),
                                        isPrimary = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// A-18 (P3): Payment providers
// ==========================================
@Composable
fun AdminPaymentProvidersScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p6 = strings.admin6
    val isOffline by viewModel.isOffline.collectAsState()

    var providers by remember { mutableStateOf(emptyList<PaymentProviderDto>()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadProviders() {
        coroutineScope.launch {
            val res = viewModel.repository.getPaymentProviders()
            if (res.success && res.data != null) {
                providers = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadProviders() }

    SuqiScreen(
        title = strings.admin.paymentProvidersTitle,
        subtitle = "${providers.size} providers",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SuqiSectionCard(padding = 16.dp) {
                SuqiButton(
                    text = p6.addProviderBtn,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (isLoading) {
                com.example.ui.components.SuqiSkeletonList(count = 3, modifier = Modifier.fillMaxWidth())
            } else if (providers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    com.example.ui.components.SuqiEmptyState(
                        icon = LucideIcons.Banknote,
                        title = strings.admin.paymentProvidersTitle,
                        subtitle = "No payment providers configured",
                        testTag = "providers_empty"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(providers, key = { it.id }) { provider ->
                        SuqiSectionCard(
                            padding = 14.dp,
                            containerColor = if (provider.isActive) colors.primaryTint.copy(alpha = 0.08f) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = provider.displayName,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = colors.primaryText
                                    )
                                    Text(
                                        text = provider.logoEmoji + "  •  ${provider.accountFormat}",
                                        fontSize = 12.sp,
                                        color = colors.secondaryText
                                    )
                                    Text(
                                        text = p6.referenceFormatHint.replace("{example}", provider.referenceHint.ifBlank { "FT + 10 digits" }),
                                        fontSize = 11.sp,
                                        color = colors.mutedText
                                    )
                                }
                                SuqiSwitchRow(
                                    title = p6.activeSwitchLabel,
                                    checked = provider.isActive,
                                    onCheckedChange = {},
                                    testTag = "provider_active_${provider.id}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// A-19 (P3): Content manager
// ==========================================
@Composable
fun AdminContentManagerScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p6 = strings.admin6
    val isOffline by viewModel.isOffline.collectAsState()

    var entries by remember { mutableStateOf(emptyList<ContentEntryDto>()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedLang by rememberSaveable { mutableStateOf(SuqiLanguageCode.EN) }

    fun loadEntries() {
        coroutineScope.launch {
            val res = viewModel.repository.getContentEntries()
            if (res.success && res.data != null) {
                entries = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadEntries() }

    val langLabel = when (selectedLang) {
        SuqiLanguageCode.EN -> "English"
        SuqiLanguageCode.AM -> "አማርኛ"
        SuqiLanguageCode.OM -> "Afaan Oromoo"
    }

    SuqiScreen(
        title = strings.admin.contentManagerTitle,
        subtitle = "${entries.size} entries",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(SuqiLanguageCode.EN to "EN", SuqiLanguageCode.AM to "AM", SuqiLanguageCode.OM to "OM").forEach { (lang, label) ->
                    val selected = lang == selectedLang
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) SuqiPrimary else colors.cardBackground)
                            .border(if (!selected) 1.dp else 0.dp, colors.border, RoundedCornerShape(12.dp))
                            .clickable { selectedLang = lang }
                    ) {
                        Text(
                            text = label,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selected) Color.White else colors.primaryText,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (isLoading) {
                com.example.ui.components.SuqiSkeletonList(count = 3, modifier = Modifier.fillMaxWidth())
            } else if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    com.example.ui.components.SuqiEmptyState(
                        icon = LucideIcons.FileText,
                        title = strings.admin.contentManagerTitle,
                        subtitle = "No content entries yet",
                        testTag = "content_empty"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        SuqiSectionCard(
                            padding = 14.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = entry.section,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                        color = SuqiPrimary
                                    )
                                    SuqiStatusBadge(
                                        if (entry.isPublished) "Published" else "Draft",
                                        if (entry.isPublished) StatusType.SUCCESS else StatusType.WARNING
                                    )
                                }
                                Text(
                                    text = when (selectedLang) {
                                        SuqiLanguageCode.AM -> entry.titleAm
                                        SuqiLanguageCode.OM -> entry.titleOm
                                        else -> entry.titleEn
                                    }.ifBlank { entry.titleEn },
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.primaryText
                                )
                                Text(
                                    text = when (selectedLang) {
                                        SuqiLanguageCode.AM -> entry.bodyAm
                                        SuqiLanguageCode.OM -> entry.bodyOm
                                        else -> entry.bodyEn
                                    }.ifBlank { entry.bodyEn },
                                    fontSize = 13.sp,
                                    color = colors.secondaryText,
                                    maxLines = 3
                                )
                                if (entry.isMissingTranslation(selectedLang)) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        SuqiStatusBadge(p6.missingTranslationBadge, StatusType.WARNING)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = langLabel,
                                            fontSize = 11.sp,
                                            color = colors.mutedText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// A-23 (P3): Platform switches 2.0
// ==========================================
@Composable
fun AdminPlatformSwitchesScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p6 = strings.admin6
    val isOffline by viewModel.isOffline.collectAsState()

    var switches by remember { mutableStateOf(emptyList<PlatformSwitchDto>()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadSwitches() {
        coroutineScope.launch {
            val res = viewModel.repository.getPlatformSwitches()
            if (res.success && res.data != null) {
                switches = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadSwitches() }

    val groups = listOf("COMMERCE" to p6.groupCommerce, "MEDIA" to p6.groupMedia, "CATALOG" to p6.groupCatalog, "MONETIZATION" to p6.groupMonetization, "EXPERIENCE" to p6.groupExperience)

    SuqiScreen(
        title = strings.admin.platformSwitchesTitle,
        subtitle = "${switches.size} switches",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            groups.forEach { (groupKey, groupLabel) ->
                val groupItems = switches.filter { it.groupName == groupKey }
                if (groupItems.isNotEmpty()) {
                    SuqiSectionCard(padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = groupLabel,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.primaryText,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        groupItems.forEach { sw ->
                            SuqiSwitchRow(
                                title = when (sw.id) {
                                    "q_and_a" -> p6.switchQandA
                                    "video_uploads" -> p6.switchVideoUploads
                                    "product_variants" -> p6.switchVariants
                                    "flash_sales" -> p6.switchFlashSales
                                    "compare_products" -> p6.switchCompare
                                    else -> sw.id
                                },
                                subtitle = p6.affectsLabel + ": ${sw.groupName}",
                                checked = sw.isEnabled,
                                onCheckedChange = { enabled ->
                                    coroutineScope.launch {
                                        viewModel.repository.savePlatformSwitch(sw.id, enabled, sw.numericValue)
                                    }
                                },
                                testTag = "switch_${sw.id}"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            if (switches.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    com.example.ui.components.SuqiEmptyState(
                        icon = LucideIcons.SlidersHorizontal,
                        title = strings.admin.platformSwitchesTitle,
                        subtitle = "No switches configured",
                        testTag = "switches_empty"
                    )
                }
            }
        }
    }
}
