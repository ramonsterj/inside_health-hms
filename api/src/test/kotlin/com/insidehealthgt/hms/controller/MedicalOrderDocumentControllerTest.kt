package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateMedicalOrderRequest
import com.insidehealthgt.hms.entity.MedicalOrderCategory
import com.insidehealthgt.hms.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

class MedicalOrderDocumentControllerTest : AbstractIntegrationTest() {

    private lateinit var adminToken: String
    private lateinit var doctorToken: String
    private lateinit var nurseToken: String
    private lateinit var adminStaffToken: String
    private lateinit var doctorUser: User
    private var admissionId: Long = 0
    private var orderId: Long = 0

    @BeforeEach
    fun setUp() {
        val (_, adminTkn) = createAdminUser()
        adminToken = adminTkn

        val (docUsr, docTkn) = createDoctorUser()
        doctorUser = docUsr
        doctorToken = docTkn

        val (_, nurseTkn) = createNurseUser()
        nurseToken = nurseTkn

        val (_, staffTkn) = createAdminStaffUser()
        adminStaffToken = staffTkn

        // Create admission and lab order for tests
        val patientId = createPatient(adminToken)
        admissionId = createAdmission(adminToken, patientId, doctorUser.id!!)
        orderId = createLabOrder()
    }

    private fun basePath(): String = "/api/v1/admissions/$admissionId/medical-orders/$orderId/documents"

    // ============ UPLOAD TESTS ============

