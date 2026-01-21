package com.insidehealthgt.hms.config

import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.entity.UserStatus
import com.insidehealthgt.hms.repository.RoleRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminInitializer(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(AdminInitializer::class.java)

    @Transactional
    override fun run(args: ApplicationArguments) {
        val adminEmail = "admin@example.com"
        val adminUsername = "admin"
        val adminPassword = "admin123"

        val existingAdmin = userRepository.findByEmail(adminEmail)

        if (existingAdmin == null) {
            // Create admin user if not exists
            val admin = User(
                username = adminUsername,
                email = adminEmail,
                passwordHash = passwordEncoder.encode(adminPassword)!!,
                firstName = "System",
                lastName = "Administrator",
                status = UserStatus.ACTIVE,
                emailVerified = true,
            )

            // Assign ADMIN role
            val adminRole = roleRepository.findByCode("ADMIN")
            if (adminRole != null) {
                admin.roles.add(adminRole)
            }

            userRepository.save(admin)
            logger.info("Default admin user created: username='$adminUsername', email='$adminEmail'")
        } else {
            var needsSave = false

            // Update password hash if admin exists (fixes incorrect hash from migration)
            if (!passwordEncoder.matches(adminPassword, existingAdmin.passwordHash)) {
                existingAdmin.passwordHash = passwordEncoder.encode(adminPassword)!!
                needsSave = true
                logger.info("Admin password hash updated")
            }

            // Ensure admin has ADMIN role (handles edge cases where role assignment failed)
            val adminRole = roleRepository.findByCode("ADMIN")
            if (adminRole != null && !existingAdmin.hasRole("ADMIN")) {
                existingAdmin.roles.add(adminRole)
                needsSave = true
                logger.info("ADMIN role assigned to existing admin user")
            }

            if (needsSave) {
                userRepository.save(existingAdmin)
            }
        }
    }
}
