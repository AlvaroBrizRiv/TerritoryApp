# 🗺️ TerritoryApp

## 📖 Resumen

**TerritoryApp** es una solución integral orientada a optimizar la asignación, visualización y gestión de territorios. A través de una robusta aplicación móvil conectada a un servidor central, permite a los administradores coordinar equipos y gestionar accesos, mientras que los usuarios en terreno pueden interactuar con mapas integrados y listas de verificación (checklists) para llevar un control exacto y en tiempo real de las direcciones asignadas.

## 🛠️ Stack Tecnológico

El proyecto está construido con un enfoque moderno y unificado, utilizando Kotlin tanto en el lado del cliente como en el servidor.

* **📱 Frontend (Móvil):** Desarrollado 100% en Kotlin utilizando **Android Studio** como entorno de desarrollo oficial. Implementa vistas nativas e integra componentes web (`WebView`) para la renderización fluida de mapas.
* **⚙️ Backend:** Construido en Kotlin (arquitectura basada en rutas y servicios como Ktor o similar), gestionando de manera eficiente la lógica de negocio y las conexiones.
* **🔐 Seguridad:** Json Web Tokens (JWT) para la protección de endpoints y `SecurePrefs` para el almacenamiento encriptado de credenciales en el dispositivo.
* **🐳 Infraestructura:** Contenedores con Docker (`docker-compose.yml`) para un despliegue del servidor y la base de datos rápido, predecible y escalable.

## 🏗️ Arquitectura del Sistema

La arquitectura sigue un modelo Cliente-Servidor comunicado mediante una API REST protegida, aislando las responsabilidades en capas claras:

```text
 📱 DISPOSITIVO MÓVIL (Android)                ⚙️ SERVIDOR BACKEND (Kotlin)                  🗄️ BASE DE DATOS
+-------------------------------+             +-------------------------------+             +------------------+
|          TerritoryApp         |             |          API REST             |             |                  |
|                               |             |                               |             |                  |
|  [ Vistas y UI ]              |             |  [ Enrutamiento ]             |             |                  |
|   - AdminActivity             |  Peticiones |   - AuthRoutes                |             |  - Usuarios      |
|   - MapaChecklistActivity     | ===========>|   - TerritoriosRoutes         | Consultas   |  - Territorios   |
|                               |  (HTTP/JSON)|   - DireccionesRoutes         | ===========>|  - Direcciones   |
|  [ Lógica y Estado ]          | <===========|                               |   (SQL)     |                  |
|   - ViewModels (MVVM)         | Respuestas  |  [ Core Lógico ]              | <===========|                  |
|   - AuthInterceptor           |             |   - Database.kt               |             |                  |
|                               |             |   - JwtConfig.kt              |             |                  |
|  [ Almacenamiento Local ]     |             |                               |             |                  |
|   - SecurePrefs               |             +---------------^---------------+             +------------------+
+-------------------------------+                             |
                                                              |
                                                      🐳 Docker Compose
                                                     (Entorno Contenerizado)

```

## ✨ Características Principales

* 🗺️ **Exploración Geoespacial Integrada:** Visores de mapas embebidos de alto rendimiento (`globalmap.html`, `mapview.html`) que permiten a los usuarios ubicarse y visualizar las fronteras de sus territorios asignados.
* ✅ **Sistema de Checklist Dinámico:** Los usuarios pueden marcar, actualizar y hacer seguimiento del estado de cada dirección dentro de su territorio de manera intuitiva y rápida.
* 👥 **Panel de Administración Dedicado:** Flujos de UI exclusivos para usuarios con rol de administrador, facilitando la creación de cuentas, asignación de zonas y control global del sistema (`AdminViewModel`, `UsuarioAdapter`).
* 🛡️ **Autenticación y Seguridad End-to-End:** Sistema de Login y Registro seguro respaldado por tokens JWT. La aplicación móvil intercepta y firma automáticamente cada petición mediante `AuthInterceptor`, garantizando que la información sensible viaje protegida.
* 🚀 **Despliegue Ágil:** Gracias al uso de `docker-compose.yml`, montar el ecosistema del backend en cualquier entorno de pruebas o producción se realiza en cuestión de minutos.

---

👨‍💻 Autor
Proyecto creado y desarrollado por Álvaro Brizuela, Desarrollador de Software.
