$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$classPath = "$scriptDir\target\classes;$scriptDir\target\libs\*"

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "ERROR: 'java' not found in PATH." -ForegroundColor Red
    Write-Host "Install Java 21+ and ensure JAVA_HOME\bin is in your PATH." -ForegroundColor Yellow
    Write-Host "See README.md for setup instructions." -ForegroundColor Yellow
    exit 1
}

Write-Host "Starting Processing Server..." -ForegroundColor Green
Write-Host "Server will be available at:" -ForegroundColor Cyan
Write-Host "  - https://localhost:8080" -ForegroundColor White
Write-Host ""
Write-Host "Note: Check console output for your local IP" -ForegroundColor Gray
Write-Host ""

java -cp $classPath com.processing.server.Main