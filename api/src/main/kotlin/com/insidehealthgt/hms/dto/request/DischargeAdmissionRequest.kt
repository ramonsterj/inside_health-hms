package com.insidehealthgt.hms.dto.request

/**
 * Body for `POST /api/v1/admissions/{id}/discharge`.
 *
 * A discharge comment is **mandatory for every caller** permitted to discharge (ADMINISTRADOR and
 * MEDICO_RESIDENTE). The non-blank check lives in
 * [com.insidehealthgt.hms.service.AdmissionService.dischargePatient] so a single localized error
 * (`error.admission.discharge.note.required`) is returned. The body itself is optional at the binding
 * layer so a missing body is treated as a blank note and rejected with that same message.
 */
data class DischargeAdmissionRequest(val dischargeNote: String? = null)
