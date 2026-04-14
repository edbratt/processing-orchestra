#!/bin/bash
# Generate SSL keystore for Processing Server
# Usage: ./create-keystore.sh [IP_ADDRESS] [--force]
# Example: ./create-keystore.sh 192.168.1.100 --force

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
KEYSTORE="$PROJECT_ROOT/keystore.p12"
CA_CERT="$PROJECT_ROOT/processing-server-ca.cer"
STOREPASS="changeit"
VALIDITY_CA=3650
VALIDITY_SERVER=365
FORCE=0
LOCAL_IP=""
HOSTNAME_LOCAL="$(hostname).local"

for arg in "$@"; do
    case "$arg" in
        --force)
            FORCE=1
            ;;
        *)
            if [ -z "$LOCAL_IP" ]; then
                LOCAL_IP="$arg"
            fi
            ;;
    esac
done

resolve_local_ip() {
    if [ -n "$LOCAL_IP" ]; then
        return
    fi

    if command -v hostname >/dev/null 2>&1; then
        LOCAL_IP=$(hostname -I 2>/dev/null | awk '{for (i = 1; i <= NF; i++) if ($i !~ /^127\./ && $i !~ /^169\.254\./) { print $i; exit }}')
    fi

    if [ -z "$LOCAL_IP" ] && command -v ipconfig >/dev/null 2>&1; then
        LOCAL_IP=$(ipconfig getifaddr en0 2>/dev/null || true)
    fi

    if [ -z "$LOCAL_IP" ] && command -v ip >/dev/null 2>&1; then
        LOCAL_IP=$(ip -4 addr show scope global 2>/dev/null | awk '/inet / {sub(/\/.*/, "", $2); print $2; exit}')
    fi
}

resolve_local_ip

if [ -z "$LOCAL_IP" ]; then
    echo "Could not detect a local IPv4 address."
    echo "Usage: $0 <YOUR_LOCAL_IP> [--force]"
    exit 1
fi

if ! command -v keytool >/dev/null 2>&1; then
    echo "ERROR: keytool not found. Please install a Java JDK."
    exit 1
fi

if [ -f "$KEYSTORE" ] && [ "$FORCE" -ne 1 ]; then
    echo "Keystore already exists at $KEYSTORE"
    echo "Re-run with --force to overwrite it."
    exit 1
fi

TEMP_DIR="$PROJECT_ROOT/.keystore-tmp"
SERVER_CSR="$TEMP_DIR/server.csr"
SERVER_CER="$TEMP_DIR/server.cer"
CA_CER="$TEMP_DIR/ca.cer"
SAN="DNS:localhost,DNS:$HOSTNAME_LOCAL,IP:127.0.0.1,IP:$LOCAL_IP"

mkdir -p "$TEMP_DIR"
rm -f "$KEYSTORE" "$CA_CERT" "$SERVER_CSR" "$SERVER_CER" "$CA_CER"

echo "=============================================="
echo "Creating SSL Keystore for Processing Server"
echo "=============================================="
echo ""
echo "Local IP:       $LOCAL_IP"
echo "Hostname:       $HOSTNAME_LOCAL"
echo "Keystore path:  $KEYSTORE"
echo "CA cert path:   $CA_CERT"
echo ""

echo "Step 1: Creating CA certificate (valid for $VALIDITY_CA days)..."
keytool -genkeypair -alias root-ca -keyalg RSA -keysize 2048 -validity $VALIDITY_CA \
    -keystore "$KEYSTORE" -storetype PKCS12 -storepass "$STOREPASS" \
    -dname "CN=ProcessingServer-CA,O=Dev,C=US" \
    -ext BasicConstraints:critical,ca:true,pathlen:1 >/dev/null

echo "Step 2: Creating server certificate (valid for $VALIDITY_SERVER days)..."
keytool -genkeypair -alias "1" -keyalg RSA -keysize 2048 -validity $VALIDITY_SERVER \
    -keystore "$KEYSTORE" -storetype PKCS12 -storepass "$STOREPASS" \
    -dname "CN=localhost,O=Dev,C=US" \
    -ext SAN="$SAN" >/dev/null

echo "Step 3: Generating certificate signing request..."
keytool -certreq -alias "1" -keystore "$KEYSTORE" -storetype PKCS12 \
    -storepass "$STOREPASS" -file "$SERVER_CSR" >/dev/null

echo "Step 4: Signing server certificate with CA..."
keytool -gencert -alias root-ca -keystore "$KEYSTORE" -storetype PKCS12 \
    -storepass "$STOREPASS" -infile "$SERVER_CSR" -outfile "$SERVER_CER" -validity $VALIDITY_SERVER \
    -ext SAN="$SAN" >/dev/null

echo "Step 5: Importing signed server certificate..."
keytool -importcert -alias "1" -keystore "$KEYSTORE" -storetype PKCS12 \
    -storepass "$STOREPASS" -file "$SERVER_CER" >/dev/null

echo "Step 6: Exporting CA certificate..."
keytool -exportcert -alias root-ca -keystore "$KEYSTORE" -storetype PKCS12 \
    -storepass "$STOREPASS" -rfc -file "$CA_CER" >/dev/null
cp "$CA_CER" "$CA_CERT"

echo "Step 7: Converting CA to trusted certificate entry..."
keytool -delete -alias root-ca -keystore "$KEYSTORE" -storetype PKCS12 \
    -storepass "$STOREPASS" >/dev/null
keytool -importcert -alias root-ca -file "$CA_CER" -keystore "$KEYSTORE" \
    -storetype PKCS12 -storepass "$STOREPASS" -noprompt >/dev/null

echo "Step 8: Cleaning up temporary files..."
rm -rf "$TEMP_DIR"

echo ""
echo "Step 9: Verifying keystore contents..."
echo ""
keytool -list -keystore "$KEYSTORE" -storetype PKCS12 -storepass "$STOREPASS"

echo ""
echo "=============================================="
echo "Keystore created successfully!"
echo "=============================================="
echo ""
echo "What changed:"
echo "  - The keystore was written to keystore.p12 in the project root"
echo "  - The CA certificate was exported to processing-server-ca.cer"
echo "  - SAN entries include localhost, 127.0.0.1, $HOSTNAME_LOCAL, and $LOCAL_IP"
echo ""
echo "Next steps:"
echo "  1. Trust processing-server-ca.cer on devices that will open the HTTPS URL."
echo "  2. Run: ./run-https.sh"
echo "  3. Open https://$HOSTNAME_LOCAL:8443/ or https://$LOCAL_IP:8443/"
