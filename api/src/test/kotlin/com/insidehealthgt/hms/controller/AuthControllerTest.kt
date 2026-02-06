package com.insidehealthgt.hms.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.insidehealthgt.hms.TestcontainersConfiguration
import com.insidehealthgt.hms.dto.request.LoginRequest
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.repository.AdmissionConsentDocumentRepository
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.EmergencyContactRepository
import com.insidehealthgt.hms.repository.NursingNoteRepository
import com.insidehealthgt.hms.repository.PatientRepository
import com.insidehealthgt.hms.repository.RoleRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.repository.VitalSignRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var admissionConsentDocumentRepository: AdmissionConsentDocumentRepository

    @Autowired
    private lateinit var admissionRepository: AdmissionRepository

    @Autowired
    private lateinit var emergencyContactRepository: EmergencyContactRepository

    @Autowired
    private lateinit var patientRepository: PatientRepository

    @Autowired
    private lateinit var nursingNoteRepository: NursingNoteRepository

    @Autowired
    private lateinit var vitalSignRepository: VitalSignRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    fun setUp() {
        nursingNoteRepository.deleteAllHard()
        vitalSignRepository.deleteAllHard()
        admissionConsentDocumentRepository.deleteAllHard()
        admissionRepository.deleteAllHard()
        emergencyContactRepository.deleteAllHard()
        patientRepository.deleteAllHard()
        userRepository.deleteAll()
    }

    private fun createUser(
        username: String,
        email: String,
        password: String,
        firstName: String? = null,
        lastName: String? = null,
    ): User {
        val userRole = roleRepository.findByCode("USER")!!
        val user = User(
            username = username,
            email = email,
            passwordHash = passwordEncoder.encode(password)!!,
            firstName = firstName,
            lastName = lastName,
            mustChangePassword = false,
        )
        user.roles.add(userRole)
        return userRepository.save(user)
    }

    @Test
    fun `login should return tokens for valid credentials`() {
        createUser(
            username = "loginuser",
            email = "login@example.com",
            password = "password123",
        )

        val loginRequest = LoginRequest(
            identifier = "login@example.com",
            password = "password123",
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.refreshToken").exists())
    }

    @Test
    fun `login should work with username as identifier`() {
        createUser(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
        )

        val loginRequest = LoginRequest(
            identifier = "testuser",
            password = "password123",
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").exists())
    }

    @Test
    fun `login should fail with invalid credentials`() {
        val loginRequest = LoginRequest(
            identifier = "nonexistent@example.com",
            password = "wrongpassword",
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `login should fail with wrong password`() {
        createUser(
            username = "wrongpass",
            email = "wrongpass@example.com",
            password = "correctpassword",
        )

        val loginRequest = LoginRequest(
            identifier = "wrongpass@example.com",
            password = "wrongpassword",
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
    }
}
