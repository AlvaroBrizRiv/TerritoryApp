package com.territoryapp.routes

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

fun Route.direccionesRoutes() {
    route("/api/direcciones") {
        authenticate("auth-jwt") {
            get {
                val ciudad = call.request.queryParameters["ciudad"]
                val sinAsignar = call.request.queryParameters["sinAsignar"]?.toBoolean() ?: false
                val territorioId = call.request.queryParameters["territorioId"]?.toIntOrNull()

                val result = dbQuery { conn ->
                    val conditions = mutableListOf<String>()
                    val params = mutableListOf<Any>()

                    if (ciudad != null) {
                        conditions.add("ciudad ILIKE ?")
                        params.add("%$ciudad%")
                    }
                    if (sinAsignar) {
                        conditions.add("territorio_id IS NULL")
                    }
                    if (territorioId != null) {
                        conditions.add("territorio_id = ?")
                        params.add(territorioId)
                    }

                    val whereClause = if (conditions.isEmpty()) "" else "WHERE ${conditions.joinToString(" AND ")}"
                    val sql = """
                        SELECT id, ciudad, direccion, descripcion, territorio_id, 
                               ST_Y(geom::geometry) as lat, ST_X(geom::geometry) as lng 
                        FROM direcciones $whereClause
                    """.trimIndent()

                    conn.prepareStatement(sql).use { stmt ->
                        for ((i, param) in params.withIndex()) {
                            when (param) {
                                is String -> stmt.setString(i + 1, param)
                                is Int -> stmt.setInt(i + 1, param)
                            }
                        }
                        val rs = stmt.executeQuery()
                        val list = mutableListOf<Map<String, Any?>>()
                        while (rs.next()) {
                            list.add(mapOf(
                                "id" to rs.getInt("id"),
                                "ciudad" to rs.getString("ciudad"),
                                "direccion" to rs.getString("direccion"),
                                "descripcion" to rs.getString("descripcion"),
                                "territorio_id" to rs.getInt("territorio_id").takeIf { !rs.wasNull() },
                                "lat" to rs.getDouble("lat"),
                                "lng" to rs.getDouble("lng")
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
                val req = call.receive<CreateDireccionRequest>()
                
                val newId = dbQuery { conn ->
                    val sql = "INSERT INTO direcciones (ciudad, direccion, descripcion, geom) VALUES (?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326)) RETURNING id"
                    conn.prepareStatement(sql).use { stmt ->
                        stmt.setString(1, req.ciudad)
                        stmt.setString(2, req.direccion)
                        stmt.setString(3, req.descripcion)
                        stmt.setDouble(4, req.lng)
                        stmt.setDouble(5, req.lat)
                        val rs = stmt.executeQuery()
                        if (rs.next()) rs.getInt(1) else -1
                    }
                }
                call.respond(mapOf("id" to newId, "message" to "Dirección creada"))
            }

            patch("/{id}") {
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
                val req = call.receive<CreateDireccionRequest>()
                val updated = dbQuery { conn ->
                    val sql = "UPDATE direcciones SET ciudad = ?, direccion = ?, descripcion = ?, geom = ST_SetSRID(ST_MakePoint(?, ?), 4326) WHERE id = ?"
                    conn.prepareStatement(sql).use { stmt ->
                        stmt.setString(1, req.ciudad)
                        stmt.setString(2, req.direccion)
                        stmt.setString(3, req.descripcion)
                        stmt.setDouble(4, req.lng)
                        stmt.setDouble(5, req.lat)
                        stmt.setInt(6, id)
                        stmt.executeUpdate()
                    }
                }
                if (updated > 0) call.respond(MessageResponse("Dirección actualizada"))
                else call.respond(HttpStatusCode.NotFound, MessageResponse("Dirección no encontrada"))
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
                val deleted = dbQuery { conn ->
                    conn.prepareStatement("DELETE FROM direcciones WHERE id = ?").use { stmt ->
                        stmt.setInt(1, id)
                        stmt.executeUpdate()
                    }
                }
                if (deleted > 0) call.respond(MessageResponse("Dirección eliminada"))
                else call.respond(HttpStatusCode.NotFound, MessageResponse("Dirección no encontrada"))
            }
        }
    }
}
