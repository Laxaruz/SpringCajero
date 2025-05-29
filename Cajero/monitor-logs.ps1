# Script de PowerShell para monitorear logs del sistema Cajero/ATM
# Uso: .\monitor-logs.ps1 [tipo_log]
# Tipos disponibles: all, errors, debug, audit, sql

param(
    [Parameter(Position=0)]
    [ValidateSet("all", "errors", "debug", "audit", "sql", "application")]
    [string]$LogType = "all"
)

$LogsPath = ".\logs"
$ErrorLogFile = "$LogsPath\cajero-banking-errors.log"
$ApplicationLogFile = "$LogsPath\cajero-banking-application.log"
$DebugLogFile = "$LogsPath\cajero-banking-debug.log"
$AuditLogFile = "$LogsPath\cajero-banking-banking-audit.log"
$SqlLogFile = "$LogsPath\cajero-banking-sql.log"

# Colores para diferentes tipos de log
$ErrorColor = "Red"
$WarnColor = "Yellow"
$InfoColor = "Green"
$DebugColor = "Cyan"
$AuditColor = "Magenta"
$SqlColor = "Blue"

function Show-Header {
    param($Title)
    Write-Host "`n" -NoNewline
    Write-Host "=" * 80 -ForegroundColor DarkGray
    Write-Host " MONITOR DE LOGS - $Title " -ForegroundColor White -BackgroundColor DarkBlue
    Write-Host "=" * 80 -ForegroundColor DarkGray
    Write-Host " Presiona Ctrl+C para salir" -ForegroundColor DarkGray
    Write-Host "=" * 80 -ForegroundColor DarkGray
    Write-Host ""
}

function Get-LogColor {
    param($LogLine)
    
    if ($LogLine -match "ERROR") { return $ErrorColor }
    elseif ($LogLine -match "WARN") { return $WarnColor }
    elseif ($LogLine -match "INFO") { return $InfoColor }
    elseif ($LogLine -match "DEBUG") { return $DebugColor }
    elseif ($LogLine -match "AUDIT") { return $AuditColor }
    elseif ($LogLine -match "SQL") { return $SqlColor }
    else { return "White" }
}

function Monitor-LogFile {
    param($FilePath, $Title)
    
    if (-not (Test-Path $FilePath)) {
        Write-Host "Archivo de log no encontrado: $FilePath" -ForegroundColor Red
        Write-Host "Inicia la aplicación para generar logs." -ForegroundColor Yellow
        return
    }
    
    Show-Header $Title
    
    try {
        Get-Content $FilePath -Wait -Tail 10 | ForEach-Object {
            $color = Get-LogColor $_
            $timestamp = Get-Date -Format "HH:mm:ss"
            Write-Host "[$timestamp] " -ForegroundColor DarkGray -NoNewline
            Write-Host $_ -ForegroundColor $color
        }
    }
    catch {
        Write-Host "Error monitoreando el archivo: $($_.Exception.Message)" -ForegroundColor Red
    }
}

function Monitor-AllLogs {
    Show-Header "TODOS LOS LOGS"
    
    $jobs = @()
    
    # Crear trabajos en segundo plano para cada archivo de log
    if (Test-Path $ErrorLogFile) {
        $jobs += Start-Job -ScriptBlock {
            param($file)
            Get-Content $file -Wait -Tail 5 | ForEach-Object {
                Write-Output "[ERROR] $_"
            }
        } -ArgumentList $ErrorLogFile
    }
    
    if (Test-Path $ApplicationLogFile) {
        $jobs += Start-Job -ScriptBlock {
            param($file)
            Get-Content $file -Wait -Tail 5 | ForEach-Object {
                Write-Output "[APP] $_"
            }
        } -ArgumentList $ApplicationLogFile
    }
    
    if (Test-Path $AuditLogFile) {
        $jobs += Start-Job -ScriptBlock {
            param($file)
            Get-Content $file -Wait -Tail 5 | ForEach-Object {
                Write-Output "[AUDIT] $_"
            }
        } -ArgumentList $AuditLogFile
    }
    
    try {
        while ($true) {
            foreach ($job in $jobs) {
                $output = Receive-Job $job -ErrorAction SilentlyContinue
                if ($output) {
                    foreach ($line in $output) {
                        $color = Get-LogColor $line
                        $timestamp = Get-Date -Format "HH:mm:ss"
                        Write-Host "[$timestamp] " -ForegroundColor DarkGray -NoNewline
                        Write-Host $line -ForegroundColor $color
                    }
                }
            }
            Start-Sleep -Milliseconds 500
        }
    }
    finally {
        $jobs | Stop-Job
        $jobs | Remove-Job
    }
}

# Función para mostrar estadísticas de logs
function Show-LogStats {
    Write-Host "`nESTADÍSTICAS DE LOGS:" -ForegroundColor White -BackgroundColor DarkBlue
    Write-Host "-" * 50 -ForegroundColor DarkGray
    
    $files = @(
        @{Name="Aplicación"; Path=$ApplicationLogFile},
        @{Name="Errores"; Path=$ErrorLogFile},
        @{Name="Debug"; Path=$DebugLogFile},
        @{Name="Auditoría"; Path=$AuditLogFile},
        @{Name="SQL"; Path=$SqlLogFile}
    )
    
    foreach ($file in $files) {
        if (Test-Path $file.Path) {
            $size = (Get-Item $file.Path).Length
            $sizeKB = [math]::Round($size / 1KB, 2)
            $lines = (Get-Content $file.Path | Measure-Object -Line).Lines
            Write-Host "$($file.Name): $lines líneas, $sizeKB KB" -ForegroundColor Green
        } else {
            Write-Host "$($file.Name): No existe" -ForegroundColor Red
        }
    }
}

# Función para limpiar logs antiguos
function Clear-OldLogs {
    Write-Host "Limpiando logs antiguos..." -ForegroundColor Yellow
    
    $cutoffDate = (Get-Date).AddDays(-7)
    Get-ChildItem "$LogsPath\archived" -File | Where-Object { $_.LastWriteTime -lt $cutoffDate } | Remove-Item -Force
    
    Write-Host "Logs antiguos eliminados." -ForegroundColor Green
}

# Menú principal
switch ($LogType) {
    "all" { 
        Show-LogStats
        Start-Sleep -Seconds 2
        Monitor-AllLogs 
    }
    "errors" { Monitor-LogFile $ErrorLogFile "ERRORES" }
    "debug" { Monitor-LogFile $DebugLogFile "DEBUG" }
    "audit" { Monitor-LogFile $AuditLogFile "AUDITORÍA BANCARIA" }
    "sql" { Monitor-LogFile $SqlLogFile "CONSULTAS SQL" }
    "application" { Monitor-LogFile $ApplicationLogFile "APLICACIÓN" }
    default { 
        Write-Host "Tipo de log no reconocido: $LogType" -ForegroundColor Red
        Write-Host "Tipos disponibles: all, errors, debug, audit, sql, application" -ForegroundColor Yellow
    }
}
