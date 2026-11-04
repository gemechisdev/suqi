package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.ProductDto
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.*

fun formatEtb(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "ETB ${formatter.format(amount.toLong())}"
}

@Composable
fun SuqiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    isPill: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    testTag: String = "suqi_button"
) {
    val shape = if (isPill) RoundedCornerShape(999.dp) else RoundedCornerShape(16.dp)

    if (isPrimary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = SuqiPrimary,
                contentColor = Color.White,
                disabledContainerColor = SuqiPrimary.copy(alpha = 0.5f),
                disabledContentColor = Color.White.copy(alpha = 0.8f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 2.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            modifier = modifier
                .heightIn(min = 48.dp)
                .testTag(testTag)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = LocalSuqiColors.current.cardBackground,
                contentColor = SuqiPrimaryText
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder),
                width = 1.dp
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            modifier = modifier
                .heightIn(min = 48.dp)
                .testTag(testTag)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = SuqiPrimaryText
                )
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun SuqiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    testTag: String = "suqi_text_field"
) {
    Column(modifier = modifier) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = SuqiPrimaryText,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    fontFamily = SuqiFontFamily,
                    fontSize = 15.sp,
                    color = SuqiMutedText
                )
            },
            leadingIcon = if (leadingIcon != null) {
                { Icon(leadingIcon, contentDescription = null, tint = SuqiSecondaryText) }
            } else null,
            trailingIcon = trailingIcon,
            singleLine = singleLine,
            isError = isError,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SuqiSecondaryBackground,
                unfocusedContainerColor = SuqiSecondaryBackground,
                disabledContainerColor = SuqiSecondaryBackground,
                focusedBorderColor = SuqiPrimary,
                unfocusedBorderColor = SuqiBorder,
                errorBorderColor = SuqiError,
                focusedTextColor = SuqiPrimaryText,
                unfocusedTextColor = SuqiPrimaryText
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                fontFamily = SuqiFontFamily,
                fontSize = 12.sp,
                color = SuqiError,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

@Composable
fun SuqiCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    elevation: Dp = 0.dp,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val cardBg = LocalSuqiColors.current.cardBackground
    val cardBorder = borderColor ?: LocalSuqiColors.current.border
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(cardBorder)
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(padding),
            content = content
        )
    }
}

@Composable
fun SuqiStatusBadge(
    statusText: String,
    type: StatusType = StatusType.SUCCESS,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (type) {
        StatusType.SUCCESS -> Pair(SuqiSuccessBg, SuqiSuccess)
        StatusType.WARNING -> Pair(SuqiWarningBg, SuqiWarning)
        StatusType.ERROR -> Pair(SuqiErrorBg, SuqiError)
        StatusType.NEUTRAL -> Pair(SuqiSecondaryBackground, SuqiSecondaryText)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusText,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = textColor
            )
        }
    }
}

enum class StatusType { SUCCESS, WARNING, ERROR, NEUTRAL }

@Composable
fun SuqiSelectionCard(
    selected: Boolean,
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    badgeText: String? = null,
    trailingText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val colors = LocalSuqiColors.current
    val bgColor = if (selected) colors.primaryTint else colors.cardBackground
    val borderColor = if (selected) SuqiPrimary else SuqiBorder

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bgColor)
            .border(1.5.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) LocalSuqiColors.current.cardBackground else SuqiSecondaryBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) SuqiPrimary else SuqiPrimaryText,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = SuqiPrimaryText
                    )
                    if (badgeText != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SuqiSuccessBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = SuqiSuccess
                            )
                        }
                    }
                }
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontFamily = SuqiFontFamily,
                        fontSize = 13.sp,
                        color = SuqiSecondaryText
                    )
                }
            }

            if (trailingText != null) {
                Text(
                    text = trailingText,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = SuqiPrimaryText
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Radio button visual
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (selected) SuqiPrimary else SuqiBorder, CircleShape)
                    .background(LocalSuqiColors.current.cardBackground),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(SuqiPrimary)
                    )
                }
            }
        }
    }
}

@Composable
fun SuqiProductCard(
    product: ProductDto,
    onClick: () -> Unit,
    onToggleWishlist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(colors.border)
        ),
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(colors.secondaryBackground)
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

                // Discount Pill
                if (product.discountPercent > 0) {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SuqiPrimary)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "-${product.discountPercent}%",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }

                // Wishlist Icon
                IconButton(
                    onClick = onToggleWishlist,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(34.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(colors.cardBackground.copy(alpha = 0.85f))
                ) {
                    Icon(
                        imageVector = if (product.isWishlisted) LucideIcons.HeartFilled else LucideIcons.Heart,
                        contentDescription = "Wishlist",
                        tint = if (product.isWishlisted) SuqiPrimary else colors.secondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.name,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = LucideIcons.StarFilled,
                        contentDescription = null,
                        tint = SuqiWarning,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${product.rating} (${product.reviewCount})",
                        fontFamily = SuqiFontFamily,
                        fontSize = 12.sp,
                        color = colors.secondaryText
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Price line
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatEtb(product.price),
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = SuqiPrimary
                    )
                }

                if (product.discountPercent > 0) {
                    Text(
                        text = formatEtb(product.originalPrice),
                        fontFamily = SuqiFontFamily,
                        fontSize = 12.sp,
                        color = colors.mutedText,
                        textDecoration = TextDecoration.LineThrough
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Stock indicator
                if (product.stockQuantity <= 3) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SuqiWarningBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Only ${product.stockQuantity} left",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            color = SuqiWarning
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SuqiSuccessBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "In Stock",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            color = SuqiSuccess
                        )
                    }
                }
            }
        }
    }
}

