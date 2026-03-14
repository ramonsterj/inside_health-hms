package com.insidehealthgt.hms.config

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * Dev-only Flyway strategy: repair before migrate.
 *
 * This clears any FAILED entries from flyway_schema_history before running
 * migrations. In dev, seed files (R__*) can fail (e.g. enum typos, test data
 * errors) and leave a FAILED state that blocks subsequent startups. Auto-repair
 * ensures the fixed script is re-run automatically without manual intervention.
 *
 * Never enabled in production.
 */
@Configuration
@Profile("dev")
class DevFlywayConfig {

    @Bean
    fun flywayMigrationStrategy(): FlywayMigrationStrategy = FlywayMigrationStrategy { flyway ->
        flyway.repair()
        flyway.migrate()
    }
}
