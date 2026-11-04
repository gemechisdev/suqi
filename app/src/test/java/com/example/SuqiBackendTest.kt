package com.example

import com.example.data.api.BackendConfig
import com.example.data.api.SuqiEndpoints
import com.example.ui.components.formatEtb
import org.junit.Assert.*
import org.junit.Test

class SuqiBackendTest {

    @Test
    fun testEndpointsAndBackendConfig() {
        assertEquals("https://api.suqi.et/v1/", SuqiEndpoints.BASE_URL)
        assertEquals("products", SuqiEndpoints.PRODUCTS)
        assertEquals("shops", SuqiEndpoints.SHOPS)
        assertEquals("orders", SuqiEndpoints.ORDERS)
        assertEquals("cart", SuqiEndpoints.CART)

        assertTrue(BackendConfig.useBuiltinSqliteBackend)
    }

    @Test
    fun testEtbFormatting() {
        val formatted = formatEtb(24999.0)
        assertEquals("ETB 24,999", formatted)

        val formattedLarge = formatEtb(248750.0)
        assertEquals("ETB 248,750", formattedLarge)
    }
}
