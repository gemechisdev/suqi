package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContentEntryDto
import com.example.data.model.LocationAreaDto
import com.example.data.model.PaymentProviderDto
import com.example.data.model.PlatformSwitchDto
import com.example.data.model.SuqiLanguageCode
import com.example.ui.SuqiViewModel
import com.example.ui.components.StatusType
import com.example.ui.components.SuqiBottomSheet
import com.example.ui.components.SuqiButton
import com.example.ui.components.SuqiEmptyState
import com.example.ui.components.SuqiFilterChips
import com.example.ui.components.SuqiMaxContentWidth
import com.example.ui.components.SuqiScreen
import com.example.ui.components.SuqiSectionCard
import com.example.ui.components.SuqiSkeletonList
import com.example.ui.components.SuqiStatusBadge
import com.example.ui.components.SuqiSwitchRow
import com.example.ui.components.SuqiTextField
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.LocalSuqiColors
import com.example.ui.theme.LucideIcons
import com.example.ui.theme.SuqiFontFamily
import com.example.ui.theme.SuqiPrimary
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

    var addLevel by remember { mutableStateOf<String?>(null) }
    var addName by remember { mutableStateOf("") }
    var addParentId by remember { mutableStateOf("") }
    fun openAdd(level: String) {
        addLevel = level
        addName = ""
        val parentLevel = when (level) {
            "CITY" -> "REGION"
            "SUB_CITY" -> "CITY"
            else -> ""
        }
        addParentId = areas.firstOrNull { it.level == parentLevel }?.id.orEmpty()
    }

    SuqiScreen(
        title = strings.admin.locationsTitle,
        subtitle = "${areas.size}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = SuqiMaxContentWidth)
                    .fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuqiButton(
                        text = p6.addRegionBtn,
                        onClick = { openAdd("REGION") },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("loc_add_region_btn")
                    )
                    SuqiButton(
                        text = p6.addCityBtn,
                        onClick = { openAdd("CITY") },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("loc_add_city_btn"),
                        isPrimary = false
                    )
                    SuqiButton(
                        text = p6.addSubCityBtn,
                        onClick = { openAdd("SUB_CITY") },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("loc_add_subcity_btn"),
                        isPrimary = false
                    )
                }
                addLevel?.let { level ->
                    SuqiBottomSheet(onDismissRequest = { addLevel = null }) {
                        SuqiTextField(
                            value = addName,
                            onValueChange = { addName = it },
                            label = p6.areaNameHint,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "location_name_input"
                        )
                        if (level != "REGION") {
                            val parentLevel = if (level == "CITY") "REGION" else "CITY"
                            Spacer(modifier = Modifier.height(8.dp))
                            SuqiFilterChips(
                                items = areas.filter { it.level == parentLevel }.map { it.id to it.name },
                                selectedKey = addParentId,
                                onSelect = { addParentId = it },
                                testTagPrefix = "location_parent"
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        SuqiButton(
                            text = strings.saveLabel,
                            enabled = addName.isNotBlank(),
                            onClick = {
                                coroutineScope.launch {
                                    val res = viewModel.repository.saveLocationArea(
                                        LocationAreaDto(
                                            id = "",
                                            name = addName.trim(),
                                            parentId = if (level == "REGION") "" else addParentId,
                                            level = level,
                                            sortOrder = areas.count { it.level == level } + 1
                                        )
                                    )
                                    if (res.success) {
                                        addLevel = null
                                        loadAreas()
                                        viewModel.showToast(p6.areaSavedToast)
                                    } else {
                                        viewModel.showToast(res.message ?: strings.retryLabel)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("location_save_btn")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (isLoading) {
                    SuqiSkeletonList(count = 3, modifier = Modifier.fillMaxWidth().padding(16.dp))
                } else if (areas.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SuqiEmptyState(
                            icon = LucideIcons.MapPin,
                            title = strings.admin.locationsTitle,
                            subtitle = strings.adminGov.regionsHubsSubtitle,
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
                                padding = 10.dp,
                                containerColor = if (area.isDefaultZone) colors.primaryTint.copy(alpha = 0.08f) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = area.name,
                                                fontFamily = SuqiFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = colors.primaryText
                                            )
                                            Text(
                                                text = "${area.level}${if (area.parentId.isNotBlank()) " • ${area.parentId}" else ""} • #${area.sortOrder}",
                                                fontSize = 12.sp,
                                                color = colors.secondaryText
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            SuqiStatusBadge(
                                                if (area.isEnabled) strings.adminOps.activeStatus else p6.disableAreaLabel,
                                                if (area.isEnabled) StatusType.SUCCESS else StatusType.NEUTRAL
                                            )
                                            androidx.compose.material3.TextButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        viewModel.repository.saveLocationArea(area.copy(isEnabled = !area.isEnabled))
                                                        loadAreas()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .heightIn(min = 48.dp)
                                                    .testTag("toggle_area_${area.id}")
                                            ) {
                                                Text(
                                                    text = if (area.isEnabled) p6.disableAreaLabel else strings.adminOps.activeStatus,
                                                    fontFamily = SuqiFontFamily,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp,
                                                    color = SuqiPrimary
                                                )
                                            }
                                        }
                                    }
                                    SuqiSwitchRow(
                                        title = p6.defaultZoneLabel,
                                        checked = area.isDefaultZone,
                                        onCheckedChange = { on ->
                                            coroutineScope.launch {
                                                viewModel.repository.saveLocationArea(area.copy(isDefaultZone = on))
                                                loadAreas()
                                            }
                                        },
                                        testTag = "default_zone_${area.id}"
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
// A-18 (P3): Payment providers (wired Add + Edit sheet)
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
    var showSheet by remember { mutableStateOf(false) }
    var editingProvider by remember { mutableStateOf<PaymentProviderDto?>(null) }
    var displayName by remember { mutableStateOf("") }
    var logoEmoji by remember { mutableStateOf("💳") }
    var accountFormat by remember { mutableStateOf("") }
    var referenceHint by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }

    fun loadProviders() {
        coroutineScope.launch {
            val res = viewModel.repository.getPaymentProviders()
            if (res.success && res.data != null) {
                providers = res.data
            }
            isLoading = false
        }
    }

    fun openEditor(existing: PaymentProviderDto?) {
        editingProvider = existing
        displayName = existing?.displayName.orEmpty()
        logoEmoji = existing?.logoEmoji?.ifBlank { "💳" } ?: "💳"
        accountFormat = existing?.accountFormat.orEmpty()
        referenceHint = existing?.referenceHint.orEmpty()
        isActive = existing?.isActive ?: true
        showSheet = true
    }

    LaunchedEffect(Unit) { loadProviders() }

    SuqiScreen(
        title = strings.admin.paymentProvidersTitle,
        subtitle = "${providers.size}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = SuqiMaxContentWidth)
                    .fillMaxSize()
            ) {
                SuqiSectionCard(padding = 14.dp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    SuqiButton(
                        text = p6.addProviderBtn,
                        onClick = { openEditor(null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("add_provider_btn")
                    )
                }
                if (isLoading) {
                    SuqiSkeletonList(count = 3, modifier = Modifier.fillMaxWidth().padding(16.dp))
                } else if (providers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SuqiEmptyState(
                            icon = LucideIcons.Banknote,
                            title = strings.admin.paymentProvidersTitle,
                            subtitle = p6.paymentProvidersSub,
                            testTag = "providers_empty"
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(providers, key = { it.id }) { provider ->
                            SuqiSectionCard(
                                padding = 14.dp,
                                containerColor = if (provider.isActive) colors.primaryTint.copy(alpha = 0.08f) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = provider.displayName,
                                                fontFamily = SuqiFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = colors.primaryText
                                            )
                                            Text(
                                                text = "${provider.logoEmoji} • ${provider.accountFormat}",
                                                fontSize = 12.sp,
                                                color = colors.secondaryText
                                            )
                                            Text(
                                                text = p6.referenceFormatHint.replace("{example}", provider.referenceHint.ifBlank { "FT + 10 digits" }),
                                                fontSize = 11.sp,
                                                color = colors.mutedText
                                            )
                                        }
                                        SuqiButton(
                                            text = p6.editProviderBtn,
                                            onClick = { openEditor(provider) },
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("provider_edit_${provider.id}"),
                                            isPrimary = false
                                        )
                                    }
                                    SuqiSwitchRow(
                                        title = p6.activeSwitchLabel,
                                        checked = provider.isActive,
                                        onCheckedChange = { on ->
                                            coroutineScope.launch {
                                                viewModel.repository.savePaymentProvider(provider.copy(isActive = on))
                                                loadProviders()
                                            }
                                        },
                                        testTag = "provider_active_${provider.id}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSheet) {
            SuqiBottomSheet(onDismissRequest = { showSheet = false }) {
                SuqiTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = p6.providerNameHint,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "provider_name_input"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuqiTextField(
                        value = logoEmoji,
                        onValueChange = { logoEmoji = it },
                        label = "Icon",
                        modifier = Modifier.width(88.dp),
                        testTag = "provider_emoji_input"
                    )
                    SuqiTextField(
                        value = accountFormat,
                        onValueChange = { accountFormat = it },
                        label = p6.accountFormatHint,
                        modifier = Modifier.weight(1f),
                        testTag = "provider_format_input"
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                SuqiTextField(
                    value = referenceHint,
                    onValueChange = { referenceHint = it },
                    label = p6.buyerPreviewLabel,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "provider_reference_input"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SuqiSwitchRow(
                    title = p6.activeSwitchLabel,
                    checked = isActive,
                    onCheckedChange = { isActive = it },
                    testTag = "provider_sheet_active_switch"
                )
                Spacer(modifier = Modifier.height(12.dp))
                SuqiButton(
                    text = strings.saveLabel,
                    enabled = displayName.isNotBlank(),
                    onClick = {
                        coroutineScope.launch {
                            val existing = editingProvider
                            val dto = existing?.copy(
                                displayName = displayName.trim(),
                                logoEmoji = logoEmoji.ifBlank { "💳" },
                                accountFormat = accountFormat.trim(),
                                referenceHint = referenceHint.trim(),
                                isActive = isActive
                            ) ?: PaymentProviderDto(
                                id = "",
                                displayName = displayName.trim(),
                                logoEmoji = logoEmoji.ifBlank { "💳" },
                                accountFormat = accountFormat.trim(),
                                referenceHint = referenceHint.trim(),
                                isActive = isActive,
                                sortOrder = providers.size + 1
                            )
                            val res = viewModel.repository.savePaymentProvider(dto)
                            if (res.success) {
                                showSheet = false
                                loadProviders()
                                viewModel.showToast(p6.providerSavedToast)
                            } else {
                                viewModel.showToast(res.message ?: strings.retryLabel)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("provider_save_btn")
                )
            }
        }
    }
}

// ==========================================
// A-19 (P3): Content manager (wired Add + Edit trilingual sheet)
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
    var showSheet by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<ContentEntryDto?>(null) }
    var section by remember { mutableStateOf("HELP") }
    var titleEn by remember { mutableStateOf("") }
    var titleAm by remember { mutableStateOf("") }
    var titleOm by remember { mutableStateOf("") }
    var bodyEn by remember { mutableStateOf("") }
    var bodyAm by remember { mutableStateOf("") }
    var bodyOm by remember { mutableStateOf("") }
    var isPublished by remember { mutableStateOf(true) }

    fun loadEntries() {
        coroutineScope.launch {
            val res = viewModel.repository.getContentEntries()
            if (res.success && res.data != null) {
                entries = res.data
            }
            isLoading = false
        }
    }

    fun openEditor(existing: ContentEntryDto?) {
        editingEntry = existing
        section = existing?.section ?: "HELP"
        titleEn = existing?.titleEn.orEmpty()
        titleAm = existing?.titleAm.orEmpty()
        titleOm = existing?.titleOm.orEmpty()
        bodyEn = existing?.bodyEn.orEmpty()
        bodyAm = existing?.bodyAm.orEmpty()
        bodyOm = existing?.bodyOm.orEmpty()
        isPublished = existing?.isPublished ?: true
        showSheet = true
    }

    LaunchedEffect(Unit) { loadEntries() }

    val langLabel = when (selectedLang) {
        SuqiLanguageCode.EN -> "English"
        SuqiLanguageCode.AM -> "አማርኛ"
        SuqiLanguageCode.OM -> "Afaan Oromoo"
    }

    SuqiScreen(
        title = strings.admin.contentManagerTitle,
        subtitle = "${entries.size}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = SuqiMaxContentWidth)
                    .fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(SuqiLanguageCode.EN to "EN", SuqiLanguageCode.AM to "AM", SuqiLanguageCode.OM to "OM").forEach { (lang, label) ->
                        val selected = lang == selectedLang
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) SuqiPrimary else colors.cardBackground)
                                .border(if (!selected) 1.dp else 0.dp, colors.border, RoundedCornerShape(12.dp))
                                .clickable { selectedLang = lang }
                                .testTag("content_lang_$label")
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
                    SuqiButton(
                        text = p6.addContentBtn,
                        onClick = { openEditor(null) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("content_add_btn")
                    )
                }
                if (isLoading) {
                    SuqiSkeletonList(count = 3, modifier = Modifier.fillMaxWidth().padding(16.dp))
                } else if (entries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SuqiEmptyState(
                            icon = LucideIcons.FileText,
                            title = strings.admin.contentManagerTitle,
                            subtitle = strings.adminGov.contentEditorNote,
                            testTag = "content_empty"
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(entries, key = { it.id }) { entry ->
                            SuqiSectionCard(
                                padding = 14.dp,
                                onClick = { openEditor(entry) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("content_entry_${entry.id}")
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = entry.section,
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp,
                                            color = SuqiPrimary
                                        )
                                        SuqiStatusBadge(
                                            if (entry.isPublished) p6.publishBtn else p6.draftBadge,
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
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (entry.isMissingTranslation(selectedLang)) {
                                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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

        if (showSheet) {
            SuqiBottomSheet(onDismissRequest = { showSheet = false }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuqiTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = p6.contentTopicLabel,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "content_section_input"
                    )
                    SuqiTextField(value = titleEn, onValueChange = { titleEn = it }, label = "Title (EN)", modifier = Modifier.fillMaxWidth(), testTag = "content_title_en_input")
                    SuqiTextField(value = bodyEn, onValueChange = { bodyEn = it }, label = "Body (EN)", singleLine = false, modifier = Modifier.fillMaxWidth(), testTag = "content_body_en_input")
                    SuqiTextField(value = titleAm, onValueChange = { titleAm = it }, label = "Title (አማርኛ)", modifier = Modifier.fillMaxWidth(), testTag = "content_title_am_input")
                    SuqiTextField(value = bodyAm, onValueChange = { bodyAm = it }, label = "Body (አማርኛ)", singleLine = false, modifier = Modifier.fillMaxWidth(), testTag = "content_body_am_input")
                    SuqiTextField(value = titleOm, onValueChange = { titleOm = it }, label = "Title (Oromoo)", modifier = Modifier.fillMaxWidth(), testTag = "content_title_om_input")
                    SuqiTextField(value = bodyOm, onValueChange = { bodyOm = it }, label = "Body (Oromoo)", singleLine = false, modifier = Modifier.fillMaxWidth(), testTag = "content_body_om_input")
                    SuqiSwitchRow(
                        title = p6.publishBtn,
                        checked = isPublished,
                        onCheckedChange = { isPublished = it },
                        testTag = "content_publish_switch"
                    )
                    SuqiButton(
                        text = strings.saveLabel,
                        enabled = titleEn.isNotBlank() && bodyEn.isNotBlank(),
                        onClick = {
                            coroutineScope.launch {
                                val existing = editingEntry
                                val dto = ContentEntryDto(
                                    id = existing?.id.orEmpty(),
                                    section = section.trim().ifBlank { "HELP" },
                                    titleEn = titleEn.trim(),
                                    titleAm = titleAm.trim(),
                                    titleOm = titleOm.trim(),
                                    bodyEn = bodyEn.trim(),
                                    bodyAm = bodyAm.trim(),
                                    bodyOm = bodyOm.trim(),
                                    isPublished = isPublished
                                )
                                val res = viewModel.repository.saveContentEntry(dto)
                                if (res.success) {
                                    showSheet = false
                                    loadEntries()
                                    viewModel.showToast(p6.contentSavedToast)
                                } else {
                                    viewModel.showToast(res.message ?: strings.retryLabel)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("content_save_btn")
                    )
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

    val groups = listOf(
        "COMMERCE" to p6.groupCommerce,
        "MEDIA" to p6.groupMedia,
        "CATALOG" to p6.groupCatalog,
        "MONETIZATION" to p6.groupMonetization,
        "EXPERIENCE" to p6.groupExperience
    )

    SuqiScreen(
        title = strings.admin.platformSwitchesTitle,
        subtitle = "${switches.size}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = SuqiMaxContentWidth)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isLoading && switches.isEmpty()) {
                    SuqiSkeletonList(count = 3, modifier = Modifier.fillMaxWidth())
                } else if (switches.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        SuqiEmptyState(
                            icon = LucideIcons.SlidersHorizontal,
                            title = strings.admin.platformSwitchesTitle,
                            subtitle = p6.platformSwitchesSub,
                            testTag = "switches_empty"
                        )
                    }
                } else {
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
                                        subtitle = "${p6.affectsLabel}: ${sw.groupName}" + if (sw.numericValue > 0.0) " (${sw.numericValue})" else "",
                                        checked = sw.isEnabled,
                                        onCheckedChange = { enabled ->
                                            switches = switches.map { if (it.id == sw.id) it.copy(isEnabled = enabled) else it }
                                            coroutineScope.launch {
                                                viewModel.repository.savePlatformSwitch(sw.id, enabled, sw.numericValue)
                                            }
                                        },
                                        testTag = "switch_${sw.id}"
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
