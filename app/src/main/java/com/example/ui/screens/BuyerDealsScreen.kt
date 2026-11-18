package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.BuyerPreferencesStore
import com.example.data.model.CategoryDto
import com.example.data.model.FlashSaleDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.LocalSuqiLanguage
import com.example.ui.i18n.localizedName
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BuyerDealsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p5 = strings.phase5
    val lang = LocalSuqiLanguage.current
    val isOffline by viewModel.isOffline.collectAsState()
    val prefsStore = remember { BuyerPreferencesStore(context) }

    var flashSales by remember { mutableStateOf(viewModel.cachedFlashSales) }
    var categories by remember { mutableStateOf(viewModel.cachedHomeCategories) }
    var selectedCategoryId by rememberSaveable { mutableStateOf("ALL") }
    var isLoading by remember { mutableStateOf(flashSales.isEmpty()) }
    var notifyNextDrop by remember { mutableStateOf(prefsStore.loadDealsDropNotify()) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        isLoading = flashSales.isEmpty()
        viewModel.repository.getFlashSales(activeOnly = true).data?.let {
            flashSales = it
            viewModel.cachedFlashSales = it
        }
        viewModel.repository.getCategories().data?.let {
            categories = it
            viewModel.cachedHomeCategories = it
        }
        isLoading = false
        while (true) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val filtered = remember(flashSales, selectedCategoryId) {
        if (selectedCategoryId == "ALL") flashSales
        else flashSales.filter { it.categoryId == selectedCategoryId }
    }
    val nextEndMillis = filtered.mapNotNull { it.endAt.toLongOrNull() }.minOrNull()
        ?: (nowMillis + 4L * 3600_000L)
    val remainingSecs = ((nextEndMillis - nowMillis) / 1000L).coerceAtLeast(0L)
    val hours = (remainingSecs / 3600L).toInt()
    val mins = ((remainingSecs % 3600L) / 60L).toInt()
    val secs = (remainingSecs % 60L).toInt()

    SuqiScreen(
        title = strings.dealsTitle,
        subtitle = "${filtered.size}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.horizontalGradient(listOf(SuqiPrimary, Color(0xFFFF8C42))))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(LucideIcons.Flame, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(p5.flashDealsHeaderTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(p5.endsInLabel, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.SemiBold)
                            SuqiCountdownChips(hours = hours, minutes = mins, seconds = secs, modifier = Modifier.testTag("deals_countdown"))
                        }
                    }
                }
            }

            item {
                val chipLabels = listOf(strings.allLabel) + categories.take(5).map { it.localizedName(lang) }
                val activeLbl = if (selectedCategoryId == "ALL") strings.allLabel
                else categories.firstOrNull { it.id == selectedCategoryId }?.localizedName(lang) ?: strings.allLabel
                SuqiFilterChips(
                    items = chipLabels,
                    selectedItem = activeLbl,
                    onSelect = { chosen ->
                        selectedCategoryId = if (chosen == strings.allLabel) "ALL"
                        else categories.firstOrNull { it.localizedName(lang) == chosen }?.id ?: "ALL"
                    },
                    modifier = Modifier.testTag("deals_category_chips")
                )
            }

            if (isLoading && flashSales.isEmpty()) {
                item { SuqiSkeletonList(count = 3) }
            } else if (filtered.isEmpty()) {
                item {
                    SuqiSectionCard(padding = 18.dp) {
                        SuqiEmptyState(
                            icon = LucideIcons.Flame,
                            title = p5.noFlashSalesTitle,
                            subtitle = p5.noFlashSalesSub,
                            testTag = "deals_empty_state"
                        )
                        Spacer(Modifier.height(10.dp))
                        SuqiSwitchRow(
                            title = p5.notifyNextDropToggle,
                            checked = notifyNextDrop,
                            onCheckedChange = { enabled ->
                                notifyNextDrop = enabled
                                prefsStore.saveDealsDropNotify(enabled)
                            },
                            testTag = "deals_notify_drop_switch"
                        )
                    }
                }
            } else {
                items(filtered, key = { it.id }) { deal ->
                    val pctClaimed = if (deal.stockCap > 0) ((deal.claimedCount * 100f) / deal.stockCap.toFloat()).toInt().coerceIn(0, 100) else 0
                    SuqiSectionCard(
                        padding = 14.dp,
                        onClick = { viewModel.navigate(ScreenDestination.ProductDetails(deal.productId)) },
                        modifier = Modifier.testTag("deal_card_${deal.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(92.dp)) {
                                AsyncImage(
                                    model = deal.productImageUrl,
                                    contentDescription = deal.productName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SuqiError)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("-${deal.discountPercent}%", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(deal.shopName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SuqiPrimary)
                                Text(deal.productName, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(formatEtb(deal.salePrice), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SuqiPrimary)
                                    Text(
                                        formatEtb(deal.originalPrice),
                                        fontSize = 12.sp,
                                        color = colors.mutedText,
                                        textDecoration = TextDecoration.LineThrough
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { pctClaimed / 100f },
                                    color = SuqiPrimary,
                                    trackColor = colors.secondaryBackground,
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(String.format(p5.claimedPercentTemplate, pctClaimed), fontSize = 11.sp, color = colors.secondaryText)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        SuqiButton(
                            text = strings.addToCart,
                            icon = LucideIcons.ShoppingCart,
                            isPill = true,
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.repository.addToCart(deal.productId, 1, "")
                                    viewModel.showToast(strings.addedToCart)
                                }
                            },
                            testTag = "deal_add_cart_${deal.id}",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
