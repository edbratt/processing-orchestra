# Processing Server - Multi-User Web Controller

A multi-user web-based controller for Processing.org visual sketches. Multiple users can connect via web browsers, interact with UI controls (touch areas, sliders, buttons), and stream audio - all aggregated in real-time on a server-side Processing canvas.

## Features

- **Multi-user support**: Multiple concurrent browser clients
- **Real-time WebSocket communication**: Low-latency bidirectional messaging
- **Touch/mouse input**: Position tracking with visual feedback
- **Audio streaming**: Per-user audio level visualization
- **HTTPS/TLS**: Secure connections required for mobile microphone access
- **Cross-device**: Works on desktop and mobile browsers

## Prerequisites

### Required Software

| Software | Version | Download |
|----------|---------|----------|
| **Java JDK** | 21 minimum, 25+ recommended | [Adoptium](https://adoptium.net/) or [Oracle](https://www.oracle.com/java/technologies/downloads/) |
| **Maven** | 3.9+ | [Apache Maven](https://maven.apache.org/download.cgi) |
| **Git** | Latest | [Git SCM](https://git-scm.com/downloads) |

### Environment Setup

**Windows (PowerShell):**
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"  # or jdk-21 minimum
```

**Mac/Linux (Bash):**
```bash
export JAVA_HOME=/usr/lib/jvm/jdk-25  # or jdk-21 minimum  # or wherever JDK is installed
# Add to ~/.bashrc or ~/.zshrc for persistence
```

### Verify Prerequisites

**Windows (PowerShell):**
```powershell
java -version
mvn -version
echo $env:JAVA_HOME
```

**Mac/Linux (Bash):**
```bash
java -version
mvn -version
echo $JAVA_HOME
```

## Getting Started

### 1. Clone the Repository

**Windows (PowerShell):**
```powershell
cd C:\Users\yourname\Dev
git clone <repository-url> processing-server
cd processing-server
```

**Mac/Linux (Bash):**
```bash
cd ~/Dev
git clone <repository-url> processing-server
cd processing-server
```

### 2. Create SSL Certificate

HTTPS is required for mobile microphone access. Create a self-signed certificate using the provided script:

**Windows (PowerShell):**
```powershell
# Auto-detect your local IP
.\create-keystore.ps1

# Or specify your IP manually
.\create-keystore.ps1 192.168.1.100
```

**Mac/Linux (Bash):**
```bash
# Make the script executable (first time only)
chmod +x create-keystore.sh

# Auto-detect your local IP
./create-keystore.sh

# Or specify your IP manually
./create-keystore.sh 192.168.1.100
```

**What the script does:**
1. Creates a Certificate Authority (CA) certificate (valid for 10 years)
2. Creates a server certificate signed by the CA (valid for 1 year)
3. Adds Subject Alternative Names (SAN) for:
   - `DNS:localhost`
   - `IP:127.0.0.1`
   - `IP:YOUR_LOCAL_IP` (for mobile/remote access)
4. Generates `keystore.p12` with proper certificate chain

**Manual Creation (if script doesn't work):**

<details>
<summary>Click to expand manual keystore creation steps</summary>

**Windows (PowerShell):**
```powershell
# Set JAVA_HOME if needed
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"  # or jdk-21 minimum

# 1. Create CA certificate (valid for 10 years)
keytool -genkeypair -alias root-ca -keyalg RSA -keysize 2048 -validity 3650 `
    -keystore keystore.p12 -storetype PKCS12 -storepass changeit `
    -dname "CN=ProcessingServer-CA,O=Dev,C=US" `
    -ext BasicConstraints:critical,ca:true,pathlen:1

# 2. Create server key (valid for 1 year)
# Replace 192.168.1.100 with YOUR local network IP
keytool -genkeypair -alias "1" -keyalg RSA -keysize 2048 -validity 365 `
    -keystore keystore.p12 -storetype PKCS12 -storepass changeit `
    -dname "CN=localhost,O=Dev,C=US" `
    -ext SAN="DNS:localhost,IP:127.0.0.1,IP:192.168.1.100"

# 3. Generate certificate signing request
keytool -certreq -alias "1" -keystore keystore.p12 -storetype PKCS12 `
    -storepass changeit -file server.csr

# 4. Sign server certificate with CA
keytool -gencert -alias root-ca -keystore keystore.p12 -storetype PKCS12 `
    -storepass changeit -infile server.csr -outfile server.cer -validity 365 `
    -ext SAN="DNS:localhost,IP:127.0.0.1,IP:192.168.1.100"

# 5. Import signed certificate
keytool -importcert -alias "1" -keystore keystore.p12 -storetype PKCS12 `
    -storepass changeit -file server.cer

# 6. Export CA certificate
keytool -exportcert -alias root-ca -keystore keystore.p12 -storetype PKCS12 `
    -storepass changeit -file ca.cer

# 7. Convert CA to trusted certificate entry
keytool -delete -alias root-ca -keystore keystore.p12 -storetype PKCS12 `
    -storepass changeit
keytool -importcert -alias root-ca -file ca.cer -keystore keystore.p12 `
    -storetype PKCS12 -storepass changeit -noprompt

# 8. Clean up temporary files
del server.csr, server.cer, ca.cer

# 9. Verify keystore (should show 2 entries: "1" and "root-ca")
keytool -list -keystore keystore.p12 -storetype PKCS12 -storepass changeit
```

**Mac/Linux (Bash):**
```bash
# Set JAVA_HOME if needed
export JAVA_HOME=/usr/lib/jvm/jdk-25  # or jdk-21 minimum  # adjust as needed

# 1. Create CA certificate (valid for 10 years)
keytool -genkeypair -alias root-ca -keyalg RSA -keysize 2048 -validity 3650 \
    -keystore keystore.p12 -storetype PKCS12 -storepass changeit \
    -dname "CN=ProcessingServer-CA,O=Dev,C=US" \
    -ext BasicConstraints:critical,ca:true,pathlen:1

# 2. Create server key (valid for 1 year)
# Replace 192.168.1.100 with YOUR local network IP
keytool -genkeypair -alias "1" -keyalg RSA -keysize 2048 -validity 365 \
    -keystore keystore.p12 -storetype PKCS12 -storepass changeit \
    -dname "CN=localhost,O=Dev,C=US" \
    -ext SAN="DNS:localhost,IP:127.0.0.1,IP:192.168.1.100"

# 3. Generate certificate signing request
keytool -certreq -alias "1" -keystore keystore.p12 -storetype PKCS12 \
    -storepass changeit -file server.csr

# 4. Sign server certificate with CA
keytool -gencert -alias root-ca -keystore keystore.p12 -storetype PKCS12 \
    -storepass changeit -infile server.csr -outfile server.cer -validity 365 \
    -ext SAN="DNS:localhost,IP:127.0.0.1,IP:192.168.1.100"

# 5. Import signed certificate
keytool -importcert -alias "1" -keystore keystore.p12 -storetype PKCS12 \
    -storepass changeit -file server.cer

# 6. Export CA certificate
keytool -exportcert -alias root-ca -keystore keystore.p12 -storetype PKCS12 \
    -storepass changeit -file ca.cer

# 7. Convert CA to trusted certificate entry
keytool -delete -alias root-ca -keystore keystore.p12 -storetype PKCS12 \
    -storepass changeit
keytool -importcert -alias root-ca -file ca.cer -keystore keystore.p12 \
    -storetype PKCS12 -storepass changeit -noprompt

# 8. Clean up temporary files
rm server.csr server.cer ca.cer

# 9. Verify keystore (should show 2 entries: "1" and "root-ca")
keytool -list -keystore keystore.p12 -storetype PKCS12 -storepass changeit
```

</details>

**Finding your local IP:**
- **Windows:** `ipconfig` (look for IPv4 Address under your active network adapter)
- **Mac:** `ipconfig getifaddr en0` (for Wi-Fi) or System Preferences → Network
- **Linux:** `hostname -I` or `ip addr`

### 3. Copy Keystore to Resources

**Windows (PowerShell):**
```powershell
Copy-Item keystore.p12 src\main\resources\keystore.p12
```

**Mac/Linux (Bash):**
```bash
cp keystore.p12 src/main/resources/keystore.p12
```

### 4. Build the Project

**Windows (PowerShell):**
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"  # or jdk-21 minimum
mvn clean package -DskipTests
```

**Mac/Linux (Bash):**
```bash
export JAVA_HOME=/usr/lib/jvm/jdk-25  # or jdk-21 minimum  # adjust as needed
mvn clean package -DskipTests
```

### 5. Run the Server

**Option A: Using shell script**

*Windows (PowerShell):*
```powershell
.\run.ps1
```

*Mac/Linux (Bash):*
```bash
./run.sh
```

**Option B: Using Maven**

*Windows (PowerShell):*
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"  # or jdk-21 minimum
mvn exec:java
```

*Mac/Linux (Bash):*
```bash
export JAVA_HOME=/usr/lib/jvm/jdk-25  # or jdk-21 minimum
mvn exec:java
```

**Option C: Using Java directly**

*Windows (PowerShell):*
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"  # or jdk-21 minimum
java --enable-preview -cp "target\classes;target\libs\*" com.processing.server.Main
```

*Mac/Linux (Bash):*
```bash
export JAVA_HOME=/usr/lib/jvm/jdk-25  # or jdk-21 minimum
java --enable-preview -cp "target/classes:target/libs/*" com.processing.server.Main
```

### 6. Access the Application

| Access Point | URL |
|--------------|-----|
| **Local UI** | https://localhost:8080/ |
| **Local API** | https://localhost:8080/api/status |
| **Mobile/Remote** | https://YOUR_IP:8080/ |
| **WebSocket** | wss://localhost:8080/ws |

**First-time access:**
1. Browser will show a security warning (self-signed certificate)
2. Click "Advanced" → "Proceed to localhost (unsafe)"
3. The browser will remember this exception

**Mobile device access:**
1. Find your computer's local IP
2. On mobile, navigate to `https://YOUR_IP:8080/`
3. Accept the certificate warning

## Project Structure

```
processing-server/
├── pom.xml                          # Maven configuration
├── README.md                        # This file (getting started)
├── ARCHITECTURE.md                  # Detailed architecture documentation
├── CUSTOMIZATION.md                 # Guide for modifying and extending
├── run.ps1                          # PowerShell run script (Windows)
├── run.sh                           # Bash run script (Mac/Linux)
├── create-keystore.ps1              # PowerShell keystore generator (Windows)
├── create-keystore.sh               # Bash keystore generator (Mac/Linux)
├── keystore.p12                     # SSL certificate (created by script)
│
├── src/
│   └── main/
│       ├── java/com/processing/server/
│       │   ├── Main.java            # Entry point, HTTPS setup
│       │   ├── ProcessingSketch.java # Visual canvas (Processing)
│       │   ├── WebSocketHandler.java # WebSocket connection handler
│       │   ├── InputService.java    # REST API endpoints
│       │   ├── SessionManager.java  # User session tracking
│       │   ├── EventQueue.java      # Thread-safe event queue
│       │   ├── AudioBuffer.java     # Per-session audio buffering
│       │   ├── AudioConfig.java     # Audio configuration
│       │   ├── DebugConfig.java     # Debug logging settings
│       │   └── UserInputEvent.java  # Event data structure
│       │
│       └── resources/
│           ├── application.yaml     # Server configuration
│           ├── keystore.p12         # SSL certificate (copy of root)
│           └── static/
│               └── index.html       # Browser UI
│
└── target/
    ├── classes/                     # Compiled Java classes
    └── libs/                        # Dependency JARs
```

## Configuration

### application.yaml

```yaml
server:
  port: 8080                        # HTTP port
  host: "0.0.0.0"                   # Bind all interfaces

processing:
  width: 800                        # Canvas width
  height: 600                       # Canvas height
  fps: 60                           # Target frame rate

audio:
  mode: "high-quality-stereo"       # Audio mode
  modes:
    high-quality-stereo:
      sample-rate: 44100
      channels: 2
      description: "44.1kHz Stereo"
    high-quality-mono:
      sample-rate: 44100
      channels: 1
      description: "44.1kHz Mono"
    voice:
      sample-rate: 22050
      channels: 1
      description: "22.05kHz Mono"
  buffer-size: 2048
  max-buffer-chunks: 20

debug:
  logging: false                    # Enable debug output
```

### Changing the Port

Edit `application.yaml` or pass system property:

**Windows (PowerShell):**
```powershell
java --enable-preview -Dserver.port=9090 -cp "target\classes;target\libs\*" com.processing.server.Main
```

**Mac/Linux (Bash):**
```bash
java --enable-preview -Dserver.port=9090 -cp "target/classes:target/libs/*" com.processing.server.Main
```

### Changing Audio Quality

Edit `application.yaml`:

```yaml
audio:
  mode: "voice"  # Options: high-quality-stereo, high-quality-mono, voice
```

## Browser UI

The web interface (`index.html`) provides:

1. **Touch Area**: Drag to move your circle on the canvas
2. **Size Slider**: Adjust circle size
3. **Speed Slider**: Adjust animation speed  
4. **Action Buttons**: Trigger visual effects
5. **Audio Input**: Stream microphone audio (requires HTTPS)

### Enabling Debug Mode

Add `?debug` to the URL:

```
https://localhost:8080/?debug
```

This enables console logging in the browser.

## API Endpoints

### REST Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/session` | Create new session (returns JSON with sessionId) |
| `GET` | `/api/status` | Get status (session count, queue size) |
| `POST` | `/api/event` | Submit event (requires sessionId in body) |

### WebSocket Endpoint

Connect to `wss://localhost:8080/ws`

**Server → Client Messages:**

```json
{
  "type": "session",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Client → Server Messages:**

```json
{
  "type": "touch",
  "controlId": "touchArea",
  "x": 0.5,
  "y": 0.3,
  "timestamp": 1234567890
}
```

```json
{
  "type": "slider",
  "controlId": "sizeSlider",
  "value": 0.75,
  "timestamp": 1234567890
}
```

**Audio is sent as binary WebSocket frames** (not JSON).

## Troubleshooting

### "Port already in use"

Another process is using port 8080. Either:
- Stop the conflicting process
- Change the port in `application.yaml`

**Windows (PowerShell):**
```powershell
netstat -ano | findstr :8080
```

**Mac/Linux (Bash):**
```bash
lsof -i :8080
# or
netstat -tulpn | grep 8080
```

### "SSL handshake failure" / "Can't connect"

Ensure:
1. Keystore exists: `keystore.p12` in project root
2. Keystore copied to: `src/main/resources/keystore.p12`
3. Keystore has proper structure (CA + signed server cert)
4. Server started successfully (check console output)

**Verify TLS is working:**

```powershell
# Test REST endpoint (accept self-signed cert with -k)
curl -k https://localhost:8080/api/status
```

```bash
# Mac/Linux - test TLS handshake
curl -k https://localhost:8080/api/status

# View certificate details
openssl s_client -connect localhost:8080 -showcerts
```

Expected output: JSON with `activeSessions`, `queueSize`, etc.

If curl fails with SSL error, the keystore is malformed. Regenerate with `create-keystore.ps1`.

### "Microphone not working on mobile"

- Must use HTTPS (not HTTP)
- Must accept certificate warning
- Browser requires user gesture before microphone access

### "Audio not streaming"

- Check browser console for errors
- Verify HTTPS is active
- Try manual audio start: click "Start Audio" button
- Check debug output: `https://localhost:8080/?debug`

### "Users initialize on same position"

Users are placed at random non-overlapping positions. If many users connect simultaneously, some overlap may occur. Refresh the browser page to reinitialize.

### "Java version errors"

Ensure Java 21+ is installed and `JAVA_HOME` is set (Java 25+ recommended for best performance):

**Windows (PowerShell):**
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"  # or jdk-21 minimum
java -version
```

**Mac/Linux (Bash):**
```bash
export JAVA_HOME=/usr/lib/jvm/jdk-25  # or jdk-21 minimum
java -version
```

No preview flags required for Java 21+.

### "Maven not found"

Install Maven or use the full path:

**Windows (PowerShell):**
```powershell
# If installed via Chocolatey
C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.14\bin\mvn

# Or add to PATH
$env:PATH += ";C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.14\bin"
```

**Mac/Linux (Bash):**
```bash
# If installed via Homebrew (Mac)
/opt/homebrew/bin/mvn

# Or add to PATH
export PATH=$PATH:/opt/homebrew/bin
```

### "IP address changed / certificate invalid"

The keystore certificate includes your local IP address in the Subject Alternative Name (SAN) extension. If your laptop connects to a different network, your IP will likely change, causing certificate validation errors in browsers.

**Current behavior:** You must regenerate the keystore when your IP changes.

**Workarounds (choose one):**

1. **Regenerate keystore** (current approach):
   ```powershell
   .\create-keystore.ps1  # auto-detects new IP
   Copy-Item keystore.p12 src\main\resources\keystore.p12
   mvn package -DskipTests
   ./run.ps1
   ```

2. **Use hostname instead of IP** (requires script modification):
   
   Add `HOSTNAME.local` to the SAN extension in `create-keystore.ps1`:
   ```powershell
   $Hostname = "$env:COMPUTERNAME.local"
   -ext "SAN=DNS:localhost,DNS:$Hostname,IP:127.0.0.1,IP:$LocalIP"
   ```
   
   Then connect via: `https://YOURHOSTNAME.local:8080/`
   
   Works on most networks with mDNS/Bonjour (Windows 10+, macOS, Linux with avahi).

3. **Accept browser warnings** (not recommended for classrooms):
   
   Proceed past the security warning. The connection still encrypts, but users see a scary warning.

4. **Use a real domain with Let's Encrypt** (production):
   
   Requires a registered domain and DNS pointing to your server.

## Development

### Building Without Running Tests

**Windows (PowerShell):**
```powershell
mvn package -DskipTests
```

**Mac/Linux (Bash):**
```bash
mvn package -DskipTests
```

### Running Tests

```bash
mvn test
```

### Debug Logging

Enable in `application.yaml`:

```yaml
debug:
  logging: true
```

Or via system property:

**Windows (PowerShell):**
```powershell
java --enable-preview -Ddebug.logging=true -cp "target\classes;target\libs\*" com.processing.server.Main
```

**Mac/Linux (Bash):**
```bash
java --enable-preview -Ddebug.logging=true -cp "target/classes:target/libs/*" com.processing.server.Main
```

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed documentation on:
- Component interactions
- Threading model
- Audio streaming implementation
- Event processing flow
- HTTPS/TLS configuration details

## Customization

See [CUSTOMIZATION.md](CUSTOMIZATION.md) for guidance on:
- Changing canvas size, colors, and audio quality
- Adding new UI controls (sliders, buttons, color pickers)
- Creating your own Processing sketches
- Using audio data for visualizations
- Adding new event types
- Complete example: Building a particle system
- Using AI coding assistants to speed up development

## Next Steps

Now that you have the server running:

1. **Try the demo**: Open multiple browser tabs/mobile devices to see multi-user interaction
2. **Customize the sketch**: See [CUSTOMIZATION.md](CUSTOMIZATION.md) for step-by-step guides
3. **Learn the architecture**: See [ARCHITECTURE.md](ARCHITECTURE.md) for technical details

## Technology Stack

| Component | Technology |
|-----------|------------|
| Web Framework | Helidon 4.4 (SE) |
| WebSocket | Helidon WebSocket |
| HTTP Server | Helidon WebServer |
| Visual Output | Processing 4.5.3 (Java) |
| Audio | Raw PCM processing |
| Build Tool | Maven |
| Java Version | JDK 21 (min), 25+ recommended |
| HTTPS | Self-signed PKCS12 certificate |

## Acknowledgments

- [Processing Foundation](https://processing.org/) - Visual arts programming
- [Helidon](https://helidon.io/) - Lightweight Java web framework
- [Oracle](https://www.oracle.com/java/) - Java Development Kit