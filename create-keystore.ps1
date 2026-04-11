# Generate SSL keystore for Processing Server
# Usage: .\create-keystore.ps1 [IP_ADDRESS]
# Example: .\create-keystore.ps1 192.168.1.100

param(
    [string]$LocalIP
)

$ErrorActionPreference = "Stop"

# Configuration
$KEYSTORE = "keystore.p12"
$STOREPASS = "changeit"
$VALIDITY_CA = 3650
$VALIDITY_SERVER = 365

# Detect local IP if not provided
if (-not $LocalIP) {
    # Try to detect local IP
    $ipAddresses = (Get-NetIPAddress -AddressFamily IPv4 -InterfaceAlias "Wi-Fi","Ethernet","Ethernet 2" -ErrorAction SilentlyContinue).IPAddress
    
    if ($ipAddresses) {
        # Prefer 192.168.x.x addresses
        $LocalIP = $ipAddresses | Where-Object { $_ -match "^192\.168\." } | Select-Object -First 1
        
        # Fall back to any non-loopback address
        if (-not $LocalIP) {
            $LocalIP = $ipAddresses | Where-Object { $_ -ne "127.0.0.1" -and $_ -notmatch "^169\.254\." } | Select-Object -First 1
        }
    }
    
    if (-not $LocalIP) {
        Write-Host "Could not detect local IP address." -ForegroundColor Red
        Write-Host "Usage: .\create-keystore.ps1 <YOUR_LOCAL_IP>" -ForegroundColor Yellow
        Write-Host "Example: .\create-keystore.ps1 192.168.1.100" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Find your IP with: ipconfig" -ForegroundColor Gray
        exit 1
    }
}

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "Creating SSL Keystore for Processing Server" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Local IP: $LocalIP" -ForegroundColor Green
Write-Host "Keystore: $KEYSTORE" -ForegroundColor Green
Write-Host ""

# Check for keytool
$keytool = $null
if ($env:JAVA_HOME) {
    $keytool = Join-Path $env:JAVA_HOME "bin\keytool.exe"
}
if (-not $keytool -or -not (Test-Path $keytool)) {
    $keytool = "keytool"  # Fall back to PATH
}

# Verify keytool is available
try {
    & $keytool -version 2>&1 | Out-Null
} catch {
    Write-Host "ERROR: keytool not found. Please install Java JDK and set JAVA_HOME." -ForegroundColor Red
    Write-Host "Example: `$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26'" -ForegroundColor Gray
    exit 1
}

# Remove existing keystore
if (Test-Path $KEYSTORE) {
    Write-Host "Removing existing keystore..."
    Remove-Item $KEYSTORE -Force
}

# Remove temporary files if they exist
Remove-Item "server.csr" -ErrorAction SilentlyContinue
Remove-Item "server.cer" -ErrorAction SilentlyContinue
Remove-Item "ca.cer" -ErrorAction SilentlyContinue

Write-Host "Step 1: Creating CA certificate (valid for $VALIDITY_CA days)..." -ForegroundColor Yellow
& $keytool -genkeypair -alias root-ca -keyalg RSA -keysize 2048 -validity $VALIDITY_CA `
    -keystore $KEYSTORE -storetype PKCS12 -storepass $STOREPASS `
    -dname "CN=ProcessingServer-CA,O=Dev,C=US" `
    -ext "BasicConstraints:critical,ca:true,pathlen:1" `
    2>&1 | Out-Null

Write-Host "Step 2: Creating server certificate (valid for $VALIDITY_SERVER days)..." -ForegroundColor Yellow
& $keytool -genkeypair -alias "1" -keyalg RSA -keysize 2048 -validity $VALIDITY_SERVER `
    -keystore $KEYSTORE -storetype PKCS12 -storepass $STOREPASS `
    -dname "CN=localhost,O=Dev,C=US" `
    -ext "SAN=DNS:localhost,IP:127.0.0.1,IP:$LocalIP" `
    2>&1 | Out-Null

Write-Host "Step 3: Generating certificate signing request..." -ForegroundColor Yellow
& $keytool -certreq -alias "1" -keystore $KEYSTORE -storetype PKCS12 `
    -storepass $STOREPASS -file server.csr `
    2>&1 | Out-Null

Write-Host "Step 4: Signing server certificate with CA..." -ForegroundColor Yellow
& $keytool -gencert -alias root-ca -keystore $KEYSTORE -storetype PKCS12 `
    -storepass $STOREPASS -infile server.csr -outfile server.cer -validity $VALIDITY_SERVER `
    -ext "SAN=DNS:localhost,IP:127.0.0.1,IP:$LocalIP" `
    2>&1 | Out-Null

Write-Host "Step 5: Importing signed server certificate..." -ForegroundColor Yellow
& $keytool -importcert -alias "1" -keystore $KEYSTORE -storetype PKCS12 `
    -storepass $STOREPASS -file server.cer `
    2>&1 | Out-Null

Write-Host "Step 6: Exporting CA certificate..." -ForegroundColor Yellow
& $keytool -exportcert -alias root-ca -keystore $KEYSTORE -storetype PKCS12 `
    -storepass $STOREPASS -file ca.cer `
    2>&1 | Out-Null

Write-Host "Step 7: Converting CA to trusted certificate entry..." -ForegroundColor Yellow
& $keytool -delete -alias root-ca -keystore $KEYSTORE -storetype PKCS12 `
    -storepass $STOREPASS `
    2>&1 | Out-Null
& $keytool -importcert -alias root-ca -file ca.cer -keystore $KEYSTORE `
    -storetype PKCS12 -storepass $STOREPASS -noprompt `
    2>&1 | Out-Null

Write-Host "Step 8: Cleaning up temporary files..." -ForegroundColor Yellow
Remove-Item "server.csr" -ErrorAction SilentlyContinue
Remove-Item "server.cer" -ErrorAction SilentlyContinue
Remove-Item "ca.cer" -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "Step 9: Verifying keystore contents..." -ForegroundColor Yellow
Write-Host ""
& $keytool -list -keystore $KEYSTORE -storetype PKCS12 -storepass $STOREPASS

Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "Keystore created successfully!" -ForegroundColor Green
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Copy keystore to resources:"
Write-Host "     Copy-Item $KEYSTORE src\main\resources\$KEYSTORE" -ForegroundColor Gray
Write-Host ""
Write-Host "  2. Rebuild the project:"
Write-Host "     mvn clean package -DskipTests" -ForegroundColor Gray
Write-Host ""
Write-Host "  3. Run the server:"
Write-Host "     .\run.ps1" -ForegroundColor Gray
Write-Host ""
Write-Host "Access from:" -ForegroundColor Yellow
Write-Host "  - Local:    https://localhost:8080/" -ForegroundColor White
Write-Host "  - Mobile:   https://${LocalIP}:8080/" -ForegroundColor White