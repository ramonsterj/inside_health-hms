package com.insidehealthgt.hms.export.service

import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import org.springframework.stereotype.Component

/**
 * Server-side HTML sanitizer applied to every rich-text field rendered into the
 * admission export PDF. Allows only the tags emitted by the platform's rich-text
 * editor; strips `<script>`, `<iframe>`, all event handlers, and `javascript:` URLs.
 *
 * This runs at PDF rendering time independent of any frontend paste-time
 * sanitization (defense in depth — see `docs/features/admission-export.md` § Risks R1).
 */
@Component
class AdmissionExportHtmlSanitizer {

    private val safelist: Safelist = Safelist.basic()
        .addTags(
            "p", "br", "strong", "em", "u", "s", "ol", "ul", "li",
            "h1", "h2", "h3", "h4", "blockquote", "code", "pre", "span",
        )
        .addAttributes("span", "style")
        .addProtocols("a", "href", "http", "https", "mailto")

    fun clean(html: String?): String {
        if (html.isNullOrBlank()) return ""
        return Jsoup.clean(constrainInlineStyles(html), safelist)
    }

    private fun constrainInlineStyles(html: String): String {
        val document = Jsoup.parseBodyFragment(html)
        document.select("[style]").forEach { element ->
            val cleanStyle = element.attr("style")
                .split(';')
                .mapNotNull { declaration ->
                    val parts = declaration.split(':', limit = 2)
                    if (parts.size != 2) return@mapNotNull null
                    val property = parts[0].trim().lowercase()
                    val value = parts[1].trim().lowercase()
                    if (isAllowedStyle(property, value)) "$property: $value" else null
                }
                .joinToString("; ")
            if (cleanStyle.isBlank()) {
                element.removeAttr("style")
            } else {
                element.attr("style", cleanStyle)
            }
        }
        return document.body().html()
    }

    private fun isAllowedStyle(property: String, value: String): Boolean = when (property) {
        "color" -> COLOR_VALUE.matches(value)
        "font-weight" -> FONT_WEIGHT_VALUE.matches(value)
        "font-style" -> value in setOf("normal", "italic", "oblique")
        "text-decoration" -> value in setOf("none", "underline", "line-through")
        else -> false
    }

    companion object {
        private val COLOR_VALUE =
            Regex(
                """#[0-9a-f]{3}([0-9a-f]{3})?|[a-z]+|""" +
                    """rgb\(\s*\d{1,3}\s*,\s*\d{1,3}\s*,\s*\d{1,3}\s*\)""",
            )
        private val FONT_WEIGHT_VALUE = Regex("""normal|bold|bolder|lighter|[1-9]00""")
    }
}