/** Content padding reserved under the floating liquid bottom nav. */
val SuqiBottomNavContentPadding = 96.dp

/**
 * Fluid / liquid bottom navigation (Plan §11 + mockup).
 * Floating rounded surface, centre Search as a solid orange circle,
 * soft orange liquid glow under the active/centre area.
 */
@Composable
fun SuqiFloatingBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val isSearch = currentRoute == "search"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Soft liquid orange glow under the bar (mockup style)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 10.dp)
                .width(120.dp)
                .height(28.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            SuqiPrimary.copy(alpha = 0.35f),
                            SuqiPrimary.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(50)
                )
        )

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.cardBackground,
            shadowElevation = 12.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(
                    route = "home",
                    label = "Home",
                    selectedIcon = LucideIcons.House,
                    unselectedIcon = LucideIcons.House,
                    isSelected = currentRoute == "home",
                    onClick = { onNavigate("home") }
                )
                NavItem(
                    route = "categories",
                    label = "Categories",
                    selectedIcon = LucideIcons.LayoutGrid,
                    unselectedIcon = LucideIcons.LayoutGrid,
                    isSelected = currentRoute == "categories",
                    onClick = { onNavigate("categories") }
                )

                // Centre Search – solid orange circle (mockup + plan centre prominence)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(SuqiPrimary)
                        .clickable { onNavigate("search") }
                        .testTag("nav_search"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = LucideIcons.Search,
                        contentDescription = "Search",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                NavItem(
                    route = "shops",
                    label = "Shops",
                    selectedIcon = LucideIcons.Store,
                    unselectedIcon = LucideIcons.Store,
                    isSelected = currentRoute == "shops",
                    onClick = { onNavigate("shops") }
                )
                NavItem(
                    route = "account",
                    label = "Account",
                    selectedIcon = LucideIcons.User,
                    unselectedIcon = LucideIcons.User,
                    isSelected = currentRoute == "account",
                    onClick = { onNavigate("account") }
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    route: String,
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalSuqiColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("nav_$route")
    ) {
        Icon(
            imageVector = if (isSelected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = if (isSelected) SuqiPrimary else colors.secondaryText,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontFamily = SuqiFontFamily,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 11.sp,
            color = if (isSelected) SuqiPrimary else colors.secondaryText
        )
    }
}

@Composable
fun SuqiTopHeader(
    title: String? = null,
    cartCount: Int = 0,
    notificationCount: Int = 0,
    onCartClick: () -> Unit,
    onNotificationClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    showThemeToggle: Boolean = false,
    isDarkTheme: Boolean = LocalSuqiColors.current.isDark,
    onToggleTheme: (() -> Unit)? = null,
    locationText: String? = null,
    onLocationClick: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    val colors = LocalSuqiColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (onBackClick != null) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.secondaryBackground)
                    .testTag("header_back_button")
            ) {
                Icon(
                    imageVector = LucideIcons.ArrowLeft,
                    contentDescription = "Back",
                    tint = colors.primaryText,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else if (title != null) {
            Text(
                text = title,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                color = colors.primaryText
            )
        } else {
            // Suqi Brand Logo (app mark + wordmark)
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.suqi_logo),
                    contentDescription = "Suqi",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Suqi",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 24.sp,
                    color = SuqiPrimary
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (actions != null) {
                actions()
            }

            // Quick Light / Dark mode toggle switch/icon
            if (showThemeToggle && onToggleTheme != null) {
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.secondaryBackground)
                        .testTag("header_theme_toggle")
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) LucideIcons.Sun else LucideIcons.Moon,
                        contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                        tint = if (isDarkTheme) Color(0xFFFBBF24) else colors.primaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Notification Bell with badge counter
            if (onNotificationClick != null) {
                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.secondaryBackground)
                        .testTag("header_notifications")
                ) {
                    BadgedBox(
                        badge = {
                            if (notificationCount > 0) {
                                Badge(
                                    containerColor = SuqiPrimary,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = notificationCount.toString(),
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = LucideIcons.Bell,
                            contentDescription = "Notifications",
                            tint = colors.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Cart icon with count badge
            IconButton(
                onClick = onCartClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.secondaryBackground)
                    .testTag("header_cart")
            ) {
                BadgedBox(
                    badge = {
                        if (cartCount > 0) {
                            Badge(
                                containerColor = SuqiPrimary,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = cartCount.toString(),
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = LucideIcons.ShoppingCart,
                        contentDescription = "Cart",
                        tint = colors.primaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Clickable mention tag for #product or @shop inside chat or listings
 */
@Composable
fun MentionChip(
    type: String, // "product" or "shop"
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isProduct = type == "product"
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isProduct) SuqiPrimaryTint else LocalSuqiColors.current.secondaryBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isProduct) LucideIcons.ShoppingBag else LucideIcons.Store,
                contentDescription = null,
                tint = if (isProduct) SuqiPrimary else LocalSuqiColors.current.primaryText,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = (if (isProduct) "#" else "@") + label,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isProduct) SuqiPrimary else LocalSuqiColors.current.primaryText
            )
        }
    }
}

