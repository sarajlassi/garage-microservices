#!/usr/bin/env pwsh
# Garage Microservices - Quick Start Script for Windows PowerShell
# Usage: .\start.ps1 [option]
# Options: build, start, stop, logs, clean

param(
    [string]$Action = "start"
)

$ProjectRoot = Split-Path $MyInvocation.MyCommand.Path -Parent
$DockerComposePath = Join-Path $ProjectRoot "docker-compose.yml"

Write-Host "🚗 Garage Microservices Manager" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

switch ($Action.ToLower()) {
    "build" {
        Write-Host "📦 Building all services..." -ForegroundColor Yellow
        docker-compose -f $DockerComposePath build
        Write-Host "✅ Build complete!" -ForegroundColor Green
    }

    "start" {
        Write-Host "🚀 Starting all services..." -ForegroundColor Yellow
        docker-compose -f $DockerComposePath up -d
        Start-Sleep -Seconds 5
        Write-Host ""
        Write-Host "✅ Services started! Available at:" -ForegroundColor Green
        Write-Host "   🔐 Auth Service:    http://localhost:8081" -ForegroundColor Cyan
        Write-Host "   🚗 Vehicle Service: http://localhost:8082" -ForegroundColor Cyan
        Write-Host "   📦 Stock Service:   http://localhost:8083" -ForegroundColor Cyan
        Write-Host "   📊 Kafka UI:        http://localhost:8090" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Database Access:" -ForegroundColor Yellow
        Write-Host "   Auth DB:    localhost:5432/auth_db" -ForegroundColor Gray
        Write-Host "   Vehicle DB: localhost:5433/vehicle_db" -ForegroundColor Gray
        Write-Host "   Stock DB:   localhost:5434/stock_db" -ForegroundColor Gray
        Write-Host ""
        Write-Host "View logs with: .\start.ps1 logs" -ForegroundColor Yellow
    }

    "stop" {
        Write-Host "🛑 Stopping all services..." -ForegroundColor Yellow
        docker-compose -f $DockerComposePath down
        Write-Host "✅ All services stopped!" -ForegroundColor Green
    }

    "logs" {
        Write-Host "📋 Service Logs (Ctrl+C to exit):" -ForegroundColor Yellow
        docker-compose -f $DockerComposePath logs -f
    }

    "clean" {
        Write-Host "🧹 Cleaning up containers and volumes..." -ForegroundColor Yellow
        docker-compose -f $DockerComposePath down -v
        Write-Host "✅ Cleanup complete!" -ForegroundColor Green
    }

    "status" {
        Write-Host "📊 Service Status:" -ForegroundColor Yellow
        docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | Where-Object {$_ -match 'service|kafka|postgres|zookeeper'}
    }

    default {
        Write-Host "Usage: .\start.ps1 [option]" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Options:" -ForegroundColor Cyan
        Write-Host "  build    - Build all Docker images" -ForegroundColor Gray
        Write-Host "  start    - Start all containers (default)" -ForegroundColor Gray
        Write-Host "  stop     - Stop all containers" -ForegroundColor Gray
        Write-Host "  logs     - View live logs" -ForegroundColor Gray
        Write-Host "  status   - Show container status" -ForegroundColor Gray
        Write-Host "  clean    - Remove containers and volumes" -ForegroundColor Gray
        Write-Host ""
        Write-Host "Examples:" -ForegroundColor Cyan
        Write-Host "  .\start.ps1              # Start services" -ForegroundColor Gray
        Write-Host "  .\start.ps1 build        # Build images" -ForegroundColor Gray
        Write-Host "  .\start.ps1 logs         # Show logs" -ForegroundColor Gray
    }
}

