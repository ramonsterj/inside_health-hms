package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.AdmissionConsentDocumentRepository
import com.insidehealthgt.hms.repository.AdmissionConsultingPhysicianRepository
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.PatientRepository
import com.insidehealthgt.hms.repository.RoomRepository
import com.insidehealthgt.hms.repository.TriageCodeRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.AccessDeniedException
import kotlin.test.assertEquals

/**
 * Unit tests for the patient-scoped admission history gate. The full behaviour (ordering,
 * pagination, soft-delete exclusion, real DTO mapping, role-based gate) is covered by
 * AdmissionControllerTest against Testcontainers; here we assert the gate-not-filter contract:
 * access is delegated to [PatientService.assertPatientAccessible], and the admission query is
 * only reached once that gate succeeds.
 */
class AdmissionServiceTest {

    private lateinit var admissionRepository: AdmissionRepository
    private lateinit var patientService: PatientService
    private lateinit var admissionService: AdmissionService

    @BeforeEach
    fun setUp() {
        admissionRepository = mock()
        patientService = mock()

        admissionService = AdmissionService(
            admissionRepository = admissionRepository,
            admissionConsentDocumentRepository = mock<AdmissionConsentDocumentRepository>(),
            admissionConsultingPhysicianRepository = mock<AdmissionConsultingPhysicianRepository>(),
            patientRepository = mock<PatientRepository>(),
            triageCodeRepository = mock<TriageCodeRepository>(),
            roomRepository = mock<RoomRepository>(),
            userRepository = mock<UserRepository>(),
            fileStorageService = mock<FileStorageService>(),
            eventPublisher = mock<ApplicationEventPublisher>(),
            messageService = mock<MessageService>(),
            patientService = patientService,
        )
    }

    @Test
    fun `findAdmissionsByPatient queries the patient's admissions once access is granted`() {
        val patientId = 42L
        val pageable = PageRequest.of(0, 20)
        whenever(admissionRepository.findByPatientIdWithRelations(eq(patientId), any()))
            .thenReturn(Page.empty(pageable))

        val result = admissionService.findAdmissionsByPatient(
            patientId = patientId,
            doctorId = 7L,
            activeAdmissionsOnly = false,
            pageable = pageable,
        )

        // Gate is consulted with the access predicates...
        verify(patientService).assertPatientAccessible(patientId, 7L, false)
        // ...and the query is scoped only by patientId — the predicates are NOT applied as filters.
        verify(admissionRepository).findByPatientIdWithRelations(eq(patientId), any())
        assertEquals(0, result.totalElements)
    }

    @Test
    fun `findAdmissionsByPatient propagates AccessDenied and never queries admissions`() {
        whenever(admissionRepository.findByPatientIdWithRelations(any(), any()))
            .thenReturn(Page.empty())
        doThrow(AccessDeniedException("denied"))
            .whenever(patientService).assertPatientAccessible(any(), any(), any())

        assertThrows<AccessDeniedException> {
            admissionService.findAdmissionsByPatient(99L, 7L, false, PageRequest.of(0, 20))
        }

        verify(admissionRepository, never()).findByPatientIdWithRelations(any(), any())
    }

    @Test
    fun `findAdmissionsByPatient propagates NotFound and never queries admissions`() {
        doThrow(ResourceNotFoundException("missing"))
            .whenever(patientService).assertPatientAccessible(any(), anyOrNull(), any())

        assertThrows<ResourceNotFoundException> {
            admissionService.findAdmissionsByPatient(404L, null, false, PageRequest.of(0, 20))
        }

        verify(admissionRepository, never()).findByPatientIdWithRelations(any(), any())
    }
}
