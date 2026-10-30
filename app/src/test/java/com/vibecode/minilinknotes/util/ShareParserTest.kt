package com.vibecode.minilinknotes.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareParserTest {

    @Test
    fun parse_extractsUrlAndKeepsSharedText() {
        val result = ShareParser.parse("文章標題\nhttps://example.com/post", null)
        assertEquals("https://example.com/post", result?.url)
        assertEquals("文章標題\nhttps://example.com/post", result?.text)
    }

    @Test
    fun parse_usesSubjectAsTitleSource() {
        val result = ShareParser.parse("https://example.com", "頁面標題")
        assertEquals("頁面標題", result?.subject)
        assertEquals("https://example.com", result?.url)
    }

    @Test
    fun parse_acceptsBareWwwUrl() {
        assertEquals("https://www.example.com/a", ShareParser.parse("www.example.com/a", null)?.url)
    }

    @Test
    fun parse_textOnlyHasNoUrl() {
        val result = ShareParser.parse("今天想到一個點子", "備忘")
        assertNull(result?.url)
        assertEquals("備忘", result?.subject)
    }

    @Test
    fun parse_returnsNullForEmptyShare() {
        assertNull(ShareParser.parse("  ", "  "))
        assertNull(ShareParser.parse(null, null))
        assertNull(ShareParser.parse(null, "只有標題沒有內容"))
    }

    @Test
    fun parseHtml_usesFirstAnchorAndPlainText() {
        val html = """<html><head><title>原始標題</title></head>
            <body><p>段落文字</p><a href="https://example.com/x">連結</a></body></html>"""
        val result = ShareParser.parse(html, null, isHtml = true)
        assertEquals("https://example.com/x", result?.url)
        assertEquals("原始標題", result?.subject)
        assertEquals("段落文字 連結", result?.text)
    }
}
