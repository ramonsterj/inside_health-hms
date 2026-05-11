package com.insidehealthgt.hms.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Locale-independent date/time formatters used exclusively by the admission PDF export.
 *
 * Per `docs/features/admission-export.md` § Date/Time Conformance, the rendered PDF must
 * use `dd/MM/yyyy` for dates and `HH:mm` (24-hour) for times. No ISO strings or
 * `toString()` rendering are allowed in user-facing PDF output.
 */
object AdmissionExportDateFormatter {

    private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val FILENAME_TIMESTAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")

    fun formatDate(date: LocalDate?): String = date?.format(DATE) ?: ""

    fun formatDate(dateTime: LocalDateTime?): String = dateTime?.format(DATE) ?: ""

    fun formatTime(dateTime: LocalDateTime?): String = dateTime?.format(TIME) ?: ""

    fun formatDateTime(dateTime: LocalDateTime?): String =
        if (dateTime == null) "" else "${dateTime.format(DATE)} - ${dateTime.format(TIME)}"

    fun filenameTimestamp(dateTime: LocalDateTime): String = dateTime.format(FILENAME_TIMESTAMP)
}
