# Script para construir y ejecutar la aplicación Cajero en Docker
# Author: Laxaruz
# Date: May 28, 2025

param(
    [string]$Action = "build",
    [string]$ImageName = "cajero-app",
    [string]$Tag = "latest",
    [string]$ContainerName = "cajero-container",
    [string]$Port = "8080",
    [string]$MySqlUrl = "jdbc:mysql://host.docker.internal:3306/atmdatabase",
    [string]$MySqlUser = "root",
    [string]$MySqlPassword = ""
)

function Show-Help {
    Write-Host "=== Script de Docker para Aplicación Cajero ===" -ForegroundColor Green
    Write-Host ""
    Write-Host "Uso: .\docker-manager.ps1 -Action <accion> [opciones]" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Acciones disponibles:" -ForegroundColor Cyan
    Write-Host "  build     - Construir la imagen Docker"
    Write-Host "  run       - Ejecutar el contenedor"
    Write-Host "  stop      - Detener el contenedor"
    Write-Host "  remove    - Eliminar el contenedor"
    Write-Host "  logs      - Ver logs del contenedor"
    Write-Host "  shell     - Abrir shell en el contenedor"
    Write-Host "  clean     - Limpiar imágenes y contenedores"
    Write-Host "  help      - Mostrar esta ayuda"
    Write-Host ""
    Write-Host "Opciones:" -ForegroundColor Cyan
    Write-Host "  -ImageName       Nombre de la imagen (default: cajero-app)"
    Write-Host "  -Tag             Tag de la imagen (default: latest)"
    Write-Host "  -ContainerName   Nombre del contenedor (default: cajero-container)"
    Write-Host "  -Port            Puerto para mapear (default: 8080)"
    Write-Host "  -MySqlUrl        URL de MySQL (default: jdbc:mysql://host.docker.internal:3306/atmdatabase)"
    Write-Host "  -MySqlUser       Usuario de MySQL (default: root)"
    Write-Host "  -MySqlPassword   Contraseña de MySQL (default: vacía)"
    Write-Host ""
    Write-Host "Ejemplos:" -ForegroundColor Green
    Write-Host "  .\docker-manager.ps1 -Action build"
    Write-Host "  .\docker-manager.ps1 -Action run -Port 9090"
    Write-Host "  .\docker-manager.ps1 -Action run -MySqlPassword 'mipassword'"
}

function Build-Image {
    Write-Host "🔨 Construyendo imagen Docker..." -ForegroundColor Yellow
    docker build -t "${ImageName}:${Tag}" .
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Imagen construida exitosamente: ${ImageName}:${Tag}" -ForegroundColor Green
    } else {
        Write-Host "❌ Error al construir la imagen" -ForegroundColor Red
        exit 1
    }
}

function Run-Container {
    Write-Host "🚀 Ejecutando contenedor..." -ForegroundColor Yellow
    
    # Verificar si el contenedor ya existe
    $existingContainer = docker ps -a --format "{{.Names}}" | Where-Object { $_ -eq $ContainerName }
    if ($existingContainer) {
        Write-Host "⚠️  El contenedor '$ContainerName' ya existe. Eliminándolo..." -ForegroundColor Yellow
        docker rm -f $ContainerName
    }
    
    docker run -d `
        --name $ContainerName `
        -p "${Port}:8080" `
        -e MYSQL_URL="$MySqlUrl" `
        -e MYSQL_USERNAME="$MySqlUser" `
        -e MYSQL_PASSWORD="$MySqlPassword" `
        -e SPRING_PROFILES_ACTIVE="docker" `
        -v "${PWD}\logs:/app/logs" `
        "${ImageName}:${Tag}"
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Contenedor iniciado exitosamente" -ForegroundColor Green
        Write-Host "🌐 Aplicación disponible en: http://localhost:${Port}" -ForegroundColor Cyan
        Write-Host "📋 Para ver logs: .\docker-manager.ps1 -Action logs" -ForegroundColor Gray
    } else {
        Write-Host "❌ Error al ejecutar el contenedor" -ForegroundColor Red
        exit 1
    }
}

function Stop-Container {
    Write-Host "🛑 Deteniendo contenedor..." -ForegroundColor Yellow
    docker stop $ContainerName
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Contenedor detenido" -ForegroundColor Green
    } else {
        Write-Host "❌ Error al detener el contenedor" -ForegroundColor Red
    }
}

function Remove-Container {
    Write-Host "🗑️  Eliminando contenedor..." -ForegroundColor Yellow
    docker rm -f $ContainerName
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Contenedor eliminado" -ForegroundColor Green
    } else {
        Write-Host "❌ Error al eliminar el contenedor" -ForegroundColor Red
    }
}

function Show-Logs {
    Write-Host "📋 Mostrando logs del contenedor..." -ForegroundColor Yellow
    docker logs -f $ContainerName
}

function Open-Shell {
    Write-Host "🐚 Abriendo shell en el contenedor..." -ForegroundColor Yellow
    docker exec -it $ContainerName /bin/bash
}

function Clean-Docker {
    Write-Host "🧹 Limpiando imágenes y contenedores de Docker..." -ForegroundColor Yellow
    docker system prune -f
    Write-Host "✅ Limpieza completada" -ForegroundColor Green
}

# Verificar si Docker está disponible
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "❌ Docker no está instalado o no está en el PATH" -ForegroundColor Red
    exit 1
}

# Ejecutar acción
switch ($Action.ToLower()) {
    "build" { Build-Image }
    "run" { Run-Container }
    "stop" { Stop-Container }
    "remove" { Remove-Container }
    "logs" { Show-Logs }
    "shell" { Open-Shell }
    "clean" { Clean-Docker }
    "help" { Show-Help }
    default { 
        Write-Host "❌ Acción no reconocida: $Action" -ForegroundColor Red
        Show-Help
        exit 1
    }
}
