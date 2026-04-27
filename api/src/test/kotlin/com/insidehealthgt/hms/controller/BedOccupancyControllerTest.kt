package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateRoomRequest
import com.insidehealthgt.hms.entity.AdmissionStatus
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.RoomGender
import com.insidehealthgt.hms.entity.RoomType
import com.insidehealthgt.hms.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

class BedOccupancyControllerTest : AbstractIntegrationTest() {

    private lateinit var adminToken: String
    private lateinit var nurseToken: String
    private lateinit var staffToken: String
    private lateinit var doctorToken: String
    private lateinit var doctorUser: User

    @BeforeEach
    fun setUp() {
        val (_, adminTkn) = createAdminUser()
        adminToken = adminTkn

        val (_, nurseTkn) = createNurseUser()
        nurseToken = nurseTkn

        val (_, staffTkn) = createAdminStaffUser()
        staffToken = staffTkn

        val (docUsr, docTkn) = createDoctorUser()
        doctorUser = docUsr
        doctorToken = docTkn
    }

    private fun createRoom(number: String, capacity: Int, gender: RoomGender = RoomGender.FEMALE): Long {
        val req = CreateRoomRequest(
            number = number,
            type = if (capacity == 1) RoomType.PRIVATE else RoomType.SHARED,
            gender = gender,
            capacity = capacity,
        )
        val result = mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        ).andExpect(status().isCreated).andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("data").get("id").asLong()
    }

    @Test
    fun `occupancy endpoint returns empty summary when no rooms exist`() {
        mockMvc.perform(
            get("/api/v1/rooms/occupancy")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.summary.totalBeds").value(0))
            .andExpect(jsonPath("$.data.summary.occupiedBeds").value(0))
            .andExpect(jsonPath("$.data.summary.freeBeds").value(0))
            .andExpect(jsonPath("$.data.summary.occupancyPercent").value(0.0))
            .andExpect(jsonPath("$.data.rooms").isEmpty)
    }

    @Test
    fun `occupancy endpoint returns rooms with no occupants when none admitted`() {
        createRoom("101", 2)
        createRoom("102", 1)

        mockMvc.perform(
            get("/api/v1/rooms/occupancy")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.summary.totalBeds").value(3))
            .andExpect(jsonPath("$.data.summary.occupiedBeds").value(0))
            .andExpect(jsonPath("$.data.summary.freeBeds").value(3))
            .andExpect(jsonPath("$.data.rooms.length()").value(2))
            .andExpect(jsonPath("$.data.rooms[0].number").value("101"))
            .andExpect(jsonPath("$.data.rooms[0].occupants").isEmpty)
            .andExpect(jsonPath("$.data.rooms[0].occupiedBeds").value(0))
            .andExpect(jsonPath("$.data.rooms[0].availableBeds").value(2))
    }

    @Test
    fun `occupancy endpoint reflects active hospitalization admissions`() {
        val roomId = createRoom("201", 3, RoomGender.MALE)
        val triageCodeId = triageCodeRepository.findAll().first().id!!
        val patientId = createPatient(staffToken)

        createAdmission(
            token = staffToken,
            patientId = patientId,
            doctorId = doctorUser.id!!,
            type = AdmissionType.HOSPITALIZATION,
            roomId = roomId,
            triageCodeId = triageCodeId,
            admissionDate = LocalDateTime.now(),
        )

        mockMvc.perform(
            get("/api/v1/rooms/occupancy")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.summary.totalBeds").value(3))
            .andExpect(jsonPath("$.data.summary.occupiedBeds").value(1))
            .andExpect(jsonPath("$.data.summary.freeBeds").value(2))
            .andExpect(jsonPath("$.data.rooms[0].occupiedBeds").value(1))
            .andExpect(jsonPath("$.data.rooms[0].availableBeds").value(2))
            .andExpect(jsonPath("$.data.rooms[0].occupants.length()").value(1))
            .andExpect(jsonPath("$.data.rooms[0].occupants[0].patientName").value("Juan Perez"))
            .andExpect(jsonPath("$.data.rooms[0].occupants[0].admissionDate").exists())
    }

    @Test
    fun `occupancy endpoint excludes ambulatory admissions even if they hold a room`() {
        val roomId = createRoom("301", 2)
        val triageCodeId = triageCodeRepository.findAll().first().id!!
        val patientId = createPatient(staffToken)

        // AMBULATORY does not require a room — but even if a row references one,
        // it must not count as occupying a bed.
        createAdmission(
            token = staffToken,
            patientId = patientId,
            doctorId = doctorUser.id!!,
            type = AdmissionType.AMBULATORY,
            roomId = roomId,
            triageCodeId = triageCodeId,
        )

        mockMvc.perform(
            get("/api/v1/rooms/occupancy")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.summary.occupiedBeds").value(0))
            .andExpect(jsonPath("$.data.rooms[0].occupants").isEmpty)
    }

    @Test
    fun `occupancy endpoint excludes discharged admissions`() {
        val roomId = createRoom("401", 1)
        val triageCodeId = triageCodeRepository.findAll().first().id!!
        val patientId = createPatient(staffToken)

        val admissionId = createAdmission(
            token = staffToken,
            patientId = patientId,
            doctorId = doctorUser.id!!,
            type = AdmissionType.HOSPITALIZATION,
            roomId = roomId,
            triageCodeId = triageCodeId,
        )

        // Mark as discharged via the admission entity (status DISCHARGED).
        val admission = admissionRepository.findById(admissionId).orElseThrow()
        admission.status = AdmissionStatus.DISCHARGED
        admission.dischargeDate = LocalDateTime.now()
        admissionRepository.save(admission)

        mockMvc.perform(
            get("/api/v1/rooms/occupancy")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.summary.occupiedBeds").value(0))
            .andExpect(jsonPath("$.data.summary.freeBeds").value(1))
    }

    @Test
    fun `occupancy endpoint excludes soft-deleted rooms`() {
        createRoom("501", 1)
        val deletedRoomId = createRoom("502", 1)

        // Soft-delete the second room directly.
        val room = roomRepository.findById(deletedRoomId).orElseThrow()
        room.deletedAt = LocalDateTime.now()
        roomRepository.save(room)

        mockMvc.perform(
            get("/api/v1/rooms/occupancy")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.rooms.length()").value(1))
            .andExpect(jsonPath("$.data.rooms[0].number").value("501"))
    }

    @Test
    fun `occupancy endpoint computes occupancy percent`() {
        val roomId = createRoom("601", 4, RoomGender.MALE)
        val triageCodeId = triageCodeRepository.findAll().first().id!!

        val p1 = createPatient(staffToken)
        val p2 = createSecondPatient(staffToken)
        val p3 = createThirdPatient(staffToken)

        listOf(p1, p2, p3).forEach { pid ->
            createAdmission(
                token = staffToken,
                patientId = pid,
                doctorId = doctorUser.id!!,
                type = AdmissionType.HOSPITALIZATION,
                roomId = roomId,
                triageCodeId = triageCodeId,
            )
        }

        mockMvc.perform(
            get("/api/v1/rooms/occupancy")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.summary.totalBeds").value(4))
            .andExpect(jsonPath("$.data.summary.occupiedBeds").value(3))
            .andExpect(jsonPath("$.data.summary.freeBeds").value(1))
            .andExpect(jsonPath("$.data.summary.occupancyPercent").value(75.0))
    }

    @Test
    fun `occupancy endpoint is accessible to administrative staff`() {
        mockMvc.perform(
            get("/api/v1/rooms/occupancy")
                .header("Authorization", "Bearer $staffToken"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `occupancy endpoint is forbidden for doctor-only users`() {
        mockMvc.perform(
            get("/api/v1/rooms/occupancy")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `occupancy endpoint requires authentication`() {
        mockMvc.perform(get("/api/v1/rooms/occupancy"))
            .andExpect(status().isUnauthorized)
    }
}
