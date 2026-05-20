# Processing Orchestra Library Guide

This document describes the intended first pass of the Processing Orchestra library.

It is a usage guide, not an implementation document.

The purpose of the library is to let a normal Processing IDE sketch receive browser control input with much less rewriting than the current converter approach.

This first pass is meant to cover:

- touch and drag input
- buttons
- sliders
- keyboard input
- motion input

This first pass does not include:

- browser audio streaming
- OSC as the main transport
- automatic conversion of multi-user sketches
- direct replacement of all built-in Processing input variables

## Main Idea

The library lets a normal PDE sketch connect to the existing controller server and read browser input directly.

Instead of converting the whole sketch into `ProcessingSketch.java`, the sketch stays in the Processing IDE and uses a library object such as:

```java
import processing.orchestra.*;

OrchestraInput orchestra;
```

The current controller server remains the browser-facing gateway.

The runtime flow is:

1. browser clients connect to the existing server
2. browser clients send WebSocket control events
3. the Processing library connects to the server as a sketch client
4. the library receives normalized control events
5. the PDE sketch reads those events through a small library API

## Intended Use Cases

The first pass is aimed at a few simple and practical teaching cases.

### 1. Simple browser-controlled sketch

Use this when you have a sketch that would normally use one mouse or one keyboard, and you want one browser client to drive it.

Examples:

- a circle follows touch position
- a key press toggles color
- a slider changes size
- phone tilt moves an object

This is the main compatibility use case.

### 2. Small classroom demo

Use this when one browser client should be treated as the active controller for a sketch running in the Processing IDE.

Examples:

- one student controls a sketch from a phone
- one tablet acts like a remote control surface
- one motion-enabled phone drives a visual

### 3. Introductory multi-user sketch

Use this when a sketch wants to know that several different browser clients are connected, but the sketch author still wants to stay in the Processing IDE.

Examples:

- draw one shape per connected session
- label each participant by session name
- let each user control their own position and color

This is better handled by the library's explicit session API than by trying to fake Processing's single-user mouse and keyboard model.

## Two Usage Styles

### Compatibility style

This is the simplest way to use the library.

The idea is:

- one browser session is treated as the active controller
- the sketch reads derived values such as compatibility mouse position or compatibility key state

This is best for older sketches or simple examples.

Example sketch:

```java
import processing.orchestra.*;

OrchestraInput orchestra;

void setup() {
  size(800, 600);
  orchestra = new OrchestraInput(this, "ws://localhost:8080/sketch-ws");
  orchestra.enableMouseCompatibility();
}

void draw() {
  background(0);
  ellipse(orchestra.compatMouseX(), orchestra.compatMouseY(), 80, 80);
}
```

Important note:

In the first pass, the library should provide its own compatibility accessors such as `compatMouseX()` instead of trying to overwrite Processing's built-in `mouseX` and `mouseY`.

That keeps the behavior predictable.

### Session-aware style

This is for sketches that want to work with more than one connected browser client.

The idea is:

- the sketch loops over connected sessions
- the sketch reads state for each session directly

Example sketch:

```java
import processing.orchestra.*;

OrchestraInput orchestra;

void setup() {
  size(800, 600);
  orchestra = new OrchestraInput(this, "ws://localhost:8080/sketch-ws");
}

void draw() {
  background(0);

  for (String sessionId : orchestra.sessions()) {
    TouchState touch = orchestra.touch(sessionId);
    if (touch == null || !touch.active) {
      continue;
    }

    float x = touch.xNormalized * width;
    float y = touch.yNormalized * height;
    ellipse(x, y, 40, 40);
  }
}
```

This is the better model for true multi-user sketches.

## First-Pass Features

### Touch and drag

The library should expose touch position in normalized coordinates.

Expected use:

```java
TouchState touch = orchestra.touch(sessionId);
```

or in compatibility mode:

```java
float x = orchestra.compatMouseX();
float y = orchestra.compatMouseY();
```

### Sliders

The library should expose slider values as normalized floats from `0..1`.

Expected use:

```java
float size = orchestra.slider(sessionId, "sizeSlider");
```

### Buttons

Buttons are better treated as discrete actions than continuous state.

Expected use:

```java
if (orchestra.consumeButtonPress(sessionId, "action1")) {
  // trigger an effect
}
```

### Keyboard

The library should support both:

- a simple compatibility key view
- a fuller per-session key state

Compatibility example:

```java
if (orchestra.compatKeyPressed()) {
  if (orchestra.compatKey() == ' ') {
    // toggle something
  }
}
```

Session-aware example:

```java
if (orchestra.isKeyDown(sessionId, "ArrowLeft")) {
  // move left
}
```

### Motion

The first pass should include motion because it already fits the controller model well.

Compatibility example:

