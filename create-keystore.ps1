# Generate SSL keystore for Processing Server
# Usage: .\create-keystore.ps1 [IP_ADDRESS] [-Force]
# Example: .\create-keystore.ps1 192.168.1.100 -Force

param(
    [string]$LocalIP,
    [switch]$Force
)

$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $false

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$KEYSTORE = Join-Path $ProjectRoot "keystore.p12"
$CA_CERT = Join-Path $ProjectRoot "processing-server-ca.cer"
$STOREPASS = "changeit"
$VALIDITY_CA = 3650
$VALIDITY_SERVER = 365
$HostnameLocal = "$env:COMPUTERNAME.local"

function Resolve-Keytool {
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin\keytool.exe"
        if (Test-Path $candidate) {
            return $candidate
        }
    }
    return "keytool"
}

function Resolve-LocalIp {
    param([string]$ProvidedIp)

    if ($ProvidedIp) {
        return $ProvidedIp
    }

    $addresses = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
        Where-Object {
            $_.IPAddress -ne "127.0.0.1" -and
            $_.IPAddress -notmatch "^169\.254\." -and
            $_.IPAddress -notmatch "^127\."
        } |
        Select-Object -ExpandProperty IPAddress

    if (-not $addresses) {
        return $null
    }

    $preferred = $addresses | Where-Object { $_ -match "^(192\.168\.|10\.|172\.(1[6-9]|2[0-9]|3[0-1])\.)" } | Select-Object -First 1
    if ($preferred) {
        return $preferred
    }

    return $addresses | Select-Object -First 1
}

