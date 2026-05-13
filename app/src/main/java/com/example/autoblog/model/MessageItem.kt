package com.example.autoblog.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MessageItem(
    val id: String,
    val title: String,
    val body: String,
    val createdAtMs: Long,
    val imageUriString: String? = null
)

fun formatMessageDisplayDate(timeMs: Long): String {
    return SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(timeMs))
}

fun formatMessageDetailDateTime(timeMs: Long): String {
    return SimpleDateFormat("EEEE, MMMM d, yyyy 'at' hh:mm a", Locale.US).format(Date(timeMs))
}

fun previewFromBody(body: String, maxLen: Int = 200): String {
    val text = body.trim()
    if (text.isEmpty()) return ""
    val compact = text.lines().take(2).joinToString(" ").trim()
    return if (compact.length > maxLen) {
        compact.take(maxLen).trimEnd() + "…"
    } else {
        compact
    }
}

fun buildBulkShareText(messages: List<MessageItem>): String {
    if (messages.isEmpty()) return ""
    if (messages.size == 1) {
        val m = messages.first()
        return "${m.title}\n\n${m.body}"
    }
    return messages.joinToString(separator = "\n\n────────\n\n") { m ->
        "${m.title}\n\n${m.body}"
    }
}

private fun april52026_947(): Long {
    return Calendar.getInstance().apply {
        set(2026, Calendar.APRIL, 5, 9, 47, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun april32026_noon(): Long {
    return Calendar.getInstance().apply {
        set(2026, Calendar.APRIL, 3, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun march282026_morning(): Long {
    return Calendar.getInstance().apply {
        set(2026, Calendar.MARCH, 28, 10, 30, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun defaultMessages(): List<MessageItem> = listOf(
    MessageItem(
        id = "1",
        title = "Travel Memories from Tokyo",
        body = "Just returned from an amazing trip to Tokyo! The blend of traditional temples " +
            "and modern technology is fascinating. Can't wait to share more photos and stories " +
            "from this incredible city.",
        createdAtMs = april52026_947()
    ),
    MessageItem(
        id = "2",
        title = "Tips for Great Writing",
        body = "Start with a clear outline, read your draft out loud, and cut anything that " +
            "does not move the story forward. Keep sentences short when you can, and let " +
            "your first draft be messy—you can always refine it later.",
        createdAtMs = april32026_noon()
    ),
    MessageItem(
        id = "3",
        title = "My First Blog Post",
        body = "Excited to share thoughts on tech, travel, and everyday lessons. Thanks for reading!",
        createdAtMs = march282026_morning()
    )
)
