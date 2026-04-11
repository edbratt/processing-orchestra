#!/bin/bash
# Generate SSL keystore for Processing Server
# Usage: ./create-keystore.sh [IP_ADDRESS]
# Example: ./create-keystore.sh 192.168.1.100

set -e

# Configuration
KEYSTORE="keystore.p12"
STOREPASS="changeit"
VALIDITY_CA=3650
VALIDITY_SERVER=365

# Detect local IP if not provided
if [ -z "$1" ]; then
    # Try to detect local IP
    if command -v hostname &> /dev/null; then
        # Linux
        LOCAL_IP=$(hostname -I | awk '{print $1}')
    elif command -v ipconfig &> /dev/null; then
        # macOS fallback
        LOCAL_IP=$(ipconfig getifaddr en0 2>/dev/null || echo "")
    fi
    
    if [ -z "$LOCAL_IP" ]; then
        echo "Could not detect local IP address."
        echo "Usage: $0 <YOUR_LOCAL_IP>"
        echo "Example: $0 192.168.1.100"
        exit 1
    fi
else
    LOCAL_IP="$1"
fi

echo "=============================================="
echo "Creating SSL Keystore for Processing Server"
echo "=============================================="
echo ""
echo "Local IP: $LOCAL_IP"
echo "Keystore: $KEYSTORE"
echo ""

# Check for keytool
if ! command -v keytool &> /dev/null; then
    echo "ERROR: keytool not found. Please install Java JDK."
    exit 1
fi

# Set JAVA_HOME if not set
if [ -z "$JAVA_HOME" ]; then
    # Try to find Java
    if [ -d "/usr/lib/jvm/jdk-26" ]; then
        export JAVA_HOME=/usr/lib/jvm/jdk-26
    elif [ -d "/usr/lib/jvm/java-26-openjdk" ]; then
        export JAVA_HOME=/usr/lib/jvm/java-26-openjdk
    elif [ -d "/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home" ]; then
        export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home
    fi
fi

# Remove existing keystore
if [ -f "$KEYSTORE" ]; then
    echo "Removing existing keystore..."
    rm "$KEYSTORE"
fi

# Remove temporary files if they exist
rm -f server.csr server.cer ca.cer

echo "Step 1: Creating CA certificate (valid for $VALIDITY_CA days)..."
keytool -genkeypair -alias root-ca -keyalg RSA -keysize 2048 -validity $VALIDITY_CA \
    -keystore "$KEYSTORE" -storetype PKCS12 -storepass "$STOREPASS" \
    -dname "CN=ProcessingServer-CA,O=Dev,C=US" \
    -ext BasicConstraints:critical,ca:true,pathlen:1

echo "Step 2: Creating server certificate (valid for $VALIDITY_SERVER days)..."
keytool -genkeypair -alias "1" -keyalg RSA -keysize 2048 -validity $VALIDITY_SERVER \
    -keystore "$KEYSTORE" -storetype PKCS12 -storepass "$STOREPASS" \
    -dname "CN=localhost,O=Dev,C=US" \
    -ext SAN="DNS:localhost,IP:127.0.0.1,IP:$LOCAL_IP"

echo "Step 3: Generating certificate signing request..."
keytool -certreq -alias "1" -keystore "$KEYSTORE" -storetype PKCS12 \
    -storepass "$STOREPASS" -file server.csr

echo "Step 4: Signing server certificate with CA..."
keytool -gencert -alias root-ca -keystore "$KEYSTORE" -storetype PKCS12 \
    -storepass "$STOREPASS" -infile server.csr -outfile server.cer -validity $VALIDITY_SERVER \
    -ext SAN="DNS:localhost,IP:127.0.0.1,IP:$LOCAL_IP"

echo "Step 5: Importing signed server certificate..."
keytool -importcert -alias "1" -keystore "$KEYSTORE" -storetype PKCS12 \
    -storepass "$STOREPASS" -file server.cer

echo "Step 6: Exporting CA certificate..."
keytool -exportcert -alias root-ca -keystore "$KEYSTORE" -storetype PKCS12 \
    -storepass "$STOREPASS" -file ca.cer

echo "Step 7: Converting CA to trusted certificate entry..."
keytool -delete -alias root-ca -keystore "$KEYSTORE" -storetype PKCS12 \
    -storepass "$STOREPASS"
keytool -importcert -alias root-ca -file ca.cer -keystore "$KEYSTORE" \
    -storetype PKCS12 -storepass "$STOREPASS" -noprompt

echo "Step 8: Cleaning up temporary files..."
rm -f server.csr server.cer ca.cer

echo ""
echo "Step 9: Verifying keystore contents..."
echo ""
keytool -list -keystore "$KEYSTORE" -storetype PKCS12 -storepass "$STOREPASS"

echo ""
echo "=============================================="
echo "Keystore created successfully!"
echo "=============================================="
echo ""
echo "Next steps:"
echo "  1. Copy keystore to resources:"
echo "     cp $KEYSTORE src/main/resources/$KEYSTORE"
echo ""
echo "  2. Rebuild the project:"
echo "     mvn clean package -DskipTests"
echo ""
echo "  3. Run the server:"
echo "     ./run.sh"
echo ""
echo "Access from:"
echo "  - Local:    https://localhost:8080/"
echo "  - Mobile:   https://$LOCAL_IP:8080/"