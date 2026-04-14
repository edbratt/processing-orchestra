$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$jarPath = Get-ChildItem -Path (Join-Path $scriptDir "target") -Filter "processing-server-*.jar" -File |
    Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" -and $_.Name -notlike "*-original.jar" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "ERROR: 'java' not found in PATH." -ForegroundColor Red
    Write-Host "Install Java 21+ and ensure JAVA_HOME\bin is in your PATH." -ForegroundColor Yellow
    Write-Host "See README.md for setup instructions." -ForegroundColor Yellow
    exit 1
}

Write-Host "Starting Processing Server..." -ForegroundColor Green
Write-Host "Server will be available at:" -ForegroundColor Cyan
Write-Host "  - http://localhost:8080" -ForegroundColor White
Write-Host ""
Write-Host "Optional LAN HTTPS is available via .\run-https.ps1 after generating keystore.p12." -ForegroundColor Gray
Write-Host ""

if (-not $jarPath) {
    Write-Host "ERROR: Packaged jar not found under $scriptDir\target" -ForegroundColor Red
    Write-Host "Run 'mvn package -DskipTests' first." -ForegroundColor Yellow
    exit 1
}

java -jar $jarPath.FullName
