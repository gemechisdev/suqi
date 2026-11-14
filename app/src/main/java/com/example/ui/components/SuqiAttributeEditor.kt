@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AttrType
import com.example.data.model.AttributeDef
import com.example.data.model.ColorSwatches
import com.example.data.model.ItemType
import com.example.data.model.MediaItem
import com.example.data.model.ProductDetails
import com.example.data.model.ProductTemplates
import com.example.data.model.ProductVariant
import com.example.ui.theme.LocalSuqiColors
import com.example.ui.theme.LucideIcons
import com.example.ui.theme.SuqiFontFamily
import com.example.ui.theme.SuqiPrimary
import com.example.ui.theme.SuqiRadius
import com.example.ui.theme.suqiWarmShadow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val MAX_VARIANTS = 60

// ═════════════════════════════════════════════════════════════
// Form state (hoisted by the Add/Edit product screen)
// ═════════════════════════════════════════════════════════════

/**
 * Editable copy of a product's item type, parameter definitions, values and variants.
 * Disabling a parameter never deletes its saved value.
 */
class ProductFormState {
    var itemType by mutableStateOf("")
    val attributes = mutableStateListOf<AttributeDef>()
    val values = mutableStateMapOf<String, List<String>>()
    val variants = mutableStateListOf<ProductVariant>()

    fun load(details: ProductDetails) {
        itemType = details.itemType
        attributes.clear(); attributes.addAll(details.attributes)
        values.clear(); values.putAll(details.values)
        variants.clear(); variants.addAll(details.variants)
    }

    fun valuesOf(key: String): List<String> = values[key].orEmpty().filter { it.isNotBlank() }

    fun setValues(key: String, list: List<String>) {
        values[key] = list.filter { it.isNotBlank() }
    }

    /**
     * Switch item type: the template's parameters replace the list; values that were already
     * entered are kept, and custom parameters that hold data stay available (disabled).
     */
    fun applyItemType(type: ItemType) {
        val old = attributes.toList()
        val templateKeys = type.attributes.map { it.key }.toSet()
        val keepers = old
            .filter { it.key !in templateKeys && valuesOf(it.key).isNotEmpty() }
            .map { it.copy(enabled = false) }
        attributes.clear()
        attributes.addAll(type.attributes)
        attributes.addAll(keepers)
        itemType = type.id
        variants.clear()
    }

    fun update(oldKey: String, def: AttributeDef) {
        val i = attributes.indexOfFirst { it.key == oldKey }
        if (i >= 0) attributes[i] = def
    }

    fun move(index: Int, delta: Int) {
        val to = index + delta
        if (index !in attributes.indices || to !in attributes.indices) return
        val item = attributes.removeAt(index)
        attributes.add(to, item)
    }

    fun remove(key: String) {
        attributes.removeAll { it.key == key }
        values.remove(key)
    }

    fun add(def: AttributeDef) {
        attributes.add(def)
    }

    fun uniqueKey(label: String): String {
        val base = label.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "attr" }
        var key = base
        var n = 2
        while (attributes.any { it.key == key }) key = "${base}_${n++}"
        return key
    }

    fun missingRequired(): AttributeDef? =
        attributes.firstOrNull { it.enabled && it.required && valuesOf(it.key).isEmpty() }

    fun buyerOptions(): List<AttributeDef> =
        attributes.filter { it.enabled && it.selectable && valuesOf(it.key).isNotEmpty() }

    private fun combos(): List<Map<String, String>> {
        val opts = buyerOptions().filter { it.type != AttrType.BOOLEAN }
        if (opts.isEmpty()) return emptyList()
        var result: List<Map<String, String>> = listOf(emptyMap())
        for (o in opts) {
            val vals = valuesOf(o.key)
            result = result.flatMap { base -> vals.map { v -> base + (o.key to v) } }
            if (result.size > MAX_VARIANTS) break
        }
        return result
    }

    /** Rebuilds the variant list from the buyer options. Returns a message when it can't. */
    fun regenerateVariants(): String? {
        val combos = combos()
        if (combos.size > MAX_VARIANTS) {
            return "Too many combinations (over $MAX_VARIANTS). Reduce the options buyers choose from."
        }
        val opts = buyerOptions().filter { it.type != AttrType.BOOLEAN }
        val rebuilt = combos.map { combo ->
            variants.firstOrNull { it.options == combo }
                ?: ProductVariant(id = ProductDetails.variantLabel(opts, combo), options = combo)
        }
        variants.clear()
        variants.addAll(rebuilt)
        return null
    }

    fun toDetails(media: List<MediaItem>): ProductDetails {
        val validVariants = variants.filter { v ->
            v.options.isNotEmpty() && v.options.all { (k, x) -> valuesOf(k).contains(x) }
        }
        return ProductDetails(
            itemType = itemType,
            media = media,
            attributes = attributes.toList(),
            values = values.toMap(),
            variants = validVariants
        )
    }
}

