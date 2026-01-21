package com.insidehealthgt.hms.config

import com.insidehealthgt.hms.security.CustomUserDetails
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional

@Configuration
@EnableJpaAuditing
class JpaConfig {

    @Bean
    fun auditorAware(): AuditorAware<Long> = AuditorAware {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication?.principal
        val isValidAuth = authentication != null &&
            authentication.isAuthenticated &&
            principal != "anonymousUser"
        if (isValidAuth && principal is CustomUserDetails) {
            Optional.of(principal.id)
        } else {
            Optional.empty()
        }
    }
}
