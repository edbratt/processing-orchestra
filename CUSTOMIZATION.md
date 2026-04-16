# Customization Guide

This guide explains how to customize the Processing Server for your own visual sketches. Whether you want to change colors, add new controls, or create entirely new visualizations, this document will walk you through the process.

## Table of Contents

- [Quick Start: Basic Customizations](#quick-start-basic-customizations)
- [Configuration Changes](#configuration-changes)
- [Customizing the Browser UI](#customizing-the-browser-ui)
- [Creating Your Own Processing Sketch](#creating-your-own-processing-sketch)
- [Using Audio Data in Your Sketch](#using-audio-data-in-your-sketch)
- [Using Motion Data In Your Sketch](#using-motion-data-in-your-sketch)
- [Adding New Event Types](#adding-new-event-types)
- [Complete Example: Building a Particle System](#complete-example-building-a-particle-system)
- [Using AI Coding Assistants](#using-ai-coding-assistants)
- [Testing Your Customizations](#testing-your-customizations)
- [Troubleshooting](#troubleshooting)
- [Resources](#resources)
- [License](#license)

---

## Quick Start: Basic Customizations

Contents:
- [Change Canvas Size](#change-canvas-size)
- [Change Audio Quality](#change-audio-quality)
- [Change Port or Host](#change-port-or-host)

### Change Canvas Size

Edit `src/main/resources/application.yaml`:

```yaml
processing:
  width: 1920   # Change from 800
  height: 1080  # Change from 600
  fps: 30       # Change from 60 (lower for better performance)
```

### Change Audio Quality

Edit `src/main/resources/application.yaml`:

```yaml
audio:
  mode: "voice"  # Options: high-quality-stereo, high-quality-mono, voice
```

| Mode | Sample Rate | Channels | Use Case |
|------|-------------|----------|----------|
| `high-quality-stereo` | 44100 Hz | 2 | Music, high-fidelity |
| `high-quality-mono` | 44100 Hz | 1 | Voice, lower bandwidth |
| `voice` | 22050 Hz | 1 | Low bandwidth, voice only |

### Change Port or Host

Edit `src/main/resources/application.yaml`:

```yaml
server:
  port: 9000        # Change from 8080
  host: "127.0.0.1" # Change from "0.0.0.0" to restrict access
```

---

## Configuration Changes

Contents:
- [application.yaml](#applicationyaml)
- [Change Motion Behavior](#change-motion-behavior)
- [Motion Trim In The Browser](#motion-trim-in-the-browser)
- [Debug Mode](#debug-mode)

### application.yaml

Full configuration reference:

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
  sketch-class: "com.processing.server.ProcessingSketch"
  width: 800
  height: 600
  fps: 60

audio:
  mode: "high-quality-stereo"
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
  logging: false
```

If you want the server to launch a different sketch class, change:

- `processing.sketch-class`

The alternate class must:

- extend `PApplet`
- use the same constructor shape as `ProcessingSketch`
- provide a public `runSketch()` method

Included example:

- `com.processing.server.StarterSketch`

That sketch is intentionally small. It is useful when you want to prove that sketch selection is working before you move on to a more complex custom class.

### Change Motion Behavior

Edit `src/main/resources/application.yaml`:

```yaml
motion:
  update-hz: 20
  clamp:
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

What these do:
- `motion.update-hz`: how often the browser sends a combined motion sample
- `motion.clamp.beta-degrees` and `motion.clamp.gamma-degrees`: max accepted tilt
- `motion.clamp.acceleration-g`: max accepted per-axis acceleration
- `motion.clamp.magnitude-g`: max accepted total acceleration magnitude
- `motion.mapping.tilt-offset-normalized`: how far tilt can move the rendered object around its touch target
- `motion.mapping.shake-threshold-g`: minimum shake signal before triggering a burst
- `motion.mapping.shake-burst-scale`: scales the burst strength caused by shake
- `motion.debug.logging`: prints a few motion debug samples

Important:
- because `application.yaml` is packaged into the jar, changing these values requires a rebuild

### Motion Trim In The Browser

The browser UI also includes a `Motion Trim` slider.

What it does:
- scales outgoing motion samples before they are sent to the server
- affects both tilt and shake responsiveness
- is local to that browser/client only

Why it matters:
- lets each phone feel more or less sensitive without changing the shared server-side motion config
- does not require a rebuild

This trim is implemented in `src/main/resources/static/index.html`, not in `application.yaml`.

### Debug Mode

Enable debug logging to see what's happening:

**In application.yaml:**
```yaml
debug:
  logging: true
```

**Or via URL parameter:**
```
http://localhost:8080/?debug
```

---

## Customizing the Browser UI

Contents:
- [Location](#location)
- [Key Sections to Modify](#key-sections-to-modify)

### Location

The browser UI is in `src/main/resources/static/index.html`.

### Key Sections to Modify

#### 1. Add a New Slider

Find the slider section and add:

```html
<div class="slider-container">
    <label for="mySlider">My Custom Slider</label>
    <input type="range" id="mySlider" min="0" max="100" value="50">
    <span id="mySliderValue">50</span>
</div>
```

Add JavaScript to send events:

```javascript
document.getElementById('mySlider').addEventListener('input', (e) => {
    sendEvent('slider', 'mySlider', e.target.value / 100);  // Normalize to 0-1
});
```

#### 2. Add a New Button

```html
<button id="myButton" class="action-button">My Action</button>
```

```javascript
document.getElementById('myButton').addEventListener('click', () => {
    sendEvent('button', 'myButton');
});
```

#### 3. Add a Color Picker

```html
<div class="control-group">
    <label for="colorPicker">Choose Color</label>
    <input type="color" id="colorPicker" value="#ff0000">
</div>
```

```javascript
document.getElementById('colorPicker').addEventListener('input', (e) => {
    // Convert hex to RGB
    const hex = e.target.value;
    const r = parseInt(hex.slice(1, 3), 16) / 255;
    const g = parseInt(hex.slice(3, 5), 16) / 255;
    const b = parseInt(hex.slice(5, 7), 16) / 255;
    sendEvent('color', 'colorPicker', r, (g << 8) | b);  // Pack RGB
});
```

#### 4. Change Touch Area Size

Find the touch area in CSS:

```css
#touchArea {
    width: 100%;
    height: 300px;  /* Change height */
    background: #1a1a1a;
    border: 2px solid #333;
    touch-action: none;
}
```

#### 5. Keyboard Input

Keyboard input is already wired into the browser client.

What it does:
- listens for `keydown` and `keyup` on the focused page
- sends `type: "key"` events over WebSocket
- ignores range sliders and buttons so normal UI keyboard use still works

If you want to customize it, start in:

- `src/main/resources/static/index.html`

Look for:

- `handleKeyDown(event)`
- `handleKeyUp(event)`
- `sendKeyEvent(action, event)`

Important:
- browser keyboard input only works when the browser page has focus
- local Processing `keyPressed()` or `keyReleased()` code only works when the Processing sketch window has focus
- a custom sketch can support one path or both, depending on how you want students to interact with it

---

## Creating Your Own Processing Sketch

Contents:
- [Location](#location-1)
- [What to Preserve](#what-to-preserve)
- [Minimal Custom Sketch Template](#minimal-custom-sketch-template)
- [Event Types Available](#event-types-available)
- [Processing Methods You Can Override](#processing-methods-you-can-override)
- [Where To Observe Or Manipulate Runtime Data](#where-to-observe-or-manipulate-runtime-data)

### Location

The Processing sketch is `src/main/java/com/processing/server/ProcessingSketch.java`.

You can also point the server at a different sketch class by changing:

- `processing.sketch-class` in `src/main/resources/application.yaml`

The included `StarterSketch` class is a minimal example of an alternate sketch that still uses the project's constructor contract.

If you want to try a reviewed generated sketch in the app without mixing it into the main handwritten source tree, place it under:

- `generated-src/main/java/com/processing/server/`

Maven is configured to compile that folder along with the normal `src/main/java` tree.

### What to Preserve

You need to keep:

1. **Constructor signature** - Accepts `EventQueue`, `AudioBuffer`, width, height, `DebugConfig`, and `MotionConfig`
2. **The `runSketch()` method** - Starts the Processing thread
3. **The main `draw()` loop** - But you can modify it heavily

### Minimal Custom Sketch Template

```java
package com.processing.server;

import processing.core.PApplet;
import java.util.Map;
import java.util.HashMap;

public class ProcessingSketch extends PApplet {
    private final EventQueue eventQueue;
    private final AudioBuffer audioBuffer;
    private final int sketchWidth;
    private final int sketchHeight;
    private final DebugConfig debugConfig;
    private final MotionConfig motionConfig;
    
    private final Map<String, float[]> userPositions = new HashMap<>();
    
    public ProcessingSketch(EventQueue eventQueue, AudioBuffer audioBuffer,
                           int width, int height, DebugConfig debugConfig,
                           MotionConfig motionConfig) {
        this.eventQueue = eventQueue;
        this.audioBuffer = audioBuffer;
        this.sketchWidth = width;
        this.sketchHeight = height;
        this.debugConfig = debugConfig;
        this.motionConfig = motionConfig;
    }

    @Override
    public void settings() {
        size(sketchWidth, sketchHeight);
    }

    @Override
    public void setup() {
        background(0);
        frameRate(60);
    }

    @Override
    public void draw() {
        background(0);
        processEvents();
        drawUsers();
    }

    private void processEvents() {
        while (!eventQueue.isEmpty()) {
            UserInputEvent event = eventQueue.poll();
            if (event == null) break;
            
            String sessionId = event.sessionId();
            
            switch (event.eventType()) {
                case "touch" -> {
                    // Handle touch events here
                    userPositions.computeIfAbsent(sessionId, k -> new float[]{0.5f, 0.5f});
                    float[] pos = userPositions.get(sessionId);
                    pos[0] = event.x();
                    pos[1] = event.y();
                }
                case "motion" -> {
                    // Handle motion events here
                }
                // Add more event types here
            }
        }
    }

    private void drawUsers() {
        for (Map.Entry<String, float[]> entry : userPositions.entrySet()) {
            float[] pos = entry.getValue();
            float x = pos[0] * sketchWidth;
            float y = pos[1] * sketchHeight;
            
            fill(255);
            noStroke();
            ellipse(x, y, 20, 20);
        }
    }

    public void runSketch() {
        String[] args = {this.getClass().getName()};
        PApplet.runSketch(args, this);
    }
}
```

### Event Types Available

| Event Type | Fields | Description |
|------------|--------|-------------|
| `touch` | `x`, `y` (0-1 normalized) | Touch/mouse position |
| `slider` | `controlId`, `value` (0-1) | Slider value change |
| `button` | `controlId` | Button clicked |
| `key` | `key`, `keyCode`, `action` | Keyboard key pressed or released from the focused browser page |
| `motion` | `alpha`, `beta`, `gamma`, `ax`, `ay`, `az`, `magnitude` | Phone tilt and shake sample |
| `color` | `controlId`, `value` (packed RGB) | Color picker change |
| `audio` | Binary WebSocket frame | PCM audio samples |

### Processing Methods You Can Override

```java
@Override
public void settings() {
    // Set canvas size - MUST call size() here
    size(width, height);
    // OR: size(width, height, P2D);  // 2D renderer
    // OR: size(width, height, P3D);  // 3D renderer
}

@Override
public void setup() {
    // Initialize - runs once after settings()
    frameRate(60);
    colorMode(HSB, 360, 100, 100);
    // Load images, fonts, etc.
}

@Override
public void draw() {
    // Main loop - runs 60 times per second
    background(0);  // Clear screen
    
    processEvents();  // Handle user inputs
    
    // Your drawing code here
}

@Override
public void mousePressed() {
    // Handle mouse clicks on the canvas
}

@Override
public void keyPressed() {
    // Handle local keyboard input when the Processing window has focus
}

@Override
public void exit() {
    // Cleanup before closing
}
```

### Where To Observe Or Manipulate Runtime Data

If you want to inspect, transform, filter, or redirect data in this application, these are the main hook points.

#### Helidon server side

For real-time browser input, start with:

- `src/main/java/com/processing/server/WebSocketHandler.java`

The most useful methods are:

- `onOpen(WsSession session)`
  use this if you want to initialize per-connection state, send extra welcome data, or log when a browser connects
- `onMessage(WsSession session, String message, boolean last)`
  this is the entry point for JSON messages such as touch, slider, button, and motion input
- `handleControlEvent(JsonObject json)`
  use this if you want to inspect or modify control values before they become `UserInputEvent` objects
- `handleKeyEvent(JsonObject json)`
  use this if you want to remap browser keyboard input, drop repeat-style events, or translate keys into higher-level commands before they reach the sketch
- `handleMotionEvent(JsonObject json)`
  use this if you want to clamp motion differently, calculate new motion fields, or reject noisy input
- `onMessage(WsSession session, BufferData buffer, boolean last)`
  this is the entry point for binary audio frames from the browser

For server-side audio buffering, look at:

- `src/main/java/com/processing/server/AudioBuffer.java`

The most useful methods are:

- `push(String sessionId, byte[] audioData)`
  use this if you want to inspect, replace, compress, meter, or fork audio as it arrives from the browser
- `poll(String sessionId)`
  use this if you want to change how the sketch reads audio chunks
- `clearSession(String sessionId)`
  this is where per-session audio cleanup happens when a connection closes
- `getActiveSessionIds()`
  use this if you want to iterate over every session currently contributing audio

If you wanted to add an explicit audio-processing stage on the server, a likely helper method would look something like:

```java
private byte[] processIncomingAudio(String sessionId, byte[] audioData) {
    // inspect or transform audio here
    return audioData;
}
```

That helper would fit naturally inside `WebSocketHandler.onMessage(... BufferData ...)` before `audioBuffer.push(...)`.

For REST-style inspection or tooling endpoints, start with:

- `src/main/java/com/processing/server/InputService.java`

Useful methods:

- `getStatus(...)`
  a natural place to expose more runtime information for debugging or teaching
- `handleEvent(...)`
  useful if you want to support non-WebSocket event injection

#### Processing sketch side

For the Processing side, start with:

- `src/main/java/com/processing/server/ProcessingSketch.java`

The main places to look are:

- `processEvents()`
  this drains the shared event queue once per frame
- `handleEvent(UserInputEvent event)`
  this is the central router for touch, slider, button, key, and motion events
- `handleKeyEvent(String sessionId, UserInputEvent event)`
  use this if you want browser keyboard input to affect your sketch directly
- `handleMotionEvent(String sessionId, UserInputEvent event)`
  use this if you want to change how tilt and shake are interpreted
- `processAudio()`
  this is the main loop for pulling queued audio into sketch-side state
- `calculateAudioLevel(String sessionId)`
  this is the most direct place to change how raw PCM bytes become an amplitude value
- `drawUsers()`
  this is where stored per-user state becomes visible graphics
- `initializeUser(String sessionId)`
  this is where sketch-side per-session state is first created

If you want to manipulate the sketch's per-user audio state directly, the natural places are:

- inside `processAudio()`, after `calculateAudioLevel(sessionId)`
- inside `drawUsers()`, where `userAudioLevels` and `userMotion` are already being read

If you wanted a clearer custom hook for sketch-side audio behavior, a likely helper method might look like:

```java
private float transformAudioLevel(String sessionId, float rawLevel) {
    // shape, smooth, or remap the level here
    return rawLevel;
}
```

and you would call it from `processAudio()` before storing into `userAudioLevels`.

If you wanted a custom hook for control events before they affect drawing, a likely helper method might look like:

```java
private UserInputEvent transformEvent(UserInputEvent event) {
    // modify or remap event fields here
    return event;
}
```

and you would call it from `processEvents()` or `handleEvent(...)`.

#### Rule of thumb

Use the Helidon-side classes when you want to:

- inspect raw incoming browser data
- reject or clamp data before the sketch sees it
- add diagnostics or alternate server endpoints

Use the sketch-side methods when you want to:

- change how the stored data affects the visuals
- mix several inputs together into one visual behavior
- experiment artistically without rewriting the networking layer

Keyboard note:
- browser `key` events arrive through `WebSocketHandler` and `EventQueue`
- local Processing keyboard input does not go through Helidon or the browser at all
- if you preserve local Processing keyboard handlers from a PDE sketch, those handlers remain separate from the browser keyboard protocol

---

## Using Audio Data in Your Sketch

Contents:
- [How Audio Works](#how-audio-works)
- [Calculating Audio Level](#calculating-audio-level)
- [Example: Audio-Reactive Circle](#example-audio-reactive-circle)
- [Example: Global Audio Visualizer](#example-global-audio-visualizer)

### How Audio Works

1. **Each client streams audio** from their microphone via WebSocket binary frames
2. **AudioBuffer stores** per-session queues of PCM bytes
3. **Call `audioBuffer.poll(sessionId)`** to get audio data for a session
4. **Calculate audio level** from raw bytes (see below)

### Calculating Audio Level

```java
private float calculateAudioLevel(String sessionId) {
    byte[] data = audioBuffer.poll(sessionId);
    if (data == null || data.length == 0) {
        return 0;
    }
    
    // Data is 16-bit PCM, little-endian
    float sum = 0;
    int samples = data.length / 2;  // 2 bytes per sample
    
    for (int i = 0; i < samples; i++) {
        // Convert two bytes to a 16-bit sample
        short sample = (short) ((data[i * 2] & 0xFF) | (data[i * 2 + 1] << 8));
        sum += Math.abs(sample) / 32768.0f;  // Normalize to 0-1
    }
    
    return sum / samples;  // Average amplitude
}
```

### Example: Audio-Reactive Circle

```java
private void drawUsers() {
    for (String sessionId : getAllUserIds()) {
        float[] pos = userPositions.get(sessionId);
        float audioLevel = calculateAudioLevel(sessionId);
        
        // Base size plus audio-reactive size
        float baseSize = 30;
        float audioScale = 1 + audioLevel * 3;  // Scale up to 4x
        float size = baseSize * audioScale;
        
        // Get color (hue based on session)
        float hue = sessionId.hashCode() % 360;
        fill(hue, 80, 100);
        
        // Draw main circle
        noStroke();
        ellipse(pos[0] * sketchWidth, pos[1] * sketchHeight, size, size);
        
        // Draw pulsing ring
        if (audioLevel > 0.1) {
            noFill();
            stroke(hue, 60, 80);
            strokeWeight(2);
            ellipse(pos[0] * sketchWidth, pos[1] * sketchHeight, 
                   size * 1.5f, size * 1.5f);
        }
    }
}

private Set<String> getAllUserIds() {
    // Users from both events and audio
    Set<String> allIds = new HashSet<>();
    allIds.addAll(userPositions.keySet());
    allIds.addAll(audioBuffer.getActiveSessionIds());
    return allIds;
}
```

### Example: Global Audio Visualizer

```java
private void drawGlobalAudio() {
    float globalLevel = 0;
    int count = 0;
    
    for (String sessionId : audioBuffer.getActiveSessionIds()) {
        globalLevel += calculateAudioLevel(sessionId);
        count++;
    }
    
    if (count > 0) {
        globalLevel /= count;  // Average
    }
    
    // Draw frequency bars based on global audio
    int numBars = 20;
    float barWidth = sketchWidth / (float) numBars;
    
    for (int i = 0; i < numBars; i++) {
        float barHeight = globalLevel * sketchHeight * 0.5f;
        float hue = map(i, 0, numBars, 0, 360);
        fill(hue, 80, 100);
        rect(i * barWidth, sketchHeight - barHeight, barWidth - 2, barHeight);
    }
}
```

---

## Using Motion Data In Your Sketch

Contents:
- [How Motion Works](#how-motion-works)
- [Where Motion Is Handled In ProcessingSketch.java](#where-motion-is-handled-in-processingsketchjava)
- [Current Motion Mapping](#current-motion-mapping)
- [Example: Replace Tilt Positioning With Color Control](#example-replace-tilt-positioning-with-color-control)
- [Example: Replace Shake Burst With Scatter](#example-replace-shake-burst-with-scatter)
- [Practical Advice](#practical-advice)

### How Motion Works

1. Supported phone browsers collect orientation and acceleration data.
2. The browser combines those sensor values into one `motion` JSON message at `motion.update-hz`.
3. `WebSocketHandler.java` clamps the values and converts them into a `UserInputEvent`.
4. `ProcessingSketch.processEvents()` routes `eventType == "motion"` into `handleMotionEvent()`.
5. The sketch stores per-session motion state and uses it during drawing.

### Where Motion Is Handled In ProcessingSketch.java

The current sketch handles motion in three main places:

1. In `handleEvent(UserInputEvent event)`:
```java
case "motion" -> {
    if (!userPositions.containsKey(sessionId)) {
        initializeUser(sessionId);
    }
    handleMotionEvent(sessionId, event);
}
```

2. In `handleMotionEvent(String sessionId, UserInputEvent event)`:
- stores the latest per-session orientation and acceleration sample
- computes shake intensity
- converts shake into pulse/hue energy

3. In `drawUsers()`:
- reads the stored `userMotion` values
- converts `beta`/`gamma` tilt into a bounded offset around the touch target
- renders the resulting circle position and burst ring

### Current Motion Mapping

The default sketch currently uses motion like this:
- `gamma` tilt changes horizontal offset around the touch target
- `beta` tilt changes vertical offset around the touch target
- shake intensity increases the burst pulse on the outer ring
- stronger shake creates a stronger pulse

### Example: Replace Tilt Positioning With Color Control

If you want phone tilt to change color instead of position, the best place to modify is `drawUsers()`:

```java
float[] motion = userMotion.getOrDefault(sessionId, new float[]{0f, 0f, 0f, 0f, 0f, 0f, 0f});
float gammaHueShift = map(
    motion[2],
    -motionConfig.getGammaClampDegrees(),
    motionConfig.getGammaClampDegrees(),
    -40f,
    40f
);
color[0] = (color[0] + gammaHueShift + 360) % 360;
```

### Example: Replace Shake Burst With Scatter

If you want shake to move the object instead of pulsing the ring, change `handleMotionEvent()`:

```java
if (shakeIntensity > 0f) {
    float[] target = userTargetPositions.get(sessionId);
    target[0] = random(0.1f, 0.9f);
    target[1] = random(0.15f, 0.85f);
}
```

### Practical Advice

- keep touch as the primary position input unless you explicitly want motion-only control
- use tilt as an additive modifier rather than a replacement when possible
- use shake for short-lived effects, not continuous state
- if motion feels too subtle, first adjust config before rewriting sketch logic

The most useful motion tuning settings are:
- `motion.mapping.tilt-offset-normalized`
- `motion.mapping.shake-threshold-g`
- `motion.mapping.shake-burst-scale`

---

## Adding New Event Types

Contents:
- [Step 1: Add UI in Browser](#step-1-add-ui-in-browser)
- [Step 2: Handle in ProcessingSketch.java](#step-2-handle-in-processingsketchjava)
- [Step 3: Draw the Effect](#step-3-draw-the-effect)

### Step 1: Add UI in Browser

Edit `src/main/resources/static/index.html`:

```html
<button id="explosion">Trigger Explosion</button>
```

```javascript
document.getElementById('explosion').addEventListener('click', () => {
    sendEvent('explosion', 'explosionButton', 1.0);
});
```

### Step 2: Handle in ProcessingSketch.java

```java
Map<String, Float> explosionTimers = new HashMap<>();

private void handleEvent(UserInputEvent event) {
    String sessionId = event.sessionId();
    
    switch (event.eventType()) {
        case "touch" -> {
            // ... existing touch handling
        }
        case "slider" -> {
            // ... existing slider handling
        }
        case "explosion" -> {
            // New event type
            explosionTimers.put(sessionId, 1.0f);  // 1 second explosion
        }
    }
}
```

### Step 3: Draw the Effect

```java
private void drawUsers() {
    // Draw explosions
    for (Map.Entry<String, Float> entry : explosionTimers.entrySet()) {
        String sessionId = entry.getKey();
        float timer = entry.getValue();
        float[] pos = userPositions.get(sessionId);
        
        if (pos != null && timer > 0) {
            // Explosion ring expands over time
            float radius = (1 - timer) * 200;  // Expands from 0 to 200
            noFill();
            stroke(360, 80, 100);
            strokeWeight(3);
            ellipse(pos[0] * sketchWidth, pos[1] * sketchHeight, radius, radius);
            
            // Decrease timer
            explosionTimers.put(sessionId, timer - 0.016f);  // ~60 FPS
        }
    }
    
    // ... draw users
    
    // Clean up finished explosions
    explosionTimers.entrySet().removeIf(e -> e.getValue() <= 0);
}
```

---

## Complete Example: Building a Particle System

Contents:
- [Step 1: Create Particle.java](#step-1-create-particlejava)
- [Step 2: Replace ProcessingSketch.java](#step-2-replace-processingsketchjava)
- [Step 3: Build and Run](#step-3-build-and-run)

Let's create a complete custom sketch with particle trails and audio-reactive visuals.

### Step 1: Create Particle.java

Create `src/main/java/com/processing/server/Particle.java`:

```java
package com.processing.server;

public class Particle {
    float x, y;
    float vx, vy;
    float life;
    float maxLife;
    int color;
    
    public Particle(float x, float y, float vx, float vy, float life, int color) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.maxLife = life;
        this.color = color;
    }
    
    public void update() {
        x += vx;
        y += vy;
        life -= 1;
    }
    
    public boolean isDead() {
        return life <= 0;
    }
    
    public float getAlpha() {
        return life / maxLife;
    }
}
```

### Step 2: Replace ProcessingSketch.java

```java
package com.processing.server;

import processing.core.PApplet;
import java.util.*;

public class ProcessingSketch extends PApplet {
    private final EventQueue eventQueue;
    private final AudioBuffer audioBuffer;
    private final int sketchWidth;
    private final int sketchHeight;
    private final DebugConfig debugConfig;
    
    // User data
    private final Map<String, float[]> userPositions = new HashMap<>();
    private final Map<String, float[]> userVelocities = new HashMap<>();
    private final Map<String, Integer> userColors = new HashMap<>();
    private final Map<String, Float> userAudioLevels = new HashMap<>();
    
    // Particles
    private final List<Particle> particles = Collections.synchronizedList(new ArrayList<>());
    
    // Global audio level
    private float globalAudioLevel = 0;
    private float smoothedGlobalAudioLevel = 0;
    
    public ProcessingSketch(EventQueue eventQueue, AudioBuffer audioBuffer, 
                           int width, int height, DebugConfig debugConfig) {
        this.eventQueue = eventQueue;
        this.audioBuffer = audioBuffer;
        this.sketchWidth = width;
        this.sketchHeight = height;
        this.debugConfig = debugConfig;
    }

    @Override
    public void settings() {
        size(sketchWidth, sketchHeight, P2D);
    }

    @Override
    public void setup() {
        background(0);
        frameRate(60);
        colorMode(HSB, 360, 100, 100, 100);  // Add alpha channel
        smooth();
    }

    @Override
    public void draw() {
        // Fade effect - trails
        fill(0, 100);
        noStroke();
        rect(0, 0, sketchWidth, sketchHeight);
        
        // Process all inputs
        processEvents();
        processAudio();
        
        // Update and draw particles
        updateParticles();
        
        // Draw users with particle trails
        drawUsers();
        
        // Draw global audio meter
        drawGlobalAudioMeter();
    }
    
    private void processEvents() {
        while (!eventQueue.isEmpty()) {
            UserInputEvent event = eventQueue.poll();
            if (event == null) break;
            
            String sessionId = event.sessionId();
            initializeUserIfNew(sessionId);
            
            switch (event.eventType()) {
                case "touch" -> {
                    // Store velocity for particle emission
                    float[] prevPos = userPositions.get(sessionId);
                    float[] vel = userVelocities.computeIfAbsent(sessionId, k -> new float[]{0, 0});
                    
                    if (prevPos != null) {
                        vel[0] = (event.x() - prevPos[0]) * 10;
                        vel[1] = (event.y() - prevPos[1]) * 10;
                    }
                    
                    userPositions.put(sessionId, new float[]{event.x(), event.y()});
                    
                    // Emit particles at user position
                    emitParticles(sessionId);
                }
                case "slider" -> {
                    // Slider controls hue (color)
                    int hue = (int) map(event.value(), 0, 1, 0, 360);
                    userColors.put(sessionId, hue);
                }
            }
        }
    }
    
    private void processAudio() {
        globalAudioLevel = 0;
        int count = 0;
        
        for (String sessionId : audioBuffer.getActiveSessionIds()) {
            initializeUserIfNew(sessionId);
            
            float level = calculateAudioLevel(sessionId);
            userAudioLevels.put(sessionId, level);
            globalAudioLevel += level;
            count++;
        }
        
        if (count > 0) {
            globalAudioLevel /= count;
        }
        
        smoothedGlobalAudioLevel = lerp(smoothedGlobalAudioLevel, globalAudioLevel, 0.2f);
    }
    
    private float calculateAudioLevel(String sessionId) {
        byte[] data = audioBuffer.poll(sessionId);
        if (data == null || data.length == 0) {
            return 0;
        }
        
        float sum = 0;
        int samples = data.length / 2;
        
        for (int i = 0; i < samples && i < 1024; i++) {
            short sample = (short) ((data[i * 2] & 0xFF) | (data[i * 2 + 1] << 8));
            sum += Math.abs(sample) / 32768.0f;
        }
        
        return sum / samples;
    }
    
    private void initializeUserIfNew(String sessionId) {
        if (!userPositions.containsKey(sessionId)) {
            // Random non-overlapping position
            float x = random(0.1f, 0.9f);
            float y = random(0.15f, 0.85f);
            userPositions.put(sessionId, new float[]{x, y});
            userVelocities.put(sessionId, new float[]{0, 0});
            userColors.put(sessionId, (int) random(360));
            userAudioLevels.put(sessionId, 0f);
        }
    }
    
    private void emitParticles(String sessionId) {
        float[] pos = userPositions.get(sessionId);
        float[] vel = userVelocities.get(sessionId);
        int color = userColors.getOrDefault(sessionId, 0);
        float audioLevel = userAudioLevels.getOrDefault(sessionId, 0f);
        
        if (pos == null) return;
        
        // Number of particles based on audio level
        int numParticles = (int) (5 + audioLevel * 20);
        
        for (int i = 0; i < numParticles; i++) {
            float angle = random(TWO_PI);
            float speed = random(1, 3) * (1 + audioLevel * 2);
            
            float pvx = cos(angle) * speed + vel[0] * 0.5f;
            float pvy = sin(angle) * speed + vel[1] * 0.5f;
            
            // Adjust for velocity
            pvx += vel[0] * 2;
            pvy += vel[1] * 2;
            
            float life = random(30, 90);
            
            Particle p = new Particle(
                pos[0] * sketchWidth, pos[1] * sketchHeight,
                pvx, pvy, life, color
            );
            
            particles.add(p);
        }
    }
    
    private void updateParticles() {
        // Remove dead particles
        particles.removeIf(Particle::isDead);
        
        // Limit total particles
        while (particles.size() > 2000) {
            particles.remove(0);
        }
        
        // Update and draw
        for (Particle p : particles) {
            p.update();
            
            float alpha = p.getAlpha() * 100;
            stroke(p.color, 80, 100, alpha);
            strokeWeight(2);
            point(p.x, p.y);
        }
    }
    
    private void drawUsers() {
        for (Map.Entry<String, float[]> entry : userPositions.entrySet()) {
            String sessionId = entry.getKey();
            float[] pos = entry.getValue();
            int color = userColors.getOrDefault(sessionId, 0);
            float audioLevel = userAudioLevels.getOrDefault(sessionId, 0f);
            
            float x = pos[0] * sketchWidth;
            float y = pos[1] * sketchHeight;
            
            // Draw user circle
            float baseSize = 20;
            float size = baseSize * (1 + audioLevel * 2);
            
            noStroke();
            fill(color, 80, 100, 80);
            ellipse(x, y, size, size);
            
            // Draw user ID
            fill(0, 100);
            textAlign(CENTER, CENTER);
            textSize(10);
            text(sessionId.substring(0, 4), x, y);
        }
    }
    
    private void drawGlobalAudioMeter() {
        float meterWidth = sketchWidth * 0.8f;
        float meterHeight = 15;
        float meterX = (sketchWidth - meterWidth) / 2;
        float meterY = sketchHeight - 30;
        
        // Background
        fill(0, 50);
        noStroke();
        rect(meterX, meterY, meterWidth, meterHeight, 3);
        
        // Audio level bar
        float levelWidth = meterWidth * smoothedGlobalAudioLevel;
        float hue = map(smoothedGlobalAudioLevel, 0, 1, 120, 0);  // Green to red
        fill(hue, 80, 100);
        rect(meterX, meterY, levelWidth, meterHeight, 3);
        
        // User count
        fill(255);
        textAlign(CENTER, CENTER);
        textSize(12);
        int userCount = userPositions.size();
        text(userCount + " user" + (userCount != 1 ? "s" : ""), sketchWidth / 2, meterY - 10);
    }

    public void runSketch() {
        String[] args = {this.getClass().getName()};
        PApplet.runSketch(args, this);
    }
}
```

### Step 3: Build and Run

```bash
mvn clean package -DskipTests
./run.sh  # or run.ps1 on Windows
```

---

## Using AI Coding Assistants

We have written a separate AI Customization guide. 
This guide includes sample prompts and information that you might want to provide your AI Coding agent when you want to customize your application.

- [AI_CUSTOMIZATION_GUIDE.md](AI_CUSTOMIZATION_GUIDE.md)

This guide will give you:

- safe edit zones by task type
- runtime contracts the AI should preserve
- prompt templates
- file-by-file edit maps
- what source snippets you should paste into the AI


---

## Testing Your Customizations

Contents:
- [Local Testing](#local-testing)
- [Mobile Testing](#mobile-testing)
- [Performance Testing](#performance-testing)

### Local Testing

1. **Start the server:**
   ```bash
   ./run.sh  # or run.ps1
   ```

2. **Open multiple browser tabs:**
   ```
   http://localhost:8080/
   http://localhost:8080/?debug
   ```

3. **Test each control:**
   - Touch/move on the touch area
   - Adjust sliders
   - Click buttons
   - Enable audio (click "Start Audio")

4. **Check console output:**
   - Server logs: Look for errors
   - Browser console: F12 → Console

### Mobile Testing

1. **Find your local IP:**
   ```bash
   # Mac/Linux
   hostname -I
   
   # Windows
   ipconfig
   ```

2. **Regenerate keystore with your IP:**
   ```bash
   ./create-keystore.sh YOUR_LOCAL_IP
   mvn clean package -DskipTests
   ```

3. **Access from mobile:**
   ```
   https://YOUR_LOCAL_IP:8080/
   ```

4. **Accept certificate warning** on mobile

### Performance Testing

1. **Open 5+ browser tabs**
2. **Enable audio on all tabs**
3. **Monitor frame rate** (display in window title)
4. **Check server console for delays**

#### Add FPS Counter

```java
@Override
public void draw() {
    // ... your code
    
    // Display FPS
    surface.setTitle("Processing Server - " + (int)frameRate + " FPS");
}
```

---

## Troubleshooting

Contents:
- [Sketch Doesn't Start](#sketch-doesnt-start)
- [Events Not Received](#events-not-received)
- [Audio Not Working](#audio-not-working)
- [Low Frame Rate](#low-frame-rate)
- [Users Overlapping](#users-overlapping)

### Sketch Doesn't Start

**Symptom:** Processing window doesn't open or closes immediately.

**Solutions:**
1. Check console errors
2. Verify `settings()` calls `size()`
3. Ensure `runSketch()` is called in `Main.java`

### Events Not Received

**Symptom:** Touch events don't update visuals.

**Solutions:**
1. Check WebSocket connection in browser console
2. Verify `eventQueue.poll()` is called in `draw()`
3. Add debug logging:
   ```java
   System.out.println("Received event: " + event.eventType());
   ```

### Audio Not Working

**Symptom:** Audio levels always 0.

**Solutions:**
1. Check HTTPS (required for microphone)
2. Click "Start Audio" button in browser
3. Verify browser microphone permissions
4. Check `audioBuffer.getActiveSessionIds()` has your session

### Low Frame Rate

**Symptom:** Frame rate below 30 FPS.

**Solutions:**
1. Reduce particle count
2. Use P2D renderer: `size(sketchWidth, sketchHeight, P2D)`
3. Avoid object creation in `draw()`
4. Use `noSmooth()` for better performance

### Users Overlapping

**Symptom:** Users spawn at same position.

**Solutions:**
1. Implement `findNonOverlappingPosition()` (see ARCHITECTURE.md)
2. Check position bounds (0-1 range)
3. Store positions in `Map<String, float[]>`

---

## Resources

Contents:
- [Processing Documentation](#processing-documentation)
- [Helidon Documentation](#helidon-documentation)
- [Audio Processing](#audio-processing)
- [WebSocket](#websocket)

### Processing Documentation
- [Processing Reference](https://processing.org/reference/)
- [Processing Tutorials](https://processing.org/tutorials/)
- [PApplet API](https://processing.github.io/processing/core/javadoc/processing/core/PApplet.html)

### Helidon Documentation
- [Helidon SE Guide](https://helidon.io/docs/v4/se/)
- [WebSocket Support](https://helidon.io/docs/v4/se/websocket)

### Audio Processing
- [Web Audio API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Audio_API)
- [AudioContext](https://developer.mozilla.org/en-US/docs/Web/API/AudioContext)

### WebSocket
- [MDN WebSocket API](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
- [RFC 6455](https://datatracker.ietf.org/doc/html/rfc6455)

---

## License

This project is provided as-is for educational and demonstration purposes.
