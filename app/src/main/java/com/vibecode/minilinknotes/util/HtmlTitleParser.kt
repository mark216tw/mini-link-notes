package com.vibecode.minilinknotes.util

import java.io.ByteArrayInputStream
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object HtmlTitleParser {

    private val WHITESPACE = Regex("\\s+")

    fun parse(bytes: ByteArray, baseUri: String? = null): String? {
        if (bytes.isEmpty()) return null
        val document = Jsoup.parse(ByteArrayInputStream(bytes), null, baseUri.orEmpty())
        return extract(document)
    }

    fun parse(html: String, baseUri: String? = null): String? =
        extract(Jsoup.parse(html, baseUri.orEmpty()))

    private fun extract(document: Document): String? {
        val title = document.title().trim().ifBlank {
            document.selectFirst("meta[property=og:title]")?.attr("content")?.trim().orEmpty()
        }
        val cleaned = title.replace(WHITESPACE, " ").trim()
        if (cleaned.isEmpty()) return null
        return cleaned.take(200)
    }
}
