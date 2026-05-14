package com.insidehealthgt.hms.service.parser

import com.insidehealthgt.hms.entity.AdministrationRoute
import com.insidehealthgt.hms.entity.DosageForm
import com.insidehealthgt.hms.entity.MedicationReviewStatus
import com.insidehealthgt.hms.entity.MedicationSection
import org.springframework.stereotype.Component

/**
 * Parses legacy free-text item names into structured medication fields.
 * Confidence drives MedicationReviewStatus — low-confidence parses are tagged
 * NEEDS_REVIEW so the pharmacist can fix them post-backfill.
 *
 * Strategy:
 *   - Leading word identifies dosage form (TABLETA, CAPSULA, AMPOLLA, JARABE, GOTAS, ...).
 *   - Parenthetical "(BRAND xMG)" extracts commercial name + strength.
 *   - Remainder is the generic name.
 *   - Category name + dosage form choose the MedicationSection.
 */
@Component
@Suppress("ComplexMethod", "NestedBlockDepth")
class MedicationNameParser {

    private companion object {
        const val DOSAGE_FORM_PENALTY = 0.4
        const val GENERIC_FALLBACK_PENALTY = 0.3
        const val SECTION_DEFAULT_PENALTY = 0.2
        const val CONFIRMED_THRESHOLD = 0.7
        const val MAX_GENERIC_LENGTH = 150
        const val MAX_COMMERCIAL_LENGTH = 150
        const val MAX_STRENGTH_LENGTH = 50
        const val MAX_NOTES_LENGTH = 500
    }

    data class ParseResult(
        val genericName: String,
        val commercialName: String?,
        val strength: String?,
        val dosageForm: DosageForm,
        val route: AdministrationRoute?,
        val section: MedicationSection,
        val reviewStatus: MedicationReviewStatus,
        val reviewNotes: String?,
    )

    private val formMap = mapOf(
        "TABLETA" to DosageForm.TABLET, "TABLETAS" to DosageForm.TABLET, "TAB" to DosageForm.TABLET,
        "CAPSULA" to DosageForm.CAPSULE, "CAPSULAS" to DosageForm.CAPSULE, "CAP" to DosageForm.CAPSULE,
        "AMPOLLA" to DosageForm.AMPOULE, "AMPOLLAS" to DosageForm.AMPOULE, "AMP" to DosageForm.AMPOULE,
        "JARABE" to DosageForm.SYRUP, "SUSPENSION" to DosageForm.SYRUP,
        "GOTAS" to DosageForm.DROPS,
        "CREMA" to DosageForm.CREAM, "POMADA" to DosageForm.CREAM,
        "INYECCION" to DosageForm.INJECTION, "INYECTABLE" to DosageForm.INJECTION,
        "POLVO" to DosageForm.POWDER,
        "PARCHE" to DosageForm.PATCH,
    )

    private val parenStrength = Regex("\\(([^)]+)\\)")
    private val strengthRegex = Regex("\\d+\\s*(MG|MCG|ML|G|UI|MEQ|%)", RegexOption.IGNORE_CASE)

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun parse(rawName: String, categoryName: String?): ParseResult {
        val name = rawName.trim().replace(Regex("\\s+"), " ")
        var confidence = 1.0
        val notes = mutableListOf<String>()

        val firstWord = name.substringBefore(' ').uppercase()
        val dosageForm = formMap[firstWord] ?: DosageForm.OTHER
        if (dosageForm == DosageForm.OTHER) {
            confidence -= DOSAGE_FORM_PENALTY
            notes += "Could not detect dosage form from leading word '$firstWord'"
        }

        // Parenthetical extraction "(BRAND xMG)" -> commercial + strength.
        var commercial: String? = null
        var strength: String? = null
        val parenMatch = parenStrength.find(name)
        if (parenMatch != null) {
            val inner = parenMatch.groupValues[1].trim()
            val strengthMatch = strengthRegex.find(inner)
            strength = strengthMatch?.value?.trim()
            commercial = strengthMatch?.let { inner.removeRange(it.range).trim() } ?: inner
            if (commercial.isBlank()) commercial = null
        } else {
            val strengthMatch = strengthRegex.find(name)
            strength = strengthMatch?.value?.trim()
        }

        // Generic = name with leading form word stripped + paren stripped.
        var generic = name
        if (dosageForm != DosageForm.OTHER) {
            generic = generic.removePrefix(name.substringBefore(' ')).trim()
        }
        generic = generic.replace(parenStrength, "").trim()
        if (strength != null) {
            generic = generic.replace(strength, "").trim()
        }
        if (generic.isEmpty()) {
            generic = rawName.trim()
            confidence -= GENERIC_FALLBACK_PENALTY
            notes += "Generic name fell back to raw input"
        }
        if (generic.length > MAX_GENERIC_LENGTH) {
            generic = generic.substring(0, MAX_GENERIC_LENGTH)
            notes += "Generic name truncated to $MAX_GENERIC_LENGTH chars"
        }

        val section = inferSection(dosageForm, categoryName)
        if (section == MedicationSection.NO_PSIQUIATRICO && categoryName.isNullOrBlank()) {
            confidence -= SECTION_DEFAULT_PENALTY
            notes += "Section defaulted to NO_PSIQUIATRICO"
        }

        val reviewStatus = if (confidence >= CONFIRMED_THRESHOLD) {
            MedicationReviewStatus.CONFIRMED
        } else {
            MedicationReviewStatus.NEEDS_REVIEW
        }

        return ParseResult(
            genericName = generic,
            commercialName = commercial?.take(MAX_COMMERCIAL_LENGTH),
            strength = strength?.take(MAX_STRENGTH_LENGTH),
            dosageForm = dosageForm,
            route = inferRoute(dosageForm),
            section = section,
            reviewStatus = reviewStatus,
            reviewNotes = if (notes.isEmpty()) null else notes.joinToString("; ").take(MAX_NOTES_LENGTH),
        )
    }

    private fun inferRoute(form: DosageForm): AdministrationRoute? = when (form) {
        DosageForm.TABLET, DosageForm.CAPSULE, DosageForm.SYRUP, DosageForm.DROPS -> AdministrationRoute.ORAL
        DosageForm.AMPOULE, DosageForm.INJECTION -> AdministrationRoute.IM
        DosageForm.CREAM, DosageForm.PATCH -> AdministrationRoute.TOPICAL
        DosageForm.POWDER -> null
        DosageForm.OTHER -> null
    }

    private fun inferSection(form: DosageForm, categoryName: String?): MedicationSection {
        val cat = categoryName?.uppercase() ?: ""
        return when {
            form == DosageForm.SYRUP || form == DosageForm.DROPS -> MedicationSection.JARABE_GOTAS
            form == DosageForm.AMPOULE || form == DosageForm.INJECTION -> MedicationSection.AMPOLLA
            cat.contains("PSIQUI") -> MedicationSection.PSIQUIATRICO
            else -> MedicationSection.NO_PSIQUIATRICO
        }
    }
}