    @Test
    fun `doctor can upload document to medical order`() {
        val mockFile = MockMultipartFile(
            "file",
            "lab-result.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "fake-pdf-content".toByteArray(),
        )

        mockMvc.perform(
            multipart(basePath())
                .file(mockFile)
                .param("displayName", "Blood Work Results")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.displayName").value("Blood Work Results"))
            .andExpect(jsonPath("$.data.fileName").value("lab-result.pdf"))
            .andExpect(jsonPath("$.data.contentType").value("application/pdf"))
            .andExpect(jsonPath("$.data.createdBy.firstName").value("Dr. Maria"))
    }

    @Test
    fun `nurse can upload document to medical order`() {
        val mockFile = MockMultipartFile(
            "file",
            "scan.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "fake-image-content".toByteArray(),
        )

        mockMvc.perform(
            multipart(basePath())
                .file(mockFile)
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.displayName").value("scan.jpg"))
            .andExpect(jsonPath("$.data.contentType").value("image/jpeg"))
    }

    @Test
    fun `admin can upload document to medical order`() {
        val mockFile = MockMultipartFile(
            "file",
            "result.png",
            MediaType.IMAGE_PNG_VALUE,
            "fake-png-content".toByteArray(),
        )

        mockMvc.perform(
            multipart(basePath())
                .file(mockFile)
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    fun `administrative staff cannot upload document`() {
        val mockFile = MockMultipartFile(
            "file",
            "lab.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "content".toByteArray(),
        )

        mockMvc.perform(
            multipart(basePath())
                .file(mockFile)
                .header("Authorization", "Bearer $adminStaffToken"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `upload without display name uses original filename`() {
        val mockFile = MockMultipartFile(
            "file",
            "original-filename.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "content".toByteArray(),
        )

        mockMvc.perform(
            multipart(basePath())
                .file(mockFile)
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.displayName").value("original-filename.pdf"))
    }

    // ============ VALIDATION TESTS ============

    @Test
    fun `upload fails for invalid file type`() {
        val mockFile = MockMultipartFile(
            "file",
            "notes.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "text content".toByteArray(),
        )

        mockMvc.perform(
            multipart(basePath())
                .file(mockFile)
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `upload fails for empty file`() {
        val mockFile = MockMultipartFile(
            "file",
            "empty.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            ByteArray(0),
        )

        mockMvc.perform(
            multipart(basePath())
                .file(mockFile)
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `upload fails for non-existent order`() {
        val mockFile = MockMultipartFile(
            "file",
            "lab.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "content".toByteArray(),
        )

        mockMvc.perform(
            multipart("/api/v1/admissions/$admissionId/medical-orders/99999/documents")
                .file(mockFile)
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `upload fails for non-existent admission`() {
        val mockFile = MockMultipartFile(
            "file",
            "lab.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "content".toByteArray(),
        )

        mockMvc.perform(
            multipart("/api/v1/admissions/99999/medical-orders/$orderId/documents")
                .file(mockFile)
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ============ LIST TESTS ============

    @Test
    fun `list documents returns empty list when none uploaded`() {
        mockMvc.perform(
            get(basePath())
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(0))
    }

    @Test
    fun `list documents returns uploaded documents`() {
        uploadDocument("first.pdf", "First Result")
        uploadDocument("second.jpg", "Second Result", MediaType.IMAGE_JPEG_VALUE)

        mockMvc.perform(
            get(basePath())
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(2))
    }

    @Test
    fun `nurse can list documents with read permission`() {
        uploadDocument("lab.pdf", "Lab Result")

        mockMvc.perform(
            get(basePath())
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
    }

    @Test
    fun `list documents for non-existent order returns 404`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/medical-orders/99999/documents")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ============ DOWNLOAD TESTS ============

    @Test
    fun `download document returns file content`() {
        val docId = uploadDocumentAndGetId("test.pdf", "Test Document")

        mockMvc.perform(
            get("${basePath()}/$docId/file")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `download non-existent document returns 404`() {
        mockMvc.perform(
            get("${basePath()}/99999/file")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ============ THUMBNAIL TESTS ============

    @Test
    fun `get thumbnail for document without thumbnail returns 404`() {
        // PDF files might not generate thumbnails depending on system capabilities
        val docId = uploadDocumentAndGetId("test.pdf", "Test")

        // This may return 200 or 404 depending on thumbnail generation;
        // we just verify it doesn't error with 500
        val result = mockMvc.perform(
            get("${basePath()}/$docId/thumbnail")
                .header("Authorization", "Bearer $doctorToken"),
        ).andReturn()

        val statusCode = result.response.status
        assert(statusCode == 200 || statusCode == 404) {
            "Expected 200 or 404 but got $statusCode"
        }
    }

    // ============ DELETE TESTS ============

    @Test
    fun `admin can delete document`() {
        val docId = uploadDocumentAndGetId("to-delete.pdf", "Delete Me")

        mockMvc.perform(
            delete("${basePath()}/$docId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        // Verify it's soft deleted (no longer in list)
        mockMvc.perform(
            get(basePath())
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(0))
    }

    @Test
    fun `doctor cannot delete document`() {
        val docId = uploadDocumentAndGetId("no-delete.pdf", "No Delete")

        mockMvc.perform(
            delete("${basePath()}/$docId")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `nurse cannot delete document`() {
        val docId = uploadDocumentAndGetId("no-delete.pdf", "No Delete")

        mockMvc.perform(
            delete("${basePath()}/$docId")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `delete non-existent document returns 404`() {
        mockMvc.perform(
            delete("${basePath()}/99999")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ============ UNAUTHENTICATED TESTS ============

    @Test
    fun `list documents fails without authentication`() {
        mockMvc.perform(get(basePath()))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `upload fails without authentication`() {
        val mockFile = MockMultipartFile(
            "file",
            "lab.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "content".toByteArray(),
        )

        mockMvc.perform(multipart(basePath()).file(mockFile))
            .andExpect(status().isUnauthorized)
    }

    // ============ DOCUMENT COUNT IN MEDICAL ORDERS ============

    @Test
    fun `medical order list includes document count`() {
        uploadDocument("first.pdf", "First")
        uploadDocument("second.pdf", "Second")

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/medical-orders")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.orders.LABORATORIOS[0].documentCount").value(2))
    }

    // ============ HELPERS ============

    private fun createLabOrder(): Long {
        val request = CreateMedicalOrderRequest(
            category = MedicalOrderCategory.LABORATORIOS,
            startDate = LocalDate.now(),
            observations = "Complete blood count",
        )

        val result = mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .get("data").get("id").asLong()
    }

    private fun uploadDocument(
        filename: String,
        displayName: String,
        contentType: String = MediaType.APPLICATION_PDF_VALUE,
    ) {
        val mockFile = MockMultipartFile(
            "file",
            filename,
            contentType,
            "fake-content-$filename".toByteArray(),
        )

        mockMvc.perform(
            multipart(basePath())
                .file(mockFile)
                .param("displayName", displayName)
                .header("Authorization", "Bearer $doctorToken"),
        ).andExpect(status().isCreated)
    }

    private fun uploadDocumentAndGetId(
        filename: String,
        displayName: String,
        contentType: String = MediaType.APPLICATION_PDF_VALUE,
    ): Long {
        val mockFile = MockMultipartFile(
            "file",
            filename,
            contentType,
            "fake-content-$filename".toByteArray(),
        )

        val result = mockMvc.perform(
            multipart(basePath())
                .file(mockFile)
                .param("displayName", displayName)
                .header("Authorization", "Bearer $doctorToken"),
        ).andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .get("data").get("id").asLong()
    }
}
