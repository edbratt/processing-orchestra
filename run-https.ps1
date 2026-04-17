param(
    [string]$Properties = ""
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

function Get-LatestJar($rootDir) {
    Get-ChildItem -Path (Join-Path $rootDir "target") -Filter "processing-server-*.jar" -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" -and $_.Name -notlike "*-original.jar" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

function Get-LatestSourceWriteTime($rootDir) {
    $paths = @(
        (Join-Path $rootDir "src"),
        (Join-Path $rootDir "generated-src"),
        (Join-Path $rootDir "config"),
        (Join-Path $rootDir "pom.xml")
    )

    $latest = Get-Date "2000-01-01"
    foreach ($path in $paths) {
        if (-not (Test-Path $path)) {
            continue
        }
        $items = Get-Item $path
        if ($items.PSIsContainer) {
            $candidate = Get-ChildItem -Path $path -Recurse -File -ErrorAction SilentlyContinue |
                Sort-Object LastWriteTime -Descending |
                Select-Object -First 1
            if ($candidate -and $candidate.LastWriteTime -gt $latest) {
                $latest = $candidate.LastWriteTime
            }
        } elseif ($items.LastWriteTime -gt $latest) {
            $latest = $items.LastWriteTime
        }
    }
    return $latest
}

$jarPath = Get-LatestJar $scriptDir
$configPath = Join-Path $scriptDir "config\application-https.yaml"
$keystorePath = Join-Path $scriptDir "keystore.p12"

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "ERROR: 'java' not found in PATH." -ForegroundColor Red
    Write-Host "Install Java 21+ and ensure JAVA_HOME\bin is in your PATH." -ForegroundColor Yellow
    Write-Host "See README.md for setup instructions." -ForegroundColor Yellow
    exit 1
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "ERROR: 'mvn' not found in PATH." -ForegroundColor Red
    Write-Host "Install Maven and ensure it is available before using auto-build launch scripts." -ForegroundColor Yellow
    exit 1
}

$sourceWriteTime = Get-LatestSourceWriteTime $scriptDir
if (-not $jarPath -or $sourceWriteTime -gt $jarPath.LastWriteTime) {
    Write-Host "Source changes detected. Building packaged jar..." -ForegroundColor Yellow
    & mvn -q -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Build failed." -ForegroundColor Red
        exit $LASTEXITCODE
    }
    $jarPath = Get-LatestJar $scriptDir
}

if (-not $jarPath) {
    Write-Host "ERROR: Packaged jar not found under $scriptDir\target" -ForegroundColor Red
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

$javaArgs = @("-Dapp.config=$configPath")
if (-not [string]::IsNullOrWhiteSpace($Properties)) {
    $javaArgs += $Properties.Trim().Split(' ', [System.StringSplitOptions]::RemoveEmptyEntries)
}
$javaArgs += "-jar"
$javaArgs += $jarPath.FullName

& java @javaArgs
