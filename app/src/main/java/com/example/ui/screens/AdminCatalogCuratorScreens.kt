@file:OptIn(ExperimentalLayoutApi::class)

package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AdminSettingsDto
import com.example.data.model.CategoryDto
import com.example.data.model.HomeSectionKeys
import com.example.data.model.ItemTypeDto
import com.example.data.model.ProductDto
import com.example.data.model.PromoBannerDto
import com.example.data.model.ShopDto
import com.example.ui.SuqiViewModel
import com.example.ui.components.PromoBanner
import com.example.ui.components.StatusType
import com.example.ui.components.SuqiBottomSheet
import com.example.ui.components.SuqiButton
import com.example.ui.components.SuqiCategoryIcons
import com.example.ui.components.SuqiDateField
import com.example.ui.components.SuqiDragHandleList
import com.example.ui.components.SuqiImagePickerTile
import com.example.ui.components.SuqiPromoBannerSlide
import com.example.ui.components.SuqiScreen
import com.example.ui.components.SuqiSectionCard
import com.example.ui.components.SuqiStatusBadge
import com.example.ui.components.SuqiSwitchRow
import com.example.ui.components.SuqiTabItem
import com.example.ui.components.SuqiTabs
import com.example.ui.components.SuqiTextField
import com.example.ui.components.promoBadgeIconOptions
import com.example.ui.components.promoBadgeIconVector
import com.example.ui.i18n.LocalSuqiLanguage
import com.example.ui.i18n.localizedName
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.LocalSuqiColors
import com.example.ui.theme.LucideIcons
import com.example.ui.theme.SuqiError
import com.example.ui.theme.SuqiFontFamily
import com.example.ui.theme.SuqiPrimary
import kotlinx.coroutines.launch

