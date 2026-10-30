package com.vibecode.minilinknotes.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlUtilsTest {

    @Test
    fun normalize_addsHttpsWhenSchemeMissing() {
        assertEquals("https://www.example.com/page", UrlUtils.normalize("www.example.com/page"))
    }

    @Test
    fun normalize_keepsHttpAndHttps() {
        assertEquals("http://example.com", UrlUtils.normalize("http://example.com"))
        assertEquals("https://example.com/a?b=1", UrlUtils.normalize("https://example.com/a?b=1"))
    }

    @Test
    fun normalize_rejectsUnsafeSchemesAndInvalidHosts() {
        assertNull(UrlUtils.normalize("javascript:alert(1)"))
        assertNull(UrlUtils.normalize("file:///sdcard/a.html"))
        assertNull(UrlUtils.normalize("mailto:test@example.com"))
        assertNull(UrlUtils.normalize("localhost"))
        assertNull(UrlUtils.normalize(""))
    }

    @Test
    fun extractFirst_findsUrlInsideSharedText() {
        val text = "Example title\nhttps://example.com/article?id=3"
        assertEquals("https://example.com/article?id=3", UrlUtils.extractFirst(text))
    }

    @Test
    fun extractFirst_trimsTrailingPunctuation() {
        val text = "請看 https://example.com/a。"
        assertEquals("https://example.com/a", UrlUtils.extractFirst(text))
    }

    @Test
    fun extractFirst_returnsNullWhenNoUrl() {
        assertNull(UrlUtils.extractFirst("只有一段文字，沒有連結"))
    }

    @Test
    fun dedupeKey_ignoresTrailingSlashFragmentAndDefaultPort() {
        val first = UrlUtils.dedupeKey("https://Example.com:443/path/")
        val second = UrlUtils.dedupeKey("https://example.com/path#section")
        assertEquals(first, second)
    }

    @Test
    fun hostOf_removesWwwPrefix() {
        assertEquals("example.com", UrlUtils.hostOf("https://www.example.com/a"))
    }
}