// ═════════════════════════════════════════════════════════════
// Shared small UI pieces
// ═════════════════════════════════════════════════════════════

@Composable
private fun PillChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    leading: String? = null,
    onClick: () -> Unit
) {
    val colors = LocalSuqiColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) SuqiPrimary else colors.cardBackground)
            .border(1.dp, if (selected) SuqiPrimary else colors.border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = if (leading != null) "$leading  $text" else text,
            fontFamily = SuqiFontFamily,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 12.sp,
            color = if (selected) Color.White else colors.primaryText,
            maxLines = 1
        )
    }
}

@Composable
private fun SmallIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean = true,
    tint: Color = LocalSuqiColors.current.secondaryText,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) tint else tint.copy(alpha = 0.3f),
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = LocalSuqiColors.current
    val shape = RoundedCornerShape(SuqiRadius.Card)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .suqiWarmShadow(shape, elevation = 6.dp)
            .clip(shape)
            .background(colors.cardBackground)
            .padding(16.dp)
    ) { content() }
}

// ═════════════════════════════════════════════════════════════
// Item type selector
// ═════════════════════════════════════════════════════════════

/** "What are you selling?" card + searchable picker of predefined item types. */
@Composable
fun ItemTypeSelector(
    state: ProductFormState,
    onTypeChosen: (ItemType) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    var showPicker by remember { mutableStateOf(false) }
    val current = state.itemType.takeIf { it.isNotBlank() }?.let { ProductTemplates.byId(it) }

    SectionCard(modifier) {
        Text(
            text = "What are you selling?",
            fontFamily = SuqiFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = colors.primaryText
        )
        Text(
            text = "Pick an item type to get the right parameters (specs, sizes, colors…). You can change anything afterwards.",
            fontFamily = SuqiFontFamily,
            fontSize = 12.sp,
            color = colors.secondaryText
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.primaryTint)
                .clickable { showPicker = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(current?.emoji ?: "🛍️", fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = current?.label ?: "Choose item type",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.primaryText
                )
                if (current != null) {
                    Text(
                        text = "${current.attributes.size} predefined parameters",
                        fontFamily = SuqiFontFamily,
                        fontSize = 11.sp,
                        color = colors.secondaryText
                    )
                }
            }
            Text(
                text = if (current == null) "Select" else "Change",
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = SuqiPrimary
            )
        }
    }

    if (showPicker) {
        ItemTypeDialog(
            currentId = state.itemType,
            onPick = { type ->
                showPicker = false
                onTypeChosen(type)
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun ItemTypeDialog(currentId: String, onPick: (ItemType) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalSuqiColors.current
    var query by remember { mutableStateOf("") }
    val types = ProductTemplates.pickerTypes.filter {
        query.isBlank() || it.label.contains(query, ignoreCase = true) || it.group.contains(query, ignoreCase = true)
    }
    val grouped = types.groupBy { it.group }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(SuqiRadius.Hero))
                .background(colors.cardBackground)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Choose item type",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.primaryText,
                    modifier = Modifier.weight(1f)
                )
                SmallIconButton(LucideIcons.X, "Close", onClick = onDismiss)
            }
            Spacer(Modifier.height(8.dp))
            SuqiTextField(
                value = query,
                onValueChange = { query = it },
                label = "",
                placeholder = "Search (laptop, shoes, coffee, car…)",
                leadingIcon = LucideIcons.Search
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                grouped.forEach { (group, list) ->
                    item(key = "h_$group") {
                        Text(
                            text = group,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = colors.secondaryText,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    item(key = "g_$group") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            list.forEach { type ->
                                PillChip(
                                    text = type.label,
                                    leading = type.emoji,
                                    selected = type.id == currentId,
                                    onClick = { onPick(type) }
                                )
                            }
                        }
                    }
                }
                if (types.isEmpty()) {
                    item {
                        Text(
                            "No match — choose “Custom / Other” and build your own parameters.",
                            fontFamily = SuqiFontFamily,
                            fontSize = 13.sp,
                            color = colors.secondaryText
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════
// Parameters editor
// ═════════════════════════════════════════════════════════════

/** List of the product's parameters: toggle, fill, reorder, customise, add or remove. */
@Composable
fun ParametersEditor(state: ProductFormState, modifier: Modifier = Modifier) {
    val colors = LocalSuqiColors.current
    var dialogFor by remember { mutableStateOf<AttributeDef?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text(
                text = "Product parameters",
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = colors.primaryText
            )
            Text(
                text = "Switch off what doesn't apply. Turn on “Buyers choose” for things like size or color that shoppers pick when buying.",
                fontFamily = SuqiFontFamily,
                fontSize = 12.sp,
                color = colors.secondaryText
            )
        }

        if (state.attributes.isEmpty()) {
            Text(
                text = "No parameters yet. Choose an item type above or add your own below.",
                fontFamily = SuqiFontFamily,
                fontSize = 12.sp,
                color = colors.mutedText
            )
        }

        state.attributes.forEachIndexed { index, def ->
            androidx.compose.runtime.key(def.key) {
                AttributeCard(
                    def = def,
                    selected = state.valuesOf(def.key),
                    canMoveUp = index > 0,
                    canMoveDown = index < state.attributes.lastIndex,
                    onToggle = { state.update(def.key, def.copy(enabled = it)) },
                    onSelectable = { state.update(def.key, def.copy(selectable = it)) },
                    onValues = { state.setValues(def.key, it) },
                    onMove = { state.move(index, it) },
                    onEdit = { dialogFor = def },
                    onRemove = { state.remove(def.key) }
                )
            }
        }

        OutlinedButton(
            onClick = { showAdd = true },
            shape = RoundedCornerShape(999.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SuqiPrimary),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Icon(LucideIcons.Plus, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add parameter", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, color = SuqiPrimary)
        }
    }

    if (showAdd) {
        AddParameterDialog(
            state = state,
            onDismiss = { showAdd = false }
        )
    }
    dialogFor?.let { editing ->
        CustomParameterDialog(
            initial = editing,
            state = state,
            onDismiss = { dialogFor = null },
            onSave = { updated ->
                state.update(editing.key, updated)
                dialogFor = null
            }
        )
    }
}

@Composable
private fun AttributeCard(
    def: AttributeDef,
    selected: List<String>,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: (Boolean) -> Unit,
    onSelectable: (Boolean) -> Unit,
    onValues: (List<String>) -> Unit,
    onMove: (Int) -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = LocalSuqiColors.current
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (def.enabled) colors.cardBackground else colors.secondaryBackground)
            .border(1.dp, if (def.enabled) colors.border else Color.Transparent, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = def.label + if (def.required) " *" else "",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (def.enabled) colors.primaryText else colors.mutedText
                )
                Text(
                    text = buildString {
                        append(AttrType.label(def.type))
                        if (def.selectable) append(" • buyers choose")
                        if (!def.enabled && selected.isNotEmpty()) append(" • value kept")
                    },
                    fontFamily = SuqiFontFamily,
                    fontSize = 11.sp,
                    color = colors.mutedText
                )
            }
            Switch(
                checked = def.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary)
            )
        }

        if (def.enabled) {
            Spacer(Modifier.height(8.dp))
            AttributeValueInput(def, selected, onValues)
            if (def.required && selected.isEmpty()) {
                Text(
                    text = "Required",
                    fontFamily = SuqiFontFamily,
                    fontSize = 11.sp,
                    color = Color(0xFFD92D20),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (def.type != AttrType.BOOLEAN && def.type != AttrType.DATE) {
                    PillChip(
                        text = "Buyers choose",
                        selected = def.selectable,
                        onClick = { onSelectable(!def.selectable) }
                    )
                }
                Spacer(Modifier.weight(1f))
                SmallIconButton(LucideIcons.ChevronUp, "Move up", canMoveUp) { onMove(-1) }
                SmallIconButton(LucideIcons.ChevronDown, "Move down", canMoveDown) { onMove(1) }
                SmallIconButton(LucideIcons.Pencil, "Edit parameter", onClick = onEdit)
                SmallIconButton(LucideIcons.Trash2, "Remove parameter", tint = Color(0xFFD92D20), onClick = onRemove)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                SmallIconButton(LucideIcons.ChevronUp, "Move up", canMoveUp) { onMove(-1) }
                SmallIconButton(LucideIcons.ChevronDown, "Move down", canMoveDown) { onMove(1) }
                SmallIconButton(LucideIcons.Trash2, "Remove parameter", tint = Color(0xFFD92D20), onClick = onRemove)
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════
// Value inputs (one per attribute type)
// ═════════════════════════════════════════════════════════════

private val multilineKeys = setOf("ingredients", "care", "notes", "description", "embroidery")

/** Renders the right input for an attribute definition. Values are always a list of strings. */
@Composable
fun AttributeValueInput(def: AttributeDef, selected: List<String>, onChange: (List<String>) -> Unit) {
    if (def.selectable) {
        when (def.type) {
            AttrType.COLOR -> ColorPaletteInput(selected, multi = true, onChange = onChange)
            else -> ChoiceListInput(
                options = if (def.type == AttrType.SELECT || def.type == AttrType.MULTI_SELECT) def.options else emptyList(),
                selected = selected,
                multi = true,
                placeholder = when (def.type) {
                    AttrType.MEASUREMENT -> "Add value, e.g. 250 ${def.options.firstOrNull() ?: def.unit}"
                    AttrType.NUMBER -> "Add a number"
                    else -> "Add another option"
                },
                onChange = onChange
            )
        }
        return
    }
    when (def.type) {
        AttrType.NUMBER -> SuqiTextField(
            value = selected.firstOrNull().orEmpty(),
            onValueChange = { raw ->
                val clean = raw.filter { it.isDigit() || it == '.' }
                onChange(if (clean.isBlank()) emptyList() else listOf(clean))
            },
            label = "",
            placeholder = def.label
        )
        AttrType.BOOLEAN -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val v = selected.firstOrNull()
            PillChip("Yes", selected = v == "true") { onChange(if (v == "true") emptyList() else listOf("true")) }
            PillChip("No", selected = v == "false") { onChange(if (v == "false") emptyList() else listOf("false")) }
        }
        AttrType.SELECT -> ChoiceListInput(def.options, selected, multi = false, placeholder = "Other…", onChange = onChange)
        AttrType.MULTI_SELECT -> ChoiceListInput(def.options, selected, multi = true, placeholder = "Other…", onChange = onChange)
        AttrType.COLOR -> ColorPaletteInput(selected, multi = false, onChange = onChange)
        AttrType.DATE -> DateInput(selected.firstOrNull().orEmpty()) { onChange(if (it.isBlank()) emptyList() else listOf(it)) }
        AttrType.MEASUREMENT -> MeasurementInput(def, selected.firstOrNull().orEmpty()) {
            onChange(if (it.isBlank()) emptyList() else listOf(it))
        }
        else -> SuqiTextField(
            value = selected.firstOrNull().orEmpty(),
            onValueChange = { onChange(if (it.isBlank()) emptyList() else listOf(it)) },
            label = "",
            placeholder = def.label,
            singleLine = def.key !in multilineKeys
        )
    }
}

@Composable
private fun ChoiceListInput(
    options: List<String>,
    selected: List<String>,
    multi: Boolean,
    placeholder: String,
    onChange: (List<String>) -> Unit
) {
    var custom by remember { mutableStateOf("") }
    val all = options + selected.filter { it !in options }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (all.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                all.forEach { option ->
                    val isSel = option in selected
                    PillChip(option, isSel) {
                        onChange(
                            when {
                                multi && isSel -> selected - option
                                multi -> selected + option
                                isSel -> emptyList()
                                else -> listOf(option)
                            }
                        )
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = custom,
                onValueChange = { custom = it },
                placeholder = { Text(placeholder, fontSize = 12.sp) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontFamily = SuqiFontFamily),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(52.dp)
            )
            TextButton(
                onClick = {
                    val v = custom.trim()
                    if (v.isNotBlank() && v !in selected) onChange(if (multi) selected + v else listOf(v))
                    custom = ""
                }
            ) { Text("+ Add", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, color = SuqiPrimary) }
        }
    }
}

@Composable
private fun ColorPaletteInput(selected: List<String>, multi: Boolean, onChange: (List<String>) -> Unit) {
    val colors = LocalSuqiColors.current
    var hex by remember { mutableStateOf("") }
    val names = ColorSwatches.named.keys.toList()
    val all = names + selected.filter { it !in names }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            all.forEach { name ->
                val argb = ColorSwatches.argbOf(name) ?: return@forEach
                val isSel = name in selected
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(argb))
                        .border(if (isSel) 2.5.dp else 1.dp, if (isSel) SuqiPrimary else colors.border, CircleShape)
                        .clickable {
                            onChange(
                                when {
                                    multi && isSel -> selected - name
                                    multi -> selected + name
                                    isSel -> emptyList()
                                    else -> listOf(name)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSel) {
                        val light = (argb and 0xFFFFFF) > 0x888888
                        Icon(LucideIcons.Check, null, tint = if (light) Color.Black else Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        if (selected.isNotEmpty()) {
            Text(
                text = selected.joinToString(", "),
                fontFamily = SuqiFontFamily,
                fontSize = 11.sp,
                color = colors.secondaryText
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = hex,
                onValueChange = { hex = it.take(7) },
                placeholder = { Text("Custom color #RRGGBB", fontSize = 12.sp) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontFamily = SuqiFontFamily),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(52.dp)
            )
            TextButton(
                onClick = {
                    val v = hex.trim().uppercase(Locale.US)
                    if (ColorSwatches.argbOf(v) != null && v !in selected) {
                        onChange(if (multi) selected + v else listOf(v))
                        hex = ""
                    }
                }
            ) { Text("+ Add", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, color = SuqiPrimary) }
        }
    }
}

@Composable
private fun DateInput(value: String, onChange: (String) -> Unit) {
    var show by remember { mutableStateOf(false) }
    val colors = LocalSuqiColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { show = true },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).height(48.dp)
        ) {
            Icon(LucideIcons.Calendar, null, tint = SuqiPrimary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = value.ifBlank { "Pick a date" },
                fontFamily = SuqiFontFamily,
                fontSize = 13.sp,
                color = if (value.isBlank()) colors.mutedText else colors.primaryText
            )
        }
        if (value.isNotBlank()) {
            SmallIconButton(LucideIcons.X, "Clear date") { onChange("") }
        }
    }
    if (show) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                        onChange(fmt.format(Date(millis)))
                    }
                    show = false
                }) { Text("OK", color = SuqiPrimary) }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun MeasurementInput(def: AttributeDef, value: String, onChange: (String) -> Unit) {
    val units = if (def.options.isNotEmpty()) def.options else listOf(def.unit.ifBlank { "unit" })
    val initialUnit = units.firstOrNull { value.endsWith(" $it") } ?: units.first()
    var unit by remember(def.key, def.options) { mutableStateOf(initialUnit) }
    var number by remember(def.key) { mutableStateOf(value.removeSuffix(" $initialUnit").takeIf { value.isNotBlank() }.orEmpty()) }

    fun push() = onChange(if (number.isBlank()) "" else "$number $unit")

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = number,
            onValueChange = { raw ->
                number = raw.filter { it.isDigit() || it == '.' }
                push()
            },
            placeholder = { Text(def.label, fontSize = 12.sp) },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontFamily = SuqiFontFamily),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).height(52.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            items(units) { u ->
                PillChip(u, selected = u == unit) {
                    unit = u
                    push()
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════
// Add / customise a parameter
// ═════════════════════════════════════════════════════════════

@Composable
private fun AddParameterDialog(state: ProductFormState, onDismiss: () -> Unit) {
    val colors = LocalSuqiColors.current
    var tab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    val existing = state.attributes.map { it.key }.toSet()
    val library = ProductTemplates.library.filter {
        it.key !in existing && (query.isBlank() || it.label.contains(query, ignoreCase = true))
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(SuqiRadius.Hero))
                .background(colors.cardBackground)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Add parameter",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.primaryText,
                    modifier = Modifier.weight(1f)
                )
                SmallIconButton(LucideIcons.X, "Close", onClick = onDismiss)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillChip("Choose from list", tab == 0) { tab = 0 }
                PillChip("Create custom", tab == 1) { tab = 1 }
            }
            Spacer(Modifier.height(10.dp))

            if (tab == 0) {
                SuqiTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = "",
                    placeholder = "Search parameters",
                    leadingIcon = LucideIcons.Search
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(library, key = { it.key }) { def ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.secondaryBackground)
                                .clickable {
                                    state.add(def.copy(key = state.uniqueKey(def.key)))
                                    onDismiss()
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(def.label, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
                                Text(AttrType.label(def.type), fontFamily = SuqiFontFamily, fontSize = 11.sp, color = colors.mutedText)
                            }
                            Icon(LucideIcons.Plus, null, tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (library.isEmpty()) {
                        item {
                            Text(
                                "Nothing left to add from the list. Try “Create custom”.",
                                fontFamily = SuqiFontFamily,
                                fontSize = 12.sp,
                                color = colors.secondaryText
                            )
                        }
                    }
                }
            } else {
                CustomParameterForm(
                    initial = null,
                    saveLabel = "Add parameter",
                    onSave = { def ->
                        state.add(def.copy(key = state.uniqueKey(def.label)))
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun CustomParameterDialog(
    initial: AttributeDef,
    state: ProductFormState,
    onDismiss: () -> Unit,
    onSave: (AttributeDef) -> Unit
) {
    val colors = LocalSuqiColors.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(SuqiRadius.Hero))
                .background(colors.cardBackground)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Edit parameter",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.primaryText,
                    modifier = Modifier.weight(1f)
                )
                SmallIconButton(LucideIcons.X, "Close", onClick = onDismiss)
            }
            Spacer(Modifier.height(10.dp))
            CustomParameterForm(initial = initial, saveLabel = "Save changes", onSave = { onSave(it.copy(key = initial.key)) })
        }
    }
}

@Composable
private fun CustomParameterForm(initial: AttributeDef?, saveLabel: String, onSave: (AttributeDef) -> Unit) {
    val colors = LocalSuqiColors.current
    var label by remember { mutableStateOf(initial?.label.orEmpty()) }
    var type by remember { mutableStateOf(initial?.type ?: AttrType.TEXT) }
    var optionsText by remember { mutableStateOf(initial?.options?.joinToString(", ").orEmpty()) }
    var section by remember { mutableStateOf(initial?.group ?: "General") }
    var required by remember { mutableStateOf(initial?.required ?: false) }
    var selectable by remember { mutableStateOf(initial?.selectable ?: false) }
    var error by remember { mutableStateOf<String?>(null) }

    val usesOptions = type == AttrType.SELECT || type == AttrType.MULTI_SELECT || type == AttrType.MEASUREMENT

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SuqiTextField(
            value = label,
            onValueChange = { label = it; error = null },
            label = "Name *",
            placeholder = "e.g. Warranty, Thread count, Mileage",
            isError = error != null,
            errorMessage = error
        )
        Text("Input type", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = colors.primaryText)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AttrType.all.forEach { t -> PillChip(AttrType.label(t), type == t) { type = t } }
        }
        if (usesOptions) {
            SuqiTextField(
                value = optionsText,
                onValueChange = { optionsText = it },
                label = if (type == AttrType.MEASUREMENT) "Units (comma separated)" else "Choices (comma separated)",
                placeholder = if (type == AttrType.MEASUREMENT) "cm, m, kg" else "Small, Medium, Large"
            )
        }
        SuqiTextField(value = section, onValueChange = { section = it }, label = "Section on details page", placeholder = "General")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Required", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.primaryText, modifier = Modifier.weight(1f))
            Switch(checked = required, onCheckedChange = { required = it }, colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary))
        }
        if (type != AttrType.BOOLEAN && type != AttrType.DATE) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Buyers choose this", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.primaryText)
                    Text("Shown as a selector (like size or color)", fontFamily = SuqiFontFamily, fontSize = 11.sp, color = colors.mutedText)
                }
                Switch(checked = selectable, onCheckedChange = { selectable = it }, colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary))
            }
        }
        Button(
            onClick = {
                if (label.isBlank()) {
                    error = "Enter a name"
                    return@Button
                }
                val opts = optionsText.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() }
                onSave(
                    AttributeDef(
                        key = initial?.key ?: "",
                        label = label.trim(),
                        type = type,
                        options = if (usesOptions) opts else emptyList(),
                        unit = if (type == AttrType.MEASUREMENT) opts.firstOrNull().orEmpty() else "",
                        required = required,
                        enabled = initial?.enabled ?: true,
                        selectable = selectable && type != AttrType.BOOLEAN && type != AttrType.DATE,
                        group = section.trim().ifBlank { "General" }
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text(saveLabel, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold) }
    }
}

