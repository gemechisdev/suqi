package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrderDto
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.LocalSuqiLanguage
import com.example.ui.i18n.SuqiLanguage
import com.example.ui.i18n.localizeEta
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import com.example.util.EthiopianMarketplaceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * "Arrives Tue, 12 Nov": order date plus the upper bound of the shop's ETA window.
 * Returns null when the ETA is empty, the order is closed, or the dates cannot be parsed
 * (callers then fall back to the plain ETA text).
 */
fun estimatedArrivalDate(order: OrderDto): String? {
    if (order.orderStatus == "Cancelled" || order.orderStatus == "Delivered") return null
    val days = Regex("(\\d+)").findAll(order.deliveryEstimatedDays).map { it.value.toInt() }.toList()
    val maxDays = days.maxOrNull() ?: if (order.deliveryEstimatedDays.contains("same", true)) 0 else return null
    val created = EthiopianMarketplaceUtils.parseMarketplaceTimestampMillis(order.createdAt) ?: return null
    return SimpleDateFormat("EEE, d MMM", Locale.US).format(Date(created + maxDays * 86_400_000L))
}

@Composable
fun EstimatedDeliveryChip(order: OrderDto, modifier: Modifier = Modifier) {
    val strings = suqiStrings()
    val colors = LocalSuqiColors.current
    val lang = LocalSuqiLanguage.current
    if (order.orderStatus == "Cancelled" || order.orderStatus == "Delivered" || order.deliveryEstimatedDays.isBlank()) return
    val label = estimatedArrivalDate(order)
        ?: strings.extras.localizeEta(order.deliveryEstimatedDays, lang)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colors.primaryTint)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("order_eta_chip")
    ) {
        Icon(LucideIcons.Truck, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(String.format(strings.buyerWork.etaArrivesTemplate, label), fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SuqiPrimary)
    }
}

/** B-08 returns pill: active within 7 days of delivery, or links to existing return status. */
@Composable
fun ReturnItemPill(
    order: OrderDto,
    modifier: Modifier = Modifier,
    existingReturn: com.example.data.model.ReturnRequestDto? = null,
    onRequestReturn: () -> Unit = {},
    onViewReturnStatus: (com.example.data.model.ReturnRequestDto) -> Unit = {}
) {
    val strings = suqiStrings()
    val colors = LocalSuqiColors.current
    if (!order.orderStatus.equals("Delivered", ignoreCase = true)) return
    val ageHours = com.example.util.EthiopianMarketplaceUtils.computeElapsedHours(order.createdAt)
    val within7Days = ageHours == null || ageHours <= 7.0 * 24.0
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        if (existingReturn != null) {
            OutlinedButton(
                onClick = { onViewReturnStatus(existingReturn) },
                shape = RoundedCornerShape(999.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SuqiPrimary),
                modifier = Modifier.heightIn(min = 48.dp).testTag("order_return_pill")
            ) {
                Icon(LucideIcons.RotateCcw, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("${strings.phase5.viewReturnStatusBtn} (${existingReturn.status})", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, color = SuqiPrimary)
            }
        } else {
            OutlinedButton(
                onClick = onRequestReturn,
                enabled = within7Days,
                shape = RoundedCornerShape(999.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (within7Days) SuqiPrimary else colors.border),
                modifier = Modifier.heightIn(min = 48.dp).testTag("order_return_pill")
            ) {
                Icon(LucideIcons.RotateCcw, contentDescription = null, tint = if (within7Days) SuqiPrimary else colors.mutedText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    strings.buyerWork.returnItemBtn,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = if (within7Days) SuqiPrimary else colors.mutedText
                )
            }
            if (!within7Days) {
                Text(
                    strings.phase5.returnWindowExpiredMsg,
                    fontFamily = SuqiFontFamily,
                    fontSize = 11.sp,
                    color = colors.mutedText,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp).testTag("order_return_reason")
                )
            }
        }
    }
}

private fun invoiceLines(order: OrderDto, w: com.example.ui.i18n.SuqiPhase4Strings): List<Pair<String, String>> = buildList {
    add(w.invoiceOrderRef to "#${order.orderReference}")
    add(w.invoiceDateLabel to order.createdAt)
    order.items.forEach { add("${it.quantity} x ${it.productName}" to formatEtb(it.totalPrice)) }
    add(w.invoiceSubtotalLabel to formatEtb(order.subtotal))
    add(w.invoiceDeliveryLabel to formatEtb(order.deliveryFee))
    if (order.discountAmount > 0.0) add(w.invoiceDiscountLabel to "- ${formatEtb(order.discountAmount)}")
    add(w.invoiceTotalLabel to formatEtb(order.totalAmount))
    add(w.invoicePaymentLabel to "${order.paymentMethodTitle} · ${order.transactionReference}")
}

