package com.insidehealthgt.hms.export.service

import com.insidehealthgt.hms.export.dto.AdmissionExportSnapshot
import com.insidehealthgt.hms.export.dto.AttachmentIndexEntry
import com.insidehealthgt.hms.export.dto.AttachmentSnapshot
import com.insidehealthgt.hms.export.dto.AttachmentSource
import com.insidehealthgt.hms.export.dto.ClinicalHistorySnapshot
import com.insidehealthgt.hms.export.dto.InvoiceSnapshot
import com.insidehealthgt.hms.export.dto.MedicalOrderSnapshot
import com.insidehealthgt.hms.export.dto.MedicationAdministrationSnapshot
import com.insidehealthgt.hms.export.dto.NursingNoteSnapshot
import com.insidehealthgt.hms.export.dto.PatientChargeSnapshot
import com.insidehealthgt.hms.export.dto.PatientSnapshot
import com.insidehealthgt.hms.export.dto.ProgressNoteSnapshot
import com.insidehealthgt.hms.export.dto.PsychotherapyActivitySnapshot
import com.insidehealthgt.hms.export.dto.VitalSignSnapshot
import com.insidehealthgt.hms.service.MessageService
import com.insidehealthgt.hms.util.AdmissionExportDateFormatter
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.svgsupport.BatikSVGDrawer
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.OutputStream
import java.util.Locale

/**
 * Renders the admission export "body" PDF (every section other than the binary appendix).
 * The renderer consumes a detached [AdmissionExportSnapshot] only — no JPA entities are
 * touched here. Rich-text fields are passed through [AdmissionExportHtmlSanitizer]
 * before being inlined into the HTML.
 */