function Invoke-Keytool {
    param(
        [string[]]$Arguments,
        [switch]$Quiet
    )

    $stdout = [System.IO.Path]::GetTempFileName()
    $stderr = [System.IO.Path]::GetTempFileName()

    try {
        $process = Start-Process -FilePath $keytool -ArgumentList $Arguments -Wait -NoNewWindow -PassThru `
            -RedirectStandardOutput $stdout -RedirectStandardError $stderr

        $stdoutText = if (Test-Path $stdout) { Get-Content $stdout -Raw } else { "" }
        $stderrText = if (Test-Path $stderr) { Get-Content $stderr -Raw } else { "" }

        if (-not $Quiet -and $stdoutText) {
            Write-Host $stdoutText.TrimEnd()
        }
        if (-not $Quiet -and $stderrText) {
            Write-Host $stderrText.TrimEnd()
        }

        if ($process.ExitCode -ne 0) {
            if ($Quiet -and $stdoutText) {
                Write-Host $stdoutText.TrimEnd()
            }
            if ($Quiet -and $stderrText) {
                Write-Host $stderrText.TrimEnd()
            }
            throw "keytool failed with exit code $($process.ExitCode)."
        }
    } finally {
        Remove-Item $stdout, $stderr -Force -ErrorAction SilentlyContinue
    }
}

$keytool = Resolve-Keytool
try {
    & $keytool -version 2>&1 | Out-Null
} catch {
    Write-Host "ERROR: keytool not found. Please install a Java JDK and set JAVA_HOME if needed." -ForegroundColor Red
    exit 1
}

$LocalIP = Resolve-LocalIp $LocalIP
if (-not $LocalIP) {
    Write-Host "Could not detect a local IPv4 address." -ForegroundColor Red
    Write-Host "Usage: .\create-keystore.ps1 <YOUR_LOCAL_IP> [-Force]" -ForegroundColor Yellow
    exit 1
}

if ((Test-Path $KEYSTORE) -and -not $Force) {
    Write-Host "Keystore already exists at $KEYSTORE" -ForegroundColor Yellow
    Write-Host "Re-run with -Force to overwrite it." -ForegroundColor Yellow
    exit 1
}

$TempDir = Join-Path $ProjectRoot ".keystore-tmp"
New-Item -ItemType Directory -Path $TempDir -Force | Out-Null
$ServerCsr = Join-Path $TempDir "server.csr"
$ServerCer = Join-Path $TempDir "server.cer"
$CaCer = Join-Path $TempDir "ca.cer"
$San = "DNS:localhost,DNS:$HostnameLocal,IP:127.0.0.1,IP:$LocalIP"

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "Creating SSL Keystore for Processing Server" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Local IP:       $LocalIP" -ForegroundColor Green
Write-Host "Hostname:       $HostnameLocal" -ForegroundColor Green
Write-Host "Keystore path:  $KEYSTORE" -ForegroundColor Green
Write-Host "CA cert path:   $CA_CERT" -ForegroundColor Green
Write-Host ""

if (Test-Path $KEYSTORE) {
    Remove-Item $KEYSTORE -Force
}
if (Test-Path $CA_CERT) {
    Remove-Item $CA_CERT -Force
}
Remove-Item $ServerCsr, $ServerCer, $CaCer -Force -ErrorAction SilentlyContinue

Write-Host "Step 1: Creating CA certificate (valid for $VALIDITY_CA days)..." -ForegroundColor Yellow
Invoke-Keytool -Arguments @(
    "-genkeypair", "-alias", "root-ca", "-keyalg", "RSA", "-keysize", "2048", "-validity", "$VALIDITY_CA",
    "-keystore", $KEYSTORE, "-storetype", "PKCS12", "-storepass", $STOREPASS,
    "-dname", "CN=ProcessingServer-CA,O=Dev,C=US",
    "-ext", "BasicConstraints:critical,ca:true,pathlen:1"
) -Quiet

Write-Host "Step 2: Creating server certificate (valid for $VALIDITY_SERVER days)..." -ForegroundColor Yellow
Invoke-Keytool -Arguments @(
    "-genkeypair", "-alias", "1", "-keyalg", "RSA", "-keysize", "2048", "-validity", "$VALIDITY_SERVER",
    "-keystore", $KEYSTORE, "-storetype", "PKCS12", "-storepass", $STOREPASS,
    "-dname", "CN=localhost,O=Dev,C=US",
    "-ext", "SAN=$San"
) -Quiet

Write-Host "Step 3: Generating certificate signing request..." -ForegroundColor Yellow
Invoke-Keytool -Arguments @(
    "-certreq", "-alias", "1", "-keystore", $KEYSTORE, "-storetype", "PKCS12",
    "-storepass", $STOREPASS, "-file", $ServerCsr
) -Quiet

Write-Host "Step 4: Signing server certificate with CA..." -ForegroundColor Yellow
Invoke-Keytool -Arguments @(
    "-gencert", "-alias", "root-ca", "-keystore", $KEYSTORE, "-storetype", "PKCS12",
    "-storepass", $STOREPASS, "-infile", $ServerCsr, "-outfile", $ServerCer, "-validity", "$VALIDITY_SERVER",
    "-ext", "SAN=$San"
) -Quiet

Write-Host "Step 5: Importing signed server certificate..." -ForegroundColor Yellow
Invoke-Keytool -Arguments @(
    "-importcert", "-alias", "1", "-keystore", $KEYSTORE, "-storetype", "PKCS12",
    "-storepass", $STOREPASS, "-file", $ServerCer
) -Quiet

Write-Host "Step 6: Exporting CA certificate..." -ForegroundColor Yellow
Invoke-Keytool -Arguments @(
    "-exportcert", "-alias", "root-ca", "-keystore", $KEYSTORE, "-storetype", "PKCS12",
    "-storepass", $STOREPASS, "-rfc", "-file", $CaCer
) -Quiet
Copy-Item $CaCer $CA_CERT -Force

Write-Host "Step 7: Converting CA to trusted certificate entry..." -ForegroundColor Yellow
Invoke-Keytool -Arguments @(
    "-delete", "-alias", "root-ca", "-keystore", $KEYSTORE, "-storetype", "PKCS12",
    "-storepass", $STOREPASS
) -Quiet
Invoke-Keytool -Arguments @(
    "-importcert", "-alias", "root-ca", "-file", $CaCer, "-keystore", $KEYSTORE,
    "-storetype", "PKCS12", "-storepass", $STOREPASS, "-noprompt"
) -Quiet

Write-Host "Step 8: Cleaning up temporary files..." -ForegroundColor Yellow
Remove-Item $TempDir -Recurse -Force -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "Step 9: Verifying keystore contents..." -ForegroundColor Yellow
Write-Host ""
Invoke-Keytool -Arguments @(
    "-list", "-keystore", $KEYSTORE, "-storetype", "PKCS12", "-storepass", $STOREPASS
)

Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "Keystore created successfully!" -ForegroundColor Green
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "What changed:" -ForegroundColor Yellow
Write-Host "  - The keystore was written to keystore.p12 in the project root" -ForegroundColor Gray
Write-Host "  - The CA certificate was exported to processing-server-ca.cer" -ForegroundColor Gray
Write-Host "  - SAN entries include localhost, 127.0.0.1, $HostnameLocal, and $LocalIP" -ForegroundColor Gray
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Trust processing-server-ca.cer on devices that will open the HTTPS URL." -ForegroundColor Gray
Write-Host "  2. Run: .\run-https.ps1" -ForegroundColor Gray
Write-Host "  3. Open https://$HostnameLocal`:8443/ or https://$LocalIP`:8443/" -ForegroundColor Gray
