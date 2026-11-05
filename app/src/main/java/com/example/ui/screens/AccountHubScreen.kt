package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AccountHubScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    var user by remember { mutableStateOf<UserDto?>(null) }
    var followedCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val res = viewModel.repository.getCurrentUser()
        if (res.success && res.data != null) {
            user = res.data
        }
        val fRes = viewModel.repository.getShops(filter = "FOLLOWING")
        if (fRes.success && fRes.data != null) {
            followedCount = fRes.data.size
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Account",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    color = SuqiPrimaryText
                )

                IconButton(
                    onClick = { viewModel.navigate(ScreenDestination.AppSettings) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SuqiSecondaryBackground)
                ) {
                    Icon(LucideIcons.Settings, contentDescription = "Settings", tint = SuqiPrimaryText)
                }
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Profile Header Card (Mockup 3)
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SuqiSecondaryBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(SuqiPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "FG",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user?.name ?: "Fitsum Gebre",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = SuqiPrimaryText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = user?.phone ?: "+251 912 345 678",
                                fontFamily = SuqiFontFamily,
                                fontSize = 13.sp,
                                color = SuqiSecondaryText
                            )
                        }

                        Icon(
                            imageVector = LucideIcons.ArrowRight,
                            contentDescription = null,
                            tint = SuqiSecondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Seller Studio / Become a Seller Banner (Checklist 14)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = LocalSuqiColors.current.primaryTint),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SuqiPrimary.copy(alpha = 0.3f))
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SuqiPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(LucideIcons.Store, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Seller Studio",
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = SuqiPrimaryText
                                )
                                Text(
                                    text = "Manage shop, products and orders",
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 12.sp,
                                    color = SuqiSecondaryText
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.navigate(ScreenDestination.SellerStudio) },
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("Open", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Menu Items List
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    AccountMenuItem(
                        icon = LucideIcons.Receipt,
                        title = "My Orders",
                        onClick = { viewModel.navigate(ScreenDestination.MyOrders) }
                    )
                    AccountMenuItem(
                        icon = LucideIcons.Heart,
                        title = "Wishlist",
                        badge = "12",
                        onClick = { viewModel.navigate(ScreenDestination.Wishlist) }
                    )
                    AccountMenuItem(
                        icon = LucideIcons.HeartFilled,
                        title = "Following",
                        badge = if (followedCount > 0) "$followedCount" else null,
                        onClick = { viewModel.navigate(ScreenDestination.Following) }
                    )
                    AccountMenuItem(
                        icon = LucideIcons.MapPin,
                        title = "Saved Addresses",
                        onClick = { viewModel.navigate(ScreenDestination.SavedAddresses) }
                    )
                    AccountMenuItem(
                        icon = LucideIcons.MessageCircle,
                        title = "Messages & Support Chat",
                        onClick = { viewModel.navigate(ScreenDestination.ChatList) }
                    )
                    AccountMenuItem(
                        icon = LucideIcons.Settings,
                        title = "App Settings",
                        onClick = { viewModel.navigate(ScreenDestination.AppSettings) }
                    )
                    AccountMenuItem(
                        icon = LucideIcons.HelpCircle,
                        title = "Help & Support",
                        onClick = { viewModel.navigate(ScreenDestination.HelpLegal) }
                    )
                    AccountMenuItem(
                        icon = LucideIcons.Shield,
                        title = "Admin Console",
                        onClick = { viewModel.navigate(ScreenDestination.AdminConsole) }
                    )
                }
            }

            // Suqi Premium / Subscription Banner
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = LocalSuqiColors.current.primaryTint),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SuqiWarning.copy(alpha = 0.4f))
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(LucideIcons.Award, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Suqi Premium",
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Exclusive seller analytics & placement",
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 12.sp,
                                    color = SuqiSecondaryText
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.navigate(ScreenDestination.SellerSubscription) },
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuqiWarning),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Subscribe", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountMenuItem(
    icon: ImageVector,
    title: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(SuqiSecondaryBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = SuqiPrimaryText, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title,
            fontFamily = SuqiFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = SuqiPrimaryText,
            modifier = Modifier.weight(1f)
        )

        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SuqiPrimary)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Icon(
            imageVector = LucideIcons.ArrowRight,
            contentDescription = null,
            tint = SuqiMutedText,
            modifier = Modifier.size(16.dp)
        )
    }
}
