package com.vibecode.minilinknotes.util

import java.net.URI

object UrlUtils {

    private val URL_PATTERN = Regex(
        """(?i)\b((?:https?://|www\.)[^\s<>"'\u3000]+)""",
    )

    private val TRAILING_PUNCTUATION = charArrayOf(
        '.', ',', ';', ':', '!', '?', ')', ']', '}', '>', '"', '\'', '\u3002', '\uFF0C', '\uFF1B', '\uFF01', '\uFF1F',
    )

    fun normalize(raw: String?): String? {
        if (raw == null) return null
        var value = raw.trim()
        if (value.isEmpty()) return null
        if (value.startsWith("<") && value.endsWith(">")) {
            value = value.substring(1, value.length - 1).trim()
        }
        if (!value.contains("://")) {
            if (!looksLikeDomain(value)) return null
            value = "https://$value"
        }
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host?.trim()?.lowercase()
        if (host.isNullOrBlank() || !host.contains('.')) return null
        if (uri.userInfo != null) return null
        return uri.toString()
    }

    fun extractFirst(text: String?): String? {
        if (text.isNullOrBlank()) return null
        for (match in URL_PATTERN.findAll(text)) {
            val candidate = match.value
                .trimEnd(*TRAILING_PUNCTUATION)
                .trim()
            normalize(candidate)?.let { return it }
        }
        return null
    }

    fun dedupeKey(url: String): String {
        val uri = runCatching { URI(url) }.getOrNull() ?: return url.lowercase()
        val scheme = uri.scheme?.lowercase() ?: return url.lowercase()
        val host = uri.host?.lowercase() ?: return url.lowercase()
        val defaultPort = (scheme == "http" && uri.port == 80) || (scheme == "https" && uri.port == 443)
        val port = if (uri.port == -1 || defaultPort) "" else ":${uri.port}"
        val path = uri.path?.trimEnd('/').orEmpty()
        val query = uri.query?.let { "?$it" }.orEmpty()
        return "$scheme://$host$port$path$query"
    }

    fun hostOf(url: String): String? =
        runCatching { URI(url).host }.getOrNull()
            ?.removePrefix("www.")
            ?.ifBlank { null }

    private fun looksLikeDomain(value: String): Boolean {
        if (value.any { it.isWhitespace() }) return false
        val host = value.substringBefore('/').substringBefore('?').substringBefore('#')
        return host.contains('.') && !host.startsWith('.') && !host.endsWith('.')
    }
}
