# TerritoryApp

Aplicación móvil de gestión y distribución de territorios geográficos.

## Stack
- **Backend**: Node.js + Fastify (TypeScript)
- **Base de Datos**: PostgreSQL + PostGIS
- **Frontend**: .NET MAUI (Android)
- **Mapas**: MapLibre GL con OpenStreetMap

## Inicio Rápido

### 1. Requisitos
- Docker Desktop
- Node.js 20+
- .NET 9 SDK + MAUI workload

### 2. Levantar Base de Datos
```bash
docker-compose up postgis -d
```

### 3. Configurar Backend
```bash
cd backend
npm install
npm run migrate   # Crea las tablas
npm run seed      # Crea el usuario admin inicial
npm run dev       # Inicia el servidor en http://localhost:3000
```

### 4. Credenciales Admin por defecto
- Email: `admin@territoryapp.com`
- Password: `Admin123!`
- ⚠️ Cambiar en producción via variables de entorno

### 5. Deploy en Railway
1. Crear proyecto en [railway.app](https://railway.app)
2. Agregar servicio PostgreSQL (incluye PostGIS)
3. Conectar el repositorio del backend
4. Configurar variables de entorno (ver `.env.example`)
5. Actualizar `AppConstants.cs` en MAUI con la URL de Railway

### 6. Generar APK para Android
```bash
cd frontend/TerritoryApp
dotnet build -f net9.0-android -c Release
```
El APK firmado se generará en `bin/Release/net9.0-android/`.

## Privacidad
No se almacenan nombres de personas. Solo: dirección, descripción del lugar y fecha de registro.
