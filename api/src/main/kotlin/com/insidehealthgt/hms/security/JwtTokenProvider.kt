package com.insidehealthgt.hms.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}")
    private val jwtSecret: String,

    @Value("\${jwt.access-token-expiration}")
    private val accessTokenExpiration: Long,

    @Value("\${jwt.refresh-token-expiration}")
    private val refreshTokenExpiration: Long,
) {
    private val logger = LoggerFactory.getLogger(JwtTokenProvider::class.java)

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    }

    fun generateAccessToken(userId: Long, email: String): String {
        val now = Date()
        val expiryDate = Date(now.time + accessTokenExpiration)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("type", "access")
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }

    fun generateRefreshToken(userId: Long): String {
        val now = Date()
        val expiryDate = Date(now.time + refreshTokenExpiration)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", "refresh")
            .claim("jti", UUID.randomUUID().toString())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }

    fun getUserIdFromToken(token: String): Long {
        val claims = getClaims(token)
        return claims.subject.toLong()
    }

    fun getEmailFromToken(token: String): String? {
        val claims = getClaims(token)
        return claims["email"] as? String
    }

    fun validateToken(token: String): Boolean = try {
        val claims = getClaims(token)
        !claims.expiration.before(Date())
    } catch (ex: ExpiredJwtException) {
        logger.debug("JWT token is expired: {}", ex.message)
        false
    } catch (ex: JwtException) {
        logger.debug("Invalid JWT token: {}", ex.message)
        false
    } catch (ex: IllegalArgumentException) {
        logger.debug("JWT claims string is empty: {}", ex.message)
        false
    }

    fun isAccessToken(token: String): Boolean = try {
        val claims = getClaims(token)
        claims["type"] == "access"
    } catch (ex: ExpiredJwtException) {
        logger.debug("JWT token is expired when checking type: {}", ex.message)
        false
    } catch (ex: JwtException) {
        logger.debug("Invalid JWT token when checking type: {}", ex.message)
        false
    }

    fun getAccessTokenExpiration(): Long = accessTokenExpiration

    fun getRefreshTokenExpiration(): Long = refreshTokenExpiration

    private fun getClaims(token: String): Claims = Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .payload
}