// ==========================================
// A-10 (P2): Categories Manager 2.0
// ==========================================
@Composable
fun AdminCategoriesScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val lang = LocalSuqiLanguage.current
    val isOffline by viewModel.isOffline.collectAsState()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Categories, 1 = Shop Types
    var categories by remember {
        mutableStateOf(viewModel.cachedAllCategories.sortedWith(compareBy({ it.sortOrder }, { it.name })))
    }
    var itemTypes by remember { mutableStateOf(viewModel.cachedAdminItemTypes) }
    var shops by remember { mutableStateOf(viewModel.cachedAllShops) }
    var expandedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var editingCategory by remember { mutableStateOf<CategoryDto?>(null) }
    var showCategorySheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    BackHandler(enabled = showCategorySheet) {
        showCategorySheet = false
    }

    val shopTypeEntries = remember(strings) {
        listOf(
            Triple("Retail", strings.extras.shopTypeRetailName, strings.extras.shopTypeRetailDesc),
            Triple("Brand", strings.extras.shopTypeBrandName, strings.extras.shopTypeBrandDesc),
            Triple("Artisan", strings.extras.shopTypeArtisanName, strings.extras.shopTypeArtisanDesc),
            Triple("Wholesale", strings.extras.shopTypeWholesaleName, strings.extras.shopTypeWholesaleDesc)
        )
    }

    fun loadCatalog() {
        coroutineScope.launch {
            val catRes = viewModel.repository.getCategories()
            if (catRes.success && catRes.data != null) {
                val sorted = catRes.data.sortedWith(compareBy({ it.sortOrder }, { it.name }))
                categories = sorted
                viewModel.cachedAllCategories = sorted
            }
            val typeRes = viewModel.repository.getItemTypes()
            if (typeRes.success && typeRes.data != null) {
                itemTypes = typeRes.data
                viewModel.cachedAdminItemTypes = typeRes.data
            }
            val shopRes = viewModel.repository.getShops()
            if (shopRes.success && shopRes.data != null) {
                shops = shopRes.data
                viewModel.cachedAllShops = shopRes.data
            }
        }
    }

    LaunchedEffect(Unit) {
        loadCatalog()
    }

    val activeSectionsBadge = String.format(
        strings.adminGov.sectionsCountTemplate,
        categories.count { it.isActive }
    )

    SuqiScreen(
        title = strings.admin.categoriesManagerTitle,
        subtitle = activeSectionsBadge,
        onBackClick = { viewModel.goBack() },
        headerActions = {
            if (selectedTab == 0) {
                IconButton(
                    onClick = {
                        editingCategory = null
                        showCategorySheet = true
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SuqiPrimary)
                        .testTag("add_category_btn")
                ) {
                    Icon(LucideIcons.Plus, contentDescription = strings.adminGov.newCategoryBtn, tint = Color.White)
                }
            }
        },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SuqiTabs(
                tabs = listOf(
                    SuqiTabItem("categories", strings.categoriesTitle, count = categories.size),
                    SuqiTabItem("shop_types", strings.sellerOps.shopBrandingRowTitle, count = shopTypeEntries.size)
                ),
                selectedIndex = selectedTab,
                onSelectTab = { selectedTab = it },
                testTagPrefix = "admin_cat_tab"
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTab == 0) {
                    item {
                        SuqiDragHandleList(
                            items = categories,
                            onMove = { from, to ->
                                val mutable = categories.toMutableList()
                                val item = mutable.removeAt(from)
                                mutable.add(to, item)
                                val reindexed = mutable.mapIndexed { idx, cat -> cat.copy(sortOrder = idx) }
                                categories = reindexed
                                coroutineScope.launch {
                                    viewModel.repository.reorderCategories(reindexed.map { it.id })
                                }
                            }
                        ) { _, cat, moveUp, moveDown ->
                            val isExpanded = cat.id in expandedIds
                            SuqiSectionCard(padding = 14.dp, modifier = Modifier.testTag("admin_cat_${cat.id}")) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Reorder arrows
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconButton(
                                            onClick = { moveUp?.invoke() },
                                            enabled = moveUp != null,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = LucideIcons.ChevronUp,
                                                contentDescription = strings.common.moveUpDesc,
                                                tint = if (moveUp != null) colors.primaryText else colors.mutedText.copy(alpha = 0.3f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { moveDown?.invoke() },
                                            enabled = moveDown != null,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = LucideIcons.ChevronDown,
                                                contentDescription = strings.common.moveDownDesc,
                                                tint = if (moveDown != null) colors.primaryText else colors.mutedText.copy(alpha = 0.3f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // 48dp thumbnail + category icon badge
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(colors.primaryTint)
                                    ) {
                                        if (cat.imageUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = cat.imageUrl,
                                                contentDescription = cat.localizedName(lang),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = SuqiCategoryIcons.iconFor(cat.icon),
                                                contentDescription = cat.localizedName(lang),
                                                tint = SuqiPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cat.localizedName(lang),
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = colors.primaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${cat.subCategories.size} ${strings.subCategoriesLabel.lowercase()}",
                                            fontFamily = SuqiFontFamily,
                                            fontSize = 11.sp,
                                            color = colors.secondaryText
                                        )
                                    }

                                    Switch(
                                        checked = cat.isActive,
                                        onCheckedChange = { checked ->
                                            val updated = cat.copy(isActive = checked)
                                            categories = categories.map { if (it.id == cat.id) updated else it }
                                            coroutineScope.launch {
                                                viewModel.repository.saveCategory(updated)
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedTrackColor = SuqiPrimary,
                                            checkedThumbColor = Color.White
                                        )
                                    )

                                    IconButton(
                                        onClick = {
                                            expandedIds = if (isExpanded) expandedIds - cat.id else expandedIds + cat.id
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isExpanded) LucideIcons.ChevronUp else LucideIcons.ChevronDown,
                                            contentDescription = strings.subCategoriesLabel,
                                            tint = colors.secondaryText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(modifier = Modifier.padding(top = 10.dp)) {
                                        if (cat.nameAmharic.isNotBlank() || cat.nameAfaanOromo.isNotBlank()) {
                                            Text(
                                                text = listOfNotNull(
                                                    cat.nameAmharic.takeIf { it.isNotBlank() },
                                                    cat.nameAfaanOromo.takeIf { it.isNotBlank() }
                                                ).joinToString("  •  "),
                                                fontFamily = SuqiFontFamily,
                                                fontSize = 11.sp,
                                                color = colors.mutedText
                                            )
                                            Spacer(Modifier.height(6.dp))
                                        }
                                        if (cat.subCategories.isNotEmpty()) {
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                cat.subCategories.forEach { sub ->
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(999.dp))
                                                            .background(colors.secondaryBackground)
                                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(sub, fontSize = 11.sp, color = colors.primaryText)
                                                    }
                                                }
                                            }
                                        }
                                        if (cat.linkedItemTypes.isNotEmpty()) {
                                            Spacer(Modifier.height(6.dp))
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                cat.linkedItemTypes.forEach { tid ->
                                                    val label = itemTypes.firstOrNull { it.id == tid }?.let { "${it.emoji} ${it.label}" } ?: tid
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(999.dp))
                                                            .background(colors.primaryTint)
                                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SuqiPrimary)
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            OutlinedButton(
                                                onClick = {
                                                    editingCategory = cat
                                                    showCategorySheet = true
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, colors.border),
                                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                            ) {
                                                Icon(LucideIcons.Pencil, contentDescription = null, tint = colors.primaryText, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text(strings.editLabel, color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        viewModel.repository.deleteCategory(cat.id)
                                                        loadCatalog()
                                                        viewModel.showToast(strings.deleteLabel)
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, SuqiError.copy(alpha = 0.5f)),
                                                modifier = Modifier.heightIn(min = 48.dp)
                                            ) {
                                                Icon(LucideIcons.Trash2, contentDescription = strings.deleteLabel, tint = SuqiError, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    shopTypeEntries.forEach { (key, title, desc) ->
                        val matchingCount = shops.count { s ->
                            s.shopType.contains(key, ignoreCase = true) ||
                                (key == "Retail" && s.shopType.contains("Boutique", ignoreCase = true)) ||
                                (key == "Artisan" && s.shopType.contains("Decor", ignoreCase = true))
                        }
                        item(key = title) {
                            SuqiSectionCard(padding = 16.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(colors.primaryTint)
                                        ) {
                                            Icon(LucideIcons.Store, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(title, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                                            Text(desc, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                                        }
                                    }
                                    SuqiStatusBadge(
                                        statusText = "$matchingCount",
                                        type = if (matchingCount > 0) StatusType.SUCCESS else StatusType.NEUTRAL
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCategorySheet) {
        val editing = editingCategory
        var nameEn by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
        var nameAm by remember(editing) { mutableStateOf(editing?.nameAmharic.orEmpty()) }
        var nameOm by remember(editing) { mutableStateOf(editing?.nameAfaanOromo.orEmpty()) }
        var selectedIcon by remember(editing) { mutableStateOf(editing?.icon ?: "sparkles") }
        var imageUrl by remember(editing) { mutableStateOf(editing?.imageUrl.orEmpty()) }
        var isActive by remember(editing) { mutableStateOf(editing?.isActive ?: true) }
        val subCats = remember(editing) {
            mutableStateListOf<String>().apply { addAll(editing?.subCategories.orEmpty()) }
        }
        var newSubInput by remember { mutableStateOf("") }
        val linkedTypes = remember(editing) {
            mutableStateListOf<String>().apply { addAll(editing?.linkedItemTypes.orEmpty()) }
        }

        SuqiBottomSheet(
            onDismissRequest = { showCategorySheet = false },
            title = if (editing == null) strings.adminGov.newCategoryBtn else strings.editLabel
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SuqiTextField(value = nameEn, onValueChange = { nameEn = it }, label = "Name (English) *")
                SuqiTextField(value = nameAm, onValueChange = { nameAm = it }, label = "ስም (አማርኛ)")
                SuqiTextField(value = nameOm, onValueChange = { nameOm = it }, label = "Maqaa (Afaan Oromoo)")

                SuqiImagePickerTile(
                    label = strings.buyer.categoryLabel,
                    imageUri = imageUrl.takeIf { it.isNotBlank() },
                    onImagePicked = { imageUrl = it },
                    onRemove = { imageUrl = "" },
                    aspectRatio = 1f,
                    tileHeight = 110.dp,
                    testTag = "admin_cat_image_picker"
                )

                // Category icon picker grid
                Text(
                    text = strings.adminGov.iconPickerLabel,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = colors.primaryText
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuqiCategoryIcons.options.forEach { (key, iconVec) ->
                        val isSel = selectedIcon == key
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSel) SuqiPrimary else colors.secondaryBackground)
                                .border(1.dp, if (isSel) SuqiPrimary else colors.border, RoundedCornerShape(14.dp))
                                .clickable { selectedIcon = key }
                                .testTag("cat_icon_pick_$key")
                        ) {
                            Icon(
                                imageVector = iconVec,
                                contentDescription = key,
                                tint = if (isSel) Color.White else colors.primaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Sub-categories interactive chip editor (replaces CSV input)
                Text(
                    text = strings.subCategoriesLabel,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = colors.primaryText
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuqiTextField(
                        value = newSubInput,
                        onValueChange = { newSubInput = it },
                        label = "",
                        placeholder = strings.adminGov.addSubCategoryHint,
                        modifier = Modifier.weight(1f),
                        testTag = "new_subcategory_input"
                    )
                    Button(
                        onClick = {
                            val trimmed = newSubInput.trim()
                            if (trimmed.isNotBlank() && trimmed !in subCats) {
                                subCats.add(trimmed)
                                newSubInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("add_subcategory_btn")
                    ) {
                        Text(strings.common.addLabel, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                if (subCats.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        subCats.forEach { sub ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(colors.primaryTint)
                                    .clickable { subCats.remove(sub) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(sub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SuqiPrimary)
                                Spacer(Modifier.width(6.dp))
                                Icon(LucideIcons.X, contentDescription = strings.common.removeLabel, tint = SuqiPrimary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // Linked Item Types multi-select chips
                Text(
                    text = strings.adminGov.linkedItemTypesLabel,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = colors.primaryText
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemTypes.forEach { t ->
                        val isLinked = t.id in linkedTypes
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isLinked) SuqiPrimary else colors.secondaryBackground)
                                .border(1.dp, if (isLinked) SuqiPrimary else colors.border, RoundedCornerShape(999.dp))
                                .clickable {
                                    if (isLinked) linkedTypes.remove(t.id) else linkedTypes.add(t.id)
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "${t.emoji} ${t.label}",
                                fontFamily = SuqiFontFamily,
                                fontSize = 12.sp,
                                fontWeight = if (isLinked) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isLinked) Color.White else colors.primaryText
                            )
                        }
                    }
                }

                SuqiSwitchRow(
                    title = strings.adminGov.activeCategorySwitch,
                    checked = isActive,
                    onCheckedChange = { isActive = it }
                )

                SuqiButton(
                    text = strings.saveLabel,
                    onClick = {
                        if (nameEn.isBlank()) {
                            viewModel.showToast(strings.seller.enterNameError)
                            return@SuqiButton
                        }
                        coroutineScope.launch {
                            viewModel.repository.saveCategory(
                                CategoryDto(
                                    id = editing?.id.orEmpty(),
                                    name = nameEn.trim(),
                                    nameAmharic = nameAm.trim(),
                                    nameAfaanOromo = nameOm.trim(),
                                    icon = selectedIcon,
                                    imageUrl = imageUrl.trim(),
                                    subCategories = subCats.toList(),
                                    isActive = isActive,
                                    sortOrder = editing?.sortOrder ?: categories.size,
                                    linkedItemTypes = linkedTypes.toList()
                                )
                            )
                            showCategorySheet = false
                            loadCatalog()
                            viewModel.showToast(strings.saveLabel)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "save_category_btn"
                )
            }
        }
    }
}

// ==========================================
// A-12 (P1): Banners & Homepage Curator
// ==========================================
@Composable
fun AdminBannersScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val lang = LocalSuqiLanguage.current
    val isOffline by viewModel.isOffline.collectAsState()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Banners, 1 = Home Sections
    var banners by remember { mutableStateOf(viewModel.cachedHomeBanners.sortedBy { it.sortOrder }) }
    var categories by remember { mutableStateOf(viewModel.cachedAllCategories) }
    var shops by remember { mutableStateOf(viewModel.cachedAllShops) }
    var products by remember { mutableStateOf(viewModel.cachedAllProducts) }
    var adminSettings by remember { mutableStateOf(viewModel.cachedAdminSettings ?: AdminSettingsDto()) }
    val listState = rememberLazyListState()

    var editingBanner by remember { mutableStateOf<PromoBannerDto?>(null) }
    var showBannerSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = showBannerSheet) {
        showBannerSheet = false
    }

    fun loadCuratorData() {
        coroutineScope.launch {
            viewModel.repository.getPromoBanners(activeOnly = false).data?.let { list ->
                banners = list.sortedBy { it.sortOrder }
            }
            viewModel.repository.getAdminSettings().data?.let { s ->
                adminSettings = s
                viewModel.cachedAdminSettings = s
            }
            viewModel.repository.getCategories().data?.let {
                categories = it
                viewModel.cachedAllCategories = it
            }
            viewModel.repository.getShops().data?.let {
                shops = it
                viewModel.cachedAllShops = it
            }
            viewModel.repository.getProducts().data?.let {
                products = it
                viewModel.cachedAllProducts = it
            }
        }
    }

    LaunchedEffect(Unit) {
        loadCuratorData()
    }

    SuqiScreen(
        title = strings.admin.bannersManagerTitle,
        subtitle = "${banners.count { it.isActive }} / ${banners.size}",
        onBackClick = { viewModel.goBack() },
        headerActions = {
            if (selectedTab == 0) {
                IconButton(
                    onClick = {
                        editingBanner = null
                        showBannerSheet = true
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SuqiPrimary)
                        .testTag("add_banner_btn")
                ) {
                    Icon(LucideIcons.Plus, contentDescription = strings.adminGov.newBannerBtn, tint = Color.White)
                }
            }
        },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SuqiTabs(
                tabs = listOf(
                    SuqiTabItem("banners", strings.adminGov.bannersListTab, count = banners.size),
                    SuqiTabItem("sections", strings.adminGov.homeSectionsTitle, count = HomeSectionKeys.defaultOrder.size)
                ),
                selectedIndex = selectedTab,
                onSelectTab = { selectedTab = it },
                testTagPrefix = "admin_curator_tab"
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTab == 0) {
                    item {
                        SuqiDragHandleList(
                            items = banners,
                            onMove = { from, to ->
                                val mutable = banners.toMutableList()
                                val moved = mutable.removeAt(from)
                                mutable.add(to, moved)
                                val reindexed = mutable.mapIndexed { idx, b -> b.copy(sortOrder = idx) }
                                banners = reindexed
                                coroutineScope.launch {
                                    viewModel.repository.reorderPromoBanners(reindexed.map { it.id })
                                }
                            }
                        ) { _, banner, moveUp, moveDown ->
                            SuqiSectionCard(padding = 14.dp, modifier = Modifier.testTag("admin_banner_${banner.id}")) {
                                // Mini live preview slide
                                Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                                    SuqiPromoBannerSlide(
                                        banner = PromoBanner(
                                            id = banner.id.ifBlank { "preview" },
                                            badge = banner.badge,
                                            title = banner.title,
                                            highlight = banner.highlight,
                                            subtitle = banner.subtitle,
                                            buttonText = banner.buttonText,
                                            imageUrl = banner.imageUrl,
                                            badgeIcon = promoBadgeIconVector(banner.badgeIcon),
                                            targetCategory = banner.targetCategory,
                                            targetName = banner.targetName,
                                            destinationType = banner.destinationType,
                                            targetId = banner.targetId
                                        ),
                                        onClick = {
                                            editingBanner = banner
                                            showBannerSheet = true
                                        }
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(
                                        onClick = { moveUp?.invoke() },
                                        enabled = moveUp != null,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            LucideIcons.ChevronUp,
                                            contentDescription = strings.common.moveUpDesc,
                                            tint = if (moveUp != null) colors.primaryText else colors.mutedText.copy(alpha = 0.3f)
                                        )
                                    }
                                    IconButton(
                                        onClick = { moveDown?.invoke() },
                                        enabled = moveDown != null,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            LucideIcons.ChevronDown,
                                            contentDescription = strings.common.moveDownDesc,
                                            tint = if (moveDown != null) colors.primaryText else colors.mutedText.copy(alpha = 0.3f)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${banner.destinationType}: ${banner.targetName.ifBlank { banner.targetId.ifBlank { banner.targetCategory } }}",
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = colors.primaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val scheduleLabel = listOfNotNull(
                                            banner.startDate.takeIf { it.isNotBlank() },
                                            banner.endDate.takeIf { it.isNotBlank() }
                                        ).joinToString(" → ")
                                        Text(
                                            text = listOfNotNull(
                                                banner.audience,
                                                scheduleLabel.takeIf { it.isNotBlank() }
                                            ).joinToString(" • "),
                                            fontFamily = SuqiFontFamily,
                                            fontSize = 11.sp,
                                            color = colors.secondaryText
                                        )
                                    }
                                    Switch(
                                        checked = banner.isActive,
                                        onCheckedChange = { active ->
                                            val updated = banner.copy(isActive = active)
                                            banners = banners.map { if (it.id == banner.id) updated else it }
                                            coroutineScope.launch {
                                                viewModel.repository.savePromoBanner(updated)
                                            }
                                        },
                                        colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary, checkedThumbColor = Color.White)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = {
                                            editingBanner = banner
                                            showBannerSheet = true
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, colors.border),
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                    ) {
                                        Icon(LucideIcons.Pencil, contentDescription = null, tint = colors.primaryText, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(strings.editLabel, color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.repository.deletePromoBanner(banner.id)
                                                loadCuratorData()
                                                viewModel.showToast(strings.deleteLabel)
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, SuqiError.copy(alpha = 0.5f)),
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Icon(LucideIcons.Trash2, contentDescription = strings.deleteLabel, tint = SuqiError, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val orderedSections = (adminSettings.homeSectionsOrder + HomeSectionKeys.defaultOrder).distinct()
                    val hiddenSections = adminSettings.hiddenHomeSections.toSet()

                    fun sectionLabel(key: String): String = when (key) {
                        HomeSectionKeys.FEATURED_SHOPS -> strings.adminGov.sectionFeaturedShops
                        HomeSectionKeys.NEW_ARRIVALS -> strings.adminGov.sectionNewArrivals
                        HomeSectionKeys.POPULAR -> strings.adminGov.sectionPopular
                        HomeSectionKeys.RECOMMENDED -> strings.adminGov.sectionRecommended
                        HomeSectionKeys.RECENTLY_VIEWED -> strings.adminGov.sectionRecentlyViewed
                        else -> key
                    }

                    item {
                        SuqiDragHandleList(
                            items = orderedSections,
                            onMove = { from, to ->
                                val mutable = orderedSections.toMutableList()
                                val moved = mutable.removeAt(from)
                                mutable.add(to, moved)
                                adminSettings = adminSettings.copy(homeSectionsOrder = mutable)
                                coroutineScope.launch {
                                    viewModel.repository.updateHomeSectionsConfig(mutable, hiddenSections.toList())
                                }
                            }
                        ) { _, key, moveUp, moveDown ->
                            val isVisible = key !in hiddenSections
                            SuqiSectionCard(padding = 12.dp, modifier = Modifier.testTag("home_section_row_$key")) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        IconButton(
                                            onClick = { moveUp?.invoke() },
                                            enabled = moveUp != null,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                LucideIcons.ChevronUp,
                                                contentDescription = strings.common.moveUpDesc,
                                                tint = if (moveUp != null) colors.primaryText else colors.mutedText.copy(alpha = 0.3f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { moveDown?.invoke() },
                                            enabled = moveDown != null,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                LucideIcons.ChevronDown,
                                                contentDescription = strings.common.moveDownDesc,
                                                tint = if (moveDown != null) colors.primaryText else colors.mutedText.copy(alpha = 0.3f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        SuqiSwitchRow(
                                            title = sectionLabel(key),
                                            subtitle = if (isVisible) strings.adminGov.visibleLabel else strings.adminGov.hiddenLabel,
                                            checked = isVisible,
                                            onCheckedChange = { nextVisible ->
                                                val nextHidden = if (nextVisible) hiddenSections - key else hiddenSections + key
                                                adminSettings = adminSettings.copy(hiddenHomeSections = nextHidden.toList())
                                                coroutineScope.launch {
                                                    viewModel.repository.updateHomeSectionsConfig(orderedSections, nextHidden.toList())
                                                }
                                            },
                                            testTag = "section_switch_$key"
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

    if (showBannerSheet) {
        val editing = editingBanner
        var badge by remember(editing) { mutableStateOf(editing?.badge ?: strings.checkout.hotDealBadge) }
        var badgeIcon by remember(editing) { mutableStateOf(editing?.badgeIcon ?: "sparkles") }
        var title by remember(editing) { mutableStateOf(editing?.title ?: strings.checkout.upTo50OffTitle) }
        var highlight by remember(editing) { mutableStateOf(editing?.highlight ?: "50%") }
        var subtitle by remember(editing) { mutableStateOf(editing?.subtitle ?: strings.checkout.onSelectedItemsSub) }
        var buttonText by remember(editing) { mutableStateOf(editing?.buttonText ?: strings.checkout.shopNowCta) }
        var imageUrl by remember(editing) { mutableStateOf(editing?.imageUrl.orEmpty()) }
        var destinationType by remember(editing) { mutableStateOf(editing?.destinationType ?: "CATEGORY") }
        var targetId by remember(editing) { mutableStateOf(editing?.targetId?.ifBlank { editing.targetCategory } ?: editing?.targetCategory.orEmpty()) }
        var targetName by remember(editing) { mutableStateOf(editing?.targetName.orEmpty()) }
        var startDate by remember(editing) { mutableStateOf(editing?.startDate.orEmpty()) }
        var endDate by remember(editing) { mutableStateOf(editing?.endDate.orEmpty()) }
        var audience by remember(editing) { mutableStateOf(editing?.audience ?: "ALL") }
        var isActive by remember(editing) { mutableStateOf(editing?.isActive ?: true) }

        val destOptions = listOf(
            "CATEGORY" to strings.adminGov.destCategory,
            "SHOP" to strings.adminGov.destShop,
            "PRODUCT" to strings.adminGov.destProduct,
            "DEALS" to strings.adminGov.destDeals,
            "URL" to strings.adminGov.destUrl
        )
        val audienceOptions = listOf(
            "ALL" to strings.adminGov.audienceAll,
            "NEW_USERS" to strings.adminGov.audienceNewUsers,
            "ADDIS_ONLY" to strings.adminGov.audienceAddisOnly
        )

        SuqiBottomSheet(
            onDismissRequest = { showBannerSheet = false },
            title = if (editing == null) strings.adminGov.newBannerBtn else strings.editLabel
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Live Banner Preview
                Text(
                    text = strings.adminGov.livePreviewLabel,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = colors.secondaryText
                )
                Box(modifier = Modifier.fillMaxWidth().height(176.dp)) {
                    SuqiPromoBannerSlide(
                        banner = PromoBanner(
                            id = "editor_preview",
                            badge = badge,
                            title = title,
                            highlight = highlight,
                            subtitle = subtitle,
                            buttonText = buttonText,
                            imageUrl = imageUrl,
                            badgeIcon = promoBadgeIconVector(badgeIcon),
                            targetCategory = targetId,
                            targetName = targetName,
                            destinationType = destinationType,
                            targetId = targetId
                        ),
                        onClick = {}
                    )
                }

                SuqiTextField(value = badge, onValueChange = { badge = it }, label = strings.extras.bannerBadgeLabel)

                // Badge icon picker
                Text(
                    text = strings.adminGov.badgeIconLabel,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = colors.primaryText
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    promoBadgeIconOptions.forEach { (key, iconVec) ->
                        val isSel = badgeIcon == key
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isSel) SuqiPrimary else colors.secondaryBackground)
                                .border(1.dp, if (isSel) SuqiPrimary else colors.border, CircleShape)
                                .clickable { badgeIcon = key }
                        ) {
                            Icon(iconVec, contentDescription = key, tint = if (isSel) Color.White else colors.primaryText, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                SuqiTextField(value = title, onValueChange = { title = it }, label = "${strings.description} *")

                // Interactive clickable highlight word chips from headline words
                val titleWords = title.split(" ").map { it.trim() }.filter { it.isNotBlank() }.distinct()
                if (titleWords.isNotEmpty()) {
                    Text(
                        text = strings.adminGov.highlightWordLabel,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = colors.primaryText
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        titleWords.forEach { word ->
                            val isSel = highlight == word
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (isSel) SuqiPrimary else colors.secondaryBackground)
                                    .border(1.dp, if (isSel) SuqiPrimary else colors.border, RoundedCornerShape(999.dp))
                                    .clickable { highlight = if (isSel) "" else word }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = word,
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color.White else colors.primaryText
                                )
                            }
                        }
                    }
                }

                SuqiTextField(value = subtitle, onValueChange = { subtitle = it }, label = strings.description)
                SuqiTextField(value = buttonText, onValueChange = { buttonText = it }, label = strings.extras.bannerCtaLabel)

                // Destination type selector
                Text(
                    text = strings.adminGov.destinationTypeLabel,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = colors.primaryText
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    destOptions.forEach { (key, label) ->
                        val isSel = destinationType == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isSel) SuqiPrimary else colors.secondaryBackground)
                                .border(1.dp, if (isSel) SuqiPrimary else colors.border, RoundedCornerShape(999.dp))
                                .clickable {
                                    destinationType = key
                                    if (key == "DEALS") {
                                        targetId = "deals"
                                        targetName = strings.dealsTitle
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (isSel) Color.White else colors.primaryText)
                        }
                    }
                }

                // Target entity picker based on destinationType
                when (destinationType) {
                    "CATEGORY" -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        ) {
                            categories.forEach { cat ->
                                val isSel = targetId == cat.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (isSel) SuqiPrimary else colors.secondaryBackground)
                                        .clickable {
                                            targetId = cat.id
                                            targetName = cat.localizedName(lang)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(cat.localizedName(lang), fontSize = 12.sp, color = if (isSel) Color.White else colors.primaryText)
                                }
                            }
                        }
                    }
                    "SHOP" -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        ) {
                            shops.forEach { shop ->
                                val isSel = targetId == shop.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (isSel) SuqiPrimary else colors.secondaryBackground)
                                        .clickable {
                                            targetId = shop.id
                                            targetName = shop.name
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(shop.name, fontSize = 12.sp, color = if (isSel) Color.White else colors.primaryText)
                                }
                            }
                        }
                    }
                    "PRODUCT" -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        ) {
                            products.take(15).forEach { prod ->
                                val isSel = targetId == prod.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (isSel) SuqiPrimary else colors.secondaryBackground)
                                        .clickable {
                                            targetId = prod.id
                                            targetName = prod.localizedName(lang)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(prod.localizedName(lang), fontSize = 12.sp, maxLines = 1, color = if (isSel) Color.White else colors.primaryText)
                                }
                            }
                        }
                    }
                    "URL" -> {
                        SuqiTextField(
                            value = targetId,
                            onValueChange = {
                                targetId = it
                                targetName = it
                            },
                            label = strings.adminGov.externalUrlLabel,
                            placeholder = "https://..."
                        )
                    }
                }

                // Schedule Dates
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SuqiDateField(
                        label = strings.adminGov.scheduleStartLabel,
                        dateText = startDate,
                        onDateSelected = { startDate = it },
                        modifier = Modifier.weight(1f)
                    )
                    SuqiDateField(
                        label = strings.adminGov.scheduleEndLabel,
                        dateText = endDate,
                        onDateSelected = { endDate = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Audience Chips
                Text(
                    text = strings.adminGov.audienceLabel,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = colors.primaryText
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    audienceOptions.forEach { (key, label) ->
                        val isSel = audience == key
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isSel) SuqiPrimary else colors.secondaryBackground)
                                .border(1.dp, if (isSel) SuqiPrimary else colors.border, RoundedCornerShape(999.dp))
                                .clickable { audience = key }
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isSel) Color.White else colors.primaryText, maxLines = 1)
                        }
                    }
                }

                SuqiImagePickerTile(
                    label = strings.admin.bannersManagerTitle,
                    imageUri = imageUrl.takeIf { it.isNotBlank() },
                    onImagePicked = { imageUrl = it },
                    onRemove = { imageUrl = "" },
                    aspectRatio = 0.8f,
                    tileHeight = 140.dp,
                    testTag = "admin_banner_image_picker"
                )

                SuqiSwitchRow(
                    title = strings.adminOps.activeStatus,
                    checked = isActive,
                    onCheckedChange = { isActive = it }
                )

                SuqiButton(
                    text = strings.saveLabel,
                    onClick = {
                        if (title.isBlank()) {
                            viewModel.showToast(strings.seller.enterNameError)
                            return@SuqiButton
                        }
                        coroutineScope.launch {
                            viewModel.repository.savePromoBanner(
                                PromoBannerDto(
                                    id = editing?.id.orEmpty(),
                                    badge = badge.trim(),
                                    title = title.trim(),
                                    highlight = highlight.trim(),
                                    subtitle = subtitle.trim(),
                                    buttonText = buttonText.trim().ifBlank { strings.checkout.shopNowCta },
                                    imageUrl = imageUrl.trim(),
                                    targetCategory = if (destinationType == "CATEGORY") targetId else editing?.targetCategory.orEmpty(),
                                    targetName = targetName.trim(),
                                    isActive = isActive,
                                    sortOrder = editing?.sortOrder ?: banners.size,
                                    badgeIcon = badgeIcon,
                                    destinationType = destinationType,
                                    targetId = targetId.trim(),
                                    startDate = startDate.trim(),
                                    endDate = endDate.trim(),
                                    audience = audience
                                )
                            )
                            showBannerSheet = false
                            loadCuratorData()
                            viewModel.showToast(strings.saveLabel)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "save_banner_btn"
                )
            }
        }
    }
}
