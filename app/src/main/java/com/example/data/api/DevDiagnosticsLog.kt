package com.example.data.api

import okhttp3.Interceptor
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

data class DevRequestEntry(
    val id: String,
    val timestamp: String,
    val operation: String,
    val target: String,
    val success: Boolean,
    val durationMs: Long,
    val detail: String = ""
)

object DevDiagnosticsLog {
    private const val MAX_ENTRIES = 50
    private val buffer = CopyOnWriteArrayList<DevRequestEntry>()
    private val totalErrors = AtomicInteger(0)
    private val seq = AtomicInteger(0)

    fun record(
        operation: String,
        target: String,
        success: Boolean,
        durationMs: Long,
        detail: String = ""
    ) {
        val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)
        val entry = DevRequestEntry(
            id = "req_${seq.incrementAndGet()}",
            timestamp = fmt.format(Date()),
            operation = operation,
            target = target,
            success = success,
            durationMs = durationMs.coerceAtLeast(0L),
            detail = detail.take(180)
        )
        if (!success) {
            totalErrors.incrementAndGet()
        }
        buffer.add(0, entry)
        while (buffer.size > MAX_ENTRIES) {
            runCatching { buffer.removeAt(buffer.size - 1) }
        }
    }

    fun entries(): List<DevRequestEntry> = buffer.toList()

    fun errorCount(): Int = totalErrors.get()

    fun clear() {
        buffer.clear()
        totalErrors.set(0)
    }

    val diagnosticsInterceptor: Interceptor = Interceptor { chain ->
        val req = chain.request()
        val startNs = System.nanoTime()
        try {
            val response: Response = chain.proceed(req)
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
            record(
                operation = "${req.method} ${req.url.encodedPath}",
                target = req.url.host,
                success = response.isSuccessful,
                durationMs = elapsedMs,
                detail = "HTTP ${response.code}"
            )
            response
        } catch (t: Throwable) {
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
            record(
                operation = "${req.method} ${req.url.encodedPath}",
                target = req.url.host,
                success = false,
                durationMs = elapsedMs,
                detail = t.message ?: t.javaClass.simpleName
            )
            throw t
        }
    }
}
