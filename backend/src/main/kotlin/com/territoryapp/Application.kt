package com.territoryapp

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.territoryapp.config.Database
import com.territoryapp.config.JwtConfig
import com.territoryapp.routes.authRoutes
import com.territoryapp.routes.direccionesRoutes
import com.territoryapp.routes.territoriosRoutes
import com.territoryapp.routes.usuariosRoutes
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    // Load environment variables
    val env = try {
        dotenv {
            directory = "../"
            ignoreIfMissing = true
        }
    } catch (e: Exception) {
        dotenv {
            directory = "./"
            ignoreIfMissing = true
        }
    }

    val databaseUrl = env["DATABASE_URL"]
        ?: System.getenv("DATABASE_URL")
        ?: throw IllegalStateException("DATABASE_URL not configured")

    val jwtSecret = env["JWT_SECRET"]
        ?: System.getenv("JWT_SECRET")
        ?: "fallback_dev_secret"

    val adminEmail = env["ADMIN_EMAIL"] ?: System.getenv("ADMIN_EMAIL")
    val adminPassword = env["ADMIN_PASSWORD"] ?: System.getenv("ADMIN_PASSWORD")

    // Initialize configurations
    Database.init(databaseUrl)
    JwtConfig.init(jwtSecret)

    // Warmup database and seed admin
    try {
        Database.warmup()
        println("✅ Database connection ready!")
        
        if (adminEmail != null && adminPassword != null) {
            Database.seedAdmin(adminEmail, adminPassword)
        }
    } catch (e: Exception) {
        println("⚠️ Database warmup/seed failed: ${e.message}")
    }

    // Install CORS
    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("Bypass-Tunnel-Reminder")
        allowCredentials = true
    }

    // Install Content Negotiation (Jackson)
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }
    }

    // Install Status Pages (error handling)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            println("❌ Unhandled error: ${cause.message}")
            cause.printStackTrace()
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "Internal Server Error", "message" to (cause.message ?: "Unknown error"))
            )
        }
    }

    // Install JWT Authentication
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(
                com.auth0.jwt.JWT.require(JwtConfig.getAlgorithm())
                    .withIssuer(JwtConfig.getIssuer())
                    .withClaim("type", "access")
                    .build()
            )
            validate { credential ->
                val userId = credential.payload.getClaim("userId")?.asInt()
                val email = credential.payload.getClaim("email")?.asString()
                val rol = credential.payload.getClaim("rol")?.asString()
                if (userId != null && email != null && rol != null) {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Unauthorized", "message" to "Token inválido o expirado")
                )
            }
        }

        jwt("admin-jwt") {
            verifier(
                com.auth0.jwt.JWT.require(JwtConfig.getAlgorithm())
                    .withIssuer(JwtConfig.getIssuer())
                    .withClaim("type", "access")
                    .build()
            )
            validate { credential ->
                val rol = credential.payload.getClaim("rol")?.asString()
                if (rol == "admin") {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf("error" to "Forbidden", "message" to "Se requiere rol de administrador")
                )
            }
        }
    }

    // Register routes
    routing {
        authRoutes()
        territoriosRoutes()
        direccionesRoutes()
        usuariosRoutes()

        get("/health") {
            call.respond(
                mapOf(
                    "status" to "ok",
                    "timestamp" to java.time.Instant.now().toString(),
                    "service" to "TerritoryApp API v2.0.0 (Ktor/Kotlin)"
                )
            )
        }
    }

    println("🚀 TerritoryApp API running (Ktor/Kotlin)")
}
