package com.insidehealthgt.hms.security

import com.insidehealthgt.hms.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomUserDetailsService(private val userRepository: UserRepository) : UserDetailsService {

    @Transactional(readOnly = true)
    override fun loadUserByUsername(identifier: String): UserDetails {
        // Support login by email or username, fetch roles and permissions eagerly
        val user = userRepository.findByIdentifierWithRolesAndPermissions(identifier)
            ?: throw UsernameNotFoundException("User not found with email or username: $identifier")
        return CustomUserDetails(user)
    }

    @Transactional(readOnly = true)
    fun loadUserById(id: Long): UserDetails {
        val user = userRepository.findByIdWithRolesAndPermissions(id)
            ?: throw UsernameNotFoundException("User not found with id: $id")
        return CustomUserDetails(user)
    }
}