/** Draws the invoice as a PNG bitmap for Save / Share. */
fun renderInvoiceBitmap(order: OrderDto, w: com.example.ui.i18n.SuqiPhase4Strings): Bitmap {
    val lines = invoiceLines(order, w)
    val width = 900
    val rowH = 56
    val height = 260 + lines.size * rowH + 80
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    c.drawColor(AndroidColor.WHITE)
    val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(255, 101, 0); textSize = 56f; typeface = Typeface.DEFAULT_BOLD }
    val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(23, 23, 23); textSize = 36f; typeface = Typeface.DEFAULT_BOLD }
    val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(82, 82, 82); textSize = 30f }
    val value = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(23, 23, 23); textSize = 30f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.RIGHT }
    c.drawText("Suqi · ${w.invoiceTitle}", 48f, 90f, title)
    c.drawText(order.shopName, 48f, 150f, sub)
    var y = 230f
    lines.forEach { (l, v) ->
        c.drawText(l.take(34), 48f, y, label)
        c.drawText(v.take(32), width - 48f, y, value)
        y += rowH
    }
    return bmp
}

/** B-07 invoice sheet: shop header, items, fees, total, payment, QR with the order reference, Share and Save. */
@Composable
fun InvoiceSheet(order: OrderDto, viewModel: SuqiViewModel, onDismiss: () -> Unit) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val w = strings.buyerWork
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    SuqiBottomSheet(onDismissRequest = onDismiss, title = w.invoiceTitle) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).testTag("invoice_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SuqiSectionCard(containerColor = colors.secondaryBackground, padding = 14.dp) {
                Text(order.shopName, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = colors.primaryText)
                Text("${w.invoiceOrderRef}: #${order.orderReference}", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                Text("${w.invoiceDateLabel}: ${order.createdAt}", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
            }
            SuqiSectionCard(padding = 14.dp) {
                Text(w.invoiceItemsLabel, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                Spacer(modifier = Modifier.height(6.dp))
                order.items.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${item.quantity} × ${item.productName}", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText, modifier = Modifier.weight(1f))
                        Text(formatEtb(item.totalPrice), fontFamily = SuqiFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.primaryText)
                    }
                }
                HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 8.dp))
                InvoiceRow(w.invoiceSubtotalLabel, formatEtb(order.subtotal))
                InvoiceRow(w.invoiceDeliveryLabel, formatEtb(order.deliveryFee))
                if (order.discountAmount > 0.0) InvoiceRow(w.invoiceDiscountLabel, "- ${formatEtb(order.discountAmount)}")
                HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 8.dp))
                InvoiceRow(w.invoiceTotalLabel, formatEtb(order.totalAmount), bold = true)
                Spacer(modifier = Modifier.height(8.dp))
                InvoiceRow(w.invoicePaymentLabel, order.paymentMethodTitle)
                InvoiceRow(strings.checkout.txRefLabel, order.transactionReference)
            }
            SuqiQrCard(
                title = "#${order.orderReference}",
                subtitle = w.invoiceScanNote,
                codeValue = order.orderReference,
                onCopyCode = {
                    clipboard.setText(AnnotatedString(order.orderReference))
                    viewModel.showToast(strings.common.copiedToast)
                },
                modifier = Modifier.testTag("invoice_qr")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SuqiButton(
                    text = strings.saveLabel,
                    isPrimary = false,
                    icon = LucideIcons.Download,
                    onClick = {
                        scope.launch {
                            runCatching {
                                val bmp = withContext(Dispatchers.Default) { renderInvoiceBitmap(order, w) }
                                SuqiQrGenerator.saveQrBitmapToPictures(context, bmp, "suqi_invoice_${order.orderReference}")
                                bmp.recycle()
                            }.onSuccess { viewModel.showToast(strings.common.qrSavedToast) }
                                .onFailure { viewModel.showToast(strings.common.qrSaveFailedToast) }
                        }
                    },
                    testTag = "invoice_save_btn",
                    modifier = Modifier.weight(1f)
                )
                SuqiButton(
                    text = w.invoiceShareBtn,
                    icon = LucideIcons.Share2,
                    onClick = {
                        scope.launch {
                            runCatching {
                                val bmp = withContext(Dispatchers.Default) { renderInvoiceBitmap(order, w) }
                                val uri = SuqiQrGenerator.createShareQrImageUri(context, bmp, "suqi_invoice_${order.orderReference}")
                                bmp.recycle()
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, String.format(w.invoiceShareSubjectTemplate, order.orderReference))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, w.invoiceTitle))
                            }.onFailure { viewModel.showToast(strings.common.qrSaveFailedToast) }
                        }
                    },
                    testTag = "invoice_share_btn",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun InvoiceRow(label: String, value: String, bold: Boolean = false) {
    val colors = LocalSuqiColors.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontFamily = SuqiFontFamily, fontSize = if (bold) 15.sp else 13.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = colors.secondaryText)
        Text(value, fontFamily = SuqiFontFamily, fontSize = if (bold) 15.sp else 13.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold, color = if (bold) SuqiPrimary else colors.primaryText)
    }
}
