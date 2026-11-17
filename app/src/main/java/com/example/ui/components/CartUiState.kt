package com.example.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.model.ProductDto

/**
 * What is already in the cart, provided once at the app root so every product card can swap
 * "Add to cart" for a - qty + stepper without per-screen plumbing.
 *
 * [quantities] maps productId to total units across all variations of that product.
 * [onChange] adds (delta > 0) or removes (delta < 0) units of a product.
 */
@Immutable
class CartUiState(
    val quantities: Map<String, Int> = emptyMap(),
    val onChange: (ProductDto, Int) -> Unit = { _, _ -> }
)

val LocalCartUiState = staticCompositionLocalOf { CartUiState() }
