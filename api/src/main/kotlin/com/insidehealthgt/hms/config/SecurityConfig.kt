package com.insidehealthgt.hms.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.insidehealthgt.hms.security.JwtAuthenticationFilter
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val CORS_MAX_AGE_SECONDS = 3600L
    }

    /**
     * Returns 401 Unauthorized for unauthenticated requests.
     * This ensures the frontend can detect session expiration and show the modal.
     */
    @Bean
    fun authenticationEntryPoint(): AuthenticationEntryPoint = AuthenticationEntryPoint { _, response, authException ->
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json"
        response.writer.write(
            objectMapper.writeValueAsString(
                mapOf(
                    "success" to false,
                    "message" to (authException.message ?: "Authentication required"),
                    "error" to mapOf(
                        "code" to "UNAUTHORIZED",
                        "details" to "Full authentication is required to access this resource",
                    ),
                ),
            ),
        )
    }

    /**
     * Returns 403 Forbidden for authenticated users who lack permission.
     */
    @Bean
    fun accessDeniedHandler(): AccessDeniedHandler = AccessDeniedHandler { _, response, accessDeniedException ->
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = "application/json"
        response.writer.write(
            objectMapper.writeValueAsString(
                mapOf(
                    "success" to false,
                    "message" to (accessDeniedException.message ?: "Access denied"),
                    "error" to mapOf(
                        "code" to "FORBIDDEN",
                        "details" to "You do not have permission to access this resource",
                    ),
                ),
            ),
        )
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint())
                    .accessDeniedHandler(accessDeniedHandler())
            }
            .headers { headers ->
                headers.contentSecurityPolicy { csp ->
                    csp.policyDirectives(
                        "default-src 'self'; " +
                            "script-src 'self' 'unsafe-eval'; " + // PrimeVue requires unsafe-eval for dynamic rendering
                            "style-src 'self' 'unsafe-inline'; " + // PrimeVue inline styles
                            "img-src 'self' data: blob:; " +
                            "font-src 'self' data:; " +
                            "connect-src 'self'",
                    )
                }
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf(
            "http://localhost:5173", // Vite dev server
            "http://localhost:3000", // Alternative dev port
        )
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = CORS_MAX_AGE_SECONDS

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/api/**", configuration)
        return source
    }

    @Bean
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration): AuthenticationManager =
        authenticationConfiguration.authenticationManager

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
