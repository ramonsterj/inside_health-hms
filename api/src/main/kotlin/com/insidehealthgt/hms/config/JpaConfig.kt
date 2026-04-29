package com.insidehealthgt.hms.config

import com.insidehealthgt.hms.security.CurrentUserProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import java.util.Optional

@Configuration
@EnableJpaAuditing
class JpaConfig {

    @Bean
    fun auditorAware(currentUserProvider: CurrentUserProvider): AuditorAware<Long> = AuditorAware {
        Optional.ofNullable(currentUserProvider.currentUserId())
    }
}
