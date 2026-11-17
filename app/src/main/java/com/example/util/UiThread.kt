package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Applies Compose state updates on the main dispatcher after an async repository load.
 *
 * In the app this is effectively a no-op: LaunchedEffect already resumes on the main
 * dispatcher after `withContext(Dispatchers.IO)` returns. It exists only because Compose UI
 * tests run effects on an UnconfinedTestDispatcher, which can resume on the IO worker thread
 * and mutate snapshot state during the first layout pass.
 *
 * TODO: remove this helper once tests inject a standard test dispatcher for the backend's IO work.
 */
suspend fun <T> applyOnMain(block: suspend () -> T): T =
    withContext(Dispatchers.Main) { block() }