```java
float x = width / 2 + orchestra.compatGamma() * 4;
float y = height / 2 + orchestra.compatBeta() * 4;
ellipse(x, y, 60, 60);
```

Session-aware example:

```java
MotionState motion = orchestra.motion(sessionId);
if (motion != null) {
  float size = 30 + motion.magnitude * 20;
  ellipse(x, y, size, size);
}
```

## Session Metadata

The first pass should expose at least simple session metadata, especially a saved display name.

That supports examples such as:

- draw the participant's name above their circle
- choose a compatibility session by name
- show who is currently connected

Example:

```java
String name = orchestra.sessionName(sessionId);
text(name, x, y - 20);
```

## What A Student Should Expect

This library is meant to reduce friction, not erase all differences between a browser-controlled sketch and a local Processing sketch.

Students should expect:

- less rewriting than the converter approach for common control events
- a normal PDE workflow in the Processing IDE
- one clear compatibility path for simple sketches
- one clear session-aware path for multi-user sketches

Students should not expect:

- audio streaming in the first pass
- magic replacement of every built-in Processing input variable
- automatic support for every keyboard quirk on every mobile device

## Example Sketch Patterns

### Pattern 1: One circle controlled by browser touch

```java
import processing.orchestra.*;

OrchestraInput orchestra;

void setup() {
  size(800, 600);
  orchestra = new OrchestraInput(this, "ws://localhost:8080/sketch-ws");
  orchestra.enableMouseCompatibility();
}

void draw() {
  background(0);
  fill(255, 150, 0);
  ellipse(orchestra.compatMouseX(), orchestra.compatMouseY(), 80, 80);
}
```

### Pattern 2: Browser keys toggle and adjust color

```java
import processing.orchestra.*;

OrchestraInput orchestra;
float hueValue = 0;
boolean ringVisible = false;

void setup() {
  size(800, 600);
  colorMode(HSB, 360, 100, 100);
  orchestra = new OrchestraInput(this, "ws://localhost:8080/sketch-ws");
  orchestra.enableKeyboardCompatibility();
}

void draw() {
  background(0);

  if (orchestra.compatKeyPressed()) {
    if (orchestra.compatKey() == ' ') {
      ringVisible = true;
    }
  }

  fill(hueValue, 80, 100);
  ellipse(width / 2, height / 2, 100, 100);

  if (ringVisible) {
    noFill();
    stroke(hueValue, 80, 100);
    ellipse(width / 2, height / 2, 150, 150);
  }
}
```

### Pattern 3: Motion-controlled sketch

```java
import processing.orchestra.*;

OrchestraInput orchestra;

void setup() {
  size(800, 600);
  orchestra = new OrchestraInput(this, "ws://localhost:8080/sketch-ws");
  orchestra.enableMotionCompatibility();
}

void draw() {
  background(0);
  float x = width / 2 + orchestra.compatGamma() * 4;
  float y = height / 2 + orchestra.compatBeta() * 4;
  float size = 60 + orchestra.compatMagnitude() * 20;
  ellipse(x, y, size, size);
}
```

### Pattern 4: Multi-user touch sketch with names

```java
import processing.orchestra.*;

OrchestraInput orchestra;

void setup() {
  size(800, 600);
  textAlign(CENTER);
  orchestra = new OrchestraInput(this, "ws://localhost:8080/sketch-ws");
}

void draw() {
  background(0);

  for (String sessionId : orchestra.sessions()) {
    TouchState touch = orchestra.touch(sessionId);
    if (touch == null || !touch.active) {
      continue;
    }

    float x = touch.xNormalized * width;
    float y = touch.yNormalized * height;

    ellipse(x, y, 40, 40);
    text(orchestra.sessionName(sessionId), x, y - 20);
  }
}
```

## Recommended First-Pass Limits

To keep the first pass reliable:

- support WebSocket only
- support control events and motion only
- keep audio out
- avoid trying to overwrite Processing internals
- treat compatibility mode as single-controller mode
- keep multi-user support explicit

## Relationship To The Converter

The converter and the library solve different problems.

The converter is still useful when:

- you want to move a sketch into the server app
- you want full integration with the current `ProcessingSketch.java` model
- you are teaching how the server-side sketch is wired

The library is better when:

- you want to stay in the Processing IDE
- you want to prototype quickly
- you want simple browser control without rewriting the whole sketch

## Next Step

The next step after this first-pass guide is to write the implementation-oriented documentation for:

- the library API surface
- the expected WebSocket message stream for sketch clients
- the small amount of server support needed for a dedicated sketch-client connection

For implementation details, see:

- [PROCESSING_ORCHESTRA_LIBRARY_SPEC.md](PROCESSING_ORCHESTRA_LIBRARY_SPEC.md)
