# Processing Server Architecture

## Overview

This application is a **multi-user web-based controller** for a Processing.org visual sketch. Multiple users can connect via web browsers, interact with UI controls (touch areas, sliders, buttons), stream audio from their microphones, and their inputs are aggregated in real-time on a server-side Processing canvas.

## Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                              BROWSER (Client)                                     │
│  ┌────────────────────────────────────────────────────────────────────────────┐   │
│  │  index.html                                                                │   │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐            │   │
│  │  │ Touch Area      │  │ Sliders        │  │ Buttons         │            │   │
│  │  │ (x,y coords)    │  │ (value 0-1)    │  │ (action)        │            │   │
│  │  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘            │   │
│  │           │                    │                    │                     │   │
│  │           └────────────────────┼────────────────────┘                     │   │
│  │                                │                                          │   │
│  │  ┌─────────────────────────────▼─────────────────────────────────────┐    │   │
│  │  │                     WebSocket Connection                           │    │   │
│  │  │  wss://host/ws (HTTPS) - JSON events + binary audio                 │    │   │
│  │  └────────────────────────────────┬────────────────────────────────────┘    │   │
│  │                                 │                                          │   │
│  │  ┌───────────────────────────────▼─────────────────────────────────────┐   │   │
│  │  │                      Audio Capture                                   │   │   │
│  │  │  navigator.mediaDevices.getUserMedia({audio: true})                 │   │   │
│  │  │  AudioContext → AnalyserNode/ScriptProcessor                       │   │   │
│  │  │  Sends PCM audio (16-bit, 44.1kHz stereo) as binary WebSocket       │   │   │
│  │  └─────────────────────────────────────────────────────────────────────┘   │   │
│  └────────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────┬────────────────────────────────────────┘
                                          │
                                          │ WebSocket (real-time, bidirectional)
                                          │ - JSON: touch, slider, button events
                                          │ - Binary: PCM audio samples
                                          │
