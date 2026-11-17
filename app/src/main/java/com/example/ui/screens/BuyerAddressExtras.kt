package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import com.example.data.model.OrderDto
import com.example.data.model.SavedAddressDto

/**
 * Same rule the backend uses to refuse deleting an address: an order that is neither Delivered
 * nor Cancelled mentions the address's street or landmark in its delivery address.
 */
fun isAddressInUse(address: SavedAddressDto, orders: List<OrderDto>): Boolean {
    if (address.streetOrLandmark.isBlank()) return false
    return orders.any {
        it.orderStatus !in listOf("Delivered", "Cancelled") &&
            it.deliveryAddress.contains(address.streetOrLandmark, ignoreCase = true)
    }
}

/** Freshest last-known device position across enabled providers, or null (no permission, no fix). */
@SuppressLint("MissingPermission")
fun lastKnownCoordinates(context: Context): Pair<Double, Double>? {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val best = runCatching { lm.getProviders(true) }.getOrDefault(emptyList())
        .mapNotNull { provider -> runCatching { lm.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }
    return best?.let { it.latitude to it.longitude }
}

fun formatCoordinates(lat: Double, lng: Double): String =
    String.format(java.util.Locale.US, "%.4f, %.4f", lat, lng)
