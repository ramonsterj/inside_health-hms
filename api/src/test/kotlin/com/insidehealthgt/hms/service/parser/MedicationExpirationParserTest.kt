package com.insidehealthgt.hms.service.parser

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MedicationExpirationParserTest {

    private val parser = MedicationExpirationParser()

    @Test
    fun `parses MM_YY format to last day of month`() {
        assertThat(parser.parse("06/26")).isEqualTo(LocalDate.of(2026, 6, 30))
    }

    @Test
    fun `parses MM_YYYY format to last day of month`() {
        assertThat(parser.parse("06/2026")).isEqualTo(LocalDate.of(2026, 6, 30))
    }

    @Test
    fun `parses dd_MM_yyyy format to exact day`() {
        assertThat(parser.parse("15/06/2026")).isEqualTo(LocalDate.of(2026, 6, 15))
    }

    @Test
    fun `parses February correctly (28 days non-leap)`() {
        assertThat(parser.parse("02/25")).isEqualTo(LocalDate.of(2025, 2, 28))
    }

    @Test
    fun `parses February correctly (29 days leap)`() {
        assertThat(parser.parse("02/28")).isEqualTo(LocalDate.of(2028, 2, 29))
    }

    @Test
    fun `parse rejects unparseable input`() {
        assertThatThrownBy { parser.parse("garbage") }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { parser.parse("") }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `parseOrNull returns null on failure`() {
        assertThat(parser.parseOrNull("garbage")).isNull()
        assertThat(parser.parseOrNull(null)).isNull()
        assertThat(parser.parseOrNull("06/26")).isEqualTo(LocalDate.of(2026, 6, 30))
    }
}