┌─────────────────────────────────────────▼────────────────────────────────────────┐
│                           JAVA SERVER (Helidon 4.4)                               │
│                                                                                   │
│  ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐                 │
│  │ WebSocketHandler│   │  InputService   │   │  SessionManager │                 │
│  │   /ws endpoint  │   │  /api endpoints │   │  (uuid->session) │                 │
│  │                 │   │                 │   │                 │                 │
│  │ - JSON events───────┼──► EventQueue   │   │                 │                 │
│  │ - Binary audio───────┼──► AudioBuffer  │   │                 │                 │
│  └────────┬────────┘   └────────┬────────┘   └────────┬────────┘                 │
│           │                     │                     │                           │
│           └─────────────────────┼─────────────────────┘                           │
│                                 │                                                 │
│           ┌─────────────────────┼─────────────────────┐                          │
│           │                     │                     │                          │
│           ▼                     ▼                     ▼                          │
│  ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐                 │
│  │   EventQueue    │   │   AudioBuffer   │   │ SessionManager  │                 │
│  │ (thread-safe)   │   │ (per-session)   │   │ (track users)   │                 │
│  │ ConcurrentLinked│   │ Map<SessionId,  │   │                 │                 │
│  │     Queue       │   │ Queue<byte[]>>  │   │                 │                 │
│  └────────┬────────┘   └────────┬────────┘   └─────────────────┘                 │
│           │                     │                                                 │
│           │ poll()             │ poll() per session                             │
│           ▼                     ▼                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │                         ProcessingSketch                                     ││
│  │  - Runs on separate Processing thread (60 FPS)                              ││
│  │  - Processes all queued touch/slider/button events each frame               ││
│  │  - Processes audio levels from ALL sessions (per-user visualization)       ││
│  │  - Draws all users on shared canvas with audio-reactive circles            ││
│  │                                                                             ││
│  │  User Visualization:                                                        ││
│  │  ┌─────────────────────────────────────────────────────────────┐            ││
│  │  │                                                              │            ││
│  │  │    ● User1 (audio level affects circle size)                │            ││
│  │  │       ○ pulsing ring based on audio amplitude               │            ││
│  │  │                                                              │            ││
│  │  │              ● User2                                        │            ││
│  │  │                 ○                                           │            ││
│  │  │                                                              │            ││
│  │  │   ● User3 (position initialized non-overlapping)            │            ││
│  │  │      ○                                                      │            ││
│  │  │                                                              │            ││
│  │  └─────────────────────────────────────────────────────────────┘            ││
│  │                                                                             ││
│  │  ┌─────────────────────────────────────────────────────────────┐            ││
│  │  │         Global Audio Meter (averaged across all users)      │            ││
│  │  │         ▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░               │            ││
│  │  └─────────────────────────────────────────────────────────────┘            ││
│  └─────────────────────────────────────────────────────────────────────────────┘│
│                                                                                   │
└───────────────────────────────────────────────────────────────────────────────────┘
```

---

## Data Flow

### Touch/Slider/Button Events (JSON)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        USER TOUCHES/MOVES MOUSE                               │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                   JavaScript touchmove/mousemove Handler                     │
│                 Calculate normalized x,y (0.0 to 1.0)                         │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                        WebSocket.send(JSON)                                   │
│      {"type":"touch","controlId":"touchArea","x":0.5,"y":0.7,...}           │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │
                                    │ Network (WebSocket frame)
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                     WebSocketHandler.onMessage()                              │
│              Parse JSON → UserInputEvent → EventQueue.push()                 │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │
                                    │ Thread-safe queue insertion
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                              EventQueue                                       │
│           [event1] → [event2] → [event3] → ...                              │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │
                                    │ poll() during frame render
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                       ProcessingSketch.draw()                                 │
│        Update user position/color → Render circle at x,y                    │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Audio Stream (Binary)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    USER GRANTS MICROPHONE ACCESS                             │
│                   (requires HTTPS for mobile)                                │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                 AudioContext + getUserMedia({audio: true})                   │
│                    Create AnalyserNode or ScriptProcessor                    │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                 onaudioprocess (or AnalyserNode.getFloatTimeDomainData)      │
│             Convert Float32Array → Int16Array (PCM 16-bit)                  │
│             Send as ArrayBuffer (binary) via WebSocket                       │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │
                                    │ Network (WebSocket binary frame)
                                    │ ~44,100 samples/sec per user
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                  WebSocketHandler.onMessage(WsSession, BufferData)           │
│              Extract sessionId → AudioBuffer.push(sessionId, byte[])        │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │
                                    │ Per-session queue insertion
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                              AudioBuffer                                      │
│   ┌──────────┐  ┌──────────┐  ┌──────────┐                                  │
│   │Session 1 │  │Session 2 │  │Session N │                                  │
│   │[byte[][]]│  │[byte[][]]│  │[byte[][]]│                                  │
│   └──────────┘  └──────────┘  └──────────┘                                  │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │
                                    │ poll(sessionId) per active session
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                       ProcessingSketch.processAudio()                         │
│   For each session:                                                           │
│     1. Poll audio bytes from AudioBuffer                                      │
│     2. Convert to PCM samples (int16)                                         │
│     3. Calculate RMS amplitude (0.0 to 1.0)                                   │
│     4. Store per-user audio level for visualization                           │
│     5. Average all levels for global audio meter                              │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │
                                    │ Update visual elements
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                         Visual Output                                         │
│   - Each user's circle size scales with their audio level                    │
│   - Pulsing ring around circle shows audio activity                           │
│   - Global meter shows averaged audio across all users                        │
│   - Position initialized to non-overlapping coordinates                       │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Components

### 1. Main.java — Application Entry Point

**Location:** `src/main/java/com/processing/server/Main.java`

```java
public static void main(String[] args)
```

**Responsibilities:**
1. Initialize logging (`LogConfig.configureRuntime()`)
2. Load configuration from `application.yaml`
3. Create shared singletons:
   - `EventQueue` — thread-safe queue for user input events
   - `SessionManager` — tracks connected users
4. Start the Processing sketch (opens a window on the server machine)
5. Create HTTP/WebSocket handlers
6. Start Helidon web server

**Configuration loaded from `application.yaml`:**
- Server port (8080)
- Processing canvas dimensions (800x600)
- Static content settings

---

### 2. SessionManager.java — User Session Tracking

**Location:** `src/main/java/com/processing/server/SessionManager.java`

**Purpose:** Track connected users and assign unique session IDs.

```java
public class SessionManager {
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    
    public String createSession();        // Returns new UUID
    public boolean isActive(String id);   // Check if session exists
    public void removeSession(String id); // Cleanup on disconnect
    public int getActiveSessionCount();   // For status endpoint
}
```

**Thread Safety:** Uses `ConcurrentHashMap` to safely handle concurrent WebSocket connections.

**Key Fields in `SessionInfo` record:**
- `sessionId` — UUID string
- `createdAt` — timestamp for potential timeout logic

---

### 3. EventQueue.java — Thread-Safe Event Buffer

**Location:** `src/main/java/com/processing/server/EventQueue.java`

**Purpose:** Bridge between HTTP threads and the Processing render thread.

```java
public class EventQueue {
    private final Queue<UserInputEvent> queue = new ConcurrentLinkedQueue<>();
    
