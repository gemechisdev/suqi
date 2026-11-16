@file:OptIn(ExperimentalLayoutApi::class)

package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AttrType
import com.example.data.model.AttributeDef
import com.example.data.model.CategoryDto
import com.example.data.model.ColorSwatches
import com.example.data.model.ItemTypeDto
import com.example.data.model.ProductDto
import com.example.data.model.ProductTemplates
import com.example.ui.SuqiViewModel
import com.example.ui.components.AttributeValueInput
import com.example.ui.components.ProductFormState
import com.example.ui.components.StatusType
import com.example.ui.components.SuqiButton
import com.example.ui.components.SuqiDragHandleList
import com.example.ui.components.SuqiScreen
import com.example.ui.components.SuqiSectionCard
import com.example.ui.components.SuqiSkeletonList
import com.example.ui.components.SuqiStatusBadge
import com.example.ui.components.SuqiSwitchRow
import com.example.ui.components.SuqiTabItem
import com.example.ui.components.SuqiTabs
import com.example.ui.components.SuqiTextField
import com.example.ui.i18n.LocalSuqiLanguage
import com.example.ui.i18n.localizedName
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.LocalSuqiColors
import com.example.ui.theme.LucideIcons
import com.example.ui.theme.SuqiError
import com.example.ui.theme.SuqiFontFamily
import com.example.ui.theme.SuqiPrimary
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun AdminItemTypesScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val lang = LocalSuqiLanguage.current
    val isOffline by viewModel.isOffline.collectAsState()

    var itemTypes by remember { mutableStateOf<List<ItemTypeDto>>(emptyList()) }
    var categories by remember { mutableStateOf<List<CategoryDto>>(emptyList()) }
    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var editingType by remember { mutableStateOf<ItemTypeDto?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    BackHandler(enabled = editingType != null || isCreatingNew) {
        editingType = null
        isCreatingNew = false
    }

    fun loadData() {
        coroutineScope.launch {
            isLoading = true
            viewModel.repository.getItemTypes().data?.let { itemTypes = it }
            viewModel.repository.getCategories().data?.let { categories = it }
            viewModel.repository.getProducts().data?.let { products = it }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    if (editingType != null || isCreatingNew) {
        ItemTypeTemplateEditor(
            initial = editingType,
            categories = categories,
            defaultSortOrder = itemTypes.size,
            onBack = {
                editingType = null
                isCreatingNew = false
            },
            onSave = { dto ->
                coroutineScope.launch {
                    val res = viewModel.repository.saveItemType(dto)
                    if (res.success) {
                        editingType = null
                        isCreatingNew = false
                        loadData()
                        viewModel.showToast(strings.saveLabel)
                    } else {
                        viewModel.showToast(res.message ?: strings.retryLabel)
                    }
                }
            },
            modifier = modifier
        )
        return
    }

    val grouped = remember(itemTypes) { itemTypes.groupBy { it.group.ifBlank { "Other" } } }

    SuqiScreen(
        title = strings.admin.itemTypesManagerTitle,
        subtitle = "${itemTypes.size}",
        onBackClick = { viewModel.goBack() },
        headerActions = {
            IconButton(
                onClick = {
                    editingType = null
                    isCreatingNew = true
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SuqiPrimary)
                    .testTag("new_item_type_btn")
            ) {
                Icon(LucideIcons.Plus, contentDescription = strings.adminGov.newItemTypeBtn, tint = Color.White)
            }
        },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        if (isLoading && itemTypes.isEmpty()) {
            SuqiSkeletonList(count = 5, modifier = Modifier.padding(paddingValues).padding(top = 12.dp))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                grouped.forEach { (groupName, groupList) ->
                    item(key = "header_$groupName") {
                        Text(
                            text = groupName,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = SuqiPrimary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(groupList, key = { it.id }) { t ->
                        val usageCount = products.count { it.details.itemType == t.id }
                        SuqiSectionCard(
                            padding = 14.dp,
                            onClick = { editingType = t },
                            modifier = Modifier.testTag("item_type_card_${t.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colors.primaryTint)
                                ) {
                                    Text(t.emoji.ifBlank { "✨" }, fontSize = 22.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = t.label,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = colors.primaryText
                                    )
                                    val catName = categories.firstOrNull { it.id == t.categoryId }?.localizedName(lang)
                                    Text(
                                        text = listOfNotNull(
                                            String.format(strings.adminGov.paramsCountTemplate, t.attributes.size),
                                            String.format(strings.adminGov.productsUsingTemplate, usageCount),
                                            catName
                                        ).joinToString(" • "),
                                        fontFamily = SuqiFontFamily,
                                        fontSize = 11.sp,
                                        color = colors.secondaryText,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { editingType = t },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(LucideIcons.Pencil, contentDescription = strings.editLabel, tint = colors.primaryText, modifier = Modifier.size(17.dp))
                                }
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.repository.deleteItemType(t.id)
                                            loadData()
                                            viewModel.showToast(strings.deleteLabel)
                                        }
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(LucideIcons.Trash2, contentDescription = strings.deleteLabel, tint = SuqiError, modifier = Modifier.size(17.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemTypeTemplateEditor(
    initial: ItemTypeDto?,
    categories: List<CategoryDto>,
    defaultSortOrder: Int,
    onBack: () -> Unit,
    onSave: (ItemTypeDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val lang = LocalSuqiLanguage.current

    var editorTab by remember { mutableIntStateOf(0) } // 0: Edit, 1: Seller Form, 2: Buyer Specs
    var label by remember(initial) { mutableStateOf(initial?.label.orEmpty()) }
    var emoji by remember(initial) { mutableStateOf(initial?.emoji ?: "✨") }
    var categoryId by remember(initial) { mutableStateOf(initial?.categoryId ?: categories.firstOrNull()?.id.orEmpty()) }
    var group by remember(initial) { mutableStateOf(initial?.group ?: "Electronics") }
    val attrs = remember(initial) {
        mutableStateListOf<AttributeDef>().apply { addAll(initial?.attributes.orEmpty()) }
    }

    // Dialog states
    var showLibraryPicker by remember { mutableStateOf(false) }
    var showCustomDialogFor by remember { mutableStateOf<AttributeDef?>(null) }
    var isNewCustomParam by remember { mutableStateOf(false) }

    // Live interactive preview form state synced with current attributes
    val previewFormState = remember { ProductFormState() }
    LaunchedEffect(attrs.toList()) {
        previewFormState.attributes.clear()
        previewFormState.attributes.addAll(attrs)
        // Seed sensible sample preview values if empty so Buyer Specs shows rich options right away
        attrs.forEach { def ->
            if (previewFormState.valuesOf(def.key).isEmpty()) {
                when (def.type) {
                    AttrType.COLOR -> previewFormState.setValues(def.key, listOf("Black", "Blue", "Orange"))
                    AttrType.SELECT, AttrType.MULTI_SELECT -> if (def.options.isNotEmpty()) {
                        previewFormState.setValues(def.key, def.options.take(3))
                    }
                    AttrType.BOOLEAN -> previewFormState.setValues(def.key, listOf("true"))
                    AttrType.NUMBER -> previewFormState.setValues(def.key, listOf("2"))
                    AttrType.MEASUREMENT -> previewFormState.setValues(
                        def.key,
                        listOf("15 ${def.options.firstOrNull() ?: def.unit.ifBlank { "cm" }}")
                    )
                    else -> previewFormState.setValues(def.key, listOf("Sample ${def.label}"))
                }
            }
        }
    }

    val emojiOptions = listOf("💻", "📱", "👗", "👕", "👟", "🛋️", "💄", "☕", "🚗", "📚", "🎨", "🎁", "⚽", "🍼", "🛠️", "🔌", "🎧", "✨")
    val groupOptions = listOf("Electronics", "Fashion", "Home & Living", "Beauty & Care", "Groceries & Food", "Sports & Outdoors", "Automotive", "Other")

    SuqiScreen(
        title = if (initial == null) strings.adminGov.newItemTypeBtn else initial.label,
        subtitle = group,
        onBackClick = onBack,
        headerActions = {
            Button(
                onClick = {
                    if (label.isBlank()) return@Button
                    val safeId = initial?.id?.takeIf { it.isNotBlank() }
                        ?: ("type_" + label.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_') + "_" + (System.currentTimeMillis() % 10000))
                    onSave(
                        ItemTypeDto(
                            id = safeId,
                            label = label.trim(),
                            emoji = emoji.ifBlank { "✨" },
                            categoryId = categoryId,
                            group = group.trim().ifBlank { "Other" },
                            attributes = attrs.toList(),
                            sortOrder = initial?.sortOrder ?: defaultSortOrder
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("save_item_type_btn")
            ) {
                Text(strings.saveLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SuqiTabs(
                tabs = listOf(
                    SuqiTabItem("edit", strings.adminGov.editTemplateTab),
                    SuqiTabItem("seller_preview", strings.adminGov.sellerViewTab),
                    SuqiTabItem("buyer_preview", strings.adminGov.buyerPreviewTab)
                ),
                selectedIndex = editorTab,
                onSelectTab = { editorTab = it },
                testTagPrefix = "item_type_editor_tab"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (editorTab) {
                    0 -> {
                        // Metadata Card
                        item {
                            SuqiSectionCard(padding = 16.dp) {
                                SuqiTextField(
                                    value = label,
                                    onValueChange = { label = it },
                                    label = "${strings.seller.paramNameRequiredLabel}",
                                    testTag = "item_type_label_input"
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = strings.adminGov.emojiLabel,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = colors.primaryText
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                                ) {
                                    emojiOptions.forEach { e ->
                                        val isSel = emoji == e
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSel) SuqiPrimary else colors.secondaryBackground)
                                                .clickable { emoji = e }
                                        ) {
                                            Text(e, fontSize = 20.sp)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = strings.adminGov.sectionGroupLabel,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = colors.primaryText
                                )
                                Spacer(Modifier.height(6.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    groupOptions.forEach { g ->
                                        val isSel = group == g
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(if (isSel) SuqiPrimary else colors.secondaryBackground)
                                                .clickable { group = g }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(g, fontSize = 12.sp, color = if (isSel) Color.White else colors.primaryText)
                                        }
                                    }
                                }
                                if (categories.isNotEmpty()) {
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = strings.adminGov.defaultCategoryLabel,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = colors.primaryText
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                                    ) {
                                        categories.forEach { cat ->
                                            val isSel = categoryId == cat.id
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(if (isSel) SuqiPrimary else colors.secondaryBackground)
                                                    .clickable { categoryId = cat.id }
                                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                            ) {
                                                Text(cat.localizedName(lang), fontSize = 12.sp, color = if (isSel) Color.White else colors.primaryText)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Parameters Header + Add buttons
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { showLibraryPicker = true },
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, SuqiPrimary),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("add_library_param_btn")
                                ) {
                                    Icon(LucideIcons.ListIcon, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.adminGov.addLibraryParamBtn, color = SuqiPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        showCustomDialogFor = null
                                        isNewCustomParam = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("create_custom_param_btn")
                                ) {
                                    Icon(LucideIcons.Plus, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.adminGov.createCustomParamBtn, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        // Reorderable Attribute List
                        item {
                            SuqiDragHandleList(
                                items = attrs.toList(),
                                onMove = { from, to ->
                                    val moved = attrs.removeAt(from)
                                    attrs.add(to, moved)
                                }
                            ) { idx, def, moveUp, moveDown ->
                                SuqiSectionCard(padding = 14.dp, modifier = Modifier.testTag("template_attr_${def.key}")) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Column {
                                            IconButton(onClick = { moveUp?.invoke() }, enabled = moveUp != null, modifier = Modifier.size(34.dp)) {
                                                Icon(LucideIcons.ChevronUp, contentDescription = strings.common.moveUpDesc, tint = if (moveUp != null) colors.primaryText else colors.mutedText.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(onClick = { moveDown?.invoke() }, enabled = moveDown != null, modifier = Modifier.size(34.dp)) {
                                                Icon(LucideIcons.ChevronDown, contentDescription = strings.common.moveDownDesc, tint = if (moveDown != null) colors.primaryText else colors.mutedText.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = def.label + if (def.required) " *" else "",
                                                fontFamily = SuqiFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = colors.primaryText
                                            )
                                            Text(
                                                text = "${AttrType.label(def.type)} • ${def.group}" + if (def.options.isNotEmpty()) " (${def.options.take(3).joinToString(", ")})" else "",
                                                fontFamily = SuqiFontFamily,
                                                fontSize = 11.sp,
                                                color = colors.secondaryText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                showCustomDialogFor = def
                                                isNewCustomParam = false
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(LucideIcons.Pencil, contentDescription = strings.common.editParamDesc, tint = colors.primaryText, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { attrs.removeAt(idx) },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(LucideIcons.Trash2, contentDescription = strings.common.removeParamDesc, tint = SuqiError, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    HorizontalDivider(color = colors.border)
                                    SuqiSwitchRow(
                                        title = strings.adminGov.enabledByDefaultLabel,
                                        checked = def.enabled,
                                        onCheckedChange = { attrs[idx] = def.copy(enabled = it) }
                                    )
                                    SuqiSwitchRow(
                                        title = strings.adminGov.requiredSwitchLabel,
                                        checked = def.required,
                                        onCheckedChange = { attrs[idx] = def.copy(required = it) }
                                    )
                                    if (def.type != AttrType.BOOLEAN && def.type != AttrType.DATE) {
                                        SuqiSwitchRow(
                                            title = strings.adminGov.buyersChooseLabel,
                                            checked = def.selectable,
                                            onCheckedChange = { attrs[idx] = def.copy(selectable = it) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Seller Form Live Preview using actual AttributeValueInput widgets
                        item {
                            Text(
                                text = strings.adminGov.sellerViewTab,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.primaryText
                            )
                        }
                        items(attrs.filter { it.enabled }, key = { "seller_prev_${it.key}" }) { def ->
                            SuqiSectionCard(padding = 14.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = def.label + if (def.required) " *" else "",
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colors.primaryText
                                    )
                                    if (def.selectable) {
                                        SuqiStatusBadge(strings.seller.buyersChooseChip, StatusType.PRIMARY)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                AttributeValueInput(
                                    def = def,
                                    selected = previewFormState.valuesOf(def.key),
                                    onChange = { previewFormState.setValues(def.key, it) }
                                )
                            }
                        }
                    }

                    2 -> {
                        // Buyer Specs & Variant Selection Live Preview
                        val details = previewFormState.toDetails(emptyList())
                        val buyerOptions = details.buyerOptions
                        val specs = details.specifications
                        item {
                            SuqiSectionCard(padding = 16.dp) {
                                Text(
                                    text = "${emoji} ${label.ifBlank { strings.admin.itemTypesManagerTitle }}",
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = colors.primaryText
                                )
                                if (buyerOptions.isNotEmpty()) {
                                    Spacer(Modifier.height(12.dp))
                                    buyerOptions.forEach { opt ->
                                        var chosen by remember(opt.key) {
                                            mutableStateOf(details.valuesOf(opt.key).firstOrNull().orEmpty())
                                        }
                                        Text(
                                            text = opt.label,
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = colors.primaryText
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            details.valuesOf(opt.key).forEach { valItem ->
                                                val isSel = chosen == valItem
                                                val swatchArgb = if (opt.type == AttrType.COLOR) ColorSwatches.argbOf(valItem) else null
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(if (isSel) SuqiPrimary else colors.secondaryBackground)
                                                        .border(1.dp, if (isSel) SuqiPrimary else colors.border, RoundedCornerShape(999.dp))
                                                        .clickable { chosen = valItem }
                                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                                ) {
                                                    if (swatchArgb != null) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(14.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(swatchArgb))
                                                                .border(1.dp, colors.border, CircleShape)
                                                        )
                                                        Spacer(Modifier.width(6.dp))
                                                    }
                                                    Text(
                                                        text = valItem,
                                                        fontFamily = SuqiFontFamily,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSel) Color.White else colors.primaryText
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(10.dp))
                                    }
                                }
                            }
                        }

                        if (specs.isNotEmpty()) {
                            item {
                                SuqiSectionCard(padding = 16.dp) {
                                    Text(
                                        text = strings.specifications,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = colors.primaryText
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    specs.forEachIndexed { idx, spec ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(spec.label, fontSize = 13.sp, color = colors.secondaryText)
                                            Text(details.displayValue(spec), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.primaryText)
                                        }
                                        if (idx < specs.lastIndex) {
                                            HorizontalDivider(color = colors.border)
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

    if (showLibraryPicker) {
        var search by remember { mutableStateOf("") }
        val existingKeys = attrs.map { it.key }.toSet()
        val library = ProductTemplates.library.filter {
            it.key !in existingKeys && (search.isBlank() || it.label.contains(search, ignoreCase = true))
        }
        Dialog(onDismissRequest = { showLibraryPicker = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.cardBackground)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = strings.adminGov.addLibraryParamBtn,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = colors.primaryText,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showLibraryPicker = false }, modifier = Modifier.size(48.dp)) {
                        Icon(LucideIcons.X, contentDescription = strings.common.closeLabel, tint = colors.primaryText)
                    }
                }
                SuqiTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = "",
                    placeholder = strings.seller.searchParametersHint,
                    leadingIcon = LucideIcons.Search
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(library, key = { it.key }) { libAttr ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.secondaryBackground)
                                .clickable {
                                    attrs.add(libAttr)
                                    showLibraryPicker = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(libAttr.label, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.primaryText)
                                Text(AttrType.label(libAttr.type), fontFamily = SuqiFontFamily, fontSize = 11.sp, color = colors.secondaryText)
                            }
                            Icon(LucideIcons.Plus, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    if (isNewCustomParam || showCustomDialogFor != null) {
        val editing = showCustomDialogFor
        var paramLabel by remember(editing) { mutableStateOf(editing?.label.orEmpty()) }
        var paramType by remember(editing) { mutableStateOf(editing?.type ?: AttrType.TEXT) }
        var optionsCsv by remember(editing) { mutableStateOf(editing?.options?.joinToString(", ").orEmpty()) }
        var paramGroup by remember(editing) { mutableStateOf(editing?.group ?: "General") }
        var paramRequired by remember(editing) { mutableStateOf(editing?.required ?: false) }
        var paramSelectable by remember(editing) { mutableStateOf(editing?.selectable ?: false) }

        Dialog(
            onDismissRequest = {
                showCustomDialogFor = null
                isNewCustomParam = false
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.cardBackground)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (editing == null) strings.adminGov.createCustomParamBtn else strings.seller.editParameterTitle,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = colors.primaryText,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            showCustomDialogFor = null
                            isNewCustomParam = false
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(LucideIcons.X, contentDescription = strings.common.closeLabel, tint = colors.primaryText)
                    }
                }

                SuqiTextField(
                    value = paramLabel,
                    onValueChange = { paramLabel = it },
                    label = strings.seller.paramNameRequiredLabel,
                    placeholder = strings.seller.paramNamePlaceholder
                )

                Text(
                    text = strings.seller.inputTypeLabel,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = colors.primaryText
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AttrType.all.forEach { t ->
                        val isSel = paramType == t
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isSel) SuqiPrimary else colors.secondaryBackground)
                                .clickable { paramType = t }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(AttrType.label(t), fontSize = 12.sp, color = if (isSel) Color.White else colors.primaryText)
                        }
                    }
                }

                if (paramType == AttrType.SELECT || paramType == AttrType.MULTI_SELECT || paramType == AttrType.MEASUREMENT) {
                    SuqiTextField(
                        value = optionsCsv,
                        onValueChange = { optionsCsv = it },
                        label = strings.adminGov.choicesOrUnitsLabel,
                        placeholder = if (paramType == AttrType.MEASUREMENT) "cm, m, inch" else "Small, Medium, Large"
                    )
                }

                SuqiTextField(
                    value = paramGroup,
                    onValueChange = { paramGroup = it },
                    label = strings.seller.detailsSectionLabel
                )

                SuqiSwitchRow(
                    title = strings.adminGov.requiredSwitchLabel,
                    checked = paramRequired,
                    onCheckedChange = { paramRequired = it }
                )
                if (paramType != AttrType.BOOLEAN && paramType != AttrType.DATE) {
                    SuqiSwitchRow(
                        title = strings.adminGov.buyersChooseLabel,
                        checked = paramSelectable,
                        onCheckedChange = { paramSelectable = it }
                    )
                }

                SuqiButton(
                    text = strings.saveLabel,
                    onClick = {
                        if (paramLabel.isBlank()) return@SuqiButton
                        val parsedOpts = optionsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        val safeKey = editing?.key ?: run {
                            val base = paramLabel.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "param" }
                            var k = base
                            var n = 2
                            while (attrs.any { it.key == k }) k = "${base}_${n++}"
                            k
                        }
                        val nextDef = AttributeDef(
                            key = safeKey,
                            label = paramLabel.trim(),
                            type = paramType,
                            options = parsedOpts,
                            unit = if (paramType == AttrType.MEASUREMENT) parsedOpts.firstOrNull().orEmpty() else "",
                            required = paramRequired,
                            enabled = editing?.enabled ?: true,
                            selectable = paramSelectable && paramType != AttrType.BOOLEAN && paramType != AttrType.DATE,
                            group = paramGroup.trim().ifBlank { "General" }
                        )
                        if (editing != null) {
                            val idx = attrs.indexOfFirst { it.key == editing.key }
                            if (idx >= 0) attrs[idx] = nextDef
                        } else {
                            attrs.add(nextDef)
                        }
                        showCustomDialogFor = null
                        isNewCustomParam = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