// ═════════════════════════════════════════════════════════════
// Variants (pricing / stock / SKU / image per combination)
// ═════════════════════════════════════════════════════════════

/** Optional per-combination price, stock, SKU and image for the options buyers choose. */
@Composable
fun VariantsEditor(
    state: ProductFormState,
    media: List<MediaItem>,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    if (state.buyerOptions().isEmpty()) return

    SectionCard(modifier) {
        Text(
            "Variants (optional)",
            fontFamily = SuqiFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = colors.primaryText
        )
        Text(
            "Give specific combinations their own price, stock, SKU or photo. Anything left empty uses the product's main price and stock.",
            fontFamily = SuqiFontFamily,
            fontSize = 12.sp,
            color = colors.secondaryText
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { state.regenerateVariants()?.let(onMessage) },
            shape = RoundedCornerShape(999.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SuqiPrimary),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            Icon(LucideIcons.RefreshCw, null, tint = SuqiPrimary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (state.variants.isEmpty()) "Generate combinations" else "Refresh combinations",
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = SuqiPrimary
            )
        }

        val opts = state.buyerOptions().filter { it.type != AttrType.BOOLEAN }
        state.variants.forEachIndexed { index, variant ->
            androidx.compose.runtime.key(variant.id) {
                Spacer(Modifier.height(10.dp))
                VariantRow(
                    title = ProductDetails.variantLabel(opts, variant.options),
                    variant = variant,
                    media = media.filter { !it.isVideo },
                    onChange = { state.variants[index] = it }
                )
            }
        }
    }
}

