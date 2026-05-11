package com.insidehealthgt.hms.export.service

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdmissionExportHtmlSanitizerTest {

    private val sanitizer = AdmissionExportHtmlSanitizer()

    @Test
    fun `strips script tags`() {
        val result = sanitizer.clean("<p>hello</p><script>alert('xss')</script>")
        assertFalse(result.contains("<script"))
        assertFalse(result.contains("alert"))
        assertTrue(result.contains("<p>hello</p>"))
    }

    @Test
    fun `strips iframe tags`() {
        val result = sanitizer.clean("<iframe src=\"https://evil.example\"></iframe><p>ok</p>")
        assertFalse(result.contains("<iframe"))
        assertTrue(result.contains("<p>ok</p>"))
    }

    @Test
    fun `strips on event handlers`() {
        val result = sanitizer.clean("<p onclick=\"alert(1)\">click</p>")
        assertFalse(result.contains("onclick"))
        assertTrue(result.contains("click"))
    }

    @Test
    fun `strips javascript URLs`() {
        val result = sanitizer.clean("<a href=\"javascript:alert(1)\">link</a>")
        assertFalse(result.contains("javascript:"))
    }

    @Test
    fun `keeps basic rich text tags`() {
        val input = "<p><strong>bold</strong> and <em>italic</em> and <u>under</u></p>" +
            "<ul><li>one</li><li>two</li></ul>"
        val result = sanitizer.clean(input)
        assertTrue(result.contains("<strong>bold</strong>"))
        assertTrue(result.contains("<em>italic</em>"))
        assertTrue(result.contains("<u>under</u>"))
        assertTrue(result.contains("<li>one</li>"))
    }

    @Test
    fun `keeps only constrained inline style declarations`() {
        val result = sanitizer.clean(
            "<span style=\"color: #336699; position: fixed; font-weight: bold; " +
                "background-image: url(javascript:alert(1))\">safe</span>",
        )

        assertTrue(result.contains("color: #336699"))
        assertTrue(result.contains("font-weight: bold"))
        assertFalse(result.contains("position"))
        assertFalse(result.contains("javascript"))
        assertFalse(result.contains("background-image"))
    }

    @Test
    fun `empty input returns empty string`() {
        assertEquals("", sanitizer.clean(null))
        assertEquals("", sanitizer.clean(""))
        assertEquals("", sanitizer.clean("   "))
    }

    @Test
    fun `strips nested script attempts`() {
        val result = sanitizer.clean("<div><p>safe</p><scr<script>ipt>alert(1)</script></div>")
        assertFalse(result.contains("alert(1)"))
    }
}
