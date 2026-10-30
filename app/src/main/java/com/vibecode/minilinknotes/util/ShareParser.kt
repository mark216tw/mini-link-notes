package com.vibecode.minilinknotes.util

import org.jsoup.Jsoup

data class SharedContent(
    val url: String?,
    val text: String,
    val subject: String?,
)

object ShareParser {

    fun parse(text: String?, subject: String?, isHtml: Boolean = false): SharedContent? {
        val rawText = text.orEmpty()
        val cleanSubject = subject?.trim()?.ifBlank { null }
        if (rawText.isBlank()) return null

        if (isHtml) {
            val document = Jsoup.parse(rawText)
            val url = document.selectFirst("a[href]")
                ?.attr("href")
                ?.let(UrlUtils::normalize)
                ?: UrlUtils.extractFirst(document.text())
            val plainText = (document.body()?.text() ?: document.text()).trim()
            return SharedContent(
                url = url,
                text = plainText,
                subject = cleanSubject ?: document.title().trim().ifBlank { null },
            )
        }

        val plainText = rawText.trim()
        return SharedContent(
            url = UrlUtils.extractFirst(plainText),
            text = plainText,
            subject = cleanSubject,
        )
    }
}
