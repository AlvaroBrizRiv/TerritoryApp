package com.territoryapp.routes

import at.favre.lib.crypto.bcrypt.BCrypt
import com.territoryapp.config.Database.dbQuery
import com.territoryapp.config.JwtConfig
import com.territoryapp.models.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.postgresql.util.PSQLException

fun Route.authRoutes() {
    route("/api/auth") {
        post("/register") {
            val req = call.receive<CreateUsuarioRequest>()
            
            val hash = BCrypt.withDefaults().hashToString(12, req.password.toCharArray())
            
            try {
                val newId = dbQuery { conn ->
                    val sql = "INSERT INTO usuarios (email, hash, rol) VALUES (?, ?, 'publicador') RETURNING id"
                    conn.prepareStatement(sql).use { stmt ->
                        stmt.setString(1, req.email)
                        stmt.setString(2, hash)
                        val rs = stmt.executeQuery()
                        if (rs.next()) rs.getInt(1) else -1
                    }
                }
                call.respond(mapOf("id" to newId, "message" to "Usuario creado exitosamente"))
            } catch (e: PSQLException) {
                if (e.sqlState == "23505") { // unique_violation
                    call.respond(HttpStatusCode.Conflict, MessageResponse("El email ya está registrado"))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, MessageResponse("Error al crear usuario"))
                }
            }
        }

        post("/login") {
            val req = call.receive<LoginRequest>()
            
            val userRecord = dbQuery { conn ->
                conn.prepareStatement("SELECT id, email, hash, rol, activo FROM usuarios WHERE email = ?").use { stmt ->
                    stmt.setString(1, req.email)
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        mapOf(
                            "id" to rs.getInt("id"),
                            "email" to rs.getString("email"),
                            "hash" to rs.getString("hash"),
                            "rol" to rs.getString("rol"),
                            "activo" to rs.getBoolean("activo")
                        )
                    } else null
                }
            }

            if (userRecord == null) {
                call.respond(HttpStatusCode.Unauthorized, MessageResponse("Credenciales inválidas"))
                return@post
            }

            if (userRecord["activo"] as Boolean == false) {
                call.respond(HttpStatusCode.Forbidden, MessageResponse("Usuario inactivo"))
                return@post
            }

            val hash = userRecord["hash"] as String
            val result = BCrypt.verifyer().verify(req.password.toCharArray(), hash.toCharArray())
            if (!result.verified) {
                call.respond(HttpStatusCode.Unauthorized, MessageResponse("Credenciales inválidas"))
                return@post
            }

            val userId = userRecord["id"] as Int
            val email = userRecord["email"] as String
            val rol = userRecord["rol"] as String

            val token = JwtConfig.generateAccessToken(userId, email, rol)
            val refreshToken = JwtConfig.generateRefreshToken(userId, email, rol)
            val hashRefresh = JwtConfig.hashRefreshToken(refreshToken)

            dbQuery { conn ->
                conn.prepareStatement("INSERT INTO refresh_tokens (usuario_id, token_hash) VALUES (?, ?)").use { stmt ->
                    stmt.setInt(1, userId)
                    stmt.setString(2, hashRefresh)
                    stmt.executeUpdate()
                }
            }

            call.respond(AuthResponse(token, refreshToken, UserInfo(userId, email, rol)))
        }

        post("/refresh") {
            val req = call.receive<RefreshRequest>()
            try {
                val decoded = JwtConfig.verifyRefreshToken(req.refreshToken)
                val userId = decoded.getClaim("userId").asInt()
                val email = decoded.getClaim("email").asString()
                val rol = decoded.getClaim("rol").asString()
                
                val hashRefresh = JwtConfig.hashRefreshToken(req.refreshToken)

                val tokenExists = dbQuery { conn ->
                    conn.prepareStatement("SELECT 1 FROM refresh_tokens WHERE token_hash = ?").use { stmt ->
                        stmt.setString(1, hashRefresh)
                        val rs = stmt.executeQuery()
                        rs.next()
                    }
                }

                if (!tokenExists) {
                    call.respond(HttpStatusCode.Unauthorized, MessageResponse("Refresh token inválido o expirado"))
                    return@post
                }

                // Rotate token
                dbQuery { conn ->
                    conn.prepareStatement("DELETE FROM refresh_tokens WHERE token_hash = ?").use { stmt ->
                        stmt.setString(1, hashRefresh)
                        stmt.executeUpdate()
                    }
                }

                val newToken = JwtConfig.generateAccessToken(userId, email, rol)
                val newRefreshToken = JwtConfig.generateRefreshToken(userId, email, rol)
                val newHashRefresh = JwtConfig.hashRefreshToken(newRefreshToken)

                dbQuery { conn ->
                    conn.prepareStatement("INSERT INTO refresh_tokens (usuario_id, token_hash) VALUES (?, ?)").use { stmt ->
                        stmt.setInt(1, userId)
                        stmt.setString(2, newHashRefresh)
                        stmt.executeUpdate()
                    }
                }

                call.respond(AuthResponse(newToken, newRefreshToken, UserInfo(userId, email, rol)))

            } catch (e: Exception) {
                call.respond(HttpStatusCode.Unauthorized, MessageResponse("Refresh token inválido"))
            }
        }

        authenticate("auth-jwt") {
            post("/logout") {
                val req = call.receive<LogoutRequest>()
                req.refreshToken?.let { rt ->
                    val hashRefresh = JwtConfig.hashRefreshToken(rt)
                    dbQuery { conn ->
                        conn.prepareStatement("DELETE FROM refresh_tokens WHERE token_hash = ?").use { stmt ->
                            stmt.setString(1, hashRefresh)
                            stmt.executeUpdate()
                        }
                    }
                }
                call.respond(MessageResponse("Sesión cerrada correctamente"))
            }
        }
    }
}
