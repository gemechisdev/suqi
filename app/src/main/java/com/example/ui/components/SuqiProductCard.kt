package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.ProductDto
import com.example.ui.theme.LocalSuqiColors
import com.example.ui.theme.LucideIcons
import com.example.ui.theme.SuqiFontFamily
import com.example.ui.theme.SuqiImageBackdrop
import com.example.ui.theme.SuqiPrimary
import com.example.ui.theme.SuqiPrimaryTextLight
import com.example.ui.theme.SuqiRadius
import com.example.ui.theme.suqiWarmShadow
import java.text.NumberFormat
import java.util.Locale

private val LowStockText = Color(0xFFB45309)

private fun categoryLabel(categoryId: String): String = when (categoryId) {
    "cat_electronics" -> "Electronics"
    "cat_fashion" -> "Fashion"
    "cat_home" -> "Home & Living"
    "cat_beauty" -> "Beauty & Care"
    "cat_sports" -> "Sports"
    "cat_groceries" -> "Groceries"
    else -> ""
}

private fun plainNumber(amount: Double): String =
    NumberFormat.getNumberInstance(Locale.US).format(amount.toLong())

/**
 * Product item card (Suqi UI direction).
 *
 * Large product image with discount badge + favourite button, then name,
 * category, ETB pricing, shop info and an orange Add to Cart button.
 *
 * Designed for a two-column grid (≈160dp wide) but scales to wider slots.
 * Pass [onAddToCart] = null to hide the button (e.g. for a compact variant).
 */
@Composable
fun SuqiProductCard(
    product: ProductDto,
    onClick: () -> Unit,
    onToggleWishlist: () -> Unit,
    modifier: Modifier = Modifier,
    onAddToCart: (() -> Unit)? = null,
    onShopClick: (() -> Unit)? = null
) {
    val colors = LocalSuqiColors.current
    val cardShape = RoundedCornerShape(SuqiRadius.Card)
    val imageShape = RoundedCornerShape(18.dp)
    val soldOut = product.stockQuantity <= 0
    val lowStock = !soldOut && product.stockQuantity <= 3
    val hasDiscount = product.discountPercent > 0
    val subtitle = listOf(categoryLabel(product.categoryId), product.subCategory)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

    Column(
        modifier = modifier
            .suqiWarmShadow(cardShape, elevation = 10.dp)
            .clip(cardShape)
            .background(colors.cardBackground)
            .clickable(onClick = onClick)
            .padding(8.dp)
            .testTag("product_card_${product.id}")
    ) {
        // ── Image area ────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(imageShape)
                .background(if (colors.isDark) colors.secondaryBackground else SuqiImageBackdrop)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(product.primaryImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (soldOut) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF171717))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Sold out",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // Discount badge
            if (hasDiscount) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(SuqiPrimary)
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "-${product.discountPercent}%",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }

            // Favourite button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .suqiWarmShadow(CircleShape, elevation = 4.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (product.isWishlisted) "Remove from wishlist" else "Add to wishlist",
                        onClick = onToggleWishlist
                    )
                    .testTag("btn_wishlist_${product.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (product.isWishlisted) LucideIcons.HeartFilled else LucideIcons.Heart,
                    contentDescription = if (product.isWishlisted) "Remove from wishlist" else "Add to wishlist",
                    tint = if (product.isWishlisted) SuqiPrimary else SuqiPrimaryTextLight,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Low-stock hint (only when it matters)
            if (lowStock) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Only ${product.stockQuantity} left",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        color = LowStockText
                    )
                }
            }
        }

        // ── Details ───────────────────────────────────────────
        Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp)) {
            Text(
                text = product.name,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontFamily = SuqiFontFamily,
                fontSize = 12.sp,
                color = colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ETB pricing (original price is numbers-only so it fits a 160dp card)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatEtb(product.price),
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SuqiPrimary,
                    maxLines = 1,
                    softWrap = false
                )
                if (hasDiscount) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = plainNumber(product.originalPrice),
                        fontFamily = SuqiFontFamily,
                        fontSize = 11.sp,
                        color = colors.mutedText,
                        textDecoration = TextDecoration.LineThrough,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(bottom = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Shop info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (onShopClick != null) Modifier.clickable(onClick = onShopClick)
                        else Modifier
                    )
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = LucideIcons.Store,
                    contentDescription = null,
                    tint = colors.primaryText,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = product.shopName,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = colors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (product.shopVerified) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = LucideIcons.BadgeCheck,
                        contentDescription = "Verified shop",
                        tint = SuqiPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Icon(
                    imageVector = LucideIcons.ChevronRight,
                    contentDescription = null,
                    tint = colors.secondaryText,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Add to Cart
            if (onAddToCart != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAddToCart,
                    enabled = !soldOut,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuqiPrimary,
                        contentColor = Color.White,
                        disabledContainerColor = colors.primaryTint,
                        disabledContentColor = colors.mutedText
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        disabledElevation = 0.dp
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("btn_card_add_to_cart_${product.id}")
                ) {
                    Icon(
                        imageVector = LucideIcons.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (soldOut) "Sold out" else "Add to Cart",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
