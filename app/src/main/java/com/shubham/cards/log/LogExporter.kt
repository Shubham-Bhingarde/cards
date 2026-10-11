package com.shubham.cards.log

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Builds a share intent for the diagnostics log. The log is copied into a
 * dedicated, FileProvider-exposed folder so only that one file is ever shared,
 * and access is granted per-URI and temporarily (FLAG_GRANT_READ_URI_PERMISSION).
 */
object LogExporter {

    fun shareIntent(context: Context): Intent? {
        val source = AppLogger.currentLogFile()?.takeIf { it.exists() && it.length() > 0 }
            ?: return null

        val exportDir = File(context.cacheDir, "export").apply { mkdirs() }
        val exportFile = File(exportDir, "cards-diagnostics.txt")
        source.copyTo(exportFile, overwrite = true)

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportFile,
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Cards diagnostics log")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
