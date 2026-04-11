# Customization Guide

This guide explains how to customize the Processing Server for your own visual sketches. Whether you want to change colors, add new controls, or create entirely new visualizations, this document will walk you through the process.

## Table of Contents

1. [Quick Start: Basic Customizations](#quick-start-basic-customizations)
2. [Configuration Changes](#configuration-changes)
3. [Customizing the Browser UI](#customizing-the-browser-ui)
4. [Creating Your Own Processing Sketch](#creating-your-own-processing-sketch)
5. [Using Audio Data in Your Sketch](#using-audio-data-in-your-sketch)
6. [Adding New Event Types](#adding-new-event-types)
7. [Complete Example: Building a Particle System](#complete-example-building-a-particle-system)
8. [Using AI Coding Assistants](#using-ai-coding-assistants)

---

## Quick Start: Basic Customizations

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

### Debug Mode

Enable debug logging to see what's happening:

**In application.yaml:**
```yaml
debug:
  logging: true
```

**Or via URL parameter:**
```
https://localhost:8080/?debug
```

---

## Customizing the Browser UI

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

---

## Creating Your Own Processing Sketch

### Location

The Processing sketch is `src/main/java/com/processing/server/ProcessingSketch.java`.

### What to Preserve

You need to keep:

1. **Constructor signature** - Accepts `EventQueue`, `AudioBuffer`, width, height, `DebugConfig`
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
    
    private final Map<String, float[]> userPositions = new HashMap<>();
    
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
                    userPositions.computeIfAbsent(sessionId, k -> new float[]{0.5f, 0.5f});
                    float[] pos = userPositions.get(sessionId);
                    pos[0] = event.x();
                    pos[1] = event.y();
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
    // Handle keyboard input
}

@Override
public void exit() {
    // Cleanup before closing
}
```

---

## Using Audio Data in Your Sketch

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

## Adding New Event Types

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

AI coding assistants (like Cursor, GitHub Copilot, ChatGPT, Claude) can significantly speed up customization. Here's how to use them effectively.

### Give Context About the Project

When asking for help, provide context:

```
I'm working on a Processing.org visualization that receives touch, slider, 
and audio events from multiple browser clients via WebSocket. The project 
structure is:

- ProcessingSketch.java - The main Processing PApplet that draws visuals
- EventQueue.java - Thread-safe queue of UserInputEvent objects
- AudioBuffer.java - Per-session audio data ByteBuffer
- UserInputEvent.java - Record with sessionId, eventType, controlId, value, x, y

The draw() loop calls:
1. processEvents() - polls EventQueue for touch/slider events
2. processAudio() - polls AudioBuffer for audio levels
3. drawUsers() - renders user positions and audio-reactive elements

I want to [YOUR CUSTOMIZATION].
```

### Example Prompts

#### Adding a New Visual Element

```
I want to add a starfield background that responds to global audio levels.
When audio is high, particles should move faster. Can you show me how to:
1. Create a Star class for individual particles
2. Initialize stars in setup()
3. Update and draw stars in draw() based on smoothedGlobalAudioLevel
```

#### Creating a New Event Type

```
I want to add a "shake" event that makes all users' circles vibrate.
The browser sends: {"type":"shake","controlId":"shakeButton","value":0.5}
where value is intensity 0-1.

Show me:
1. How to modify index.html to send this event
2. How to handle it in processEvents()
3. How to create a shake effect that affects all users
```

#### Debugging Performance

```
When I have more than 5 users, the frame rate drops below 30 FPS.
Here's my current draw() method: [paste code]

How can I optimize this for better performance? Should I:
- Use P2D renderer instead of default?
- Reduce particle count?
- Use threading for event processing?
```

### Best Practices with AI Assistants

#### 1. Share Small, Focused Files

Instead of pasting the entire 200-line file, extract what's relevant:

```java
// Here's my current handleEvent method:
private void handleEvent(UserInputEvent event) {
    switch (event.eventType()) {
        case "touch" -> { /* ... */ }
        // Add new case here
    }
}
```

#### 2. Ask for Incremental Changes

Instead of: "Rewrite the entire sketch to add particles"

Ask: "Show me how to add a List<Particle> field and update it in draw()"

#### 3. Request Explanations

```
Can you explain why my particle system is causing flickering?
Here's my code: [paste]

Also explain how to use P2D renderer to fix this.
```

#### 4. Ask for Complete Examples

```
Can you provide a complete implementation of:
- Particle.java class
- List<Particle> field
- Particle emission on touch events
- Particle update and draw in draw()

Make it work with my existing code structure.
```

### Common Issues AI Can Help With

| Issue | Prompt |
|-------|--------|
| Users spawning on top of each other | "How do I ensure new users spawn at non-overlapping positions?" |
| Audio levels not accurate | "Show me how to calculate RMS amplitude from PCM byte data" |
| Performance issues | "Optimize my draw() method to maintain 60 FPS with 10+ users" |
| WebSocket not connecting | "Debug why WebSocket fails to connect over HTTPS" |
| Adding images/sprites | "Show me how to load and draw images in Processing" |
| 3D effects | "Convert my 2D ellipse to a 3D sphere using P3D renderer" |
| Saving screenshots | "Add a keyPress handler to save the canvas as PNG" |

### Code Review Prompt

After making changes, ask:

```
Review my modifications for:
1. Thread safety in concurrent access
2. Memory leaks or resource cleanup
3. Performance issues (excessive object creation)
4. Proper use of Processing lifecycle (settings/setup/draw)

Here's the changed code: [paste]
```

---

## Testing Your Customizations

### Local Testing

1. **Start the server:**
   ```bash
   ./run.sh  # or run.ps1
   ```

2. **Open multiple browser tabs:**
   ```
   https://localhost:8080/
   https://localhost:8080/?debug
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
   cp keystore.p12 src/main/resources/
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