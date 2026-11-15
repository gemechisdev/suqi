package com.example.data.api

object BackendConfig {
    /**
     * When true, uses the builtin embedded SQLite Room backend which fully implements
     * the SuqiApiService endpoint contracts, validations, inventory tracking, multi-shop orders,
     * and verification queues.
     *
     * Set to false to switch directly to a remote HTTP/REST server using SuqiEndpoints.BASE_URL.
     */
    var useBuiltinSqliteBackend: Boolean = true

    var customBaseUrl: String = SuqiEndpoints.BASE_URL

    /**
     * Frontend-first flag controlling demo shortcuts (visible admin credentials card,
     * OTP "123456" hints, fake Telegram/Google fallback values, developer tools shortcuts,
     * and sample image helpers). Flip to false when wiring production APIs.
     */
    const val DemoMode: Boolean = true
}