@Service
@Suppress("LargeClass", "TooManyFunctions", "LongMethod")
class AdmissionExportRenderer(
    private val sanitizer: AdmissionExportHtmlSanitizer,
    private val messageService: MessageService,
) {

    private val logoSvgRaw: String = loadLogoSvg()

    /**
     * Render the body PDF and write it to [output]. The output stream is not closed.
     */
    fun render(
        snapshot: AdmissionExportSnapshot,
        locale: Locale,
        output: OutputStream,
        attachmentIndexEntries: List<AttachmentIndexEntry> = emptyList(),
    ) {
        val html = buildHtml(snapshot, locale, attachmentIndexEntries)
        val builder = PdfRendererBuilder()
        builder.useSVGDrawer(BatikSVGDrawer())
        builder.withHtmlContent(html, null)
        builder.toStream(output)
        builder.run()
    }

    /**
     * Builds the full HTML document. Exposed for unit testing the rendered fragments.
     */
    fun buildHtml(
        snapshot: AdmissionExportSnapshot,
        locale: Locale,
        attachmentIndexEntries: List<AttachmentIndexEntry> = emptyList(),
    ): String {
        val body = StringBuilder()
        body.append(renderRunningFooter(snapshot, locale))
        body.append(renderCoverPage(snapshot, locale))
        body.append(renderDemographics(snapshot, locale))
        body.append(renderConsultingPhysicians(snapshot, locale))
        body.append(renderClinicalHistory(snapshot.clinicalHistory, locale))
        body.append(renderProgressNotes(snapshot.progressNotes, locale))
        body.append(renderMedicalOrders(snapshot.medicalOrders, locale))
        body.append(renderPsychotherapy(snapshot.psychotherapyActivities, locale))
        body.append(renderNursingNotes(snapshot.nursingNotes, locale))
        body.append(renderVitalSigns(snapshot.vitalSigns, locale))
        body.append(renderMedicationAdministrations(snapshot.medicationAdministrations, locale))
        body.append(renderBilling(snapshot.patientCharges, snapshot.invoices, locale))
        body.append(renderDocumentsIndex(snapshot.attachments, attachmentIndexEntries, locale))

        return """
            <!DOCTYPE html>
            <html><head><meta charset="utf-8"/>
            <style>
                @page {
                    size: A4;
                    margin: 18mm 16mm 22mm 16mm;
                    @bottom-left { content: element(pageFooter); vertical-align: middle; }
                    @bottom-right {
                        content: counter(page) " / " counter(pages);
                        font-size: 8pt;
                        color: #666;
                        vertical-align: middle;
                    }
                }
                @page :first {
                    @bottom-left { content: none; }
                    @bottom-right { content: none; }
                }
                body { font-family: 'Helvetica', sans-serif; font-size: 10pt; color: #222; }
                h1 { font-size: 18pt; margin: 0 0 8pt 0; }
                h2 { font-size: 13pt; margin: 18pt 0 4pt 0; border-bottom: 1px solid #888; padding-bottom: 2pt; }
                h3 { font-size: 11pt; margin: 10pt 0 2pt 0; }
                table { width: 100%; border-collapse: collapse; margin-top: 4pt; }
                th, td { border: 1px solid #aaa; padding: 4pt 6pt; text-align: left; vertical-align: top; }
                th { background: #eee; }
                .meta { font-size: 9pt; color: #666; margin-bottom: 8pt; }
                .placeholder { font-style: italic; color: #888; }
                .section { page-break-inside: avoid; }
                .label { color: #555; font-weight: bold; }
                .field { margin: 2pt 0; }
                .banner { background: #f3f3f3; padding: 6pt; border-left: 4pt solid #444; margin: 6pt 0; }
                .cover-header { width: 100%; border-collapse: collapse; margin-bottom: 12pt; border-bottom: 2pt solid #005978; }
                .cover-header td { border: none; padding: 0 0 2pt 0; vertical-align: middle; }
                .cover-header .cover-logo { text-align: left; }
                .cover-header .cover-brand { text-align: right; }
                .cover-header .brand-name { font-size: 9pt; color: #333; font-weight: bold; letter-spacing: 1pt; text-transform: uppercase; }
                .cover-header .brand-meta { font-size: 8pt; color: #666; margin-top: 2pt; }
                .page-footer { position: running(pageFooter); font-size: 8pt; color: #666; }
                .page-footer .footer-text { margin-left: 6pt; vertical-align: middle; }
            </style>
            </head><body>
            $body
            </body></html>
        """.trimIndent()
    }

    private fun renderCoverPage(snap: AdmissionExportSnapshot, locale: Locale): String {
        val patient = snap.patient
        val admission = snap.admission
        val generatedAt = AdmissionExportDateFormatter.formatDateTime(snap.generatedAt)
        val title = msg("admission.export.sections.cover", locale)
        val hospitalName = msg("app.hospital.name", locale)
        val generatedLabel = msg(
            "admission.export.cover.generatedOn",
            locale,
            generatedAt,
            snap.generatedByName,
        )
        val statusBanner = msg(
            "admission.export.cover.statusBanner",
            locale,
            enumLabel("admissionStatus", admission.status.name, locale),
        )
        return """
            <div class="section">
              <table class="cover-header"><tr>
                <td class="cover-logo">${logoSvg(COVER_LOGO_WIDTH_PX)}</td>
                <td class="cover-brand">
                  <div class="brand-name">$hospitalName</div>
                  <div class="brand-meta">$generatedLabel</div>
                </td>
              </tr></table>
              <h2>$title</h2>
              <div class="banner">$statusBanner</div>
              <table>
                <tr><th colspan="2">${msg("admission.export.cover.patient", locale)}</th></tr>
                <tr><td class="label">${msg("admission.export.field.name", locale)}</td>
                    <td>${escape(patient.firstName)} ${escape(patient.lastName)}</td></tr>
                <tr><td class="label">${msg("admission.export.field.dateOfBirth", locale)}</td>
                    <td>${AdmissionExportDateFormatter.formatDate(patient.dateOfBirth)} (${patient.age})</td></tr>
                <tr><td class="label">${msg("admission.export.field.sex", locale)}</td>
                    <td>${enumLabel("sex", patient.sex.name, locale)}</td></tr>
                <tr><td class="label">${msg("admission.export.field.maritalStatus", locale)}</td>
                    <td>${enumLabel("maritalStatus", patient.maritalStatus.name, locale)}</td></tr>
                <tr><td class="label">${msg("admission.export.field.idDocument", locale)}</td>
                    <td>${escape(patient.idDocumentNumber.orEmpty())}</td></tr>
                <tr><td class="label">${msg("admission.export.field.address", locale)}</td>
                    <td>${escape(patient.address)}</td></tr>
                <tr><th colspan="2">${msg("admission.export.cover.admission", locale)}</th></tr>
                <tr><td class="label">${msg("admission.export.field.admissionId", locale)}</td>
                    <td>${admission.id}</td></tr>
                <tr><td class="label">${msg("admission.export.field.admissionType", locale)}</td>
                    <td>${enumLabel("admissionType", admission.type.name, locale)}</td></tr>
                <tr><td class="label">${msg("admission.export.field.admissionDate", locale)}</td>
                    <td>${AdmissionExportDateFormatter.formatDateTime(admission.admissionDate)}</td></tr>
                <tr><td class="label">${msg("admission.export.field.dischargeDate", locale)}</td>
                    <td>${AdmissionExportDateFormatter.formatDateTime(admission.dischargeDate)}</td></tr>
                <tr><td class="label">${msg("admission.export.field.treatingPhysician", locale)}</td>
                    <td>${escape(admission.treatingPhysicianName)}</td></tr>
                <tr><td class="label">${msg("admission.export.field.room", locale)}</td>
                    <td>${escape(admission.roomNumber.orEmpty())}</td></tr>
                <tr><td class="label">${msg("admission.export.field.triage", locale)}</td>
                    <td>${formatTriage(admission.triageCode, admission.triageDescription, locale)}</td></tr>
              </table>
            </div>
        """.trimIndent()
    }

    private fun renderDemographics(snap: AdmissionExportSnapshot, locale: Locale): String {
        val patient = snap.patient
        val contactRows = if (snap.emergencyContacts.isEmpty()) {
            placeholderRow(EMERGENCY_CONTACT_COLS, locale)
        } else {
            snap.emergencyContacts.joinToString("") {
                "<tr><td>${escape(it.name)}</td><td>${escape(it.relationship)}</td>" +
                    "<td>${escape(it.phone)}</td></tr>"
            }
        }
        return """
            <div class="section">
              <h2>${msg("admission.export.sections.demographics", locale)}</h2>
              ${renderPatientFields(patient, locale)}
              <h3>${msg("admission.export.demographics.emergencyContacts", locale)}</h3>
              <table>
                <tr><th>${msg("admission.export.field.name", locale)}</th>
                    <th>${msg("admission.export.field.relationship", locale)}</th>
                    <th>${msg("admission.export.field.phone", locale)}</th></tr>
                $contactRows
              </table>
            </div>
        """.trimIndent()
    }

    private fun renderPatientFields(p: PatientSnapshot, locale: Locale): String =
        """
            <table>
              <tr><td class="label">${msg("admission.export.field.gender", locale)}</td>
                  <td>${escape(p.gender)}</td></tr>
              <tr><td class="label">${msg("admission.export.field.religion", locale)}</td>
                  <td>${escape(p.religion)}</td></tr>
              <tr><td class="label">${msg("admission.export.field.educationLevel", locale)}</td>
                  <td>${enumLabel("educationLevel", p.educationLevel.name, locale)}</td></tr>
              <tr><td class="label">${msg("admission.export.field.occupation", locale)}</td>
                  <td>${escape(p.occupation)}</td></tr>
              <tr><td class="label">${msg("admission.export.field.email", locale)}</td>
                  <td>${escape(p.email)}</td></tr>
            </table>
        """.trimIndent()

    private fun renderConsultingPhysicians(snap: AdmissionExportSnapshot, locale: Locale): String {
        val rows = if (snap.consultingPhysicians.isEmpty()) {
            placeholderRow(EMERGENCY_CONTACT_COLS, locale)
        } else {
            snap.consultingPhysicians.joinToString("") {
                "<tr><td>${escape(it.physicianName)}</td>" +
                    "<td>${escape(it.reason.orEmpty())}</td>" +
                    "<td>${AdmissionExportDateFormatter.formatDate(it.requestedDate)}</td></tr>"
            }
        }
        return """
            <div class="section">
              <h2>${msg("admission.export.sections.consultingPhysicians", locale)}</h2>
              <table>
                <tr><th>${msg("admission.export.field.physician", locale)}</th>
                    <th>${msg("admission.export.field.reason", locale)}</th>
                    <th>${msg("admission.export.field.requestedDate", locale)}</th></tr>
                $rows
              </table>
            </div>
        """.trimIndent()
    }

    private fun renderClinicalHistory(ch: ClinicalHistorySnapshot?, locale: Locale): String {
        val title = "<h2>${msg("admission.export.sections.clinicalHistory", locale)}</h2>"
        if (ch == null) {
            return "<div class=\"section\">$title<p class=\"placeholder\">${noRecords(locale)}</p></div>"
        }
        val fields = listOf(
            "reasonForAdmission" to ch.reasonForAdmission,
            "historyOfPresentIllness" to ch.historyOfPresentIllness,
            "psychiatricHistory" to ch.psychiatricHistory,
            "medicalHistory" to ch.medicalHistory,
            "familyHistory" to ch.familyHistory,
            "personalHistory" to ch.personalHistory,
            "substanceUseHistory" to ch.substanceUseHistory,
            "legalHistory" to ch.legalHistory,
            "socialHistory" to ch.socialHistory,
            "developmentalHistory" to ch.developmentalHistory,
            "educationalOccupationalHistory" to ch.educationalOccupationalHistory,
            "sexualHistory" to ch.sexualHistory,
            "religiousSpiritualHistory" to ch.religiousSpiritualHistory,
            "mentalStatusExam" to ch.mentalStatusExam,
            "physicalExam" to ch.physicalExam,
            "diagnosticImpression" to ch.diagnosticImpression,
            "treatmentPlan" to ch.treatmentPlan,
            "riskAssessment" to ch.riskAssessment,
            "prognosis" to ch.prognosis,
            "informedConsentNotes" to ch.informedConsentNotes,
            "additionalNotes" to ch.additionalNotes,
        )
        val rendered = fields.joinToString("") { (key, value) ->
            "<h3>${msg("admission.export.clinicalHistory.$key", locale)}</h3>" +
                richText(value, locale)
        }
        return "<div class=\"section\">$title$rendered</div>"
    }

    private fun renderProgressNotes(notes: List<ProgressNoteSnapshot>, locale: Locale): String {
        val title = "<h2>${msg("admission.export.sections.progressNotes", locale)}</h2>"
        if (notes.isEmpty()) {
            return "<div class=\"section\">$title<p class=\"placeholder\">${noRecords(locale)}</p></div>"
        }
        val body = notes.joinToString("") { note ->
            """
            <div class="section">
              <h3>${AdmissionExportDateFormatter.formatDateTime(note.createdAt)}</h3>
              <div class="field"><span class="label">S:</span> ${richText(note.subjectiveData, locale)}</div>
              <div class="field"><span class="label">O:</span> ${richText(note.objectiveData, locale)}</div>
              <div class="field"><span class="label">A:</span> ${richText(note.analysis, locale)}</div>
              <div class="field"><span class="label">P:</span> ${richText(note.actionPlans, locale)}</div>
            </div>
            """.trimIndent()
        }
        return "<div>$title$body</div>"
    }

    private fun renderMedicalOrders(orders: List<MedicalOrderSnapshot>, locale: Locale): String {
        val title = "<h2>${msg("admission.export.sections.medicalOrders", locale)}</h2>"
        if (orders.isEmpty()) {
            return "<div class=\"section\">$title<p class=\"placeholder\">${noRecords(locale)}</p></div>"
        }
        val byCategory = orders.groupBy { it.category }
        val body = byCategory.entries.joinToString("") { (category, list) ->
            val rows = list.joinToString("") { renderMedicalOrderBlock(it, locale) }
            "<h3>${enumLabel("medicalOrderCategory", category.name, locale)}</h3>$rows"
        }
        return "<div>$title$body</div>"
    }

    private fun renderMedicalOrderBlock(order: MedicalOrderSnapshot, locale: Locale): String {
        val administrations = if (order.administrations.isEmpty()) {
            ""
        } else {
            val rows = order.administrations.joinToString("") {
                "<tr><td>${AdmissionExportDateFormatter.formatDateTime(it.administeredAt)}</td>" +
                    "<td>${enumLabel("administrationStatus", it.status.name, locale)}</td>" +
                    "<td>${escape(it.notes.orEmpty())}</td></tr>"
            }
            """
            <h3>${msg("admission.export.medicalOrder.administrations", locale)}</h3>
            <table><tr>
              <th>${msg("admission.export.field.administeredAt", locale)}</th>
              <th>${msg("admission.export.field.status", locale)}</th>
              <th>${msg("admission.export.field.notes", locale)}</th>
            </tr>$rows</table>
            """.trimIndent()
        }
        val docNote = if (order.documentAttachmentIds.isEmpty()) {
            ""
        } else {
            "<div class=\"field\">${msg("admission.export.medicalOrder.attachments", locale)}: " +
                order.documentAttachmentIds.joinToString(", ") + "</div>"
        }
        return """
            <div class="section">
              <table>
                <tr><td class="label">${msg("admission.export.field.startDate", locale)}</td>
                    <td>${AdmissionExportDateFormatter.formatDate(order.startDate)}</td></tr>
                <tr><td class="label">${msg("admission.export.field.endDate", locale)}</td>
                    <td>${AdmissionExportDateFormatter.formatDate(order.endDate)}</td></tr>
                <tr><td class="label">${msg("admission.export.field.medication", locale)}</td>
                    <td>${escape(order.medication.orEmpty())}</td></tr>
                <tr><td class="label">${msg("admission.export.field.dosage", locale)}</td>
                    <td>${escape(order.dosage.orEmpty())}</td></tr>
                <tr><td class="label">${msg("admission.export.field.route", locale)}</td>
                    <td>${enumLabel("administrationRoute", order.route?.name, locale)}</td></tr>
                <tr><td class="label">${msg("admission.export.field.frequency", locale)}</td>
                    <td>${escape(order.frequency.orEmpty())}</td></tr>
                <tr><td class="label">${msg("admission.export.field.status", locale)}</td>
                    <td>${enumLabel("medicalOrderStatus", order.status.name, locale)}</td></tr>
                <tr><td class="label">${msg("admission.export.field.authorizedAt", locale)}</td>
                    <td>${AdmissionExportDateFormatter.formatDateTime(order.authorizedAt)}</td></tr>
                <tr><td class="label">${msg("admission.export.field.rejectedAt", locale)}</td>
                    <td>${AdmissionExportDateFormatter.formatDateTime(order.rejectedAt)}</td></tr>
                <tr><td class="label">${msg("admission.export.field.rejectionReason", locale)}</td>
                    <td>${escape(order.rejectionReason.orEmpty())}</td></tr>
                <tr><td class="label">${msg("admission.export.field.observations", locale)}</td>
                    <td>${richText(order.observations, locale)}</td></tr>
              </table>
              $docNote
              $administrations
            </div>
        """.trimIndent()
    }

    private fun renderPsychotherapy(activities: List<PsychotherapyActivitySnapshot>, locale: Locale): String {
        val title = "<h2>${msg("admission.export.sections.psychotherapy", locale)}</h2>"
        if (activities.isEmpty()) {
            return "<div class=\"section\">$title<p class=\"placeholder\">${noRecords(locale)}</p></div>"
        }
        val rows = activities.joinToString("") {
            "<tr><td>${AdmissionExportDateFormatter.formatDateTime(it.createdAt)}</td>" +
                "<td>${escape(it.categoryName)}</td>" +
                "<td>${richText(it.description, locale)}</td></tr>"
        }
        return """
            <div class="section">
              $title
              <table>
                <tr>
                  <th>${msg("admission.export.field.recordedAt", locale)}</th>
                  <th>${msg("admission.export.field.category", locale)}</th>
                  <th>${msg("admission.export.field.description", locale)}</th>
                </tr>
                $rows
              </table>
            </div>
        """.trimIndent()
    }

    private fun renderNursingNotes(notes: List<NursingNoteSnapshot>, locale: Locale): String {
        val title = "<h2>${msg("admission.export.sections.nursingNotes", locale)}</h2>"
        if (notes.isEmpty()) {
            return "<div class=\"section\">$title<p class=\"placeholder\">${noRecords(locale)}</p></div>"
        }
        val body = notes.joinToString("") {
            "<div class=\"section\"><h3>${AdmissionExportDateFormatter.formatDateTime(it.createdAt)}</h3>" +
                richText(it.description, locale) + "</div>"
        }
        return "<div>$title$body</div>"
    }

    private fun renderVitalSigns(signs: List<VitalSignSnapshot>, locale: Locale): String {
        val title = "<h2>${msg("admission.export.sections.vitalSigns", locale)}</h2>"
        if (signs.isEmpty()) {
            return "<div class=\"section\">$title<p class=\"placeholder\">${noRecords(locale)}</p></div>"
        }
        val rows = signs.joinToString("") {
            "<tr><td>${AdmissionExportDateFormatter.formatDateTime(it.recordedAt)}</td>" +
                "<td>${it.systolicBp}/${it.diastolicBp}</td>" +
                "<td>${it.heartRate}</td>" +
                "<td>${it.respiratoryRate}</td>" +
                "<td>${it.temperature}</td>" +
                "<td>${it.oxygenSaturation}</td>" +
                "<td>${it.glucose ?: ""}</td>" +
                "<td>${escape(it.other.orEmpty())}</td></tr>"
        }
        return """
            <div class="section">
              $title
              <table>
                <tr>
                  <th>${msg("admission.export.field.recordedAt", locale)}</th>
                  <th>${msg("admission.export.vitalSigns.bp", locale)}</th>
                  <th>${msg("admission.export.vitalSigns.hr", locale)}</th>
                  <th>${msg("admission.export.vitalSigns.rr", locale)}</th>
                  <th>${msg("admission.export.vitalSigns.temp", locale)}</th>
                  <th>${msg("admission.export.vitalSigns.spo2", locale)}</th>
                  <th>${msg("admission.export.vitalSigns.glucose", locale)}</th>
                  <th>${msg("admission.export.vitalSigns.other", locale)}</th>
                </tr>
                $rows
              </table>
            </div>
        """.trimIndent()
    }

    private fun renderMedicationAdministrations(list: List<MedicationAdministrationSnapshot>, locale: Locale): String {
        val title = "<h2>${msg("admission.export.sections.medicationAdministrations", locale)}</h2>"
        if (list.isEmpty()) {
            return "<div class=\"section\">$title<p class=\"placeholder\">${noRecords(locale)}</p></div>"
        }
        val rows = list.joinToString("") {
            "<tr><td>${AdmissionExportDateFormatter.formatDateTime(it.administeredAt)}</td>" +
                "<td>${it.medicalOrderId}</td>" +
                "<td>${enumLabel("administrationStatus", it.status.name, locale)}</td>" +
                "<td>${escape(it.notes.orEmpty())}</td></tr>"
        }
        return """
            <div class="section">
              $title
              <table>
                <tr>
                  <th>${msg("admission.export.field.administeredAt", locale)}</th>
                  <th>${msg("admission.export.field.orderId", locale)}</th>
                  <th>${msg("admission.export.field.status", locale)}</th>
                  <th>${msg("admission.export.field.notes", locale)}</th>
                </tr>
                $rows
              </table>
            </div>
        """.trimIndent()
    }

    private fun renderBilling(
        charges: List<PatientChargeSnapshot>,
        invoices: List<InvoiceSnapshot>,
        locale: Locale,
    ): String {
        val title = "<h2>${msg("admission.export.sections.billing", locale)}</h2>"
        val chargesTable = if (charges.isEmpty()) {
            "<p class=\"placeholder\">${noRecords(locale)}</p>"
        } else {
            val rows = charges.joinToString("") {
                "<tr><td>${AdmissionExportDateFormatter.formatDate(it.chargeDate)}</td>" +
                    "<td>${enumLabel("chargeType", it.chargeType.name, locale)}</td>" +
                    "<td>${escape(it.description)}</td>" +
                    "<td>${it.quantity}</td>" +
                    "<td>${it.unitPrice}</td>" +
                    "<td>${it.totalAmount}</td>" +
                    "<td>${escape(it.reason.orEmpty())}</td></tr>"
            }
            """
            <h3>${msg("admission.export.billing.charges", locale)}</h3>
            <table><tr>
              <th>${msg("admission.export.field.chargeDate", locale)}</th>
              <th>${msg("admission.export.field.chargeType", locale)}</th>
              <th>${msg("admission.export.field.description", locale)}</th>
              <th>${msg("admission.export.field.quantity", locale)}</th>
              <th>${msg("admission.export.field.unitPrice", locale)}</th>
              <th>${msg("admission.export.field.totalAmount", locale)}</th>
              <th>${msg("admission.export.field.reason", locale)}</th>
            </tr>$rows</table>
            """.trimIndent()
        }
        val invoicesTable = if (invoices.isEmpty()) {
            ""
        } else {
            val rows = invoices.joinToString("") {
                "<tr><td>${escape(it.invoiceNumber)}</td>" +
                    "<td>${it.totalAmount}</td>" +
                    "<td>${it.chargeCount}</td>" +
                    "<td>${escape(it.notes.orEmpty())}</td>" +
                    "<td>${AdmissionExportDateFormatter.formatDateTime(it.createdAt)}</td></tr>"
            }
            """
            <h3>${msg("admission.export.billing.invoices", locale)}</h3>
            <table><tr>
              <th>${msg("admission.export.field.invoiceNumber", locale)}</th>
              <th>${msg("admission.export.field.totalAmount", locale)}</th>
              <th>${msg("admission.export.field.chargeCount", locale)}</th>
              <th>${msg("admission.export.field.notes", locale)}</th>
              <th>${msg("admission.export.field.createdAt", locale)}</th>
            </tr>$rows</table>
            """.trimIndent()
        }
        return "<div>$title$chargesTable$invoicesTable</div>"
    }

    private fun renderDocumentsIndex(
        attachments: List<AttachmentSnapshot>,
        indexEntries: List<AttachmentIndexEntry>,
        locale: Locale,
    ): String {
        val title = "<h2>${msg("admission.export.sections.documentsIndex", locale)}</h2>"
        if (attachments.isEmpty()) {
            return "<div class=\"section\">$title<p class=\"placeholder\">${noRecords(locale)}</p></div>"
        }
        val entriesByKey = indexEntries.associateBy { it.source to it.attachmentId }
        val rows = attachments.joinToString("") {
            val entry = entriesByKey[it.source to it.id]
            "<tr><td>${escape(it.fileName)}</td>" +
                "<td>${sourceLabel(it.source, locale)}</td>" +
                "<td>${escape(it.uploadedByName ?: it.uploadedBy?.toString().orEmpty())}</td>" +
                "<td>${AdmissionExportDateFormatter.formatDateTime(it.uploadedAt)}</td>" +
                "<td>${escape(it.contentType)}</td>" +
                "<td>${it.byteSize}</td>" +
                "<td>${escape(entry?.checksum.orEmpty())}</td>" +
                "<td>${entry?.appendixPageNumber ?: ""}</td></tr>"
        }
        return """
            <div class="section">
              $title
              <table>
                <tr>
                  <th>${msg("admission.export.field.fileName", locale)}</th>
                  <th>${msg("admission.export.field.source", locale)}</th>
                  <th>${msg("admission.export.field.uploadedBy", locale)}</th>
                  <th>${msg("admission.export.field.uploadedAt", locale)}</th>
                  <th>${msg("admission.export.field.contentType", locale)}</th>
                  <th>${msg("admission.export.field.fileSize", locale)}</th>
                  <th>${msg("admission.export.field.checksum", locale)}</th>
                  <th>${msg("admission.export.field.appendixPage", locale)}</th>
                </tr>
                $rows
              </table>
            </div>
        """.trimIndent()
    }

    private fun sourceLabel(source: AttachmentSource, locale: Locale): String = msg(
        "admission.export.attachmentSource.${source.name.lowercase()}",
        locale,
    )

    private fun placeholderRow(columns: Int, locale: Locale): String =
        "<tr><td colspan=\"$columns\" class=\"placeholder\">${noRecords(locale)}</td></tr>"

    private fun noRecords(locale: Locale): String = msg("admission.export.no_records", locale)

    private fun richText(html: String?, locale: Locale): String {
        val cleaned = sanitizer.clean(html)
        return if (cleaned.isBlank()) {
            "<span class=\"placeholder\">${noRecords(locale)}</span>"
        } else {
            cleaned
        }
    }

    private fun msg(key: String, locale: Locale, vararg args: Any): String =
        escape(messageService.getMessage(key, locale, *args))

    private fun formatTriage(code: String?, dbDescription: String?, locale: Locale): String {
        if (code.isNullOrBlank()) return ""
        val key = "admission.export.triageCode.${code.uppercase(Locale.ROOT)}"
        val raw = messageService.getMessage(key, locale)
        val description = if (raw == key) dbDescription.orEmpty() else raw
        return if (description.isBlank()) escape(code) else "${escape(code)} — ${escape(description)}"
    }

    private fun enumLabel(category: String, value: String?, locale: Locale): String {
        if (value.isNullOrBlank()) return ""
        val key = "admission.export.enum.$category.$value"
        val raw = messageService.getMessage(key, locale)
        return escape(if (raw == key) value else raw)
    }

    private fun renderRunningFooter(snap: AdmissionExportSnapshot, locale: Locale): String {
        val hospitalName = msg("app.hospital.name", locale)
        val admissionId = snap.admission.id
        val generatedAt = AdmissionExportDateFormatter.formatDateTime(snap.generatedAt)
        val footerText = msg(
            "admission.export.footer.text",
            locale,
            hospitalName,
            admissionId,
            generatedAt,
        )
        return """
            <div class="page-footer">
              ${logoSvg(FOOTER_LOGO_WIDTH_PX)}
              <span class="footer-text">$footerText</span>
            </div>
        """.trimIndent()
    }

    private fun logoSvg(widthPx: Int): String {
        if (logoSvgRaw.isEmpty()) return ""
        val height = (widthPx * LOGO_HEIGHT_RATIO).toInt().coerceAtLeast(1)
        return logoSvgRaw.replaceFirst("<svg ", """<svg width="$widthPx" height="$height" """)
    }

    private fun loadLogoSvg(): String {
        val resource = ClassPathResource(LOGO_RESOURCE_PATH)
        if (!resource.exists()) return ""
        val raw = resource.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val xmlDeclEnd = raw.indexOf("?>")
        val withoutXmlDecl = if (raw.trimStart().startsWith("<?xml") && xmlDeclEnd >= 0) {
            raw.substring(xmlDeclEnd + 2).trimStart()
        } else {
            raw
        }
        return withoutXmlDecl
    }

    companion object {
        private const val EMERGENCY_CONTACT_COLS = 3
        private const val LOGO_RESOURCE_PATH = "export/logo.svg"
        private const val COVER_LOGO_WIDTH_PX = 130
        private const val FOOTER_LOGO_WIDTH_PX = 70
        private const val LOGO_HEIGHT_RATIO = 228.6 / 565.4 // viewBox aspect ratio of logo.svg

        @JvmStatic
        fun escape(input: String?): String {
            if (input == null) return ""
            val sb = StringBuilder(input.length)
            for (c in input) {
                when (c) {
                    '&' -> sb.append("&amp;")
                    '<' -> sb.append("&lt;")
                    '>' -> sb.append("&gt;")
                    '"' -> sb.append("&quot;")
                    '\'' -> sb.append("&#39;")
                    else -> sb.append(c)
                }
            }
            return sb.toString()
        }
    }
}
