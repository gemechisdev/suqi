package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductDto
import com.example.ui.SuqiViewModel
import com.example.ui.components.StatusType
import com.example.ui.components.SuqiButton
import com.example.ui.components.SuqiMaxContentWidth
import com.example.ui.components.SuqiProductCard
import com.example.ui.components.SuqiScreen
import com.example.ui.components.SuqiSearchField
import com.example.ui.components.SuqiSectionCard
import com.example.ui.components.SuqiStatusBadge
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.LocalSuqiColors
import com.example.ui.theme.SuqiError
import com.example.ui.theme.SuqiFontFamily
import com.example.ui.theme.SuqiPrimary
import com.example.ui.theme.SuqiPrimaryHover
import com.example.ui.theme.SuqiSecondaryBgLight
import com.example.ui.theme.SuqiSecondaryTextDark
import com.example.ui.theme.SuqiSecondaryTextLight
import com.example.ui.theme.SuqiSuccess
import com.example.ui.theme.SuqiSurface
import com.example.ui.theme.SuqiSurfaceDark
import com.example.ui.theme.SuqiWarning

@Composable
fun DesignGalleryScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    var demoQuery by remember { mutableStateOf("") }

    val sampleProduct = remember {
        ProductDto(
            id = "gallery_sample",
            shopId = "shop_kaffa",
            shopName = "Kaffa Roasters",
            shopVerified = true,
            name = "Ethiopian Single-Origin Coffee",
            description = "Washed Yirgacheffe whole bean coffee, 500g roast.",
            primaryImageUrl = "",
            categoryId = "cat_food",
            subCategory = "Coffee",
            price = 850.0,
            originalPrice = 1100.0,
            discountPercent = 23,
            stockQuantity = 18,
            rating = 4.9,
            reviewCount = 42,
            soldCount = "120+ sold"
        )
    }

    SuqiScreen(
        title = strings.admin6.designGalleryTitle,
        subtitle = "Tokens, M3 Components & Accessibility",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier.testTag("design_gallery_screen")
    ) { paddingValues ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = SuqiMaxContentWidth)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SuqiSectionCard(padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Color Palette & Contrast Tokens",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val swatches = listOf(
                            "Primary" to SuqiPrimary,
                            "Pressed" to SuqiPrimaryHover,
                            "Success" to SuqiSuccess,
                            "Warning" to SuqiWarning,
                            "Error" to SuqiError,
                            "Sand" to SuqiSecondaryBgLight,
                            "LightText2" to SuqiSecondaryTextLight,
                            "DarkText2" to SuqiSecondaryTextDark
                        )
                        swatches.chunked(4).forEach { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { (label, swatch) ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(swatch)
                                                .border(1.dp, colors.border, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = label,
                                            fontFamily = SuqiFontFamily,
                                            fontSize = 10.sp,
                                            color = colors.secondaryText,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SuqiSurface)
                                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Text("Light Surface", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = Color(0xFF1F1F1F))
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SuqiSurfaceDark)
                                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Text("Dark Surface", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = Color(0xFFF5F3F0))
                            }
                        }
                    }
                }

                item {
                    SuqiSectionCard(padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Typography Scale",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Headline • 22sp ExtraBold", style = MaterialTheme.typography.headlineSmall, color = colors.primaryText)
                        Text("Title • 16sp SemiBold", style = MaterialTheme.typography.titleMedium, color = colors.primaryText)
                        Text("Body • 14sp Regular — Suqi marketplace bilingual layout", style = MaterialTheme.typography.bodyMedium, color = colors.secondaryText)
                        Text("Caption • 12sp Medium", style = MaterialTheme.typography.labelMedium, color = colors.mutedText)
                    }
                }

                item {
                    SuqiSectionCard(padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Status Badges & Controls",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            SuqiStatusBadge("Success", StatusType.SUCCESS)
                            SuqiStatusBadge("Warning", StatusType.WARNING)
                            SuqiStatusBadge("Error", StatusType.ERROR)
                            SuqiStatusBadge("Neutral", StatusType.NEUTRAL)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        SuqiSearchField(
                            value = demoQuery,
                            onValueChange = { demoQuery = it },
                            placeholder = strings.searchPlaceholder,
                            testTag = "gallery_search_input"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            SuqiButton(
                                text = "Primary CTA",
                                onClick = { viewModel.showToast("Primary CTA tapped") },
                                modifier = Modifier.weight(1f),
                                testTag = "gallery_primary_btn"
                            )
                            SuqiButton(
                                text = "Secondary Pill",
                                isPrimary = false,
                                isPill = true,
                                onClick = { viewModel.showToast("Secondary Pill tapped") },
                                modifier = Modifier.weight(1f),
                                testTag = "gallery_secondary_btn"
                            )
                        }
                    }
                }

                item {
                    SuqiSectionCard(padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Product Card Preview",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(modifier = Modifier.width(180.dp)) {
                            SuqiProductCard(
                                product = sampleProduct,
                                onClick = {},
                                onAddToCart = {},
                                onToggleWishlist = {}
                            )
                        }
                    }
                }
            }
        }
    }
}
