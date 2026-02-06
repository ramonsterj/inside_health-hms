package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.entity.VitalSign
import java.math.BigDecimal
import java.time.LocalDateTime

data class VitalSignResponse(
    val id: Long,
    val admissionId: Long,
    val recordedAt: LocalDateTime,
    val systolicBp: Int,
    val diastolicBp: Int,
    val heartRate: Int,
    val respiratoryRate: Int,
    val temperature: BigDecimal,
    val oxygenSaturation: Int,
    val other: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val createdBy: MedicalStaffResponse?,
    val updatedBy: MedicalStaffResponse?,
    val canEdit: Boolean,
) {
    companion object {
        fun from(
            vitalSign: VitalSign,
            createdByUser: User? = null,
            updatedByUser: User? = null,
            canEdit: Boolean = false,
        ): VitalSignResponse = VitalSignResponse(
            id = vitalSign.id!!,
            admissionId = vitalSign.admission.id!!,
            recordedAt = vitalSign.recordedAt,
            systolicBp = vitalSign.systolicBp,
            diastolicBp = vitalSign.diastolicBp,
            heartRate = vitalSign.heartRate,
            respiratoryRate = vitalSign.respiratoryRate,
            temperature = vitalSign.temperature,
            oxygenSaturation = vitalSign.oxygenSaturation,
            other = vitalSign.other,
            createdAt = vitalSign.createdAt,
            updatedAt = vitalSign.updatedAt,
            createdBy = createdByUser?.let { MedicalStaffResponse.from(it) },
            updatedBy = updatedByUser?.let { MedicalStaffResponse.from(it) },
            canEdit = canEdit,
        )
    }
}