    public void push(UserInputEvent event);  // Called by WebSocket/HTTP handlers
    public UserInputEvent poll();            // Called by Processing draw loop
    public boolean isEmpty();
    public int size();
}
```

**Why `ConcurrentLinkedQueue`?**
- Multiple HTTP threads can push events simultaneously
- Single Processing thread polls events
- Non-blocking, thread-safe, high performance

---

### 4. UserInputEvent.java — Event Data Structure

**Location:** `src/main/java/com/processing/server/UserInputEvent.java`

```java
public record UserInputEvent(
    String sessionId,    // Which user sent this
    String eventType,    // "touch", "slider", or "button"
    String controlId,    // Which UI element (e.g., "sizeSlider")
    float value,         // Slider value (0-1) or 0 for touch/button
    float x,             // Touch X coordinate (normalized 0-1)
    float y,             // Touch Y coordinate (normalized 0-1)
    long timestamp       // When it occurred
)
```

**Usage:**
- Created from JSON in `WebSocketHandler` or `InputService`
- Stored in `EventQueue`
- Consumed by `ProcessingSketch`

---

### 5. WebSocketHandler.java — Real-Time Input Channel

**Location:** `src/main/java/com/processing/server/WebSocketHandler.java`

**Purpose:** Handle persistent WebSocket connections from browser clients.

**Lifecycle:**

```
Browser connects → onOpen() → Create session ID → Send welcome message
                  ↓
Browser sends JSON → onMessage() → Parse → Create UserInputEvent → Push to queue
                  ↓
Browser disconnects → onClose() → Remove session
                  ↓
Error occurs → onError() → Remove session
```

**Key Methods:**

| Method | Purpose |
|--------|---------|
| `onOpen()` | Creates session, sends `{"type":"session","sessionId":"uuid"}` |
| `onMessage()` | Parses JSON, creates `UserInputEvent`, pushes to queue |
| `onClose()` | Removes session from `SessionManager` |
| `onError()` | Cleanup on connection failure |

**JSON Message Format from Client:**
```json
{
  "type": "touch",      
  "controlId": "touchArea",
  "value": 0.5,         
  "x": 0.3,             
  "y": 0.7,
  "timestamp": 1234567890
}
```

---

### 6. InputService.java — REST API Endpoints

**Location:** `src/main/java/com/processing/server/InputService.java`

**Purpose:** Alternative HTTP REST interface (not used by the frontend, but available).

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/event` | POST | Submit an event (requires valid sessionId) |
| `/api/session` | POST | Create a new session |
| `/api/session/{id}` | DELETE | Remove a session |
| `/api/status` | GET | Get active sessions and queue size |

**Example REST Event Submission:**
```bash
# Create session
curl -X POST http://localhost:8080/api/session

# Submit event
curl -X POST http://localhost:8080/api/event \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "eventType": "touch",
    "controlId": "touchArea",
    "value": 0,
    "x": 0.5,
    "y": 0.5,
    "timestamp": 1234567890
  }'
```

---

### 7. ProcessingSketch.java — Visual Output

