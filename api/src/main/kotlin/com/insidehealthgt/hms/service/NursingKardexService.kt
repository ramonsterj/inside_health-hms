package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.response.KardexAdmissionSummary
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.MedicalOrderCategory
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.MedicalOrderRepository
import com.insidehealthgt.hms.repository.MedicationAdministrationRepository
import com.insidehealthgt.hms.repository.NursingNoteRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.repository.VitalSignRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NursingKardexService(
    private val admissionRepository: AdmissionRepository,
    private val medicalOrderRepository: MedicalOrderRepository,
    private val medicationAdministrationRepository: MedicationAdministrationRepository,
    private val vitalSignRepository: VitalSignRepository,
    private val nursingNoteRepository: NursingNoteRepository,
    private val userRepository: UserRepository,
    private val messageService: MessageService,
) {

    companion object {
        val KARDEX_CATEGORIES = listOf(
            MedicalOrderCategory.MEDICAMENTOS,
            MedicalOrderCategory.DIETA,
            MedicalOrderCategory.CUIDADOS_ESPECIALES,
            MedicalOrderCategory.RESTRICCIONES_MOVILIDAD,
            MedicalOrderCategory.PERMISOS_VISITA,
        )

        val CARE_INSTRUCTION_CATEGORIES = listOf(
            MedicalOrderCategory.DIETA,
            MedicalOrderCategory.CUIDADOS_ESPECIALES,
            MedicalOrderCategory.RESTRICCIONES_MOVILIDAD,
            MedicalOrderCategory.PERMISOS_VISITA,
        )
    }

    @Transactional(readOnly = true)
    fun getKardexSummaries(type: AdmissionType?, search: String?, pageable: Pageable): Page<KardexAdmissionSummary> {
        val admissionsPage = admissionRepository.findActiveKardexAdmissions(
            type = type?.name,
            search = search,
            pageable = pageable,
        )

        val admissionIds = admissionsPage.content.mapNotNull { it.id }
        if (admissionIds.isEmpty()) return PageImpl(emptyList(), pageable, 0)

        return assembleKardexPage(admissionsPage, admissionIds, pageable)
    }

    @Transactional(readOnly = true)
    fun getKardexSummary(admissionId: Long): KardexAdmissionSummary {
        val admission = admissionRepository.findByIdWithRelations(admissionId)
            ?: throw ResourceNotFoundException(messageService.errorAdmissionNotFound(admissionId))

        if (!admission.isActive()) {
            throw ResourceNotFoundException(messageService.errorAdmissionNotFound(admissionId))
        }

        val ids = listOf(admissionId)

        val orders = medicalOrderRepository.findActiveByAdmissionIdsAndCategories(ids, KARDEX_CATEGORIES)
        val medications = orders.filter { it.category == MedicalOrderCategory.MEDICAMENTOS }
        val careInstructions = orders.filter { it.category in CARE_INSTRUCTION_CATEGORIES }

        val medicationOrderIds = medications.mapNotNull { it.id }
        val latestAdministrations = if (medicationOrderIds.isNotEmpty()) {
            medicationAdministrationRepository.findLatestByMedicalOrderIds(medicationOrderIds)
                .associateBy { it.medicalOrder.id!! }
        } else {
            emptyMap()
        }

        val latestVitalSign = vitalSignRepository.findLatestByAdmissionIds(ids).firstOrNull()
        val latestNote = nursingNoteRepository.findLatestByAdmissionIds(ids).firstOrNull()

        val userIds = collectUserIds(
            admissionsPage = listOf(admission),
            latestAdministrations = latestAdministrations.values.toList(),
            latestVitals = listOfNotNull(latestVitalSign),
        )
        val usersById = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }

        return KardexAdmissionSummary.from(
            admission = admission,
            medications = medications,
            careInstructions = careInstructions,
            latestVitalSign = latestVitalSign,
            latestNursingNote = latestNote,
            latestAdministrations = latestAdministrations,
            usersById = usersById,
        )
    }

    @Suppress("LongMethod")
    private fun assembleKardexPage(
        admissionsPage: Page<com.insidehealthgt.hms.entity.Admission>,
        admissionIds: List<Long>,
        pageable: Pageable,
    ): Page<KardexAdmissionSummary> {
        // Batch-fetch with proper joins (native query doesn't populate JPA relations)
        val admissionsWithRelations = admissionRepository.findByIdsWithRelations(admissionIds)
        val admissionsById = admissionsWithRelations.associateBy { it.id!! }

        // Batch-fetch active orders
        val allOrders = medicalOrderRepository.findActiveByAdmissionIdsAndCategories(
            admissionIds,
            KARDEX_CATEGORIES,
        )
        val ordersByAdmission = allOrders.groupBy { it.admission.id!! }

        // Batch-fetch latest medication administrations
        val medicationOrderIds = allOrders
            .filter { it.category == MedicalOrderCategory.MEDICAMENTOS }
            .mapNotNull { it.id }
        val latestAdministrations = if (medicationOrderIds.isNotEmpty()) {
            medicationAdministrationRepository.findLatestByMedicalOrderIds(medicationOrderIds)
                .associateBy { it.medicalOrder.id!! }
        } else {
            emptyMap()
        }

        // Batch-fetch latest vitals and notes
        val latestVitals = vitalSignRepository.findLatestByAdmissionIds(admissionIds)
            .associateBy { it.admission.id!! }
        val latestNotes = nursingNoteRepository.findLatestByAdmissionIds(admissionIds)
            .associateBy { it.admission.id!! }

        // Batch-fetch user display names
        val userIds = collectUserIds(
            admissionsPage = admissionsWithRelations,
            latestAdministrations = latestAdministrations.values.toList(),
            latestVitals = latestVitals.values.toList(),
        )
        val usersById = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }

        // Assemble summaries preserving page order
        val summaries = admissionIds.mapNotNull { admissionId ->
            val admission = admissionsById[admissionId] ?: return@mapNotNull null
            val orders = ordersByAdmission[admissionId] ?: emptyList()
            val medications = orders.filter { it.category == MedicalOrderCategory.MEDICAMENTOS }
            val careInstructions = orders.filter { it.category in CARE_INSTRUCTION_CATEGORIES }

            KardexAdmissionSummary.from(
                admission = admission,
                medications = medications,
                careInstructions = careInstructions,
                latestVitalSign = latestVitals[admissionId],
                latestNursingNote = latestNotes[admissionId],
                latestAdministrations = latestAdministrations,
                usersById = usersById,
            )
        }

        return PageImpl(summaries, pageable, admissionsPage.totalElements)
    }

    private fun collectUserIds(
        admissionsPage: List<com.insidehealthgt.hms.entity.Admission>,
        latestAdministrations: List<com.insidehealthgt.hms.entity.MedicationAdministration>,
        latestVitals: List<com.insidehealthgt.hms.entity.VitalSign>,
    ): List<Long> = (
        admissionsPage.mapNotNull { it.treatingPhysician.id } +
            latestAdministrations.mapNotNull { it.createdBy } +
            latestVitals.mapNotNull { it.createdBy }
        ).distinct()
}
