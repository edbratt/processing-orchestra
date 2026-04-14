$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$jarPath = Get-ChildItem -Path (Join-Path $scriptDir "target") -Filter "processing-server-*.jar" -File |
    Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" -and $_.Name -notlike "*-original.jar" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
$configPath = Join-Path $scriptDir "config\application-https.yaml"
$keystorePath = Join-Path $scriptDir "keystore.p12"

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "ERROR: 'java' not found in PATH." -ForegroundColor Red
    Write-Host "Install Java 21+ and ensure JAVA_HOME\bin is in your PATH." -ForegroundColor Yellow
    Write-Host "See README.md for setup instructions." -ForegroundColor Yellow
    exit 1
}

if (-not $jarPath) {
    Write-Host "ERROR: Packaged jar not found under $scriptDir\target" -ForegroundColor Red
    Write-Host "Run 'mvn package -DskipTests' first." -ForegroundColor Yellow
    exit 1
}

if (-not (Test-Path $configPath)) {
    Write-Host "ERROR: HTTPS config file not found: $configPath" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $keystorePath)) {
    Write-Host "ERROR: HTTPS keystore not found: $keystorePath" -ForegroundColor Red
    Write-Host "Run '.\create-keystore.ps1' first." -ForegroundColor Yellow
    exit 1
}

Write-Host "Starting Processing Server..." -ForegroundColor Green
Write-Host "Server will be available at:" -ForegroundColor Cyan
Write-Host "  - http://localhost:8080" -ForegroundColor White
Write-Host "  - https://localhost:8443" -ForegroundColor White
Write-Host ""
Write-Host "HTTPS config:  $configPath" -ForegroundColor Gray
Write-Host "HTTPS keystore: $keystorePath" -ForegroundColor Gray
Write-Host ""

& java "-Dapp.config=$configPath" "-jar" $jarPath.FullName
