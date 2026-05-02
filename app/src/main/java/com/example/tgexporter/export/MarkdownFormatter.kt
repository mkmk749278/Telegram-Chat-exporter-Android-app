package com.example.tgexporter.export

import com.example.tgexporter.data.model.MessageItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object MarkdownFormatter {

    private fun timestampFmt(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private fun headerFmt(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US)

    fun format(chatTitle: String, selected: List<MessageItem>): String {
        // Skip media-only messages: keep only those that actually have text.
        val filtered = selected.filter { it.text.isNotBlank() }.sortedBy { it.date }
        val ts = timestampFmt()
        val header = headerFmt().apply { timeZone = TimeZone.getDefault() }
        val sb = StringBuilder()
        sb.append("# ").append(chatTitle).append("\n\n")
        sb.append("_Exported ")
            .append(header.format(Date()))
            .append(" — ")
            .append(filtered.size)
            .append(" messages_\n\n---\n\n")

        for (m in filtered) {
            val edited = if (m.isEdited) " _(edited)_" else ""
            sb.append("## [").append(ts.format(Date(m.date))).append("] ")
                .append(m.senderName).append(edited).append("\n")

            m.forwardedFrom?.takeIf { it.isNotBlank() }?.let {
                sb.append("_Forwarded from ").append(it).append("_\n\n")
            }
            if (m.replyToSender != null && m.replyToText != null) {
                sb.append("> ").append(m.replyToSender).append(": ")
                    .append(m.replyToText.replace("\n", " "))
                    .append("\n\n")
            }
            sb.append(m.text.trimEnd()).append("\n\n")
        }
        return sb.toString()
    }
}
