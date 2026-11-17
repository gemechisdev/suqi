@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.example.ui.screens

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ProductDetails
import com.example.data.model.ProductDto
import com.example.data.model.toJson
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.LocalSuqiLanguage
import com.example.ui.i18n.localizedName
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SellerInventoryScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val work = strings.sellerWork
    val lang = LocalSuqiLanguage.current
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var products by remember { mutableStateOf(viewModel.cachedSellerProducts) }
    var isLoading by remember { mutableStateOf(products.isEmpty()) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("ALL") }
    val expandedIds = remember { mutableStateMapOf<String, Boolean>() }
    val selectedIds = remember { mutableStateListOf<String>() }
    var isSelectionMode by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    var showBulkStockDialog by remember { mutableStateOf(false) }
    var bulkStockInput by remember { mutableStateOf("10") }

    // Variant inline edit modal
    var editingVariantTarget by remember { mutableStateOf<Pair<ProductDto, Int>?>(null) }
    var variantSkuInput by remember { mutableStateOf("") }
    var variantStockInput by remember { mutableStateOf("") }
    var variantPriceInput by remember { mutableStateOf("") }

    BackHandler(enabled = showBulkStockDialog || editingVariantTarget != null || isSelectionMode) {
        when {
            showBulkStockDialog -> showBulkStockDialog = false
            editingVariantTarget != null -> editingVariantTarget = null
            isSelectionMode -> {
                isSelectionMode = false
                selectedIds.clear()
            }
        }
    }

    fun loadInventory() {
        coroutineScope.launch {
            isLoading = products.isEmpty()
            val res = viewModel.repository.getSellerProducts()
            if (res.success && res.data != null) {
                products = res.data
                viewModel.cachedSellerProducts = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadInventory()
    }

    fun toggleSelect(id: String) {
        if (!isSelectionMode) isSelectionMode = true
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
            if (selectedIds.isEmpty()) isSelectionMode = false
        } else {
            selectedIds.add(id)
        }
    }

    fun updateSingleProductStock(prod: ProductDto, nextStock: Int) {
        val clamped = nextStock.coerceAtLeast(0)
        coroutineScope.launch {
            val res = viewModel.repository.updateProductStock(prod.id, clamped)
            if (res.success && res.data != null) {
                products = products.map { if (it.id == prod.id) res.data else it }
            } else {
                viewModel.showToast(res.message ?: strings.common.failedToAddToast)
            }
        }
    }

    fun updateVariantStock(prod: ProductDto, variantIndex: Int, delta: Int) {
        val variants = prod.details.variants.toMutableList()
        if (variantIndex !in variants.indices) return
        val current = variants[variantIndex]
        val nextVal = ((current.stock ?: 0) + delta).coerceAtLeast(0)
        variants[variantIndex] = current.copy(stock = nextVal)
        val updatedDetails = prod.details.copy(variants = variants)
        val computedTotal = variants.sumOf { (it.stock ?: 0).coerceAtLeast(0) }
        coroutineScope.launch {
            val res = viewModel.repository.updateProductStock(
                id = prod.id,
                stockQuantity = computedTotal,
                detailsJson = updatedDetails.toJson()
            )
            if (res.success && res.data != null) {
                products = products.map { if (it.id == prod.id) res.data else it }
            }
        }
    }

    val lowStockCount = remember(products) { products.count { it.stockQuantity in 1..5 } }
    val outOfStockCount = remember(products) { products.count { it.stockQuantity <= 0 } }
    val pausedCount = remember(products) { products.count { it.status == "PAUSED" } }

    val filterItems = remember(products, strings) {
        listOf(
            "ALL" to "${work.filterAll} (${products.size})",
            "LOW_STOCK" to "${work.filterLowStock} ($lowStockCount)",
            "OUT_OF_STOCK" to "${work.filterOutOfStock} ($outOfStockCount)",
            "PAUSED" to "${work.filterPaused} ($pausedCount)"
        )
    }

    val filtered = remember(products, searchQuery, selectedFilter, lang) {
        products.filter { prod ->
            val matchesFilter = when (selectedFilter) {
                "LOW_STOCK" -> prod.stockQuantity in 1..5
                "OUT_OF_STOCK" -> prod.stockQuantity <= 0
                "PAUSED" -> prod.status == "PAUSED"
                else -> true
            }
            val q = searchQuery.trim()
            val matchesQuery = q.isBlank() ||
                prod.name.contains(q, ignoreCase = true) ||
                prod.localizedName(lang).contains(q, ignoreCase = true) ||
                prod.details.variants.any { it.sku.contains(q, ignoreCase = true) }
            matchesFilter && matchesQuery
        }
    }

    SuqiScreen(
        title = work.inventoryTitle,
        subtitle = String.format(work.inventorySubTemplate, products.size, lowStockCount),
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
            loadInventory()
        },
        headerActions = {
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
                        .testTag("inventory_select_toggle")
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
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = String.format(work.selectedCountTemplate, selectedIds.size),
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showBulkStockDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("inventory_bulk_set_stock")
                            ) {
                                Text(work.setStockAction, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val res = viewModel.repository.bulkUpdateProducts(selectedIds.toList(), "PAUSE")
                                        if (res.success) {
                                            viewModel.showToast(work.stockSavedToast)
                                            isSelectionMode = false
                                            selectedIds.clear()
                                            loadInventory()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("inventory_bulk_pause")
                            ) {
                                Text(work.pauseAction, fontSize = 12.sp, color = colors.primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val res = viewModel.repository.bulkUpdateProducts(selectedIds.toList(), "OUT_OF_STOCK")
                                        if (res.success) {
                                            viewModel.showToast(work.stockSavedToast)
                                            isSelectionMode = false
                                            selectedIds.clear()
                                            loadInventory()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, SuqiError.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("inventory_bulk_out_of_stock")
                            ) {
                                Text(work.markOutOfStockAction, fontSize = 12.sp, color = SuqiError, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
            SuqiSearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = work.inventorySearchHint,
                testTag = "inventory_search",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            SuqiFilterChips(
                items = filterItems,
                selectedKey = selectedFilter,
                onSelect = { selectedFilter = it },
                testTagPrefix = "inventory_filter"
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
                        if (products.isEmpty()) {
                            viewModel.navigate(ScreenDestination.AddEditProduct())
                        } else {
                            selectedFilter = "ALL"
                            searchQuery = ""
                        }
                    },
                    testTag = "inventory_empty"
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { prod ->
                        val title = prod.localizedName(lang)
                        val variants = prod.details.variants
                        val hasVariants = variants.isNotEmpty()
                        val isExpanded = expandedIds[prod.id] == true
                        val isSelected = selectedIds.contains(prod.id)

                        SuqiSwipeRow(
                            onSwipeRightAction = {
                                val nextStatus = if (prod.status == "ACTIVE") "PAUSED" else "ACTIVE"
                                coroutineScope.launch {
                                    val res = viewModel.repository.updateProductStatus(prod.id, nextStatus)
                                    if (res.success) {
                                        viewModel.showToast(title)
                                        loadInventory()
                                    }
                                }
                            },
                            rightActionLabel = if (prod.status == "ACTIVE") work.quickPauseSwipeLabel else strings.sellerOps.activateListingAction
                        ) {
                            SuqiSectionCard(
                                padding = 12.dp,
                                containerColor = if (isSelected) colors.primaryTint else colors.cardBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (isSelectionMode) {
                                                toggleSelect(prod.id)
                                            } else if (hasVariants) {
                                                expandedIds[prod.id] = !isExpanded
                                            } else {
                                                viewModel.navigate(ScreenDestination.AddEditProduct(prod.id))
                                            }
                                        },
                                        onLongClick = { toggleSelect(prod.id) }
                                    )
                                    .testTag("inventory_card_${prod.id}")
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isSelectionMode) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { toggleSelect(prod.id) },
                                                colors = CheckboxDefaults.colors(checkedColor = SuqiPrimary),
                                                modifier = Modifier.testTag("inventory_check_${prod.id}")
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(colors.imageBackdrop)
                                        ) {
                                            AsyncImage(
                                                model = prod.primaryImageUrl,
                                                contentDescription = title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = title,
                                                fontFamily = SuqiFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = colors.primaryText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .clickable { viewModel.navigate(ScreenDestination.AddEditProduct(prod.id)) }
                                                    .testTag("inventory_name_${prod.id}")
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (hasVariants) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(999.dp))
                                                            .background(colors.primaryTint)
                                                            .clickable { expandedIds[prod.id] = !isExpanded }
                                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                                            .testTag("inventory_variants_badge_${prod.id}")
                                                    ) {
                                                        Text(
                                                            text = String.format(work.variantsCountTemplate, variants.size),
                                                            fontFamily = SuqiFontFamily,
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 11.sp,
                                                            color = SuqiPrimary
                                                        )
                                                    }
                                                }
                                                when {
                                                    prod.stockQuantity <= 0 -> SuqiStatusBadge(work.outOfStockBadge, StatusType.ERROR)
                                                    prod.stockQuantity in 1..5 -> SuqiStatusBadge(work.lowStockBadge, StatusType.WARNING)
                                                }
                                                if (prod.status == "PAUSED") {
                                                    SuqiStatusBadge(strings.seller.statusPaused, StatusType.NEUTRAL)
                                                }
                                            }
                                        }

                                        // Inline Stock Stepper
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            IconButton(
                                                onClick = { updateSingleProductStock(prod, prod.stockQuantity - 1) },
                                                enabled = prod.stockQuantity > 0 && !hasVariants,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("inventory_dec_${prod.id}")
                                            ) {
                                                Icon(
                                                    LucideIcons.Minus,
                                                    contentDescription = strings.common.decreaseQtyDesc,
                                                    tint = if (prod.stockQuantity > 0 && !hasVariants) colors.primaryText else colors.mutedText,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Text(
                                                text = prod.stockQuantity.toString(),
                                                fontFamily = SuqiFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (prod.stockQuantity <= 5) SuqiWarning else colors.primaryText,
                                                modifier = Modifier
                                                    .widthIn(min = 24.dp)
                                                    .testTag("inventory_stock_${prod.id}")
                                            )
                                            IconButton(
                                                onClick = { updateSingleProductStock(prod, prod.stockQuantity + 1) },
                                                enabled = !hasVariants,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("inventory_inc_${prod.id}")
                                            ) {
                                                Icon(
                                                    LucideIcons.Plus,
                                                    contentDescription = strings.common.increaseQtyDesc,
                                                    tint = if (!hasVariants) SuqiPrimary else colors.mutedText,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Expanded variant rows
                                    if (hasVariants && isExpanded) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = colors.border)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val buyerOpts = prod.details.buyerOptions
                                        variants.forEachIndexed { vIdx, v ->
                                            val vLabel = ProductDetails.variantLabel(buyerOpts, v.options).ifBlank { v.id }
                                            val vStock = v.stock ?: 0
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(colors.secondaryBackground)
                                                    .clickable {
                                                        editingVariantTarget = prod to vIdx
                                                        variantSkuInput = v.sku
                                                        variantStockInput = vStock.toString()
                                                        variantPriceInput = v.price?.toLong()?.toString().orEmpty()
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    .testTag("inventory_variant_row_${prod.id}_$vIdx")
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = vLabel,
                                                        fontFamily = SuqiFontFamily,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 13.sp,
                                                        color = colors.primaryText
                                                    )
                                                    val meta = buildString {
                                                        append("${work.variantSkuPlaceholder}: ${v.sku.ifBlank { "—" }}")
                                                        if (v.price != null) {
                                                            append(" · ${formatEtb(v.price)}")
                                                        }
                                                    }
                                                    Text(
                                                        text = meta,
                                                        fontFamily = SuqiFontFamily,
                                                        fontSize = 11.sp,
                                                        color = colors.secondaryText
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { updateVariantStock(prod, vIdx, -1) },
                                                    enabled = vStock > 0,
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("variant_dec_${prod.id}_$vIdx")
                                                ) {
                                                    Icon(LucideIcons.Minus, contentDescription = strings.common.decreaseQtyDesc, modifier = Modifier.size(14.dp))
                                                }
                                                Text(
                                                    text = vStock.toString(),
                                                    fontFamily = SuqiFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (vStock <= 5) SuqiWarning else colors.primaryText
                                                )
                                                IconButton(
                                                    onClick = { updateVariantStock(prod, vIdx, +1) },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("variant_inc_${prod.id}_$vIdx")
                                                ) {
                                                    Icon(LucideIcons.Plus, contentDescription = strings.common.increaseQtyDesc, tint = SuqiPrimary, modifier = Modifier.size(14.dp))
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
        }
    }

    if (showBulkStockDialog) {
        val hasVariantSelection = products.any { it.id in selectedIds && it.details.variants.isNotEmpty() }
        AlertDialog(
            onDismissRequest = { showBulkStockDialog = false },
            title = { Text(work.enterNewStockTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, color = colors.primaryText) },
            text = {
                SuqiTextField(
                    value = bulkStockInput,
                    onValueChange = { bulkStockInput = it.filter(Char::isDigit) },
                    label = if (hasVariantSelection) work.stockUnitsPerVariantLabel else work.stockUnitsLabel
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = bulkStockInput.toIntOrNull() ?: 0
                        showBulkStockDialog = false
                        coroutineScope.launch {
                            val res = viewModel.repository.bulkUpdateProducts(selectedIds.toList(), "SET_STOCK", qty.toString())
                            if (res.success) {
                                viewModel.showToast(work.stockSavedToast)
                                isSelectionMode = false
                                selectedIds.clear()
                                loadInventory()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(strings.saveLabel, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkStockDialog = false }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(strings.cancelLabel)
                }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    editingVariantTarget?.let { (prod, vIdx) ->
        val variants = prod.details.variants.toMutableList()
        val variant = variants.getOrNull(vIdx)
        if (variant != null) {
            val vLabel = ProductDetails.variantLabel(prod.details.buyerOptions, variant.options).ifBlank { variant.id }
            AlertDialog(
                onDismissRequest = { editingVariantTarget = null },
                title = { Text(vLabel, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, color = colors.primaryText) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SuqiTextField(
                            value = variantSkuInput,
                            onValueChange = { variantSkuInput = it },
                            label = work.variantSkuPlaceholder
                        )
                        SuqiTextField(
                            value = variantStockInput,
                            onValueChange = { variantStockInput = it.filter(Char::isDigit) },
                            label = work.stockUnitsLabel
                        )
                        SuqiTextField(
                            value = variantPriceInput,
                            onValueChange = { variantPriceInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = "${work.priceOverrideLabel} (ETB)"
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val nextStock = variantStockInput.toIntOrNull() ?: 0
                            val nextPrice = variantPriceInput.toDoubleOrNull()
                            variants[vIdx] = variant.copy(
                                sku = variantSkuInput.trim(),
                                stock = nextStock,
                                price = nextPrice
                            )
                            val updatedDetails = prod.details.copy(variants = variants)
                            val total = variants.sumOf { (it.stock ?: 0).coerceAtLeast(0) }
                            editingVariantTarget = null
                            coroutineScope.launch {
                                val res = viewModel.repository.updateProductStock(prod.id, total, updatedDetails.toJson())
                                if (res.success && res.data != null) {
                                    products = products.map { if (it.id == prod.id) res.data else it }
                                    viewModel.showToast(work.stockSavedToast)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(strings.saveLabel, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingVariantTarget = null }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(strings.cancelLabel)
                    }
                },
                containerColor = colors.cardBackground,
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}
