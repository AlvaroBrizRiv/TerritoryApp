package com.territoryapp.routes

import com.fasterxml.jackson.databind.ObjectMapper
import com.territoryapp.config.Database.dbQuery
import com.territoryapp.config.Database.dbTransaction
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
import java.sql.Statement

fun Route.territoriosRoutes() {
    val mapper = ObjectMapper()

    route("/api/territorios") {
        get {
            val ciudad = call.request.queryParameters["ciudad"] ?: ""
            val result = dbQuery { conn ->
                val sql = """
                    SELECT t.id, t.numero, t.ciudad, t.estado, t.fecha_creacion, 
                           ST_AsGeoJSON(t.geom)::json as geom, COUNT(d.id)::int as total_direcciones 
                    FROM territorios t 
                    LEFT JOIN direcciones d ON d.territorio_id = t.id 
                    WHERE t.ciudad ILIKE ? 
                    GROUP BY t.id 
                    ORDER BY t.numero
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, "%$ciudad%")
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<Map<String, Any?>>()
                    while (rs.next()) {
                        val geomStr = rs.getString("geom")
                        list.add(mapOf(
                            "id" to rs.getInt("id"),
                            "numero" to rs.getInt("numero"),
                            "ciudad" to rs.getString("ciudad"),
                            "estado" to rs.getString("estado"),
                            "fecha_creacion" to rs.getString("fecha_creacion"),
                            "geom" to if (geomStr != null) mapper.readTree(geomStr) else null,
                            "total_direcciones" to rs.getInt("total_direcciones")
                        ))
                    }
                    list
                }
            }
            call.respond(result)
        }

        get("/cercanos") {
            val lat = call.request.queryParameters["lat"]?.toDoubleOrNull()
            val lng = call.request.queryParameters["lng"]?.toDoubleOrNull()
            val radius = call.request.queryParameters["radius"]?.toDoubleOrNull() ?: 1000.0
            
            if (lat == null || lng == null) {
                call.respond(HttpStatusCode.BadRequest, MessageResponse("Faltan coordenadas"))
                return@get
            }

            val result = dbQuery { conn ->
                val sql = """
                    SELECT id, numero, ciudad, estado, ST_AsGeoJSON(geom)::json as geom
                    FROM territorios
                    WHERE ST_DWithin(geom::geography, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, ?)
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setDouble(1, lng)
                    stmt.setDouble(2, lat)
                    stmt.setDouble(3, radius)
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<Map<String, Any?>>()
                    while (rs.next()) {
                        val geomStr = rs.getString("geom")
                        list.add(mapOf(
                            "id" to rs.getInt("id"),
                            "numero" to rs.getInt("numero"),
                            "ciudad" to rs.getString("ciudad"),
                            "estado" to rs.getString("estado"),
                            "geom" to if (geomStr != null) mapper.readTree(geomStr) else null
                        ))
                    }
                    list
                }
            }
            call.respond(result)
        }

        get("/{id}/direcciones") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, MessageResponse("ID inválido"))
                return@get
            }
            val result = dbQuery { conn ->
                val sql = """
                    SELECT id, ciudad, direccion, descripcion,
                           ST_Y(geom::geometry) as lat, ST_X(geom::geometry) as lng
                    FROM direcciones WHERE territorio_id = ?
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setInt(1, id)
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<Map<String, Any?>>()
                    while (rs.next()) {
                        list.add(mapOf(
                            "id" to rs.getInt("id"),
                            "ciudad" to rs.getString("ciudad"),
                            "direccion" to rs.getString("direccion"),
                            "descripcion" to rs.getString("descripcion"),
                            "lat" to rs.getDouble("lat"),
                            "lng" to rs.getDouble("lng")
                        ))
                    }
                    list
                }
            }
            call.respond(result)
        }

        authenticate("auth-jwt") {
            post("/calcular-cluster") {
                val principal = call.principal<JWTPrincipal>()
                val rol = principal?.payload?.getClaim("rol")?.asString()
                if (rol != "admin") {
                    call.respond(HttpStatusCode.Forbidden, MessageResponse("Requiere rol admin"))
                    return@post
                }

                val req = call.receive<ClusterRequest>()
                
                try {
                    val result = dbTransaction { conn ->
                        // Delete unassigned or empty territories
                        conn.prepareStatement("DELETE FROM territorios WHERE ciudad = ? AND estado = 'Disponible' AND id NOT IN (SELECT DISTINCT territorio_id FROM direcciones WHERE territorio_id IS NOT NULL)").use { stmt ->
                            stmt.setString(1, req.ciudad)
                            stmt.executeUpdate()
                        }
                        conn.prepareStatement("UPDATE direcciones SET territorio_id = NULL WHERE ciudad = ? AND territorio_id IS NOT NULL AND territorio_id IN (SELECT id FROM territorios WHERE estado = 'Disponible')").use { stmt ->
                            stmt.setString(1, req.ciudad)
                            stmt.executeUpdate()
                        }

                        val maxNumero = conn.prepareStatement("SELECT COALESCE(MAX(numero), 0) as m FROM territorios WHERE ciudad = ?").use { stmt ->
                            stmt.setString(1, req.ciudad)
                            val rs = stmt.executeQuery()
                            if (rs.next()) rs.getInt("m") else 0
                        }
                        
                        var numeroActual = maxNumero + 1

                        val kQuery = conn.prepareStatement("SELECT CEIL(COUNT(*)::float / ?) as k FROM direcciones WHERE ciudad = ? AND territorio_id IS NULL").use { stmt ->
                            stmt.setInt(1, req.maxDireccionesPorTerritorio)
                            stmt.setString(2, req.ciudad)
                            val rs = stmt.executeQuery()
                            if (rs.next()) rs.getInt("k") else 0
                        }

                        if (kQuery <= 0) {
                            return@dbTransaction "No hay direcciones sin asignar para clusterizar"
                        }

                        val clusterSql = """
                            WITH clustered AS (
                                SELECT id, ST_ClusterKMeans(geom, ?) OVER() as cid
                                FROM direcciones
                                WHERE ciudad = ? AND territorio_id IS NULL
                            ),
                            polygons AS (
                                SELECT cid, ST_Buffer(ST_ConvexHull(ST_Collect(d.geom)), 0.0005) as poly, array_agg(d.id) as dir_ids
                                FROM clustered c
                                JOIN direcciones d ON c.id = d.id
                                GROUP BY cid
                            )
                            SELECT cid, ST_AsText(poly) as p_text, dir_ids FROM polygons
                        """.trimIndent()

                        conn.prepareStatement(clusterSql).use { stmt ->
                            stmt.setInt(1, kQuery)
                            stmt.setString(2, req.ciudad)
                            val rs = stmt.executeQuery()
                            while (rs.next()) {
                                val pText = rs.getString("p_text")
                                val dirIds = rs.getArray("dir_ids").array as Array<*>

                                val insertSql = "INSERT INTO territorios (numero, ciudad, estado, geom) VALUES (?, ?, 'Disponible', ST_SetSRID(ST_GeomFromText(?), 4326)) RETURNING id"
                                val newId = conn.prepareStatement(insertSql).use { insertStmt ->
                                    insertStmt.setInt(1, numeroActual++)
                                    insertStmt.setString(2, req.ciudad)
                                    insertStmt.setString(3, pText)
                                    val rsInsert = insertStmt.executeQuery()
                                    if (rsInsert.next()) rsInsert.getInt(1) else -1
                                }

                                if (newId != -1) {
                                    val updateDirs = "UPDATE direcciones SET territorio_id = ? WHERE id = ANY(?)"
                                    conn.prepareStatement(updateDirs).use { updateStmt ->
                                        updateStmt.setInt(1, newId)
                                        updateStmt.setArray(2, conn.createArrayOf("integer", dirIds))
                                        updateStmt.executeUpdate()
                                    }
                                }
                            }
                        }
                        "Clusterización completada"
                    }
                    call.respond(MessageResponse(result))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, MessageResponse("Error en clusterización: ${e.message}"))
                }
            }

            patch("/{id}/completar") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("ID inválido"))
                    return@patch
                }
                val updated = dbQuery { conn ->
                    conn.prepareStatement("UPDATE territorios SET estado = 'Completado' WHERE id = ?").use { stmt ->
                        stmt.setInt(1, id)
                        stmt.executeUpdate()
                    }
                }
                if (updated > 0) call.respond(MessageResponse("Territorio marcado como completado"))
                else call.respond(HttpStatusCode.NotFound, MessageResponse("Territorio no encontrado"))
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
                val req = call.receive<EstadoRequest>()
                val updated = dbQuery { conn ->
                    conn.prepareStatement("UPDATE territorios SET estado = ? WHERE id = ?").use { stmt ->
                        stmt.setString(1, req.estado)
                        stmt.setInt(2, id)
                        stmt.executeUpdate()
                    }
                }
                if (updated > 0) call.respond(MessageResponse("Estado actualizado"))
                else call.respond(HttpStatusCode.NotFound, MessageResponse("Territorio no encontrado"))
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
                val deleted = dbTransaction { conn ->
                    conn.prepareStatement("UPDATE direcciones SET territorio_id = NULL WHERE territorio_id = ?").use { stmt ->
                        stmt.setInt(1, id)
                        stmt.executeUpdate()
                    }
                    conn.prepareStatement("DELETE FROM territorios WHERE id = ?").use { stmt ->
                        stmt.setInt(1, id)
                        stmt.executeUpdate()
                    }
                }
                if (deleted > 0) call.respond(MessageResponse("Territorio eliminado"))
                else call.respond(HttpStatusCode.NotFound, MessageResponse("Territorio no encontrado"))
            }
        }
    }
}
