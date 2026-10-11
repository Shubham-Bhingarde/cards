package com.shubham.cards.log

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * A small, dependency-free file logger for diagnostics the user can export and
 * share. Design goals:
 *
 *  - PRIVACY BY DEFAULT: sensitive values (card UIDs, NDEF payloads) are redacted
 *    unless the user explicitly turns on "verbose" logging for a session.
 *  - BOUNDED: the log file is capped and rotated so it can never grow without
 *    limit or fill the device.
 *  - PRIVATE STORAGE: the file lives in app-private storage (filesDir), never on
 *    shared/world-readable media. It only leaves the device via a user-initiated
 *    share sheet.
 *  - NO CRASHES: all I/O is best-effort; logging must never take the app down.
 *
 * This is a process-wide singleton initialised once from [CardsApp].
 */
object AppLogger {

    enum class Level { DEBUG, INFO, WARN, ERROR }

    private const val LOG_FILE = "cards-diagnostics.log"
    private const val MAX_BYTES = 256 * 1024 // 256 KB cap, then rotate once.

    private val io = Executors.newSingleThreadExecutor()
    private val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Volatile private var logFile: File? = null
    @Volatile var verbose: Boolean = false
        private set

    fun init(context: Context) {
        val dir = File(context.filesDir, "logs").apply { mkdirs() }
        logFile = File(dir, LOG_FILE)
        i("AppLogger", "Logger initialised. Build=${com.shubham.cards.BuildConfig.VERSION_NAME}")
    }

    /** Verbose mode includes sensitive identifiers. Off by default; user opt-in. */
    fun setVerbose(enabled: Boolean) {
        verbose = enabled
        i("AppLogger", "Verbose logging ${if (enabled) "ENABLED (includes identifiers)" else "disabled (identifiers redacted)"}")
    }

    fun d(tag: String, msg: String) = write(Level.DEBUG, tag, msg)
    fun i(tag: String, msg: String) = write(Level.INFO, tag, msg)
    fun w(tag: String, msg: String) = write(Level.WARN, tag, msg)
    fun e(tag: String, msg: String, t: Throwable? = null) =
        write(Level.ERROR, tag, msg + (t?.let { " | ${it.javaClass.simpleName}: ${it.message}" } ?: ""))

    /**
     * Logs a value that MAY be sensitive. When [sensitive] is true and verbose is
     * off, the value is replaced with a redaction marker that preserves length
     * info only (useful for debugging without leaking the identifier).
     */
    fun value(tag: String, label: String, value: String, sensitive: Boolean) {
        val shown = if (sensitive && !verbose) redact(value) else value
        write(Level.DEBUG, tag, "$label = $shown")
    }

    fun currentLogFile(): File? = logFile

    fun readAll(): String = logFile?.takeIf { it.exists() }?.readText().orEmpty()

    fun clear() = io.execute { logFile?.writeText("") }

    private fun redact(value: String): String {
        val len = value.replace(" ", "").length
        return "[redacted:${len}chars]"
    }

    private fun write(level: Level, tag: String, msg: String) {
        val line = "${timeFmt.format(Date())} ${level.name.padEnd(5)} [$tag] $msg\n"
        io.execute {
            try {
                val f = logFile ?: return@execute
                if (f.exists() && f.length() > MAX_BYTES) rotate(f)
                f.appendText(line)
            } catch (_: Exception) {
                // Logging must never crash the app.
            }
        }
    }

    private fun rotate(f: File) {
        try {
            val prev = File(f.parentFile, "$LOG_FILE.1")
            if (prev.exists()) prev.delete()
            f.renameTo(prev)
        } catch (_: Exception) {
            // If rotation fails, truncate to stay bounded.
            try { f.writeText("") } catch (_: Exception) {}
        }
    }
}
