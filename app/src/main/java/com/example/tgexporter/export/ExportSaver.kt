package com.example.tgexporter.export

import android.content.ContentResolver
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportSaver {
    fun defaultFilename(chatTitle: String): String {
        val safe = chatTitle.replace(Regex("[^A-Za-z0-9_-]"), "_").take(40).ifBlank { "chat" }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        return "${safe}_$stamp.md"
    }

    fun writeTo(resolver: ContentResolver, uri: Uri, content: String) {
        resolver.openOutputStream(uri, "wt")?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            ?: error("Could not open output stream for $uri")
    }
}
