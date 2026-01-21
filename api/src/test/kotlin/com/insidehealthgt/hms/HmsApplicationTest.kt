package com.insidehealthgt.hms

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class HmsApplicationTest {

    @Test
    fun `context loads successfully`() {
        // Verifies that the Spring context loads without errors
    }
}
