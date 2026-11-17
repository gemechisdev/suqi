@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.ProductDraftData
import com.example.data.local.SellerPreferencesStore
import com.example.data.model.*
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.LocalSuqiLanguage
import com.example.ui.i18n.localizedName
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProductManagementScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val work = strings.sellerWork
    val lang = LocalSuqiLanguage.current
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var products by remember { mutableStateOf(viewModel.cachedSellerProducts) }
    var categories by remember { mutableStateOf(viewModel.cachedAllCategories) }
    var isLoading by remember { mutableStateOf(products.isEmpty()) }
    var selectedTab by rememberSaveable { mutableStateOf("ALL") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortOption by rememberSaveable { mutableStateOf("NEWEST") }
    var showSortSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    var actionProduct by remember { mutableStateOf<ProductDto?>(null) }
    var confirmDeleteProduct by remember { mutableStateOf<ProductDto?>(null) }

    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }
    var confirmBulkDelete by remember { mutableStateOf(false) }
    var showBulkCategorySheet by remember { mutableStateOf(false) }
    var showBulkDiscountDialog by remember { mutableStateOf(false) }
    var bulkDiscountInput by remember { mutableStateOf("10") }

    BackHandler(
        enabled = confirmDeleteProduct != null ||
            confirmBulkDelete ||
            showBulkCategorySheet ||
            showBulkDiscountDialog ||
            actionProduct != null ||
            showSortSheet ||
            isSelectionMode
    ) {
        when {
            confirmDeleteProduct != null -> confirmDeleteProduct = null
            confirmBulkDelete -> confirmBulkDelete = false
            showBulkCategorySheet -> showBulkCategorySheet = false
            showBulkDiscountDialog -> showBulkDiscountDialog = false
            actionProduct != null -> actionProduct = null
            showSortSheet -> showSortSheet = false
            isSelectionMode -> {
                isSelectionMode = false
                selectedIds.clear()
            }
        }
    }

    fun loadProducts() {
        coroutineScope.launch {
            isLoading = products.isEmpty()
            val res = viewModel.repository.getSellerProducts()
            if (res.success && res.data != null) {
                products = res.data
                viewModel.cachedSellerProducts = res.data
            }
            val catRes = viewModel.repository.getCategories()
            if (catRes.success && catRes.data != null) {
                categories = catRes.data
                viewModel.cachedAllCategories = catRes.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadProducts()
    }

    fun toggleSelection(id: String) {
        if (!isSelectionMode) isSelectionMode = true
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
            if (selectedIds.isEmpty()) isSelectionMode = false
        } else {
            selectedIds.add(id)
        }
    }

    val tabs = remember(products, strings) {
        listOf(
            "ALL" to String.format(strings.sellerOps.allCountTemplate, products.size),
            "ACTIVE" to String.format(strings.sellerOps.activeCountTemplate, products.count { it.status == "ACTIVE" }),
            "PAUSED" to String.format(strings.sellerOps.pausedCountTemplate, products.count { it.status == "PAUSED" }),
            "DRAFT" to String.format(work.draftCountTemplate, products.count { it.status == "DRAFT" }),
            "ARCHIVED" to String.format(strings.sellerOps.archivedCountTemplate, products.count { it.status == "ARCHIVED" }),
            "LOW_STOCK" to String.format(strings.sellerOps.lowStockCountTemplate, products.count { it.stockQuantity in 0..5 })
        )
    }

    fun parseSoldNumeric(soldCount: String): Double {
        val raw = soldCount.lowercase(Locale.US).replace("sold", "").trim()
        return when {
            raw.endsWith("k") -> (raw.removeSuffix("k").toDoubleOrNull() ?: 0.0) * 1000.0
            else -> raw.toDoubleOrNull() ?: 0.0
        }
    }

    val filtered = remember(products, selectedTab, searchQuery, sortOption, lang) {
        val base = products.filter { prod ->
            val matchesStatus = when (selectedTab) {
                "ALL" -> true
                "ACTIVE" -> prod.status == "ACTIVE"
                "PAUSED" -> prod.status == "PAUSED"
                "DRAFT" -> prod.status == "DRAFT"
                "ARCHIVED" -> prod.status == "ARCHIVED"
                "LOW_STOCK" -> prod.stockQuantity in 0..5
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                prod.name.contains(searchQuery, ignoreCase = true) ||
                prod.localizedName(lang).contains(searchQuery, ignoreCase = true) ||
                prod.subCategory.contains(searchQuery, ignoreCase = true)
            matchesStatus && matchesQuery
        }
        when (sortOption) {
            "BEST_SELLING" -> base.sortedByDescending { parseSoldNumeric(it.soldCount) }
            "LOW_STOCK" -> base.sortedBy { it.stockQuantity }
            "PRICE_LOW_HIGH" -> base.sortedBy { it.price }
            else -> base.reversed()
        }
    }

    SuqiScreen(
        title = strings.seller.productsTabTitle,
        subtitle = String.format(strings.sellerOps.catalogItemsSubTemplate, products.size),
        onBackClick = {
            if (isSelectionMode) {
                isSelectionMode = false
                selectedIds.clear()
            } else {
                viewModel.goBack()
            }
        },
        isOffline = isOffline,
        onRetryOffline = {
            viewModel.setOfflineMode(false)
            loadProducts()
        },
        headerActions = {
            IconButton(
                onClick = { showSortSheet = true },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("products_sort_button")
            ) {
                Icon(LucideIcons.ArrowUpDown, contentDescription = strings.sortLabel, tint = colors.primaryText)
            }
            if (products.isNotEmpty()) {
                TextButton(
                    onClick = {
                        if (isSelectionMode) {
                            isSelectionMode = false
                            selectedIds.clear()
                        } else {
                            isSelectionMode = true
                        }
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("products_select_toggle")
                ) {
                    Text(
                        text = if (isSelectionMode) work.exitSelectLabel else work.bulkSelectToggle,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = SuqiPrimary
                    )
                }
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { viewModel.navigate(ScreenDestination.AddEditProduct()) },
                    containerColor = SuqiPrimary,
                    contentColor = Color.White,
                    modifier = Modifier
                        .padding(bottom = SuqiBottomNavContentPadding - 16.dp)
                        .testTag("seller_add_product_fab")
                ) {
                    Icon(LucideIcons.Plus, contentDescription = strings.seller.addProductTitle)
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = isSelectionMode && selectedIds.isNotEmpty()) {
                Surface(
                    color = colors.cardBackground,
                    shadowElevation = if (colors.isDark) 0.dp else 12.dp,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(
                            text = String.format(work.selectedCountTemplate, selectedIds.size),
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = colors.primaryText,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.repository.bulkUpdateProducts(selectedIds.toList(), "PAUSE")
                                            viewModel.showToast(strings.seller.statusPaused)
                                            isSelectionMode = false
                                            selectedIds.clear()
                                            loadProducts()
                                        }
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("products_bulk_pause")
                                ) { Text(work.pauseAction, fontSize = 12.sp) }
                            }
                            item {
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.repository.bulkUpdateProducts(selectedIds.toList(), "ARCHIVE")
                                            viewModel.showToast(strings.seller.statusArchived)
                                            isSelectionMode = false
                                            selectedIds.clear()
                                            loadProducts()
                                        }
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("products_bulk_archive")
                                ) { Text(work.bulkArchiveAction, fontSize = 12.sp) }
                            }
                            item {
                                OutlinedButton(
                                    onClick = { showBulkCategorySheet = true },
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("products_bulk_category")
                                ) { Text(work.bulkChangeCategoryAction, fontSize = 12.sp) }
                            }
                            item {
                                OutlinedButton(
                                    onClick = { showBulkDiscountDialog = true },
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("products_bulk_discount")
                                ) { Text(work.bulkApplyDiscountAction, fontSize = 12.sp) }
                            }
                            item {
                                OutlinedButton(
                                    onClick = { confirmBulkDelete = true },
                                    shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(1.dp, SuqiError.copy(alpha = 0.5f)),
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("products_bulk_delete")
                                ) { Text(work.bulkDeleteAction, fontSize = 12.sp, color = SuqiError) }
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuqiSearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = strings.sellerOps.catalogSearchHint,
                    testTag = "seller_products_search",
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.primaryTint)
                        .border(1.dp, SuqiPrimary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .clickable { viewModel.navigate(ScreenDestination.SellerInventory) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(LucideIcons.SlidersHorizontal, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = work.inventoryTitle.substringBefore(" "),
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = SuqiPrimary
                        )
                    }
                }
            }

            SuqiFilterChips(
                items = tabs,
                selectedKey = selectedTab,
                onSelect = { selectedTab = it },
                testTagPrefix = "seller_product_tab"
            )

            if (isLoading) {
                Box(modifier = Modifier.padding(top = 12.dp)) {
                    SuqiSkeletonList(count = 4)
                }
            } else if (filtered.isEmpty()) {
                SuqiEmptyState(
                    icon = LucideIcons.PackageOpen,
                    title = if (products.isEmpty()) strings.sellerOps.noProductsYetTitle else strings.buyer.noMatchingProducts,
                    subtitle = if (products.isEmpty()) strings.sellerOps.noProductsYetSub else strings.buyer.noMatchingProductsSub,
                    ctaText = if (products.isEmpty()) strings.sellerOps.addFirstProductBtn else strings.sellerOps.showAllProductsBtn,
                    onCtaClick = {
                        if (products.isEmpty()) viewModel.navigate(ScreenDestination.AddEditProduct())
                        else {
                            selectedTab = "ALL"
                            searchQuery = ""
                        }
                    },
                    testTag = "seller_products_empty"
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 10.dp,
                        bottom = SuqiBottomNavContentPadding + 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { prod ->
                        val prodTitle = prod.localizedName(lang)
                        val isSelected = selectedIds.contains(prod.id)
                        SuqiSectionCard(
                            padding = 12.dp,
                            containerColor = if (isSelected) colors.primaryTint else colors.cardBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) toggleSelection(prod.id)
                                        else viewModel.navigate(ScreenDestination.AddEditProduct(prod.id))
                                    },
                                    onLongClick = { toggleSelection(prod.id) }
                                )
                                .testTag("seller_product_card_${prod.id}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelectionMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { toggleSelection(prod.id) },
                                        colors = CheckboxDefaults.colors(checkedColor = SuqiPrimary),
                                        modifier = Modifier.testTag("product_check_${prod.id}")
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colors.imageBackdrop)
                                ) {
                                    AsyncImage(
                                        model = prod.primaryImageUrl,
                                        contentDescription = prodTitle,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prodTitle,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = colors.primaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = formatEtb(prod.price),
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = SuqiPrimary
                                        )
                                        if (prod.discountPercent > 0) {
                                            SuqiStatusBadge("-${prod.discountPercent}%", StatusType.WARNING)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val statusLabel = when (prod.status) {
                                            "ACTIVE" -> strings.seller.statusActive
                                            "PAUSED" -> strings.seller.statusPaused
                                            "ARCHIVED" -> strings.seller.statusArchived
                                            "DRAFT" -> strings.seller.statusDraft
                                            else -> prod.status
                                        }
                                        SuqiStatusBadge(
                                            statusText = statusLabel,
                                            type = when (prod.status) {
                                                "ACTIVE" -> StatusType.SUCCESS
                                                "PAUSED" -> StatusType.WARNING
                                                else -> StatusType.NEUTRAL
                                            }
                                        )
                                        Text(
                                            text = String.format(strings.sellerOps.stockPrefixTemplate, prod.stockQuantity),
                                            fontFamily = SuqiFontFamily,
                                            fontSize = 12.sp,
                                            color = if (prod.stockQuantity <= 5) SuqiWarning else colors.secondaryText
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { actionProduct = prod },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("product_actions_${prod.id}")
                                ) {
                                    Icon(
                                        imageVector = LucideIcons.MoreVertical,
                                        contentDescription = prodTitle,
                                        tint = colors.primaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    actionProduct?.let { target ->
        val targetTitle = target.localizedName(lang)
        SuqiBottomSheet(
            onDismissRequest = { actionProduct = null },
            title = targetTitle
        ) {
            SuqiListRow(
                icon = LucideIcons.Pencil,
                title = strings.sellerOps.editProductAction,
                subtitle = strings.sellerOps.editProductActionSub,
                onClick = {
                    actionProduct = null
                    viewModel.navigate(ScreenDestination.AddEditProduct(target.id))
                },
                testTag = "product_sheet_edit"
            )
            SuqiListRow(
                icon = LucideIcons.Copy,
                title = work.duplicateAction,
                subtitle = work.duplicateActionSub,
                onClick = {
                    actionProduct = null
                    coroutineScope.launch {
                        val res = viewModel.repository.duplicateProduct(target.id)
                        viewModel.showToast(res.message ?: targetTitle)
                        loadProducts()
                    }
                },
                testTag = "product_sheet_duplicate"
            )
            SuqiListRow(
                icon = LucideIcons.Share2,
                title = work.shareListingAction,
                subtitle = work.shareListingActionSub,
                onClick = {
                    actionProduct = null
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, targetTitle)
                        putExtra(Intent.EXTRA_TEXT, "$targetTitle - ${formatEtb(target.price)}\nhttps://suqi.et/p/${target.id}")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, work.shareListingAction))
                },
                testTag = "product_sheet_share"
            )
            SuqiListRow(
                icon = if (target.status == "ACTIVE") LucideIcons.Pause else LucideIcons.CheckCircle,
                title = if (target.status == "ACTIVE") strings.sellerOps.pauseListingAction else strings.sellerOps.activateListingAction,
                subtitle = if (target.status == "ACTIVE") strings.sellerOps.pauseListingActionSub else strings.sellerOps.activateListingActionSub,
                onClick = {
                    val nextStatus = if (target.status == "ACTIVE") "PAUSED" else "ACTIVE"
                    actionProduct = null
                    coroutineScope.launch {
                        val res = viewModel.repository.updateProductStatus(target.id, nextStatus)
                        viewModel.showToast(if (res.success) targetTitle else strings.common.failedToAddToast)
                        loadProducts()
                    }
                },
                testTag = "product_sheet_toggle"
            )
            if (target.status != "ARCHIVED") {
                SuqiListRow(
                    icon = LucideIcons.Package,
                    title = strings.sellerOps.archiveListingAction,
                    subtitle = strings.sellerOps.archiveListingActionSub,
                    onClick = {
                        actionProduct = null
                        coroutineScope.launch {
                            viewModel.repository.updateProductStatus(target.id, "ARCHIVED")
                            viewModel.showToast(targetTitle)
                            loadProducts()
                        }
                    },
                    testTag = "product_sheet_archive"
                )
            }
            SuqiListRow(
                icon = LucideIcons.Eye,
                title = strings.sellerOps.previewAsBuyerAction,
                subtitle = strings.sellerOps.previewAsBuyerActionSub,
                onClick = {
                    actionProduct = null
                    viewModel.navigate(ScreenDestination.ProductDetails(target.id))
                },
                testTag = "product_sheet_preview"
            )
            SuqiListRow(
                icon = LucideIcons.Trash2,
                title = strings.sellerOps.deleteProductAction,
                subtitle = strings.sellerOps.deleteProductActionSub,
                iconTint = SuqiError,
                iconBackground = colors.errorBg,
                onClick = {
                    actionProduct = null
                    confirmDeleteProduct = target
                },
                testTag = "product_sheet_delete"
            )
        }
    }

    if (showSortSheet) {
        val sortChoices = listOf(
            "NEWEST" to work.sortNewest,
            "BEST_SELLING" to work.sortBestSelling,
            "LOW_STOCK" to work.sortLowStock,
            "PRICE_LOW_HIGH" to work.sortPriceLowHigh
        )
        SuqiBottomSheet(onDismissRequest = { showSortSheet = false }, title = work.sortSheetTitle) {
            sortChoices.forEach { (key, label) ->
                SuqiListRow(
                    icon = if (sortOption == key) LucideIcons.CheckCircle else LucideIcons.ArrowUpDown,
                    title = label,
                    iconTint = if (sortOption == key) SuqiPrimary else colors.secondaryText,
                    onClick = {
                        sortOption = key
                        showSortSheet = false
                    },
                    testTag = "sort_option_$key"
                )
            }
        }
    }

    if (showBulkCategorySheet) {
        SuqiBottomSheet(onDismissRequest = { showBulkCategorySheet = false }, title = work.bulkChangeCategoryAction) {
            categories.forEach { cat ->
                SuqiListRow(
                    icon = LucideIcons.Tag,
                    title = cat.localizedName(lang),
                    onClick = {
                        showBulkCategorySheet = false
                        coroutineScope.launch {
                            viewModel.repository.bulkUpdateProducts(selectedIds.toList(), "CHANGE_CATEGORY", cat.id)
                            isSelectionMode = false
                            selectedIds.clear()
                            loadProducts()
                        }
                    }
                )
            }
        }
    }

    if (showBulkDiscountDialog) {
        AlertDialog(
            onDismissRequest = { showBulkDiscountDialog = false },
            title = { Text(work.bulkApplyDiscountAction, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                SuqiTextField(
                    value = bulkDiscountInput,
                    onValueChange = { bulkDiscountInput = it.filter(Char::isDigit) },
                    label = work.discountPercentLabel
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pct = (bulkDiscountInput.toIntOrNull() ?: 0).coerceIn(0, 90)
                        showBulkDiscountDialog = false
                        coroutineScope.launch {
                            viewModel.repository.bulkUpdateProducts(selectedIds.toList(), "APPLY_DISCOUNT", pct.toString())
                            isSelectionMode = false
                            selectedIds.clear()
                            loadProducts()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(strings.saveLabel, color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDiscountDialog = false }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(strings.cancelLabel)
                }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (confirmBulkDelete) {
        AlertDialog(
            onDismissRequest = { confirmBulkDelete = false },
            title = { Text(work.confirmBulkDeleteTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold) },
            text = { Text(String.format(work.confirmBulkDeleteBodyTemplate, selectedIds.size), fontFamily = SuqiFontFamily, color = colors.secondaryText) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmBulkDelete = false
                        coroutineScope.launch {
                            viewModel.repository.bulkUpdateProducts(selectedIds.toList(), "DELETE")
                            isSelectionMode = false
                            selectedIds.clear()
                            loadProducts()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiError),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(strings.deleteLabel, color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { confirmBulkDelete = false }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(strings.cancelLabel)
                }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    confirmDeleteProduct?.let { target ->
        val targetTitle = target.localizedName(lang)
        AlertDialog(
            onDismissRequest = { confirmDeleteProduct = null },
            title = { Text(strings.sellerOps.deleteProductDialogTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, color = colors.primaryText) },
            text = {
                Text(
                    String.format(strings.sellerOps.deleteProductDialogBodyTemplate, targetTitle),
                    fontFamily = SuqiFontFamily,
                    color = colors.secondaryText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteProduct = null
                        coroutineScope.launch {
                            val res = viewModel.repository.deleteProduct(target.id)
                            viewModel.showToast(if (res.success) strings.deleteLabel else (res.message ?: strings.deleteLabel))
                            loadProducts()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiError),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(strings.deleteLabel, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmDeleteProduct = null },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(strings.cancelLabel)
                }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun AddEditProductScreen(
    productId: String? = null,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefsStore = remember(context) { SellerPreferencesStore(context) }
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val work = strings.sellerWork
    val lang = LocalSuqiLanguage.current
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf("cat_electronics") }
    var categories by remember(lang) {
        mutableStateOf(
            MarketplaceCategories.all.map { it.id to it.toDto().localizedName(lang) }
        )
    }
    var priceStr by remember { mutableStateOf("") }
    var discountStr by remember { mutableStateOf("0") }
    var stockStr by remember { mutableStateOf("10") }
    var stockHold by remember { mutableStateOf(true) }
    var description by remember { mutableStateOf("") }

    var selectedTax by remember { mutableStateOf("15% Standard VAT Included") }
    val taxOptions = remember { listOf("15% Standard VAT Included", "0% Exempt / Turn-Over Tax (TOT)", "Custom (5%)") }

    val media = remember { mutableStateListOf<MediaItem>() }
    val form = remember { ProductFormState() }
    var showMissingPaymentDialog by remember { mutableStateOf(false) }

    var showMultilingual by remember { mutableStateOf(false) }
    var nameAmharic by remember { mutableStateOf("") }
    var descAmharic by remember { mutableStateOf("") }
    var nameOromo by remember { mutableStateOf("") }
    var descOromo by remember { mutableStateOf("") }

    var pendingDraft by remember { mutableStateOf<ProductDraftData?>(null) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showBuyerPreview by remember { mutableStateOf(false) }
    var initialFingerprint by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }

    fun currentFingerprint(): String =
        listOf(name, categoryId, priceStr, discountStr, stockStr, stockHold.toString(), description, nameAmharic, descAmharic, nameOromo, descOromo, selectedTax, form.toDetails(media.toList()).toJson()).joinToString("|")

    val hasUnsavedChanges = initialized && currentFingerprint() != initialFingerprint

    BackHandler(enabled = showBuyerPreview || hasUnsavedChanges) {
        if (showBuyerPreview) {
            showBuyerPreview = false
        } else if (hasUnsavedChanges) {
            showUnsavedDialog = true
        }
    }

    LaunchedEffect(productId, lang) {
        val catRes = viewModel.repository.getCategories()
        if (catRes.success && !catRes.data.isNullOrEmpty()) {
            categories = catRes.data.filter { it.isActive }.map { it.id to it.localizedName(lang) }
        }
        if (productId != null) {
            val res = viewModel.repository.getProductDetails(productId)
            val p = res.data
            if (res.success && p != null) {
                name = p.name
                categoryId = p.categoryId
                priceStr = if (p.price % 1.0 == 0.0) p.price.toLong().toString() else p.price.toString()
                discountStr = p.discountPercent.toString()
                stockStr = p.stockQuantity.toString()
                stockHold = p.stockHoldEnabled
                description = p.description
                nameAmharic = p.nameAmharic
                nameOromo = p.nameAfaanOromo
                descAmharic = p.descriptionAmharic
                descOromo = p.descriptionAfaanOromo
                media.clear()
                media.addAll(p.details.media)
                form.load(p.details)
            }
        } else {
            val saved = prefsStore.loadProductDraft()
            if (saved != null && (saved.name.isNotBlank() || saved.priceStr.isNotBlank() || saved.description.isNotBlank())) {
                pendingDraft = saved
            }
        }
        initialFingerprint = currentFingerprint()
        initialized = true
    }

    // Autosave draft whenever editing a new listing
    LaunchedEffect(
        initialized, productId, name, categoryId, priceStr, discountStr, stockStr,
        stockHold, selectedTax, description, nameAmharic, descAmharic, nameOromo, descOromo,
        media.size, form.variants.size, form.itemType
    ) {
        if (initialized && productId == null && currentFingerprint() != initialFingerprint) {
            val nowLabel = SimpleDateFormat("HH:mm", Locale.US).format(Date())
            prefsStore.saveProductDraft(
                ProductDraftData(
                    name = name,
                    nameAmharic = nameAmharic,
                    nameAfaanOromo = nameOromo,
                    categoryId = categoryId,
                    priceStr = priceStr,
                    discountStr = discountStr,
                    stockStr = stockStr,
                    stockHold = stockHold,
                    selectedTax = selectedTax,
                    description = description,
                    descAmharic = descAmharic,
                    descOromo = descOromo,
                    detailsJson = form.toDetails(media.toList()).toJson(),
                    savedAtLabel = nowLabel
                )
            )
        }
    }

    fun submitListing(targetStatus: String) {
        coroutineScope.launch {
            if (productId == null && targetStatus == "ACTIVE") {
                val activeShopId = resolveActiveSellerShopId(viewModel)
                val payRes = viewModel.repository.getShopPaymentMethods(activeShopId)
                if (activeShopId.isBlank() || (payRes.success && payRes.data.orEmpty().none { it.isEnabled })) {
                    showMissingPaymentDialog = true
                    return@launch
                }
            }

            val p = priceStr.toDoubleOrNull() ?: if (targetStatus == "DRAFT") 1.0 else 0.0
            val d = discountStr.toIntOrNull() ?: 0
            val s = stockStr.toIntOrNull() ?: 0
            if (targetStatus == "ACTIVE") {
                when {
                    media.none { !it.isVideo } -> { viewModel.showToast(strings.extras.toastAddCoverPhoto); return@launch }
                    name.isBlank() -> { viewModel.showToast(strings.extras.toastEnterProductName); return@launch }
                    p <= 0.0 -> { viewModel.showToast(strings.extras.toastEnterValidPrice); return@launch }
                    d !in 0..90 -> { viewModel.showToast(strings.extras.toastDiscountRange); return@launch }
                    description.isBlank() -> { viewModel.showToast(strings.extras.toastEnterDescription); return@launch }
                }
                form.missingRequired()?.let {
                    viewModel.showToast(String.format(strings.extras.toastFieldRequiredTemplate, it.label))
                    return@launch
                }
            } else if (name.isBlank()) {
                viewModel.showToast(strings.extras.toastEnterProductName)
                return@launch
            }

            val payload = mapOf<String, Any>(
                "name" to name.trim(),
                "nameAmharic" to nameAmharic,
                "nameAfaanOromo" to nameOromo,
                "price" to maxOf(1.0, p),
                "discountPercent" to d.coerceIn(0, 90),
                "stockQuantity" to s.coerceAtLeast(0),
                "stockHoldEnabled" to stockHold,
                "categoryId" to categoryId,
                "tax" to selectedTax,
                "description" to description.ifBlank { "Draft product" },
                "descriptionAmharic" to descAmharic,
                "descriptionAfaanOromo" to descOromo,
                "status" to targetStatus,
                "detailsJson" to form.toDetails(media.toList()).toJson()
            )
            val res = if (productId == null) {
                viewModel.repository.createProduct(payload)
            } else {
                viewModel.repository.updateProduct(productId, payload)
            }
            if (res.success) {
                if (productId == null) prefsStore.clearProductDraft()
                val msg = when {
                    targetStatus == "DRAFT" -> work.draftSavedToast
                    productId == null -> strings.extras.toastProductSaved
                    else -> strings.extras.toastProductUpdated
                }
                viewModel.showToast(msg)
                viewModel.goBack()
            } else {
                viewModel.showToast(res.message ?: strings.extras.toastProductSaveFailed)
            }
        }
    }

    val mediaDone = media.any { !it.isVideo }
    val detailsDone = name.isNotBlank() && description.isNotBlank()
    val priceDone = (priceStr.toDoubleOrNull() ?: 0.0) > 0.0
    val variantsDone = form.variants.isNotEmpty()

    SuqiScreen(
        title = if (productId == null) strings.seller.addProductTitle else strings.seller.editProductTitle,
        onBackClick = {
            if (hasUnsavedChanges) showUnsavedDialog = true else viewModel.goBack()
        },
        isOffline = isOffline,
        headerActions = {
            TextButton(
                onClick = { showBuyerPreview = true },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("preview_as_buyer_btn")
            ) {
                Icon(LucideIcons.Eye, contentDescription = work.previewAsBuyerBtn, tint = SuqiPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(work.previewAsBuyerBtn, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SuqiPrimary)
            }
        },
        bottomBar = {
            Surface(
                color = colors.cardBackground,
                shadowElevation = if (colors.isDark) 0.dp else 12.dp,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { submitListing("DRAFT") },
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("save_draft_button")
                    ) {
                        Text(work.saveAsDraftBtn, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    SuqiButton(
                        text = if (productId == null) strings.seller.publishProductBtn else strings.seller.saveChangesBtn,
                        onClick = { submitListing("ACTIVE") },
                        testTag = "save_product_button",
                        modifier = Modifier.weight(1.4f)
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Draft autosave resume banner
            pendingDraft?.let { draft ->
                item {
                    SuqiSectionCard(
                        padding = 12.dp,
                        containerColor = colors.primaryTint,
                        modifier = Modifier.testTag("draft_autosave_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(LucideIcons.History, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format(work.draftSavedBannerTemplate, draft.savedAtLabel.ifBlank { "10:42" }),
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = colors.primaryText,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    name = draft.name
                                    nameAmharic = draft.nameAmharic
                                    nameOromo = draft.nameAfaanOromo
                                    categoryId = draft.categoryId
                                    priceStr = draft.priceStr
                                    discountStr = draft.discountStr
                                    stockStr = draft.stockStr
                                    stockHold = draft.stockHold
                                    selectedTax = draft.selectedTax
                                    description = draft.description
                                    descAmharic = draft.descAmharic
                                    descOromo = draft.descOromo
                                    parseProductDetails(draft.detailsJson)?.let { parsed ->
                                        media.clear()
                                        media.addAll(parsed.media)
                                        form.load(parsed)
                                    }
                                    pendingDraft = null
                                },
                                modifier = Modifier.testTag("draft_resume_btn")
                            ) {
                                Text(work.resumeDraftBtn, fontWeight = FontWeight.Bold, color = SuqiPrimary, fontSize = 12.sp)
                            }
                            TextButton(
                                onClick = {
                                    prefsStore.clearProductDraft()
                                    pendingDraft = null
                                },
                                modifier = Modifier.testTag("draft_discard_btn")
                            ) {
                                Text(work.discardDraftBtn, color = colors.secondaryText, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Per-section completeness chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("product_completeness_row")
                ) {
                    val chips = listOf(
                        Triple("media", work.completenessMedia, mediaDone),
                        Triple("details", work.completenessDetails, detailsDone),
                        Triple("price", work.completenessPrice, priceDone),
                        Triple("variants", work.completenessVariants, variantsDone)
                    )
                    items(chips) { (key, label, done) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (done) colors.successBg else colors.secondaryBackground)
                                .border(1.dp, if (done) SuqiSuccess.copy(alpha = 0.45f) else colors.border, RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("completeness_chip_$key")
                        ) {
                            Icon(
                                imageVector = if (done) LucideIcons.Check else LucideIcons.CirclePlus,
                                contentDescription = null,
                                tint = if (done) SuqiSuccess else colors.mutedText,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = if (done) SuqiSuccess else colors.secondaryText
                            )
                        }
                    }
                }
            }

            item {
                SuqiMediaEditor(
                    media = media,
                    onMessage = { viewModel.showToast(it) }
                )
            }

            item {
                ItemTypeSelector(
                    state = form,
                    onTypeChosen = { type ->
                        form.applyItemType(type)
                        if (type.categoryId.isNotBlank()) categoryId = type.categoryId
                    }
                )
            }

            item {
                Text(
                    text = "${strings.buyer.categoryLabel} *",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(8.dp))
                SuqiFilterChips(
                    items = categories,
                    selectedKey = categoryId,
                    onSelect = { categoryId = it },
                    testTagPrefix = "product_cat_chip"
                )
            }

            item {
                SuqiSectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SuqiTextField(value = name, onValueChange = { name = it }, label = "${strings.seller.paramNameRequiredLabel} (EN)")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SuqiTextField(
                                value = priceStr,
                                onValueChange = { priceStr = it },
                                label = "${strings.seller.priceEtbLabel} *",
                                modifier = Modifier.weight(1f)
                            )
                            SuqiTextField(
                                value = discountStr,
                                onValueChange = { discountStr = it },
                                label = strings.discountLabel,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Column {
                            Text(
                                text = strings.extras.vatLabel,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = colors.primaryText
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(taxOptions) { opt ->
                                    val isSel = opt == selectedTax
                                    Box(
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) colors.primaryTint else colors.secondaryBackground)
                                            .border(1.dp, if (isSel) SuqiPrimary else colors.border, RoundedCornerShape(12.dp))
                                            .clickable { selectedTax = opt }
                                            .padding(horizontal = 12.dp, vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = opt,
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                                            fontSize = 12.sp,
                                            color = if (isSel) SuqiPrimary else colors.primaryText
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SuqiTextField(
                                value = stockStr,
                                onValueChange = { stockStr = it },
                                label = "${strings.seller.stockLabel} *",
                                modifier = Modifier.weight(1f)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.seller.stockLabel,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = colors.primaryText
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = stockHold,
                                        onCheckedChange = { stockHold = it },
                                        colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (stockHold) strings.common.yesLabel else strings.common.noLabel,
                                        fontFamily = SuqiFontFamily,
                                        fontSize = 12.sp,
                                        color = colors.secondaryText
                                    )
                                }
                            }
                        }

                        SuqiTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = "${strings.description} (EN) *",
                            singleLine = false
                        )
                    }
                }
            }

            item { ParametersEditor(state = form) }
            item {
                VariantsEditor(
                    state = form,
                    media = media,
                    onMessage = { viewModel.showToast(it) }
                )
            }

            item {
                SuqiSectionCard(containerColor = colors.secondaryBackground) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { showMultilingual = !showMultilingual },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(LucideIcons.Sparkles, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.sellerOps.bilingualInfoTitle,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = colors.primaryText
                            )
                        }
                        Icon(
                            imageVector = LucideIcons.ChevronDown,
                            contentDescription = strings.sellerOps.bilingualInfoTitle,
                            tint = colors.secondaryText,
                            modifier = Modifier.graphicsLayer { rotationZ = if (showMultilingual) 180f else 0f }
                        )
                    }

                    if (showMultilingual) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(strings.sellerOps.amharicSectionLabel, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuqiPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        SuqiTextField(value = nameAmharic, onValueChange = { nameAmharic = it }, label = strings.seller.paramNameRequiredLabel)
                        Spacer(modifier = Modifier.height(8.dp))
                        SuqiTextField(value = descAmharic, onValueChange = { descAmharic = it }, label = strings.description, singleLine = false)

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(strings.sellerOps.afaanOromoSectionLabel, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuqiPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        SuqiTextField(value = nameOromo, onValueChange = { nameOromo = it }, label = strings.seller.paramNameRequiredLabel)
                        Spacer(modifier = Modifier.height(8.dp))
                        SuqiTextField(value = descOromo, onValueChange = { descOromo = it }, label = strings.description, singleLine = false)
                    }
                }
            }
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text(work.unsavedChangesTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, color = colors.primaryText) },
            text = { Text(work.unsavedChangesBody, fontFamily = SuqiFontFamily, color = colors.secondaryText) },
            confirmButton = {
                Button(
                    onClick = {
                        showUnsavedDialog = false
                        viewModel.goBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiError),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(work.discardChangesBtn, color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(work.keepEditingBtn)
                }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showBuyerPreview) {
        val previewDetails = form.toDetails(media.toList())
        val previewPrice = priceStr.toDoubleOrNull() ?: 0.0
        val previewDiscount = discountStr.toIntOrNull()?.coerceIn(0, 90) ?: 0
        val previewOrigPrice = if (previewDiscount > 0 && previewPrice > 0.0) {
            previewPrice / (1.0 - previewDiscount / 100.0)
        } else previewPrice
        val previewStock = stockStr.toIntOrNull() ?: 0

        ModalBottomSheet(
            onDismissRequest = { showBuyerPreview = false },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.testTag("buyer_preview_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(work.previewBuyerModalTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.primaryText)
                        Text(work.previewUnsavedNote, fontFamily = SuqiFontFamily, fontSize = 11.sp, color = colors.secondaryText)
                    }
                    SuqiStatusBadge(work.previewAsBuyerBtn, StatusType.PRIMARY)
                }

                if (previewDetails.media.isNotEmpty()) {
                    SuqiMediaGallery(
                        media = previewDetails.media,
                        contentDescription = name.ifBlank { work.previewBuyerModalTitle },
                        height = 220.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.imageBackdrop),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(LucideIcons.PackageOpen, contentDescription = null, tint = colors.mutedText, modifier = Modifier.size(36.dp))
                    }
                }

                Text(
                    text = name.ifBlank { strings.seller.paramNameRequiredLabel },
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.primaryText
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = formatEtb(previewPrice),
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = SuqiPrimary
                    )
                    if (previewDiscount > 0) {
                        Text(
                            text = formatEtb(previewOrigPrice),
                            fontFamily = SuqiFontFamily,
                            fontSize = 13.sp,
                            color = colors.mutedText,
                            textDecoration = TextDecoration.LineThrough
                        )
                        SuqiStatusBadge("-$previewDiscount%", StatusType.WARNING)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    SuqiStatusBadge(
                        statusText = if (previewStock > 0) strings.inStock else strings.outOfStock,
                        type = if (previewStock > 0) StatusType.SUCCESS else StatusType.ERROR
                    )
                }

                if (previewDetails.buyerOptions.isNotEmpty()) {
                    previewDetails.buyerOptions.forEach { opt ->
                        Text(opt.label, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = colors.secondaryText)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            previewDetails.valuesOf(opt.key).forEachIndexed { idx, v ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (idx == 0) colors.primaryTint else colors.secondaryBackground)
                                        .border(1.dp, if (idx == 0) SuqiPrimary else colors.border, RoundedCornerShape(999.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(v, fontSize = 12.sp, color = if (idx == 0) SuqiPrimary else colors.primaryText)
                                }
                            }
                        }
                    }
                }

                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        fontFamily = SuqiFontFamily,
                        fontSize = 13.sp,
                        color = colors.secondaryText,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showMissingPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showMissingPaymentDialog = false },
            icon = { Icon(LucideIcons.ShieldCheck, contentDescription = null, tint = SuqiPrimary) },
            title = { Text(strings.sellerOps.paymentMethodRequiredTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, color = colors.primaryText) },
            text = {
                Text(
                    strings.sellerOps.paymentMethodRequiredBody,
                    fontFamily = SuqiFontFamily,
                    fontSize = 14.sp,
                    color = colors.secondaryText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMissingPaymentDialog = false
                        viewModel.navigate(ScreenDestination.PaymentMethodsSetup)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(strings.sellerOps.setupPaymentMethodsBtn, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showMissingPaymentDialog = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(strings.cancelLabel, fontFamily = SuqiFontFamily)
                }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
