# Script de configuración e instalación del sistema de logging
# Ejecutar como administrador si es necesario

Write-Host "==============================================================" -ForegroundColor Green
Write-Host "  CONFIGURACIÓN DEL SISTEMA DE LOGGING - CAJERO/ATM BANKING" -ForegroundColor Green
Write-Host "==============================================================" -ForegroundColor Green
Write-Host ""

# Función para verificar si se está ejecutando como administrador
function Test-Administrator {
    $currentUser = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($currentUser)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

# Configurar colores y funciones helper
function Write-Success { param($Message) Write-Host "✓ $Message" -ForegroundColor Green }
function Write-Warning { param($Message) Write-Host "⚠ $Message" -ForegroundColor Yellow }
function Write-Error { param($Message) Write-Host "✗ $Message" -ForegroundColor Red }
function Write-Info { param($Message) Write-Host "ℹ $Message" -ForegroundColor Cyan }

# 1. Verificar estructura de directorios
Write-Info "Verificando estructura de directorios..."

$directories = @(
    ".\logs",
    ".\logs\archived",
    ".\src\main\java\com\Laxaruz\Cajero\util",
    ".\src\main\resources"
)

foreach ($dir in $directories) {
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        Write-Success "Directorio creado: $dir"
    } else {
        Write-Info "Directorio existe: $dir"
    }
}

# 2. Verificar archivos de configuración
Write-Info "Verificando archivos de configuración..."

$configFiles = @(
    ".\src\main\resources\application.properties",
    ".\src\main\resources\logback-spring.xml",
    ".\src\main\java\com\Laxaruz\Cajero\util\AuditLogger.java"
)

foreach ($file in $configFiles) {
    if (Test-Path $file) {
        Write-Success "Archivo de configuración encontrado: $file"
    } else {
        Write-Warning "Archivo de configuración faltante: $file"
    }
}

# 3. Crear archivos de log iniciales (vacíos)
Write-Info "Inicializando archivos de log..."

$logFiles = @(
    ".\logs\cajero-banking-application.log",
    ".\logs\cajero-banking-errors.log",
    ".\logs\cajero-banking-debug.log",
    ".\logs\cajero-banking-banking-audit.log",
    ".\logs\cajero-banking-sql.log"
)

foreach ($logFile in $logFiles) {
    if (-not (Test-Path $logFile)) {
        New-Item -ItemType File -Path $logFile -Force | Out-Null
        Add-Content -Path $logFile -Value "# Log file initialized on $(Get-Date)"
        Write-Success "Archivo de log inicializado: $logFile"
    } else {
        Write-Info "Archivo de log existe: $logFile"
    }
}

# 4. Configurar permisos de archivos (Windows)
Write-Info "Configurando permisos de archivos..."

try {
    # Dar permisos de escritura completos a la carpeta logs
    $logsPath = Resolve-Path ".\logs"
    $acl = Get-Acl $logsPath
    
    # Agregar permisos para el usuario actual
    $currentUser = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
    $accessRule = New-Object System.Security.AccessControl.FileSystemAccessRule($currentUser, "FullControl", "ContainerInherit,ObjectInherit", "None", "Allow")
    $acl.SetAccessRule($accessRule)
    Set-Acl -Path $logsPath -AclObject $acl
    
    Write-Success "Permisos configurados para: $logsPath"
} catch {
    Write-Warning "No se pudieron configurar permisos automáticamente. Configúralos manualmente si es necesario."
}

# 5. Crear script de limpieza de logs
Write-Info "Creando scripts de utilidades..."

$cleanupScript = @"
# Script de limpieza de logs antiguos
Write-Host "Limpiando logs antiguos (más de 30 días)..." -ForegroundColor Yellow

`$cutoffDate = (Get-Date).AddDays(-30)
`$cleanedFiles = 0

# Limpiar logs archivados
if (Test-Path ".\logs\archived") {
    Get-ChildItem ".\logs\archived" -File | Where-Object { `$_.LastWriteTime -lt `$cutoffDate } | ForEach-Object {
        Remove-Item `$_.FullName -Force
        `$cleanedFiles++
        Write-Host "Eliminado: `$(`$_.Name)" -ForegroundColor Red
    }
}

