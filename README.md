# Processing Server - Multi-User Web Controller

[![CI](https://github.com/edbratt/processing-orchestra/actions/workflows/ci.yml/badge.svg)](https://github.com/edbratt/processing-orchestra/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A multi-user web-based controller for Processing.org visual sketches. Multiple users can connect via web browsers, interact with UI controls (touch areas, sliders, buttons), stream audio, and use phone motion sensors, all aggregated in real-time on a server-side Processing canvas.

## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [Browser UI](#browser-ui)
- [API Endpoints](#api-endpoints)
- [Troubleshooting](#troubleshooting)
- [Development](#development)
- [Architecture](#architecture)
- [Customization](#customization)
- [Next Steps](#next-steps)
- [Technology Stack](#technology-stack)
- [Acknowledgments](#acknowledgments)

## Features

- **Multi-user support**: Multiple concurrent browser clients
- **Real-time WebSocket communication**: Low-latency bidirectional messaging
- **Touch/mouse input**: Position tracking with visual feedback
- **Audio streaming**: Per-user audio level visualization
- **Phone motion input**: Tilt and shake from supported mobile browsers over HTTPS
- **HTTPS/TLS**: Secure connections required for mobile microphone access
- **Cross-device**: Works on desktop and mobile browsers

## Prerequisites

Contents:
- [Required Software](#required-software)
- [Environment Setup](#environment-setup)
- [Verify Prerequisites](#verify-prerequisites)

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

Contents:
- [1. Clone the Repository](#1-clone-the-repository)
- [2. Try It Locally Over HTTP](#2-try-it-locally-over-http)
- [3. Enable HTTPS for LAN or Mobile Browsers](#3-enable-https-for-lan-or-mobile-browsers)
- [4. Build the Project](#4-build-the-project)
- [5. Run the Server](#5-run-the-server)
- [6. Access the Application](#6-access-the-application)

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

### 2. Try It Locally Over HTTP

No certificate is required for local testing on this machine.

**Windows (PowerShell):**
```powershell
.\run.ps1
```

**Mac/Linux (Bash):**
```bash
./run.sh
```

Open [http://localhost:8080](http://localhost:8080).

### 3. Enable HTTPS for LAN or Mobile Browsers

HTTPS is only needed when you want to connect from another device on your LAN, especially for browser microphone access.

**Windows (PowerShell):**
```powershell
# Auto-detect your local IP and write keystore.p12 to the project root
.\create-keystore.ps1

# Or specify your IP manually
.\create-keystore.ps1 192.168.1.100

# Use -Force to overwrite an existing keystore
.\create-keystore.ps1 -Force
```

**Mac/Linux (Bash):**
```bash
chmod +x create-keystore.sh

# Auto-detect your local IP and write keystore.p12 to the project root
./create-keystore.sh

# Or specify your IP manually
./create-keystore.sh 192.168.1.100

# Use --force to overwrite an existing keystore
./create-keystore.sh --force
```

**What the script does:**
1. Writes `keystore.p12` to the project root.
2. Exports the CA certificate as `processing-server-ca.cer`.
3. Adds SAN entries for `localhost`, `127.0.0.1`, your current LAN IP, and `hostname.local`.
4. Leaves local HTTP unchanged and enables HTTPS only when you launch with the HTTPS config overlay.

**To turn HTTPS on:**
1. Run one of the keystore scripts.
2. Trust `processing-server-ca.cer` on the devices that will use the HTTPS URL.
3. Start the server with `.\run-https.ps1` or `./run-https.sh`.
4. Open `https://<hostname>.local:8443/` or `https://<LAN-IP>:8443/`.

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

**Packaging note:**
- This project intentionally builds an executable shaded jar so it can be launched with `java -jar target/processing-server-<version>.jar`
- That differs from the stock Helidon example layout in `processing-client`, which follows the thinner `target/libs` runtime pattern
- The tradeoff is a larger packaged jar and more shading warnings during build in exchange for a simpler launch experience for users
- For this application, that tradeoff is intentional because the app is used more like a runnable desktop/server tool than a minimal framework example

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

**Option A2: Using shell script with HTTPS**

*Windows (PowerShell):*
```powershell
.\run-https.ps1
```

*Mac/Linux (Bash):*
```bash
./run-https.sh
```

**Option B: Using Maven**

*Windows (PowerShell):*
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"  # or jdk-21 minimum
mvn package -DskipTests
java -jar .\target\processing-server-1.0-SNAPSHOT.jar
```

*Mac/Linux (Bash):*
```bash
export JAVA_HOME=/usr/lib/jvm/jdk-25  # or jdk-21 minimum
mvn package -DskipTests
java -jar ./target/processing-server-1.0-SNAPSHOT.jar
```

**Option C: Using Java directly**

*Windows (PowerShell):*
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"  # or jdk-21 minimum
java -jar .\target\processing-server-1.0-SNAPSHOT.jar
```

*Mac/Linux (Bash):*
```bash
export JAVA_HOME=/usr/lib/jvm/jdk-25  # or jdk-21 minimum
java -jar ./target/processing-server-1.0-SNAPSHOT.jar
```

### 6. Access the Application

| Access Point | URL |
|--------------|-----|
| **Local UI** | http://localhost:8080/ |
| **Local API** | http://localhost:8080/api/status |
| **LAN/Mobile UI** | https://YOUR_HOSTNAME.local:8443/ or https://YOUR_IP:8443/ |
| **WebSocket** | ws://localhost:8080/ws locally, wss://YOUR_IP:8443/ws over HTTPS |

**First-time local access:**
1. Open `http://localhost:8080/`
2. No certificate is required for same-machine testing

**Mobile device access:**
1. Generate the keystore and trust `processing-server-ca.cer`
2. Start the server with `.\run-https.ps1` or `./run-https.sh`
4. On mobile, navigate to `https://YOUR_IP:8443/` or `https://YOUR_HOSTNAME.local:8443/`
5. Accept the certificate warning if the CA is not yet trusted

## Project Structure

The keystore lives at the project root, and HTTPS is enabled through `config/application-https.yaml` plus `-Dapp.config=...` at launch time.

```text
processing-server/
|-- pom.xml                           # Maven configuration
|-- README.md                         # This file
|-- ARCHITECTURE.md                   # Detailed architecture documentation
|-- CUSTOMIZATION.md                  # Guide for modifying and extending
|-- TODO.md                           # Follow-up tasks and ideas
|-- run.ps1                           # PowerShell run script
|-- run.sh                            # Bash run script
|-- run-https.ps1                     # PowerShell HTTPS launch script
|-- run-https.sh                      # Bash HTTPS launch script
|-- create-keystore.ps1               # PowerShell keystore generator
|-- create-keystore.sh                # Bash keystore generator
|-- keystore.p12                      # Generated TLS keystore
|-- processing-server-ca.cer          # Exported CA certificate
|-- config/
|   `-- application-https.yaml        # HTTPS overlay config for -Dapp.config
|-- src/
|   `-- main/
|       |-- java/com/processing/server/
|       |   |-- Main.java             # Entry point and socket configuration
|       |   |-- ProcessingSketch.java # Processing canvas behavior
|       |   |-- WebSocketHandler.java # WebSocket session and message handling
|       |   |-- InputService.java     # REST API endpoints
|       |   |-- SessionManager.java   # User session tracking
|       |   |-- EventQueue.java       # Thread-safe event queue
|       |   |-- AudioBuffer.java      # Per-session audio buffering
|       |   |-- AudioConfig.java      # Audio configuration
|       |   |-- DebugConfig.java      # Debug logging settings
|       |   `-- UserInputEvent.java   # Event data structure
|       `-- resources/
|           |-- application.yaml      # Default local HTTP configuration
|           `-- static/
|               `-- index.html        # Browser UI
`-- target/                           # Build output created after the first Maven build
```

`target/` is created by Maven after the first build. It is not part of the files cloned from GitHub.

## Configuration

Contents:
- [application.yaml](#applicationyaml)
- [Motion Configuration](#motion-configuration)
- [Changing the Port](#changing-the-port)
- [Changing Audio Quality](#changing-audio-quality)

### application.yaml

```yaml
server:
  port: 8080                        # HTTP port
  host: "127.0.0.1"                 # Local-only HTTP listener

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

### Motion Configuration

Phone motion input is configured in `src/main/resources/application.yaml`:

```yaml
motion:
  update-hz: 20
  clamp:
    alpha-degrees: 180
    beta-degrees: 60
    gamma-degrees: 60
    acceleration-g: 3.0
    magnitude-g: 4.0
  mapping:
    tilt-offset-normalized: 0.12
    shake-threshold-g: 0.6
    shake-burst-scale: 1.8
  debug:
    logging: false
    sample-limit: 5
```

Adjustable motion settings:
- `motion.update-hz`: browser send rate for combined motion samples
- `motion.clamp.beta-degrees` and `motion.clamp.gamma-degrees`: max tilt accepted by the server
- `motion.clamp.acceleration-g`: max absolute acceleration retained per axis
- `motion.clamp.magnitude-g`: max retained acceleration magnitude
- `motion.mapping.tilt-offset-normalized`: how far tilt can offset the rendered position around the touch target
- `motion.mapping.shake-threshold-g`: minimum shake signal before a burst is triggered
- `motion.mapping.shake-burst-scale`: scales the shake-driven burst intensity
- `motion.debug.logging`: enables a few motion debug samples in the server/sketch

Important:
- this file is packaged into the jar
- changing these values requires a rebuild with `mvn clean package -DskipTests`

### Changing the Port

Edit `application.yaml` or pass system property:

**Windows (PowerShell):**
```powershell
java -Dserver.port=9090 -jar .\target\processing-server-1.0-SNAPSHOT.jar
```

**Mac/Linux (Bash):**
```bash
java -Dserver.port=9090 -jar ./target/processing-server-1.0-SNAPSHOT.jar
```

### Changing Audio Quality

Edit `application.yaml`:

```yaml
audio:
  mode: "voice"  # Options: high-quality-stereo, high-quality-mono, voice
```

## Browser UI

Contents:
- [Enabling Debug Mode](#enabling-debug-mode)

The web interface (`index.html`) provides:

1. **Touch Area**: Drag to move your circle on the canvas
2. **Size Slider**: Adjust the core circle size while keeping the outer audio ring proportional
3. **Speed Slider**: Adjust movement responsiveness and the decay speed of temporary effects
4. **Action Buttons**: `Burst`, `Spin Color`, and `Scatter` trigger visual effects
5. **Audio Input**: Start or stop microphone audio (HTTPS is required for mobile and remote browsers)
6. **Audio Gain Slider**: Attenuate or amplify how strongly incoming audio drives the sketch
7. **Motion Input**: Enable phone motion sensors, view live tilt values, and send tilt/shake control over HTTPS
8. **Motion Trim Slider**: Adjust motion sensitivity in the browser before motion samples are sent to the server

The touch area uses `touch-action: none`, which helps avoid accidental browser text selection or gesture interference while dragging.

The motion trim is a client-only setting. It scales the browser's outgoing motion sample before transmission, so different devices can feel less or more responsive without changing the shared server config.

### Enabling Debug Mode

Add `?debug` to the URL:

```
http://localhost:8080/?debug
```

This enables console logging in the browser.

## API Endpoints

Contents:
- [REST Endpoints](#rest-endpoints)
- [WebSocket Endpoint](#websocket-endpoint)

### REST Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/session` | Create new session (returns JSON with sessionId) |
| `GET` | `/api/status` | Get status (session count, queue size) |
| `POST` | `/api/event` | Submit event (requires sessionId in body) |

### WebSocket Endpoint

Connect to `ws://localhost:8080/ws` for local HTTP, or `wss://localhost:8443/ws` when launched with `-Dapp.config=config/application-https.yaml`.

**Server -> Client Messages:**

```json
{
  "type": "session",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

```json
{
  "type": "server-shutdown",
  "reason": "Processing Server is shutting down."
}
```

**Client -> Server Messages:**

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

```json
{
  "type": "motion",
  "controlId": "deviceMotion",
  "alpha": 12.0,
  "beta": -8.5,
  "gamma": 24.0,
  "ax": 0.1,
  "ay": 0.4,
  "az": 1.0,
  "magnitude": 1.08,
  "timestamp": 1234567890
}
```

**Audio is sent as binary WebSocket frames** (not JSON).

## Troubleshooting

Contents:
- ["Port already in use"](#port-already-in-use)
- ["SSL handshake failure" / "Can't connect"](#ssl-handshake-failure--cant-connect)
- ["Session ID: connecting..." or controls do nothing](#session-id-connecting-or-controls-do-nothing)
- ["The browser says the server disconnected"](#the-browser-says-the-server-disconnected)
- ["Microphone not working on mobile"](#microphone-not-working-on-mobile)
- ["Motion works, but shake is weak or invisible"](#motion-works-but-shake-is-weak-or-invisible)
- ["Audio not streaming"](#audio-not-streaming)
- ["Users initialize on same position"](#users-initialize-on-same-position)
- ["Java version errors"](#java-version-errors)
- ["Maven not found"](#maven-not-found)
- ["IP address changed / certificate invalid"](#ip-address-changed--certificate-invalid)

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
1. `keystore.p12` exists in the project root
2. The server was started with `.\run-https.ps1`, `./run-https.sh`, or `-Dapp.config=config/application-https.yaml`
3. Keystore has proper structure (CA + signed server cert)
4. Server started successfully (check console output)

**Verify TLS is working:**

```powershell
# Local HTTP
curl http://localhost:8080/api/status

# Optional HTTPS socket when started with the HTTPS config overlay
curl -k https://localhost:8443/api/status
```

```bash
# Local HTTP
curl http://localhost:8080/api/status

# Optional HTTPS socket
curl -k https://localhost:8443/api/status

# View certificate details
openssl s_client -connect localhost:8443 -showcerts
```

Expected output: JSON with `activeSessions`, `queueSize`, etc.

If curl fails with SSL error, the keystore is malformed. Regenerate with `create-keystore.ps1`.

### "Session ID: connecting..." or controls do nothing

- Confirm the page is using the matching WebSocket URL for its current scheme
- Use `ws://localhost:8080/ws` for local HTTP or `wss://<host>:8443/ws` for HTTPS
- Check `http://localhost:8080/api/status` or `https://localhost:8443/api/status`
- If the page is open and connected, `activeSessions` should be greater than `0`

### "The browser says the server disconnected"

- On a clean shutdown, the server sends a final `server-shutdown` WebSocket message before closing connections
- The browser shows a banner and scrolls back to the top so the notice is visible
- On an unexpected disconnect, the browser also shows a banner and attempts to reconnect

### "Microphone not working on mobile"

- Must use HTTPS (not HTTP)
- Local `http://localhost:8080` works on the same machine but not for mobile microphone access
- Must accept certificate warning
- Browser requires user gesture before microphone access

### "Motion works, but shake is weak or invisible"

- Motion tuning in `src/main/resources/application.yaml` requires a rebuild
- Restart after `mvn clean package -DskipTests`
- Try lowering `motion.mapping.shake-threshold-g`
- Try raising `motion.mapping.shake-burst-scale`
- Enable `motion.debug.logging: true` to inspect sample values

The current shake effect is driven by:
- acceleration magnitude change
- axis-to-axis acceleration change

So short sharp shakes should produce a stronger burst than slow movement.

### "Audio not streaming"

- Check browser console for errors
- Verify HTTPS is active
- Try manual audio start: click "Start Audio" button
- Check debug output: `http://localhost:8080/?debug`

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
   .\run-https.ps1
   ```

2. **Use hostname instead of IP**:

   The keystore scripts already include `HOSTNAME.local` in the SAN list.

   Then connect via: `https://YOURHOSTNAME.local:8443/`

   Works on most networks with mDNS/Bonjour (Windows 10+, macOS, Linux with avahi).

3. **Accept browser warnings** (not recommended for classrooms):
   
   Proceed past the security warning. The connection still encrypts, but users see a scary warning.

4. **Use a real domain with Let's Encrypt** (production):
   
   Requires a registered domain and DNS pointing to your server.

## Development

Contents:
- [Building Without Running Tests](#building-without-running-tests)
- [Running Tests](#running-tests)
- [Debug Logging](#debug-logging)

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
java -Ddebug.logging=true -jar .\target\processing-server-1.0-SNAPSHOT.jar
```

**Mac/Linux (Bash):**
```bash
java -Ddebug.logging=true -jar ./target/processing-server-1.0-SNAPSHOT.jar
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