**Location:** `src/main/java/com/processing/server/ProcessingSketch.java`

**Purpose:** The Processing canvas that displays aggregated user inputs.

**extends `PApplet`** — Core Processing class providing:
- `size(width, height)` — Canvas dimensions
- `background(color)` — Clear screen
- `ellipse(x, y, w, h)` — Draw circle
- `fill(color)` — Set fill color
- `text(str, x, y)` — Draw text

#### Processing Lifecycle

```java
public class ProcessingSketch extends PApplet {
    
    @Override
    void settings() {
        // Called first - set canvas size
        size(sketchWidth, sketchHeight);
    }
    
    @Override
    void setup() {
        // Called once after settings()
        frameRate(60);           
        background(0);          
        colorMode(HSB, 360, 100, 100);  
    }
    
    @Override
    void draw() {
        // Called 60 times per second (target)
        background(0);           
        processEvents();         
        drawUsers();             
    }
}
```

#### Audio Processing

The server processes audio from ALL connected clients but does **not mix** the streams for playback. Instead, each client's audio level is tracked **independently** for visualization purposes.

```java
private void processAudio() {
    globalAudioLevel = 0;
    int totalSessions = 0;
    
    // Process each user's audio stream independently
    for (String sessionId : audioBuffer.getActiveSessionIds()) {
        float level = calculateAudioLevel(sessionId);
        userAudioLevels.put(sessionId, new float[]{level});
        
        // Initialize visual representation for audio-only users
        if (!userPositions.containsKey(sessionId)) {
            initializeUser(sessionId);
        }
        
        globalAudioLevel += level;
        totalSessions++;
    }
    
    // Calculate average for global audio meter
    if (totalSessions > 0) {
        globalAudioLevel /= totalSessions;
    }
    
    // Smooth the global level for visual stability
    smoothedGlobalAudioLevel = lerp(smoothedGlobalAudioLevel, globalAudioLevel, 0.3f);
}
```

**Audio Level Calculation:**

```java
private float calculateAudioLevel(String sessionId) {
    byte[] data = audioBuffer.poll(sessionId);
    if (data == null) return previousLevel * 0.9f; // Decay
    
    // Data is 16-bit PCM, little-endian
    float sum = 0;
    int samples = data.length / 2;
    
    for (int i = 0; i < samples; i++) {
        short sample = (short) ((data[i * 2] & 0xFF) | (data[i * 2 + 1] << 8));
        sum += Math.abs(sample) / 32768.0f;
    }
    
    return sum / samples; // Normalized 0.0 to 1.0
}
```

