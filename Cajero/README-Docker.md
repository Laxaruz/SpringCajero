# 🐳 Docker Setup para Aplicación Cajero

Este directorio contiene los archivos necesarios para contenerizar la aplicación Cajero usando Docker.

## 📋 Prerequisitos

- Docker Desktop instalado y ejecutándose
- MySQL ejecutándose en el host local (puerto 3306) o en otro servidor accesible
- PowerShell (para usar los scripts de automatización)

## 🚀 Inicio Rápido

### Opción 1: Usando el Script de PowerShell (Recomendado)

```powershell
# Construir la imagen
.\docker-manager.ps1 -Action build

# Ejecutar el contenedor
.\docker-manager.ps1 -Action run

# Ver logs
.\docker-manager.ps1 -Action logs

# Detener el contenedor
.\docker-manager.ps1 -Action stop
```

### Opción 2: Usando Docker Compose

```powershell
# Construir y ejecutar
docker-compose up --build -d

# Ver logs
docker-compose logs -f cajero-app

# Detener
docker-compose down
```

### Opción 3: Comandos Docker Manuales

```powershell
# Construir la imagen
docker build -t cajero-app:latest .

# Ejecutar el contenedor
docker run -d --name cajero-container -p 8080:8080 `
  -e MYSQL_URL="jdbc:mysql://host.docker.internal:3306/atmdatabase" `
  -e MYSQL_USERNAME="root" `
  -e MYSQL_PASSWORD="" `
  -v "${PWD}\logs:/app/logs" `
  cajero-app:latest
```

## ⚙️ Configuración

### Variables de Entorno

| Variable | Descripción | Valor por Defecto |
|----------|-------------|-------------------|
| `MYSQL_URL` | URL de conexión a MySQL | `jdbc:mysql://host.docker.internal:3306/atmdatabase` |
| `MYSQL_USERNAME` | Usuario de MySQL | `root` |
| `MYSQL_PASSWORD` | Contraseña de MySQL | *(vacío)* |
| `SPRING_PROFILES_ACTIVE` | Perfil de Spring Boot | `docker` |
| `JAVA_OPTS` | Opciones de JVM | `-Xmx512m -Xms256m` |

### Puertos

- **8080**: Puerto de la aplicación web
- **3306**: Puerto de MySQL (si usas el contenedor de MySQL)

## 📁 Estructura de Archivos Docker

```
├── Dockerfile                 # Definición de la imagen Docker
├── .dockerignore              # Archivos a ignorar en la construcción
├── docker-compose.yml         # Configuración de Docker Compose
├── docker-manager.ps1         # Script de gestión de Docker
└── src/main/resources/
    └── application-docker.properties  # Configuración específica para Docker
```

## 🔧 Comandos Útiles del Script

```powershell
# Ayuda
.\docker-manager.ps1 -Action help

# Construir con nombre personalizado
.\docker-manager.ps1 -Action build -ImageName mi-cajero -Tag v1.0

# Ejecutar en puerto personalizado
.\docker-manager.ps1 -Action run -Port 9090

# Ejecutar con MySQL personalizado
.\docker-manager.ps1 -Action run -MySqlUrl "jdbc:mysql://mi-server:3306/mi-db" -MySqlUser "usuario" -MySqlPassword "password"

# Abrir shell en el contenedor
.\docker-manager.ps1 -Action shell

# Limpiar Docker
.\docker-manager.ps1 -Action clean
```

## 🩺 Health Check

La aplicación incluye un endpoint de health check en:
- http://localhost:8080/actuator/health

## 📊 Logs

Los logs se almacenan en:
- **Contenedor**: `/app/logs/`
- **Host**: `./logs/` (montado como volumen)

## 🔍 Troubleshooting

### La aplicación no puede conectarse a MySQL

1. Verifica que MySQL esté ejecutándose en el host
2. Asegúrate de que la URL de MySQL sea correcta
3. Si usas Windows, `host.docker.internal` debería funcionar
4. Si usas Linux, usa `172.17.0.1` o la IP del host

### El contenedor no inicia

```powershell
# Ver logs detallados
.\docker-manager.ps1 -Action logs

# O usar Docker directamente
docker logs cajero-container
```

### Problemas de permisos

```powershell
# En Windows, ejecutar PowerShell como administrador
# En Linux/Mac, usar sudo si es necesario
```

## 🎯 Próximos Pasos

1. **Configurar MySQL**: Si necesitas MySQL también en Docker, descomenta las líneas en `docker-compose.yml`
2. **CI/CD**: Integrar con GitHub Actions o Azure DevOps
3. **Monitoreo**: Agregar Prometheus y Grafana
4. **Seguridad**: Configurar HTTPS y certificados

## 📞 Soporte

Para problemas o preguntas:
1. Revisar los logs de la aplicación
2. Verificar la configuración de MySQL
3. Consultar la documentación de Spring Boot Docker
