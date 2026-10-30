package com.vibecode.minilinknotes.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HtmlTitleParserTest {

    @Test
    fun parse_readsTitleAndCollapsesWhitespace() {
        val html = "<html><head><title>  我的\n  網頁標題 </title></head></html>"
        assertEquals("我的 網頁標題", HtmlTitleParser.parse(html))
    }

    @Test
    fun parse_fallsBackToOpenGraphTitle() {
        val html = """<html><head><meta property="og:title" content="社群標題"></head></html>"""
        assertEquals("社群標題", HtmlTitleParser.parse(html))
    }

    @Test
    fun parse_decodesEntities() {
        val html = "<html><head><title>A &amp; B</title></head></html>"
        assertEquals("A & B", HtmlTitleParser.parse(html))
    }

    @Test
    fun parse_handlesUtf8Bytes() {
        val html = "<html><head><title>繁體中文標題</title></head></html>"
        assertEquals("繁體中文標題", HtmlTitleParser.parse(html.toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun parse_returnsNullWhenTitleMissing() {
        assertNull(HtmlTitleParser.parse("<html><body>no title</body></html>"))
        assertNull(HtmlTitleParser.parse(""))
    }
}
