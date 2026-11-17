@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ShopBusinessHoursCodec
import com.example.data.model.ShopDayHours
import com.example.data.model.ShopDeliveryOptionDto
import com.example.data.model.ShopGalleryCodec
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.LocalSuqiLanguage
import com.example.ui.i18n.localizeEta
import com.example.ui.i18n.placeLabel
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import com.example.util.EthiopianMarketplaceUtils
import kotlinx.coroutines.launch

private data class ShopProfileSnapshot(
    val shopName: String,
    val category: String,
    val description: String,
    val phone: String,
    val telegram: String,
    val whatsapp: String,
    val logoUrl: String,
    val coverUrl: String,
    val vacationMode: Boolean,
    val vacationReturnDate: String,
    val vacationNote: String,
    val businessHoursJson: String,
    val galleryJson: String,
    val instagram: String,
    val tiktok: String,
    val website: String,
    val returnPolicy: String,
    val deliveryNotesPolicy: String
)

@Composable
fun ShopSettingsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val work = strings.sellerWork
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var activeShopId by remember { mutableStateOf("") }
    var shopSlug by remember { mutableStateOf("") }
    var shopCity by remember { mutableStateOf("Addis Ababa") }
    var shopRating by remember { mutableStateOf(5.0) }
    var shopVerified by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    var shopName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var telegram by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var logoUrl by remember { mutableStateOf("") }
    var coverUrl by remember { mutableStateOf("") }
    var vacationMode by remember { mutableStateOf(false) }
    var vacationReturnDate by remember { mutableStateOf("") }
    var vacationNote by remember { mutableStateOf("") }

    var weeklyHours by remember { mutableStateOf(ShopBusinessHoursCodec.defaultWeek()) }
    var galleryUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var instagramLink by remember { mutableStateOf("") }
    var tiktokLink by remember { mutableStateOf("") }
    var websiteUrl by remember { mutableStateOf("") }
    var returnPolicy by remember { mutableStateOf("") }
    var deliveryNotesPolicy by remember { mutableStateOf("") }

    var initialSnapshot by remember { mutableStateOf<ShopProfileSnapshot?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    fun currentSnapshot(): ShopProfileSnapshot = ShopProfileSnapshot(
        shopName = shopName,
        category = category,
        description = description,
        phone = phone,
        telegram = telegram,
        whatsapp = whatsapp,
        logoUrl = logoUrl,
        coverUrl = coverUrl,
        vacationMode = vacationMode,
        vacationReturnDate = vacationReturnDate,
        vacationNote = vacationNote,
        businessHoursJson = ShopBusinessHoursCodec.toJson(weeklyHours),
        galleryJson = ShopGalleryCodec.toJson(galleryUrls),
        instagram = instagramLink,
        tiktok = tiktokLink,
        website = websiteUrl,
        returnPolicy = returnPolicy,
        deliveryNotesPolicy = deliveryNotesPolicy
    )

    val hasUnsavedChanges by remember {
        derivedStateOf {
            !isLoading && initialSnapshot != null && currentSnapshot() != initialSnapshot
        }
    }

    BackHandler(enabled = hasUnsavedChanges && !showDiscardDialog) {
        showDiscardDialog = true
    }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) logoUrl = uri.toString()
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) coverUrl = uri.toString()
    }
    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && galleryUrls.size < 6) {
            galleryUrls = (galleryUrls + uri.toString()).distinct().take(6)
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        val sid = resolveActiveSellerShopId(viewModel)
        activeShopId = sid
        if (sid.isNotBlank()) {
            val res = viewModel.repository.getShopDetails(sid)
            res.data?.let { s ->
                shopSlug = s.slug
                shopCity = s.city.ifBlank { "Addis Ababa" }
                shopRating = s.rating
                shopVerified = s.isVerified
                shopName = s.name
                category = s.category
                description = s.description
                phone = s.phone
                telegram = s.telegramLink
                whatsapp = s.whatsappLink
                logoUrl = s.logoUrl
                coverUrl = s.coverUrl
                vacationMode = s.isVacationMode
                vacationReturnDate = s.vacationReturnDate
                vacationNote = s.vacationNote
                weeklyHours = ShopBusinessHoursCodec.parse(s.businessHoursJson)
                galleryUrls = ShopGalleryCodec.parse(s.galleryJson)
                instagramLink = s.instagramLink
                tiktokLink = s.tiktokLink
                websiteUrl = s.websiteUrl
                returnPolicy = s.returnPolicy.take(400)
                deliveryNotesPolicy = s.deliveryNotesPolicy.take(400)
            }
        }
        initialSnapshot = currentSnapshot()
        isLoading = false
    }

    fun dayLabelForKey(key: String): String = when (key.uppercase()) {
        "MON" -> work.dayMon
        "TUE" -> work.dayTue
        "WED" -> work.dayWed
        "THU" -> work.dayThu
        "FRI" -> work.dayFri
        "SAT" -> work.daySat
        else -> work.daySun
    }

    SuqiScreen(
        title = strings.seller.shopBrandingTitle,
        onBackClick = {
            if (hasUnsavedChanges) showDiscardDialog = true else viewModel.goBack()
        },
        isOffline = isOffline,
        bottomBar = {
            Surface(
                color = colors.cardBackground,
                shadowElevation = if (colors.isDark) 0.dp else 12.dp,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SuqiButton(
                        text = strings.seller.saveChangesBtn,
                        onClick = {
                            if (shopName.isBlank()) {
                                viewModel.showToast(strings.seller.enterNameError)
                                return@SuqiButton
                            }
                            coroutineScope.launch {
                                val res = viewModel.repository.updateShopSettings(
                                    mapOf(
                                        "shopId" to activeShopId,
                                        "name" to shopName.trim(),
                                        "category" to category.trim(),
                                        "description" to description.trim(),
                                        "phone" to phone.trim(),
                                        "telegramLink" to telegram.trim(),
                                        "whatsappLink" to whatsapp.trim(),
                                        "logoUrl" to logoUrl,
                                        "coverUrl" to coverUrl,
                                        "isVacationMode" to vacationMode,
                                        "vacationReturnDate" to vacationReturnDate.trim(),
                                        "vacationNote" to vacationNote.trim(),
                                        "businessHoursJson" to ShopBusinessHoursCodec.toJson(weeklyHours),
                                        "galleryJson" to ShopGalleryCodec.toJson(galleryUrls),
                                        "instagramLink" to instagramLink.trim(),
                                        "tiktokLink" to tiktokLink.trim(),
                                        "websiteUrl" to websiteUrl.trim(),
                                        "returnPolicy" to returnPolicy.trim(),
                                        "deliveryNotesPolicy" to deliveryNotesPolicy.trim()
                                    )
                                )
                                if (res.success) {
                                    initialSnapshot = currentSnapshot()
                                    viewModel.showToast(strings.buyer.profileUpdatedToast)
                                    viewModel.goBack()
                                } else {
                                    viewModel.showToast(res.message ?: strings.common.failedToAddToast)
                                }
                            }
                        },
                        testTag = "save_shop_settings_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.padding(paddingValues).padding(top = 12.dp)) {
                SuqiSkeletonList(count = 4)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Live Storefront Header Preview
                item {
                    SuqiSectionCard(
                        padding = 0.dp,
                        modifier = Modifier.testTag("shop_live_header_preview")
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(112.dp)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(SuqiPrimary, SuqiPrimary.copy(alpha = 0.72f))
                                        )
                                    )
                            ) {
                                if (coverUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = coverUrl,
                                        contentDescription = strings.seller.coverImageTitle,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.28f))
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = work.liveStorefrontPreviewTitle,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(colors.primaryTint)
                                        .border(1.5.dp, colors.cardBackground, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (logoUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = logoUrl,
                                            contentDescription = shopName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(LucideIcons.Store, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = shopName.ifBlank { strings.sellerOps.mySuqiShopFallback },
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = colors.primaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (shopVerified) {
                                            Icon(LucideIcons.BadgeCheck, contentDescription = strings.verifiedLabel, tint = SuqiPrimary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Text(
                                        text = "${category.ifBlank { strings.buyer.categoryLabel }} · ${placeLabel(shopCity)} · ★ $shopRating",
                                        fontFamily = SuqiFontFamily,
                                        fontSize = 12.sp,
                                        color = colors.secondaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                SuqiStatusBadge(
                                    statusText = if (vacationMode) strings.sellerOps.vacationModeTitle else strings.common.openNowLabel,
                                    type = if (vacationMode) StatusType.WARNING else StatusType.SUCCESS
                                )
                            }
                        }
                    }
                }

                // Branding Cover & Logo Pickers
                item {
                    SuqiSectionCard(padding = 14.dp) {
                        Text(strings.sellerOps.shopLogoCoverTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(112.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.imageBackdrop)
                                .clickable {
                                    coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                                .testTag("pick_shop_cover"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (coverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = coverUrl,
                                    contentDescription = strings.seller.coverImageTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(LucideIcons.Camera, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.common.changeLabel, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(colors.primaryTint)
                                    .clickable {
                                        logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                    .testTag("pick_shop_logo"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (logoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = logoUrl,
                                        contentDescription = strings.sellerOps.shopLogoCoverTitle,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(LucideIcons.Store, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(28.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.sellerOps.shopLogoCoverTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.primaryText)
                                Text(strings.common.tapToPickPhoto, fontSize = 12.sp, color = colors.secondaryText)
                            }
                            TextButton(
                                onClick = {
                                    logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(strings.common.uploadPhotoLabel, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                // Basic Shop Identity & Contact
                item {
                    SuqiSectionCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SuqiTextField(value = shopName, onValueChange = { shopName = it }, label = strings.checkout.shopNameRequiredLabel)
                            SuqiTextField(value = category, onValueChange = { category = it }, label = strings.buyer.categoryLabel)
                            SuqiTextField(value = description, onValueChange = { description = it }, label = strings.checkout.shopDescriptionLabel, singleLine = false)
                            SuqiTextField(value = phone, onValueChange = { phone = it }, label = strings.buyer.phoneNumberLabel)
                            SuqiTextField(value = telegram, onValueChange = { telegram = it }, label = strings.checkout.telegramLabel)
                            SuqiTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = "WhatsApp")
                        }
                    }
                }

                // Opening Hours (7 days + Copy to all days)
                item {
                    SuqiSectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(LucideIcons.Clock, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                                Text(
                                    text = work.openingHoursTitle,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.primaryText
                                )
                            }
                            TextButton(
                                onClick = {
                                    val mon = weeklyHours.firstOrNull { it.dayKey == "MON" } ?: weeklyHours.first()
                                    weeklyHours = weeklyHours.map {
                                        it.copy(isOpen = mon.isOpen, openTime = mon.openTime, closeTime = mon.closeTime)
                                    }
                                    viewModel.showToast(work.copiedHoursToast)
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("copy_hours_all_days")
                            ) {
                                Icon(LucideIcons.Copy, contentDescription = null, modifier = Modifier.size(14.dp), tint = SuqiPrimary)
                                Spacer(Modifier.width(4.dp))
                                Text(work.copyToAllDaysBtn, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SuqiPrimary)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            weeklyHours.forEachIndexed { idx, day ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colors.secondaryBackground)
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = dayLabelForKey(day.dayKey),
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = colors.primaryText
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = if (day.isOpen) work.openLabel else work.closedLabel,
                                                fontFamily = SuqiFontFamily,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (day.isOpen) SuqiSuccess else colors.mutedText
                                            )
                                            Switch(
                                                checked = day.isOpen,
                                                onCheckedChange = { checked ->
                                                    weeklyHours = weeklyHours.toMutableList().apply {
                                                        this[idx] = day.copy(isOpen = checked)
                                                    }
                                                },
                                                colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary, checkedThumbColor = Color.White),
                                                modifier = Modifier.testTag("hours_switch_${day.dayKey}")
                                            )
                                        }
                                    }
                                    if (day.isOpen) {
                                        Spacer(Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                            SuqiTimeField(
                                                label = work.openTimeLabel,
                                                timeText = day.openTime,
                                                onTimeSelected = { nextTime ->
                                                    weeklyHours = weeklyHours.toMutableList().apply {
                                                        this[idx] = day.copy(openTime = nextTime)
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                            SuqiTimeField(
                                                label = work.closeTimeLabel,
                                                timeText = day.closeTime,
                                                onTimeSelected = { nextTime ->
                                                    weeklyHours = weeklyHours.toMutableList().apply {
                                                        this[idx] = day.copy(closeTime = nextTime)
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Shop Photo Gallery (up to 6 tiles)
                item {
                    SuqiSectionCard {
                        Text(
                            text = work.shopGalleryTitle,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.primaryText
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = work.shopGallerySub,
                            fontFamily = SuqiFontFamily,
                            fontSize = 12.sp,
                            color = colors.secondaryText
                        )
                        Spacer(Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shop_gallery_grid")
                        ) {
                            galleryUrls.forEachIndexed { index, url ->
                                Box(
                                    modifier = Modifier
                                        .size(94.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colors.secondaryBackground)
                                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                                ) {
                                    AsyncImage(
                                        model = url,
                                        contentDescription = work.shopGalleryTitle,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    IconButton(
                                        onClick = {
                                            galleryUrls = galleryUrls.toMutableList().apply { removeAt(index) }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(36.dp)
                                            .padding(4.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.65f))
                                    ) {
                                        Icon(
                                            LucideIcons.X,
                                            contentDescription = strings.common.removeImageDesc,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            if (galleryUrls.size < 6) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .size(94.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colors.primaryTint)
                                        .border(1.dp, SuqiPrimary.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                        .clickable {
                                            galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        }
                                        .testTag("add_shop_gallery_photo")
                                ) {
                                    Icon(LucideIcons.ImagePlus, contentDescription = work.addGalleryPhotoBtn, tint = SuqiPrimary, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = work.addGalleryPhotoBtn,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = SuqiPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Social Links & Website
                item {
                    SuqiSectionCard {
                        Text(
                            text = work.socialLinksTitle,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.primaryText
                        )
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SuqiTextField(value = instagramLink, onValueChange = { instagramLink = it }, label = work.instagramLabel)
                            SuqiTextField(value = tiktokLink, onValueChange = { tiktokLink = it }, label = work.tiktokLabel)
                            SuqiTextField(value = websiteUrl, onValueChange = { websiteUrl = it }, label = work.websiteLabel)
                        }
                    }
                }

                // Policies with character counters
                item {
                    SuqiSectionCard {
                        Text(
                            text = work.policiesSectionTitle,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.primaryText
                        )
                        Spacer(Modifier.height(10.dp))
                        SuqiTextField(
                            value = returnPolicy,
                            onValueChange = { returnPolicy = it.take(400) },
                            label = work.returnsPolicyLabel,
                            singleLine = false
                        )
                        Text(
                            text = String.format(work.charCounterTemplate, returnPolicy.length, 400),
                            fontFamily = SuqiFontFamily,
                            fontSize = 11.sp,
                            color = colors.mutedText,
                            modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        SuqiTextField(
                            value = deliveryNotesPolicy,
                            onValueChange = { deliveryNotesPolicy = it.take(400) },
                            label = work.deliveryNotesPolicyLabel,
                            singleLine = false
                        )
                        Text(
                            text = String.format(work.charCounterTemplate, deliveryNotesPolicy.length, 400),
                            fontFamily = SuqiFontFamily,
                            fontSize = 11.sp,
                            color = colors.mutedText,
                            modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                        )
                    }
                }

                // Vacation Mode
                item {
                    SuqiSectionCard {
                        SuqiSwitchRow(
                            title = strings.sellerOps.vacationModeTitle,
                            subtitle = strings.sellerOps.vacationModeSub,
                            icon = LucideIcons.Pause,
                            checked = vacationMode,
                            onCheckedChange = { vacationMode = it },
                            testTag = "toggle_vacation_mode"
                        )
                        if (vacationMode) {
                            Spacer(modifier = Modifier.height(12.dp))
                            SuqiDateField(
                                label = strings.common.selectDate,
                                dateText = vacationReturnDate,
                                onDateSelected = { vacationReturnDate = it }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            SuqiTextField(
                                value = vacationNote,
                                onValueChange = { vacationNote = it },
                                label = strings.checkout.vacationNoticeTitle,
                                singleLine = false
                            )
                        }
                    }
                }

                // Share Storefront Link & QR Card
                item {
                    val storefrontSlug = shopSlug.ifBlank { activeShopId.ifBlank { "shop" } }
                    val storefrontUrl = "https://suqi.et/shop/$storefrontSlug"
                    SuqiQrCard(
                        title = work.shareShopQrTitle,
                        subtitle = work.shareShopQrSubtitle,
                        codeValue = storefrontUrl,
                        badgeText = shopName.ifBlank { null },
                        onCopyCode = { viewModel.showToast(work.storefrontLinkCopiedToast) },
                        onShare = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, shopName.ifBlank { strings.seller.shopBrandingTitle })
                                putExtra(Intent.EXTRA_TEXT, "$shopName — $storefrontUrl")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, work.shareShopQrTitle))
                        },
                        modifier = Modifier.testTag("shop_share_qr_card")
                    )
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(work.unsavedChangesTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold) },
            text = { Text(work.unsavedChangesBody, fontFamily = SuqiFontFamily, color = colors.secondaryText) },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.goBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiError),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(work.discardChangesBtn, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDiscardDialog = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(work.keepEditingBtn)
                }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun DeliverySetupScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val work = strings.sellerWork
    val lang = LocalSuqiLanguage.current
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var activeShopId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    var stdId by remember { mutableStateOf("") }
    var expId by remember { mutableStateOf("") }
    var pickId by remember { mutableStateOf("") }

    var standardEnabled by remember { mutableStateOf(true) }
    var standardFee by remember { mutableStateOf("50") }
    var standardEta by remember { mutableStateOf("1-3 business days") }
    var standardCutoffTime by remember { mutableStateOf("16:00") }

    var expressEnabled by remember { mutableStateOf(false) }
    var expressFee by remember { mutableStateOf("120") }
    var expressEta by remember { mutableStateOf("Same-day delivery") }
    var expressCutoffTime by remember { mutableStateOf("14:00") }

    var pickupEnabled by remember { mutableStateOf(true) }
    var pickupAddress by remember { mutableStateOf("") }

    var freeThreshold by remember { mutableStateOf("0") }
    val selectedCoverageZones = remember { mutableStateListOf<String>() }
    var requireReceiptScreenshot by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        val sid = resolveActiveSellerShopId(viewModel)
        activeShopId = sid
        if (sid.isNotBlank()) {
            val shopRes = viewModel.repository.getShopDetails(sid)
            shopRes.data?.let { s ->
                pickupAddress = s.address
                val parsedZones = s.coverageSubCitiesCsv
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                selectedCoverageZones.clear()
                if (parsedZones.isNotEmpty()) {
                    selectedCoverageZones.addAll(parsedZones.distinct())
                } else {
                    val defaultZone = s.subCity.ifBlank { s.city }
                    if (defaultZone.isNotBlank()) selectedCoverageZones.add(defaultZone)
                }
                freeThreshold = if (s.freeDeliveryThreshold > 0.0) s.freeDeliveryThreshold.toLong().toString() else "0"
                requireReceiptScreenshot = s.requireReceiptScreenshot
                standardCutoffTime = s.standardCutoffTime.ifBlank { "16:00" }
                expressCutoffTime = s.expressCutoffTime.ifBlank { "14:00" }
            }
            val delRes = viewModel.repository.getShopDeliveryOptions(sid)
            val options = delRes.data.orEmpty()
            options.firstOrNull { it.optionType == "STANDARD" }?.let {
                stdId = it.id
                standardEnabled = it.isEnabled
                standardFee = it.fee.toLong().toString()
                standardEta = it.estimatedTime
            }
            options.firstOrNull { it.optionType == "EXPRESS" }?.let {
                expId = it.id
                expressEnabled = it.isEnabled
                expressFee = it.fee.toLong().toString()
                expressEta = it.estimatedTime
            }
            options.firstOrNull { it.optionType == "PICKUP" }?.let {
                pickId = it.id
                pickupEnabled = it.isEnabled
                if (it.estimatedTime.isNotBlank()) pickupAddress = it.estimatedTime
            }
        }
        isLoading = false
    }

    SuqiScreen(
        title = strings.sellerOps.deliveryRulesTitle,
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        bottomBar = {
            Surface(
                color = colors.cardBackground,
                shadowElevation = if (colors.isDark) 0.dp else 12.dp,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SuqiButton(
                        text = strings.saveLabel,
                        onClick = {
                            coroutineScope.launch {
                                if (activeShopId.isBlank()) {
                                    viewModel.showToast(strings.seller.sellerRestrictedTitle)
                                    return@launch
                                }
                                viewModel.repository.saveShopDeliveryOption(
                                    ShopDeliveryOptionDto(
                                        id = stdId.ifBlank { "del_${activeShopId}_std" },
                                        shopId = activeShopId,
                                        optionType = "STANDARD",
                                        title = "Standard Courier Delivery",
                                        fee = standardFee.toDoubleOrNull() ?: 50.0,
                                        estimatedTime = standardEta.trim().ifBlank { "1-3 business days" },
                                        isEnabled = standardEnabled
                                    )
                                )
                                viewModel.repository.saveShopDeliveryOption(
                                    ShopDeliveryOptionDto(
                                        id = expId.ifBlank { "del_${activeShopId}_exp" },
                                        shopId = activeShopId,
                                        optionType = "EXPRESS",
                                        title = "Express Same-Day Delivery",
                                        fee = expressFee.toDoubleOrNull() ?: 120.0,
                                        estimatedTime = expressEta.trim().ifBlank { "Same-day delivery" },
                                        isEnabled = expressEnabled
                                    )
                                )
                                viewModel.repository.saveShopDeliveryOption(
                                    ShopDeliveryOptionDto(
                                        id = pickId.ifBlank { "del_${activeShopId}_pickup" },
                                        shopId = activeShopId,
                                        optionType = "PICKUP",
                                        title = "In-Store Customer Pickup",
                                        fee = 0.0,
                                        estimatedTime = pickupAddress.trim().ifBlank { "Ready in 2 hours" },
                                        isEnabled = pickupEnabled
                                    )
                                )
                                viewModel.repository.updateShopSettings(
                                    mapOf(
                                        "shopId" to activeShopId,
                                        "freeDeliveryThreshold" to (freeThreshold.toDoubleOrNull() ?: 0.0),
                                        "coverageSubCitiesCsv" to selectedCoverageZones.joinToString(", "),
                                        "requireReceiptScreenshot" to requireReceiptScreenshot,
                                        "standardCutoffTime" to standardCutoffTime,
                                        "expressCutoffTime" to expressCutoffTime
                                    )
                                )
                                viewModel.showToast(strings.saveLabel)
                                viewModel.goBack()
                            }
                        },
                        testTag = "save_delivery_settings_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.padding(paddingValues).padding(top = 12.dp)) {
                SuqiSkeletonList(count = 3)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SuqiSectionCard {
                        SuqiSwitchRow(
                            title = strings.checkout.receiptScreenshotRequired,
                            subtitle = strings.checkout.receiptRequiredHint,
                            icon = LucideIcons.Receipt,
                            checked = requireReceiptScreenshot,
                            onCheckedChange = { requireReceiptScreenshot = it },
                            testTag = "toggle_require_receipt"
                        )
                    }
                }

                // Standard Delivery + Dispatch Cut-off
                item {
                    SuqiSectionCard {
                        SuqiSwitchRow(
                            title = strings.sellerOps.standardDeliveryTitle,
                            subtitle = strings.sellerOps.standardDeliveryDesc,
                            icon = LucideIcons.Truck,
                            checked = standardEnabled,
                            onCheckedChange = { standardEnabled = it }
                        )
                        if (standardEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SuqiTextField(value = standardFee, onValueChange = { standardFee = it }, label = strings.sellerOps.feeEtbLabel, modifier = Modifier.weight(1f))
                                SuqiTextField(value = standardEta, onValueChange = { standardEta = it }, label = strings.sellerOps.etaWindowLabel, modifier = Modifier.weight(1.4f))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            SuqiTimeField(
                                label = work.dispatchCutoffLabel,
                                timeText = standardCutoffTime,
                                onTimeSelected = { standardCutoffTime = it },
                                modifier = Modifier.testTag("standard_cutoff_time")
                            )
                        }
                    }
                }

                // Express Delivery + Same-day Cut-off
                item {
                    SuqiSectionCard {
                        SuqiSwitchRow(
                            title = strings.sellerOps.expressDeliveryTitle,
                            subtitle = strings.sellerOps.expressDeliveryDesc,
                            icon = LucideIcons.Zap,
                            checked = expressEnabled,
                            onCheckedChange = { expressEnabled = it }
                        )
                        if (expressEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SuqiTextField(value = expressFee, onValueChange = { expressFee = it }, label = strings.sellerOps.feeEtbLabel, modifier = Modifier.weight(1f))
                                SuqiTextField(value = expressEta, onValueChange = { expressEta = it }, label = strings.sellerOps.cutoffWindowLabel, modifier = Modifier.weight(1.4f))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            SuqiTimeField(
                                label = work.expressCutoffLabel,
                                timeText = expressCutoffTime,
                                onTimeSelected = { expressCutoffTime = it },
                                modifier = Modifier.testTag("express_cutoff_time")
                            )
                        }
                    }
                }

                // Pickup
                item {
                    SuqiSectionCard {
                        SuqiSwitchRow(
                            title = strings.sellerOps.pickupTitle,
                            subtitle = strings.sellerOps.pickupDesc,
                            icon = LucideIcons.Store,
                            checked = pickupEnabled,
                            onCheckedChange = { pickupEnabled = it }
                        )
                        if (pickupEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            SuqiTextField(value = pickupAddress, onValueChange = { pickupAddress = it }, label = strings.sellerOps.pickupLandmarkLabel, singleLine = false)
                        }
                    }
                }

                // Coverage Zones (grouped chips + Select all / Clear + Free Threshold)
                item {
                    SuqiSectionCard {
                        Text(strings.sellerOps.coverageSectionTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                        Spacer(modifier = Modifier.height(10.dp))
                        SuqiTextField(value = freeThreshold, onValueChange = { freeThreshold = it }, label = strings.sellerOps.freeDeliveryMinLabel)

                        Spacer(modifier = Modifier.height(14.dp))
                        val addisZones = EthiopianMarketplaceUtils.ADDIS_ABABA_SUB_CITIES
                        val allAddisSelected = addisZones.all { it in selectedCoverageZones }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = work.addisSubCitiesGroupTitle,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = colors.primaryText
                            )
                            TextButton(
                                onClick = {
                                    if (allAddisSelected) {
                                        selectedCoverageZones.removeAll(addisZones)
                                    } else {
                                        addisZones.forEach { if (it !in selectedCoverageZones) selectedCoverageZones.add(it) }
                                    }
                                },
                                modifier = Modifier.heightIn(min = 48.dp).testTag("select_all_addis")
                            ) {
                                Text(
                                    text = if (allAddisSelected) work.clearGroupLabel else work.selectAllLabel,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = SuqiPrimary
                                )
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            addisZones.forEach { zone ->
                                val selected = zone in selectedCoverageZones
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        if (selected) selectedCoverageZones.remove(zone) else selectedCoverageZones.add(zone)
                                    },
                                    label = {
                                        Text(
                                            text = placeLabel(zone),
                                            fontFamily = SuqiFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.primaryTint,
                                        selectedLabelColor = SuqiPrimary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        val regionalZones = EthiopianMarketplaceUtils.REGIONAL_COMMERCIAL_HUBS
                        val allRegionalSelected = regionalZones.all { it in selectedCoverageZones }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = work.regionalHubsGroupTitle,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = colors.primaryText
                            )
                            TextButton(
                                onClick = {
                                    if (allRegionalSelected) {
                                        selectedCoverageZones.removeAll(regionalZones)
                                    } else {
                                        regionalZones.forEach { if (it !in selectedCoverageZones) selectedCoverageZones.add(it) }
                                    }
                                },
                                modifier = Modifier.heightIn(min = 48.dp).testTag("select_all_regional")
                            ) {
                                Text(
                                    text = if (allRegionalSelected) work.clearGroupLabel else work.selectAllLabel,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = SuqiPrimary
                                )
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            regionalZones.forEach { zone ->
                                val selected = zone in selectedCoverageZones
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        if (selected) selectedCoverageZones.remove(zone) else selectedCoverageZones.add(zone)
                                    },
                                    label = {
                                        Text(
                                            text = placeLabel(zone),
                                            fontFamily = SuqiFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.primaryTint,
                                        selectedLabelColor = SuqiPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                // Preview as Buyer (Checkout Summary Card)
                item {
                    val thresholdVal = freeThreshold.toDoubleOrNull() ?: 0.0
                    val stdFeeVal = standardFee.toDoubleOrNull() ?: 50.0
                    val expFeeVal = expressFee.toDoubleOrNull() ?: 120.0
                    SuqiSectionCard(
                        containerColor = colors.shopTint,
                        modifier = Modifier.testTag("delivery_buyer_preview_card")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(LucideIcons.Eye, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                            Text(
                                text = work.previewAsBuyerDeliveryTitle,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.primaryText
                            )
                        }
                        if (thresholdVal > 0.0) {
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.primaryTint)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = String.format(work.freeDeliveryAboveTemplate, formatEtb(thresholdVal)),
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = SuqiPrimary
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (standardEnabled) {
                                BuyerDeliveryOptionPreviewRow(
                                    title = strings.checkout.standardDeliveryTitle,
                                    subtitle = "${strings.extras.localizeEta(standardEta.ifBlank { "1-3 business days" }, lang)} · ${work.dispatchCutoffLabel}: $standardCutoffTime",
                                    feeLabel = if (stdFeeVal <= 0.0) strings.checkout.freeLabel else formatEtb(stdFeeVal)
                                )
                            }
                            if (expressEnabled) {
                                BuyerDeliveryOptionPreviewRow(
                                    title = strings.checkout.expressDeliveryTitle,
                                    subtitle = "${strings.extras.localizeEta(expressEta.ifBlank { "Same-day delivery" }, lang)} · ${work.expressCutoffLabel}: $expressCutoffTime",
                                    feeLabel = if (expFeeVal <= 0.0) strings.checkout.freeLabel else formatEtb(expFeeVal)
                                )
                            }
                            if (pickupEnabled) {
                                BuyerDeliveryOptionPreviewRow(
                                    title = strings.checkout.pickupDeliveryTitle,
                                    subtitle = pickupAddress.ifBlank { strings.checkout.pickupDeliverySub },
                                    feeLabel = strings.checkout.freeLabel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BuyerDeliveryOptionPreviewRow(
    title: String,
    subtitle: String,
    feeLabel: String
) {
    val colors = LocalSuqiColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBackground)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = colors.primaryText
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = SuqiFontFamily,
                fontSize = 11.sp,
                color = colors.secondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = feeLabel,
            fontFamily = SuqiFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = SuqiPrimary
        )
    }
}
