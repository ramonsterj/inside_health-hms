package com.insidehealthgt.hms.service.parser

import org.springframework.stereotype.Component
import java.time.DateTimeException
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Parses the customer's printed expiration strings:
 *   - "MM/YY"           e.g. "06/26" -> 2026-06-30
 *   - "MM/YYYY"         e.g. "06/2026" -> 2026-06-30
 *   - "dd/MM/yyyy"      e.g. "15/06/2026" -> 2026-06-15
 *
 * All MM/YY and MM/YYYY values normalize to the LAST day of that month
 * because the pharmacy spreadsheet treats "expires June 2026" as the entire month.
 */
@Component
class MedicationExpirationParser {

    private val dayMonthYear = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun parse(raw: String): LocalDate {
        val s = raw.trim()
        require(s.isNotEmpty()) { "Empty expiration string" }
        return when {
            s.matches(Regex("\\d{2}/\\d{2}/\\d{4}")) -> parseDayMonthYear(s)
            s.matches(Regex("\\d{1,2}/\\d{4}")) -> parseMonthYear4(s)
            s.matches(Regex("\\d{1,2}/\\d{2}")) -> parseMonthYear2(s)
            else -> throw IllegalArgumentException("Unparseable expiration: '$raw'")
        }
    }

    @Suppress("SwallowedException")
    fun parseOrNull(raw: String?): LocalDate? = try {
        raw?.let { parse(it) }
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: DateTimeException) {
        null
    }

    private fun parseDayMonthYear(s: String): LocalDate = LocalDate.parse(s, dayMonthYear)

    private fun parseMonthYear4(s: String): LocalDate {
        val (mm, yyyy) = s.split("/")
        return YearMonth.of(yyyy.toInt(), mm.toInt()).atEndOfMonth()
    }

    private fun parseMonthYear2(s: String): LocalDate {
        val (mm, yy) = s.split("/")
        val year = TWO_DIGIT_YEAR_BASE + yy.toInt()
        return YearMonth.of(year, mm.toInt()).atEndOfMonth()
    }

    private companion object {
        const val TWO_DIGIT_YEAR_BASE = 2000
    }
}