@Composable
private fun VariantRow(
    title: String,
    variant: ProductVariant,
    media: List<MediaItem>,
    onChange: (ProductVariant) -> Unit
) {
    val colors = LocalSuqiColors.current
    var price by remember(variant.id) { mutableStateOf(variant.price?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty()) }
    var stock by remember(variant.id) { mutableStateOf(variant.stock?.toString().orEmpty()) }
    var sku by remember(variant.id) { mutableStateOf(variant.sku) }
    val textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = SuqiFontFamily)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.secondaryBackground)
            .padding(12.dp)
    ) {
        Text(title, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = price,
                onValueChange = { raw ->
                    price = raw.filter { it.isDigit() || it == '.' }
                    onChange(variant.copy(price = price.toDoubleOrNull()))
                },
                label = { Text("Price (ETB)", fontSize = 11.sp) },
                singleLine = true, textStyle = textStyle, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = stock,
                onValueChange = { raw ->
                    stock = raw.filter { it.isDigit() }.take(6)
                    onChange(variant.copy(stock = stock.toIntOrNull()))
                },
                label = { Text("Stock", fontSize = 11.sp) },
                singleLine = true, textStyle = textStyle, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = sku,
            onValueChange = {
                sku = it.take(40)
                onChange(variant.copy(sku = sku))
            },
            label = { Text("SKU", fontSize = 11.sp) },
            singleLine = true, textStyle = textStyle, shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        if (media.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Photo for this variant", fontFamily = SuqiFontFamily, fontSize = 11.sp, color = colors.secondaryText)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(media) { item ->
                    val isSel = variant.imageUrl == item.url
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(if (isSel) 2.dp else 1.dp, if (isSel) SuqiPrimary else colors.border, RoundedCornerShape(10.dp))
                            .clickable { onChange(variant.copy(imageUrl = if (isSel) "" else item.url)) }
                    ) { SuqiMediaThumb(item, Modifier.fillMaxSize()) }
                }
            }
        }
    }
}
