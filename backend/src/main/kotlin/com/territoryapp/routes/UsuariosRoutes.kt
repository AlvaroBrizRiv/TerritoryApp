package com.territoryapp.routes

import at.favre.lib.crypto.bcrypt.BCrypt
import com.territoryapp.config.Database.dbQuery
import com.territoryapp.models.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.delete
import io.ktor.server.routing.route
import org.postgresql.util.PSQLException

fun Route.usuariosRoutes() {
    route("/api/usuarios") {
        authenticate("auth-jwt") {
            get {
                val principal = call.principal<JWTPrincipal>()
                val rol = principal?.payload?.getClaim("rol")?.asString()
                if (rol != "admin") {
                    call.respond(HttpStatusCode.Forbidden, MessageResponse("Requiere rol admin"))
                    return@get
                }

                val result = dbQuery { conn ->
                    conn.prepareStatement("SELECT id, email, rol, activo, fecha_creacion FROM usuarios ORDER BY id").use { stmt ->
                        val rs = stmt.executeQuery()
                        val list = mutableListOf<Map<String, Any?>>()
                        while (rs.next()) {
                            list.add(mapOf(
                                "id" to rs.getInt("id"),
                                "email" to rs.getString("email"),
                                "rol" to rs.getString("rol"),
                                "activo" to rs.getBoolean("activo"),
                                "fecha_creacion" to rs.getString("fecha_creacion")
                            ))
                        }
                        list
                    }
                }
                call.respond(result)
            }

            post {
                val principal = call.principal<JWTPrincipal>()
                val rol = principal?.payload?.getClaim("rol")?.asString()
                if (rol != "admin") {
                    call.respond(HttpStatusCode.Forbidden, MessageResponse("Requiere rol admin"))
                    return@post
                }
                val req = call.receive<CreateUsuarioRequest>()
                
                val hash = BCrypt.withDefaults().hashToString(12, req.password.toCharArray())
                
                try {
                    val newId = dbQuery { conn ->
                        val sql = "INSERT INTO usuarios (email, hash, rol) VALUES (?, ?, ?) RETURNING id"
                        conn.prepareStatement(sql).use { stmt ->
                            stmt.setString(1, req.email)
                            stmt.setString(2, hash)
                            stmt.setString(3, req.rol)
                            val rs = stmt.executeQuery()
                            if (rs.next()) rs.getInt(1) else -1
                        }
                    }
                    call.respond(mapOf("id" to newId, "message" to "Usuario creado"))
                } catch (e: PSQLException) {
                    if (e.sqlState == "23505") { // unique_violation
                        call.respond(HttpStatusCode.Conflict, MessageResponse("El email ya está registrado"))
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, MessageResponse("Error al crear usuario"))
                    }
                }
            }

            patch("/{id}/estado") {
                val principal = call.principal<JWTPrincipal>()
                val rol = principal?.payload?.getClaim("rol")?.asString()
                if (rol != "admin") {
                    call.respond(HttpStatusCode.Forbidden, MessageResponse("Requiere rol admin"))
                    return@patch
                }
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("ID inválido"))
                    return@patch
                }
                
                val currentUserId = principal?.payload?.getClaim("userId")?.asInt()
                if (id == currentUserId) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("No puedes cambiar tu propio estado"))
                    return@patch
                }

                val req = call.receive<ActivoRequest>()
                val updated = dbQuery { conn ->
                    conn.prepareStatement("UPDATE usuarios SET activo = ? WHERE id = ?").use { stmt ->
                        stmt.setBoolean(1, req.activo)
                        stmt.setInt(2, id)
                        stmt.executeUpdate()
                    }
                }
                if (updated > 0) call.respond(MessageResponse("Estado de usuario actualizado"))
                else call.respond(HttpStatusCode.NotFound, MessageResponse("Usuario no encontrado"))
            }

            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val rol = principal?.payload?.getClaim("rol")?.asString()
                if (rol != "admin") {
                    call.respond(HttpStatusCode.Forbidden, MessageResponse("Requiere rol admin"))
                    return@delete
                }
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("ID inválido"))
                    return@delete
                }
                
                val currentUserId = principal?.payload?.getClaim("userId")?.asInt()
                if (id == currentUserId) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("No puedes eliminarte a ti mismo"))
                    return@delete
                }

                val deleted = dbQuery { conn ->
                    conn.prepareStatement("DELETE FROM usuarios WHERE id = ?").use { stmt ->
                        stmt.setInt(1, id)
                        stmt.executeUpdate()
                    }
                }
                if (deleted > 0) call.respond(MessageResponse("Usuario eliminado"))
                else call.respond(HttpStatusCode.NotFound, MessageResponse("Usuario no encontrado"))
            }
        }
    }
}