# Comprimir logs grandes (mayor a 50MB)
Get-ChildItem ".\logs" -File "*.log" | Where-Object { `$_.Length -gt 50MB } | ForEach-Object {
    `$compressedName = ".\logs\archived\`$(`$_.BaseName)_`$(Get-Date -Format 'yyyy-MM-dd-HH-mm').zip"
    Compress-Archive -Path `$_.FullName -DestinationPath `$compressedName
    Remove-Item `$_.FullName -Force
    Write-Host "Comprimido: `$(`$_.Name) -> `$(Split-Path `$compressedName -Leaf)" -ForegroundColor Green
    `$cleanedFiles++
}

Write-Host "Limpieza completada. Archivos procesados: `$cleanedFiles" -ForegroundColor Green
"@

$cleanupScript | Out-File -FilePath ".\cleanup-logs.ps1" -Encoding UTF8
Write-Success "Script de limpieza creado: .\cleanup-logs.ps1"

# 6. Crear script de análisis de logs
$analysisScript = @"
# Script de análisis de logs
param(
    [Parameter(Position=0)]
    [ValidateSet("errors", "performance", "security", "transactions")]
    [string]`$AnalysisType = "errors"
)

function Analyze-Errors {
    Write-Host "=== ANÁLISIS DE ERRORES ===" -ForegroundColor Red
    if (Test-Path ".\logs\cajero-banking-errors.log") {
        `$errors = Get-Content ".\logs\cajero-banking-errors.log" | Where-Object { `$_ -match "ERROR" }
        Write-Host "Total de errores encontrados: `$(`$errors.Count)" -ForegroundColor Yellow
        
        `$errorTypes = `$errors | ForEach-Object { 
            if (`$_ -match "(\w+Exception|\w+Error)") { `$matches[1] } 
        } | Group-Object | Sort-Object Count -Descending
        
        Write-Host "`nTipos de errores más frecuentes:" -ForegroundColor Cyan
        `$errorTypes | Select-Object -First 10 | ForEach-Object {
            Write-Host "  `$(`$_.Name): `$(`$_.Count) ocurrencias" -ForegroundColor White
        }
    }
}

function Analyze-Performance {
    Write-Host "=== ANÁLISIS DE PERFORMANCE ===" -ForegroundColor Blue
    if (Test-Path ".\logs\cajero-banking-application.log") {
        `$perfWarnings = Get-Content ".\logs\cajero-banking-application.log" | Where-Object { `$_ -match "PERFORMANCE_WARNING" }
        Write-Host "Advertencias de performance: `$(`$perfWarnings.Count)" -ForegroundColor Yellow
        
        `$perfWarnings | ForEach-Object {
            if (`$_ -match "EXECUTION_TIME: (\d+)ms") {
                [int]`$matches[1]
            }
        } | Measure-Object -Average -Maximum | ForEach-Object {
            Write-Host "Tiempo promedio: `$([math]::Round(`$_.Average, 2))ms" -ForegroundColor Cyan
            Write-Host "Tiempo máximo: `$(`$_.Maximum)ms" -ForegroundColor Red
        }
    }
}

function Analyze-Security {
    Write-Host "=== ANÁLISIS DE SEGURIDAD ===" -ForegroundColor Magenta
    if (Test-Path ".\logs\cajero-banking-banking-audit.log") {
        `$auditLogs = Get-Content ".\logs\cajero-banking-banking-audit.log"
        `$failedLogins = `$auditLogs | Where-Object { `$_ -match "LOGIN_ATTEMPT.*SUCCESS: false" }
        `$accountBlocks = `$auditLogs | Where-Object { `$_ -match "ACCOUNT_BLOCK" }
        
        Write-Host "Intentos de login fallidos: `$(`$failedLogins.Count)" -ForegroundColor Red
        Write-Host "Cuentas bloqueadas: `$(`$accountBlocks.Count)" -ForegroundColor Red
    }
}

function Analyze-Transactions {
    Write-Host "=== ANÁLISIS DE TRANSACCIONES ===" -ForegroundColor Green
    if (Test-Path ".\logs\cajero-banking-banking-audit.log") {
        `$transactions = Get-Content ".\logs\cajero-banking-banking-audit.log" | Where-Object { `$_ -match "BANKING_TRANSACTION" }
        Write-Host "Total de transacciones: `$(`$transactions.Count)" -ForegroundColor Cyan
        
        `$transactionTypes = `$transactions | ForEach-Object {
            if (`$_ -match "TYPE: (\w+)") { `$matches[1] }
        } | Group-Object | Sort-Object Count -Descending
        
        Write-Host "`nTipos de transacciones:" -ForegroundColor Yellow
        `$transactionTypes | ForEach-Object {
            Write-Host "  `$(`$_.Name): `$(`$_.Count)" -ForegroundColor White
        }
    }
}

switch (`$AnalysisType) {
    "errors" { Analyze-Errors }
    "performance" { Analyze-Performance }
    "security" { Analyze-Security }
    "transactions" { Analyze-Transactions }
}
"@

$analysisScript | Out-File -FilePath ".\analyze-logs.ps1" -Encoding UTF8
Write-Success "Script de análisis creado: .\analyze-logs.ps1"

# 7. Verificar dependencias de Maven
Write-Info "Verificando configuración de Maven..."

if (Test-Path ".\pom.xml") {
    $pomContent = Get-Content ".\pom.xml" -Raw
    if ($pomContent -match "spring-boot-starter-logging" -or $pomContent -match "logback") {
        Write-Success "Dependencias de logging configuradas en pom.xml"
    } else {
        Write-Warning "Verifica que las dependencias de logging estén en pom.xml"
    }
} else {
    Write-Warning "Archivo pom.xml no encontrado"
}

# 8. Resumen final
Write-Host ""
Write-Host "==============================================================" -ForegroundColor Green
Write-Host "  CONFIGURACIÓN COMPLETADA" -ForegroundColor Green
Write-Host "==============================================================" -ForegroundColor Green
Write-Host ""
Write-Success "Sistema de logging configurado correctamente"
Write-Info "Archivos de log en: .\logs\"
Write-Info "Scripts disponibles:"
Write-Host "  • .\monitor-logs.ps1 [tipo] - Monitorear logs en tiempo real" -ForegroundColor Cyan
Write-Host "  • .\cleanup-logs.ps1 - Limpiar logs antiguos" -ForegroundColor Cyan
Write-Host "  • .\analyze-logs.ps1 [tipo] - Analizar logs" -ForegroundColor Cyan
Write-Host ""
Write-Info "Para iniciar monitoreo: .\monitor-logs.ps1 all"
Write-Info "Para ver solo errores: .\monitor-logs.ps1 errors"
Write-Host ""

# 9. Crear archivo README para logging
$readmeContent = @"
# Sistema de Logging - Cajero/ATM Banking

## Descripción
Sistema completo de logging y auditoría para la aplicación bancaria.

## Estructura de Logs

### Archivos de Log
- `cajero-banking-application.log` - Log principal de la aplicación
- `cajero-banking-errors.log` - Solo errores (ERROR level)
- `cajero-banking-debug.log` - Información de debug
- `cajero-banking-banking-audit.log` - Auditoría de transacciones bancarias
- `cajero-banking-sql.log` - Consultas SQL de Hibernate

### Carpetas
- `logs/` - Logs activos
- `logs/archived/` - Logs archivados y comprimidos

## Scripts Disponibles

### Monitoreo en Tiempo Real
```powershell
.\monitor-logs.ps1 all          # Todos los logs
.\monitor-logs.ps1 errors       # Solo errores
.\monitor-logs.ps1 audit        # Auditoría bancaria
.\monitor-logs.ps1 sql          # Consultas SQL
```

### Análisis de Logs
```powershell
.\analyze-logs.ps1 errors       # Análisis de errores
.\analyze-logs.ps1 performance  # Análisis de rendimiento
.\analyze-logs.ps1 security     # Análisis de seguridad
.\analyze-logs.ps1 transactions # Análisis de transacciones
```

### Mantenimiento
```powershell
.\cleanup-logs.ps1              # Limpiar logs antiguos
```

## Configuración

### Niveles de Log
- `TRACE` - Información muy detallada
- `DEBUG` - Información de debug
- `INFO` - Información general
- `WARN` - Advertencias
- `ERROR` - Errores

### Configuración por Ambiente
- **Desarrollo**: DEBUG level, logs detallados
- **Producción**: INFO level, logs optimizados

## Características de Auditoría

### Transacciones Bancarias
- Retiros, consignaciones, transferencias
- Cambios de PIN
- Intentos de login
- Bloqueos de cuenta

### Información Registrada
- Timestamp preciso
- IP del cliente
- ID de transacción único
- Detalles de la operación
- Resultado de la operación

## Monitoreo de Performance
- Operaciones lentas (>1 segundo)
- Tiempo de ejecución de transacciones
- Advertencias automáticas

## Seguridad
- Ocultación automática de PINs y passwords en logs
- Registro de intentos de acceso fallidos
- Auditoría de cambios críticos
- Detección de patrones sospechosos
"@

$readmeContent | Out-File -FilePath ".\LOGGING_README.md" -Encoding UTF8
Write-Success "Documentación creada: .\LOGGING_README.md"

Write-Host ""
Write-Host "¡Configuración completada exitosamente!" -ForegroundColor Green
Write-Host "Inicia la aplicación para comenzar a generar logs." -ForegroundColor Yellow
