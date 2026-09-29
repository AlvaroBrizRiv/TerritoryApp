package com.territoryapp.config

import at.favre.lib.crypto.bcrypt.BCrypt
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.sql.Connection

object Database {
    private lateinit var dataSource: HikariDataSource

    fun init(databaseUrl: String) {
        val config = HikariConfig().apply {
            if (databaseUrl.startsWith("postgresql://")) {
                val withoutScheme = databaseUrl.substringAfter("postgresql://")
                val atIndex = withoutScheme.lastIndexOf("@")
                if (atIndex != -1) {
                    val userInfo = withoutScheme.substring(0, atIndex)
                    val hostPortDb = withoutScheme.substring(atIndex + 1)
                    val colonIndex = userInfo.indexOf(":")
                    if (colonIndex != -1) {
                        username = URLDecoder.decode(userInfo.substring(0, colonIndex), "UTF-8")
                        password = URLDecoder.decode(userInfo.substring(colonIndex + 1), "UTF-8")
                    } else {
                        username = URLDecoder.decode(userInfo, "UTF-8")
                    }
                    val jdbc = "jdbc:postgresql://$hostPortDb"
                    jdbcUrl = if (jdbc.contains("?")) "$jdbc&ssl=true&sslmode=require" else "$jdbc?ssl=true&sslmode=require"
                } else {
                    val jdbc = "jdbc:postgresql://$withoutScheme"
                    jdbcUrl = if (jdbc.contains("?")) "$jdbc&ssl=true&sslmode=require" else "$jdbc?ssl=true&sslmode=require"
                }
            } else {
                jdbcUrl = databaseUrl
            }
            
            maximumPoolSize = 10
            connectionTimeout = 10000
            idleTimeout = 30000
            isAutoCommit = true
            addDataSourceProperty("ssl", "true")
            addDataSourceProperty("sslmode", "require")
        }
        dataSource = HikariDataSource(config)
    }

    fun getConnection(): Connection = dataSource.connection

    suspend fun <T> dbQuery(block: (Connection) -> T): T {
        return withContext(Dispatchers.IO) {
            getConnection().use { conn ->
                block(conn)
            }
        }
    }

    suspend fun <T> dbTransaction(block: (Connection) -> T): T {
        return withContext(Dispatchers.IO) {
            getConnection().use { conn ->
                conn.autoCommit = false
                try {
                    val result = block(conn)
                    conn.commit()
                    result
                } catch (e: Exception) {
                    conn.rollback()
                    throw e
                } finally {
                    conn.autoCommit = true
                }
            }
        }
    }

    fun warmup() {
        getConnection().use { conn ->
            conn.prepareStatement("SELECT 1").use { it.executeQuery() }
        }
    }

    fun seedAdmin(email: String, pass: String) {
        getConnection().use { conn ->
            conn.prepareStatement("SELECT id FROM usuarios WHERE email = ?").use { stmt ->
                stmt.setString(1, email)
                val rs = stmt.executeQuery()
                if (!rs.next()) {
                    val hash = BCrypt.withDefaults().hashToString(12, pass.toCharArray())
                    conn.prepareStatement("INSERT INTO usuarios (email, hash, rol, activo) VALUES (?, ?, 'admin', true)").use { insertStmt ->
                        insertStmt.setString(1, email)
                        insertStmt.setString(2, hash)
                        insertStmt.executeUpdate()
                    }
                    println("✅ Admin user seeded: $email")
                }
            }
        }
    }
}