**Why No Audio Mixing?**
- Users connect from different locations with different microphones
- Mixing would require synchronization, sample rate conversion, and buffering
- Server visualization is the goal (showing each user's contribution)
- No speakers needed on the server (visual output only)

If you need audio playback, consider streaming mixed audio back to clients via WebRTC or Web Audio API.

#### Event Processing

```java
private void processEvents() {
    while (!eventQueue.isEmpty()) {
        UserInputEvent event = eventQueue.poll();
        if (event != null) {
            handleEvent(event);
        }
    }
}
```

**Important:** The queue is drained completely each frame. This ensures all user inputs are processed before rendering.

```java
private void handleEvent(UserInputEvent event) {
    String sessionId = event.sessionId();
    
    switch (event.eventType()) {
        case "touch" -> {
            if (!userPositions.containsKey(sessionId)) {
                initializeUser(sessionId);
            }
            float[] pos = userPositions.get(sessionId);
            pos[0] = event.x();
            pos[1] = event.y();
        }
        case "slider" -> {
            if (!userColors.containsKey(sessionId)) {
                initializeUser(sessionId);
            }
            float[] color = userColors.get(sessionId);
            color[1] = map(event.value(), 0, 1, 20, 100);
        }
        case "button" -> {
            if (!userColors.containsKey(sessionId)) {
                initializeUser(sessionId);
            }
            float[] color = userColors.get(sessionId);
            color[0] = (color[0] + 30) % 360;
        }
    }
}
```

#### Drawing Users

```java
private void drawUsers() {
    for (Map.Entry<String, float[]> entry : userPositions.entrySet()) {
        String sessionId = entry.getKey();
        float[] pos = entry.getValue();       
        float[] color = userColors.getOrDefault(sessionId, new float[]{0, 50, 100});
        
        // Convert normalized coords to pixel coords
        float pixelX = pos[0] * sketchWidth;
        float pixelY = pos[1] * sketchHeight;
        
        // Draw circle with user's color
        fill(color[0], color[1], color[2]);
        noStroke();
        ellipse(pixelX, pixelY, 30, 30);
        
        // Draw session ID prefix
        fill(0);  
        textAlign(CENTER, CENTER);
        textSize(10);
        text(sessionId.substring(0, 4), pixelX, pixelY);
    }
}
```

#### Data Structures

| Map | Key | Value | Purpose |
|-----|-----|-------|---------|
| `userPositions` | sessionId | `[x, y]` normalized 0-1 | Last touch position |
| `userColors` | sessionId | `[hue, saturation, brightness]` | User's circle color |

#### Starting the Sketch

```java
public void runSketch() {
    String[] args = {this.getClass().getName()};
    PApplet.runSketch(args, this);  
}
```

This runs the sketch in a **separate window** on the server machine, independent of the web server.

---

### 8. index.html — Browser Client

**Location:** `src/main/resources/static/index.html`

**Served via:** `application.yaml` static-content configuration

#### WebSocket Connection

```javascript
let ws;
let sessionId = null;

function connect() {
    const wsUrl = 'ws://' + window.location.host + '/ws';
    ws = new WebSocket(wsUrl);
    
    ws.onopen = () => {
        // Connection established
    };
    
    ws.onmessage = (event) => {
        const data = JSON.parse(event.data);
        if (data.type === 'session') {
            sessionId = data.sessionId;  
        }
    };
    
    ws.onclose = () => {
        setTimeout(connect, 2000);  
    };
}
```

#### Sending Events

```javascript
function sendEvent(type, controlId, value = 0, x = 0, y = 0) {
    if (ws && ws.readyState === WebSocket.OPEN) {
        const event = {
            type: type,
            controlId: controlId,
            value: value,
            x: x,
            y: y,
            timestamp: Date.now()
        };
        ws.send(JSON.stringify(event));
    }
}
```

#### Touch Area (x,y coordinates)

```javascript
touchArea.addEventListener('touchmove', handleTouch, { passive: false });

function handleTouch(e) {
    e.preventDefault();
    const touch = e.touches[0];
    const rect = touchArea.getBoundingClientRect();
    const x = (touch.clientX - rect.left) / rect.width;   
    const y = (touch.clientY - rect.top) / rect.height;
    sendEvent('touch', 'touchArea', 0, x, y);
}
```

---

## Data Flow Summary

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                            USER TOUCHES SCREEN                                │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                        JavaScript Event Listener                              │
│  touchArea.on('touchmove') → calculate normalized x,y (0-1)                  │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                          WebSocket.send(JSON)                                 │
│  {"type":"touch","controlId":"touchArea","x":0.5,"y":0.7,...}                 │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │
                                    │ Network (WebSocket frame)
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                      WebSocketHandler.onMessage()                             │
│  Parse JSON → Create UserInputEvent → eventQueue.push(event)                 │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │
                                    │ Concurrent queue insertion
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                              EventQueue                                       │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │ [event1] → [event2] → [event3] → [event4] → ...                        │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │
                                    │ poll() during frame render
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                      ProcessingSketch.draw()                                   │
│  1. background(0) - clear screen                                              │
│  2. processEvents() - drain queue, update userPositions/userColors maps      │
│  3. drawUsers() - render each user's circle at their position                │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │
                                    │ Swing/AWT rendering
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                         PROCESSING WINDOW                                      │
│  ┌───────────────────────────────────────────────────────────────────────┐    │
│  │  ● user1    ● user2                                                   │    │
│  │     (4f3a)     (7b2c)                                                  │    │
│  │                                                                        │    │
│  │                  ● user3                                              │    │
│  │                     (9d1e)                                             │    │
│  └───────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Configuration

### application.yaml

```yaml
server:
  port: 8080
  host: "0.0.0.0"
  features:
    static-content:
      classpath:
        - context: "/"
          location: "/static"
          welcome: "index.html"

processing:
  width: 800
  height: 600
  fps: 60

app:
  greeting: "Hello"
```

| Setting | Purpose |
|---------|---------|
| `server.port` | HTTP/WebSocket port |
| `server.host` | `"0.0.0.0"` binds all interfaces (allows network access) |
| `static-content.classpath` | Serves `src/main/resources/static/` at `/` |
| `processing.width/height` | Canvas dimensions |
| `processing.fps` | Target frame rate |

---

## Threading Model

```
┌────────────────────────────────────────────────────────────────┐
│                     JVM PROCESS                                │
│                                                                │
│  ┌──────────────────────┐    ┌──────────────────────────────┐ │
│  │  Helidon WebServer   │    │   Processing Thread         │ │
│  │  (virtual threads)   │    │   (PApplet animation thread) │ │
│  │                      │    │                              │ │
│  │  HTTP handlers       │    │  setup() → runs once         │ │
│  │  WebSocket handlers  │    │  draw()  → loops 60 FPS     │ │
│  │                      │    │                              │ │
│  │  - Many concurrent  │    │  - Single thread             │ │
│  │    connections       │    │  - Accesses EventQueue      │ │
│  │  - Push to queue     │    │  - Polls and processes      │ │
│  │                      │    │    events                   │ │
│  └──────────┬───────────┘    └──────────────┬───────────────┘ │
│             │                                │                 │
│             │        EventQueue              │                 │
│             │   (ConcurrentLinkedQueue)      │                 │
│             │                                │                 │
│             └──────────────┬─────────────────┘                 │
│                            │                                   │
│                            ▼                                   │
│                   Thread-safe handoff                          │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

**Thread Safety:**
- `EventQueue` — `ConcurrentLinkedQueue` handles concurrent push/poll
- `SessionManager` — `ConcurrentHashMap` for session storage
- `ProcessingSketch` — Only the Processing thread modifies `userPositions` and `userColors`

---

## How to Extend

### Add a New Event Type

1. **Frontend (index.html):**
```javascript
document.getElementById('newControl').addEventListener('input', (e) => {
    sendEvent('newType', 'newControlId', e.target.value / 100);
});
```

2. **Backend (ProcessingSketch.java):**
```java
case "newType" -> {
    // Handle your new event type
    userSomething.computeIfAbsent(sessionId, k -> new float[]{0});
    // Update state based on event
}
```

### Add a New REST Endpoint

In `InputService.java`:
```java
@Override
public void routing(HttpRules rules) {
    rules
        .post("/event", this::handleEvent)
        .get("/newEndpoint", this::newHandler);  
}

private void newHandler(ServerRequest req, ServerResponse res) {
    // Your logic here
    res.send("response");
}
```

### Stream Video Back to Clients

You would need to:
1. Use a library like `webcam-capture` or JavaCV
2. Capture Processing canvas frames
3. Encode as JPEG/WebP
4. Stream via WebSocket or MJPEG endpoint

---

## HTTPS/TLS Configuration

The server uses HTTPS (TLS) to enable secure WebSocket connections (`wss://`), which is required for browser microphone access from mobile devices and remote clients.

### Why HTTPS?

1. **Browser Security Requirements**: Modern browsers require HTTPS for:
   - `getUserMedia()` (microphone/camera access)
   - Secure WebSocket connections (`wss://`)
   - Service Workers and other advanced features

2. **Mobile Device Support**: Mobile browsers will only grant microphone access over HTTPS

### Keystore Structure

The server uses a PKCS12 keystore (`keystore.p12`) containing:

1. **CA Certificate** (`root-ca`): A self-signed Certificate Authority used to sign the server certificate
2. **Server Certificate** (`1`): A certificate signed by the CA, with Subject Alternative Names (SAN) for:
   - `DNS:localhost`
   - `IP:127.0.0.1`
   - `IP:192.168.1.100` (or your local network IP)

**Important**: The server certificate must have a proper certificate chain (server cert → CA cert). A simple self-signed certificate without a CA chain will cause TLS handshake failures in Helidon.

### Creating the Keystore

Run these commands from the project root directory:

```powershell
# Set JAVA_HOME if needed
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"  # or jdk-21 minimum

# 1. Create CA certificate (valid for 10 years)
keytool -genkeypair -alias root-ca -keyalg RSA -keysize 2048 -validity 3650 `
    -keystore keystore.p12 -storetype PKCS12 -storepass changeit `
    -dname "CN=ProcessingServer-CA,O=Dev,C=US" `
    -ext BasicConstraints:critical,ca:true,pathlen:1

# 2. Create server key (valid for 1 year)
keytool -genkeypair -alias "1" -keyalg RSA -keysize 2048 -validity 365 `
    -keystore keystore.p12 -storetype PKCS12 -storepass changeit `
    -dname "CN=localhost,O=Dev,C=US" `
    -ext SAN="DNS:localhost,IP:127.0.0.1,IP:192.168.1.100"

# 3. Generate certificate signing request (CSR) for server
keytool -certreq -alias "1" -keystore keystore.p12 -storetype PKCS12 `
    -storepass changeit -file server.csr

# 4. Sign server certificate with CA
keytool -gencert -alias root-ca -keystore keystore.p12 -storetype PKCS12 `
    -storepass changeit -infile server.csr -outfile server.cer -validity 365 `
    -ext SAN="DNS:localhost,IP:127.0.0.1,IP:192.168.1.100"

# 5. Import signed certificate back into keystore
keytool -importcert -alias "1" -keystore keystore.p12 -storetype PKCS12 `
    -storepass changeit -file server.cer

# 6. Export CA certificate
keytool -exportcert -alias root-ca -keystore keystore.p12 -storetype PKCS12 `
    -storepass changeit -file ca.cer

# 7. Remove root-ca as private key entry and re-import as trusted certificate
keytool -delete -alias root-ca -keystore keystore.p12 -storetype PKCS12 `
    -storepass changeit
keytool -importcert -alias root-ca -file ca.cer -keystore keystore.p12 `
    -storetype PKCS12 -storepass changeit -noprompt

# 8. Clean up temporary files
del server.csr, server.cer, ca.cer

# 9. Verify the keystore
keytool -list -keystore keystore.p12 -storetype PKCS12 -storepass changeit
```

**Expected output:**
```
Keystore type: PKCS12
Keystore provider: SUN

Your keystore contains 2 entries

1, <date>, PrivateKeyEntry, 
Certificate fingerprint (SHA-256): ...
root-ca, <date>, trustedCertEntry, 
Certificate fingerprint (SHA-256): ...
```

### Copy Keystore to Resources

After creating the keystore, copy it to the resources directory:

```powershell
Copy-Item keystore.p12 src\main\resources\keystore.p12
```

### TLS Configuration in Code

The TLS is configured programmatically in `Main.java`:

```java
WebServer server = WebServer.builder()
        .config(config.get("server"))
        .tls(tls -> tls
            .privateKey(key -> key
                .keystore(store -> store
                    .passphrase("changeit")
                    .keystore(Resource.create("keystore.p12"))))
            .privateKeyCertChain(key -> key
                .keystore(store -> store
                    .passphrase("changeit")
                    .keystore(Resource.create("keystore.p12")))))
        .routing(builder -> routing(builder, inputService))
        .addRouting(WsRouting.builder().endpoint("/ws", () -> wsHandler))
        .build()
        .start();
```

**Key points:**
- `privateKey`: References the keystore containing the server's private key
- `privateKeyCertChain`: References the same keystore to get the certificate chain
- Both use `Resource.create("keystore.p12")` to load from classpath
- The keystore must contain both the server cert AND the CA cert as a trusted entry

### Testing TLS

You can verify the TLS setup with OpenSSL:

```powershell
# Start the server first
.\run.ps1

# Test TLS connection (from Git Bash or WSL)
openssl s_client -connect localhost:8080 -showcerts
```

**Expected output:**
```
Certificate chain
 0 s:CN=localhost, O=Dev, C=US
   i:CN=ProcessingServer-CA, O=Dev, C=US
   ...
 1 s:CN=ProcessingServer-CA, O=Dev, C=US
   i:CN=ProcessingServer-CA, O=Dev, C=US
   ...
```

### Browser Certificate Warning

When accessing the server from a browser, you'll see a security warning because:
- The CA certificate is self-signed (not trusted by browser/Certificate Authorities)
- This is expected for development/testing

**To proceed:**
1. Click "Advanced" on the warning page
2. Click "Proceed to localhost (unsafe)" or similar
3. The browser will remember this exception for future visits

**For production:** Use certificates from a trusted Certificate Authority like Let's Encrypt, DigiCert, or similar.

### Changing the IP Address

The server certificate includes your local IP address in the Subject Alternative Name (SAN) extension. If your laptop connects to a different network, your IP will likely change, causing certificate validation errors.

**Current behavior:** You must regenerate the keystore when your IP changes.

**Workarounds:**

1. **Regenerate keystore** (current approach):
   ```powershell
   .\create-keystore.ps1  # auto-detects new IP
   Copy-Item keystore.p12 src\main\resources\keystore.p12
   mvn package -DskipTests
   ```

2. **Use hostname instead of IP** (requires script modification):
   
   Add `HOSTNAME.local` to the SAN extension in `create-keystore.ps1`:
   ```powershell
   $Hostname = "$env:COMPUTERNAME.local"
   -ext "SAN=DNS:localhost,DNS:$Hostname,IP:127.0.0.1,IP:$LocalIP"
   ```
   
   Then connect via: `https://YOURHOSTNAME.local:8080/`
   
   Works on most networks with mDNS/Bonjour (Windows 10+, macOS, Linux with avahi).

3. **Accept browser warnings** (not recommended for classrooms).

4. **Use a real domain with Let's Encrypt** (production).

### Changing the Password

To use a different keystore password, update:
1. The keystore commands (replace `changeit` with your password)
2. `Main.java` - update the `passphrase("changeit")` calls

---

## Running the Application

### Prerequisites
- Java 21 minimum (Java 25+ recommended for best performance)
- Maven 3.9+

### Build
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"  # or jdk-21 minimum
cd C:\Users\ed\Dev\processing-server
mvn package -DskipTests
```

### Run
```powershell
.\run.ps1
```

Or manually:
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"  # or jdk-21 minimum
java --enable-preview -cp "target\classes;target\libs\*" com.processing.server.Main
```

### Access
- **Local UI:** https://localhost:8080/
- **Local API:** https://localhost:8080/api/status
- **Mobile/Remote:** https://192.168.1.100:8080/ (replace with your IP, accept certificate warning)
- **WebSocket:** wss://localhost:8080/ws (secured)

**Note:** 
- The server uses HTTPS with a self-signed CA certificate
- Mobile devices will prompt to accept the certificate
- Microphone access requires HTTPS (required for browser security)

---

## File Structure

```
processing-server/
├── pom.xml                              # Maven build configuration
├── ARCHITECTURE.md                      # This file
├── run.ps1                              # PowerShell run script
├── keystore.p12                         # SSL certificate (self-signed)
├── src/
│   └── main/
│       ├── java/com/processing/server/
│       │   ├── Main.java                # Entry point, server setup, TLS config
│       │   ├── ProcessingSketch.java    # Processing canvas
│       │   ├── EventQueue.java          # Thread-safe event queue
│       │   ├── SessionManager.java      # User session tracking
│       │   ├── UserInputEvent.java      # Event data record
│       │   ├── InputService.java        # REST API handlers
│       │   ├── WebSocketHandler.java    # WebSocket handler
│       │   ├── AudioBuffer.java         # Per-session audio queue
│       │   ├── AudioConfig.java         # Audio configuration record
│       │   └── DebugConfig.java         # Debug logging flag
│       └── resources/
│           ├── application.yaml         # Configuration
│           ├── keystore.p12            # SSL certificate (copy)
│           └── static/
│               └── index.html           # Web UI
└── target/
    ├── classes/                         # Compiled classes
    └── libs/                             # Dependencies
```