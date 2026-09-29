package com.territoryapp.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.security.MessageDigest
import java.util.Date

object JwtConfig {
    private lateinit var secret: String
    private lateinit var algorithm: Algorithm
    private const val ISSUER = "territoryapp"
    private const val ACCESS_EXPIRATION_MS = 24L * 60 * 60 * 1000  // 24 hours
    private const val REFRESH_EXPIRATION_MS = 7L * 24 * 60 * 60 * 1000  // 7 days

    fun init(jwtSecret: String) {
        secret = jwtSecret
        algorithm = Algorithm.HMAC256(secret)
    }

    fun getAlgorithm(): Algorithm = algorithm
    fun getIssuer(): String = ISSUER

    fun generateAccessToken(userId: Int, email: String, rol: String): String {
        return JWT.create()
            .withIssuer(ISSUER)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withClaim("rol", rol)
            .withClaim("type", "access")
            .withExpiresAt(Date(System.currentTimeMillis() + ACCESS_EXPIRATION_MS))
            .sign(algorithm)
    }

    fun generateRefreshToken(userId: Int, email: String, rol: String): String {
        return JWT.create()
            .withIssuer(ISSUER)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withClaim("rol", rol)
            .withClaim("type", "refresh")
            .withExpiresAt(Date(System.currentTimeMillis() + REFRESH_EXPIRATION_MS))
            .sign(algorithm)
    }

    fun verifyRefreshToken(token: String): DecodedJWT {
        val verifier = JWT.require(algorithm)
            .withIssuer(ISSUER)
            .withClaim("type", "refresh")
            .build()
        return verifier.verify(token)
    }

    fun hashRefreshToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
