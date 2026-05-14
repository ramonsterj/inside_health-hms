package com.insidehealthgt.hms.service.parser

import com.insidehealthgt.hms.entity.DosageForm
import com.insidehealthgt.hms.entity.MedicationReviewStatus
import com.insidehealthgt.hms.entity.MedicationSection
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MedicationNameParserTest {

    private val parser = MedicationNameParser()

    @Test
    fun `parses leading TABLETA as TABLET form`() {
        val result = parser.parse("TABLETA OLANZAPINA 5MG", "Medicamentos")
        assertThat(result.dosageForm).isEqualTo(DosageForm.TABLET)
        assertThat(result.genericName).contains("OLANZAPINA")
        assertThat(result.strength).contains("5")
    }

    @Test
    fun `parses leading AMPOLLA and infers section AMPOLLA`() {
        val result = parser.parse("AMPOLLA HALOPERIDOL 5MG/ML", "Medicamentos")
        assertThat(result.dosageForm).isEqualTo(DosageForm.AMPOULE)
        assertThat(result.section).isEqualTo(MedicationSection.AMPOLLA)
    }

    @Test
    fun `parses leading JARABE and infers section JARABE_GOTAS`() {
        val result = parser.parse("JARABE PARACETAMOL 100MG/5ML", null)
        assertThat(result.dosageForm).isEqualTo(DosageForm.SYRUP)
        assertThat(result.section).isEqualTo(MedicationSection.JARABE_GOTAS)
    }

    @Test
    fun `extracts commercial name from parenthetical`() {
        val result = parser.parse("TABLETA OLANZAPINA (ZYPREXA 5MG)", "Medicamentos")
        assertThat(result.commercialName).contains("ZYPREXA")
        assertThat(result.strength).contains("5")
    }

    @Test
    fun `unknown leading word flagged NEEDS_REVIEW`() {
        val result = parser.parse("XYZ-XXX SOMETHING", "Medicamentos")
        assertThat(result.dosageForm).isEqualTo(DosageForm.OTHER)
        assertThat(result.reviewStatus).isEqualTo(MedicationReviewStatus.NEEDS_REVIEW)
        assertThat(result.reviewNotes).isNotNull()
    }

    @Test
    fun `psiquiátrico category maps to PSIQUIATRICO section for tablets`() {
        val result = parser.parse("TABLETA OLANZAPINA 5MG", "Medicamentos Psiquiátricos")
        assertThat(result.section).isEqualTo(MedicationSection.PSIQUIATRICO)
    }
}
