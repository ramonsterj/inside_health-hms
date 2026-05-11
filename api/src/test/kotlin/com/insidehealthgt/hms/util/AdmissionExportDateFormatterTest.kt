package com.insidehealthgt.hms.util

import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals

class AdmissionExportDateFormatterTest {

    @Test
    fun `formats date as dd MM yyyy`() {
        assertEquals("09/05/2026", AdmissionExportDateFormatter.formatDate(LocalDate.of(2026, 5, 9)))
    }

    @Test
    fun `formats time as HH mm 24h`() {
        assertEquals("14:30", AdmissionExportDateFormatter.formatTime(LocalDateTime.of(2026, 5, 9, 14, 30)))
    }

    @Test
    fun `formats datetime with dash separator`() {
        assertEquals(
            "09/05/2026 - 14:30",
            AdmissionExportDateFormatter.formatDateTime(LocalDateTime.of(2026, 5, 9, 14, 30)),
        )
    }

    @Test
    fun `null values format as empty string`() {
        assertEquals("", AdmissionExportDateFormatter.formatDate(null as LocalDate?))
        assertEquals("", AdmissionExportDateFormatter.formatDateTime(null))
    }

    @Test
    fun `filename timestamp uses yyyyMMdd-HHmm`() {
        assertEquals(
            "20260509-1430",
            AdmissionExportDateFormatter.filenameTimestamp(LocalDateTime.of(2026, 5, 9, 14, 30)),
        )
    }
}
