package com.insidehealthgt.hms

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    companion object {
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test")
    }

    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> = postgres
}
