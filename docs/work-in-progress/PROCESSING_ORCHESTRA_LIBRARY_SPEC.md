# Processing Orchestra Library Spec

This document defines a first-pass Processing library for standard Processing IDE sketches.

The goal is to let a normal PDE sketch receive browser control input with much less rewriting than the current converter approach.

This first pass includes:

- touch / drag input
- button events
- slider values
- keyboard events
- motion events

This first pass does not include:

- browser audio streaming
- direct replacement of all Processing input internals
- automatic multi-user sketch rewriting

## Purpose

The library should make it possible to:

1. keep a sketch as a normal Processing sketch
2. connect that sketch to the existing browser controller server
3. receive browser control events directly inside the sketch
4. support both:
   - a simple compatibility mode for older single-user sketches
   - a more explicit API for newer multi-user sketches

## Non-Goals

This library is not meant to:

- replace the Helidon server
- replace the browser UI
- carry raw browser audio in the first version
- hide all differences between browser input and local Processing input

## High-Level Architecture

The current controller server remains the browser-facing gateway.

Runtime flow:

1. browser clients connect to the existing Helidon server
2. browser clients send WebSocket control events
3. the Processing library connects to the server as another client
4. the library receives normalized control events
5. the sketch reads those events through:
   - compatibility fields
   - library state accessors
   - optional callback methods

So the library becomes a Processing-side runtime adapter, not a replacement for the server.

## Transport

### First-pass choice

Use WebSocket.

Why:

- the browser side already uses WebSocket
- the current server already uses WebSocket
- the existing event model is already defined in JSON
- session-aware browser control fits naturally here

### Not first-pass

Do not use OSC as the primary transport in v1.

OSC may still be useful later as an optional bridge for other media tools, but it is not the simplest path for browser-to-PDE control.

## Packaging Model

This should be a standard Processing library that can be dropped into the Processing libraries folder.

Expected shape:

- `processing-orchestra/`
  - `library/processing-orchestra.jar`
  - `examples/...`
  - `reference/...`

The library should be usable from a PDE sketch like this:

```java
import processing.orchestra.*;
```

## First-Pass API Design

### Main library object

```java
OrchestraInput orchestra;
```

Basic setup:

```java
void setup() {
  size(800, 600);
  orchestra = new OrchestraInput(this, "ws://localhost:8080/ws");
}
```

Suggested constructor options:

```java
OrchestraInput(PApplet parent, String websocketUrl)
OrchestraInput(PApplet parent, OrchestraConfig config)
```

## Two Usage Modes

### 1. Compatibility mode

This mode is meant to reduce changes for older single-user sketches.

It should support:

- one selected session as the active compatibility session
- derived compatibility values for:
  - mouse position
  - mouse pressed state
  - keyboard key
  - keyboard keyCode
  - motion values

This mode should not pretend to support multi-user sketches automatically.

Instead, it should clearly be:

- "treat one browser client as the active controller"

Possible setup:

```java
void setup() {
  size(800, 600);
  orchestra = new OrchestraInput(this, "ws://localhost:8080/ws");
  orchestra.enableMouseCompatibility();
  orchestra.enableKeyboardCompatibility();
  orchestra.enableMotionCompatibility();
}
```

### 2. Native library mode

This mode is for sketches that want explicit session-aware control.

It should expose:

- session list
- per-session touch state
- per-session button events
- per-session key state
- per-session motion state
- latest slider values

This is the better long-term model for multi-user sketches.

## Compatibility Mode Behavior

### Mouse compatibility

The library should provide compatibility fields or accessors for:

- `compatMouseX`
- `compatMouseY`
- `compatMousePressed`
- possibly `compatPmouseX`
- possibly `compatPmouseY`

Important:

These should be library-managed values, not attempts to overwrite Processing's internal `mouseX` and `mouseY` directly.

That means the sketch would use:

```java
ellipse(orchestra.compatMouseX(), orchestra.compatMouseY(), 80, 80);
```

not:

```java
ellipse(mouseX, mouseY, 80, 80);
```

in the first version.

This is safer and clearer.

### Keyboard compatibility

The library should provide:

- `compatKey()`
- `compatKeyCode()`
- `compatKeyPressed()`

This supports sketches that want "the current browser key state" without managing sessions directly.

### Motion compatibility

The library should provide:

- `compatAlpha()`
- `compatBeta()`
- `compatGamma()`
- `compatAx()`
- `compatAy()`
- `compatAz()`
- `compatMagnitude()`

These should come from the active compatibility session.

## Native Multi-User API

### Session access

```java
Set<String> sessions()
boolean hasSession(String sessionId)
```

### Touch state

```java
TouchState touch(String sessionId)
```

Suggested fields:

```java
class TouchState {
  float xNormalized;
  float yNormalized;
  boolean active;
  long timestamp;
}
```

### Slider state

```java
float slider(String sessionId, String controlId)
Map<String, Float> sliders(String sessionId)
```

### Button state or events

For buttons, the library should probably support recent-event style access rather than only current state.

Example:

```java
boolean consumeButtonPress(String sessionId, String controlId)
```

### Keyboard state

```java
KeyState keyState(String sessionId)
boolean isKeyDown(String sessionId, String key)
```

Suggested structure:

```java
class KeyState {
  String lastKey;
  int lastKeyCode;
  boolean pressed;
  Set<String> downKeys;
}
```

### Motion state

```java
MotionState motion(String sessionId)
```

Suggested structure:

```java
class MotionState {
  float alpha;
  float beta;
  float gamma;
  float ax;
  float ay;
  float az;
  float magnitude;
  long timestamp;
}
```

## Event Callback Support

The library should optionally call sketch methods if they exist.

This gives sketches a callback-style alternative to polling.

### Suggested callback names

```java
void orchestraConnected()
void orchestraDisconnected()
void orchestraSessionStarted(String sessionId)
void orchestraSessionEnded(String sessionId)

void orchestraTouch(String sessionId, float x, float y)
void orchestraButton(String sessionId, String controlId)
void orchestraSlider(String sessionId, String controlId, float value)
void orchestraKey(String sessionId, String key, int keyCode, boolean pressed)
void orchestraMotion(String sessionId, float alpha, float beta, float gamma,
                     float ax, float ay, float az, float magnitude)
```

These should be optional. If the method does not exist, the library should simply skip it.

## Active Compatibility Session

Compatibility mode needs one selected session.

### First-pass rule

Default to:

- the first connected session

Optional helpers:

```java
String compatibilitySession()
void setCompatibilitySession(String sessionId)
void selectLatestSession()
```

This keeps the model simple for classroom demos.

## Polling And Update Model

The library should run its WebSocket/network work outside the Processing `draw()` loop, but make the latest state available safely to the sketch.

Recommended model:

- background thread receives WebSocket messages
- library updates synchronized internal state
- sketch reads the current state during `draw()`

For event-style actions like button presses, the library should support consumption semantics:

```java
if (orchestra.consumeButtonPress(sessionId, "action1")) {
  // trigger effect
}
```

## Connection Model

### First-pass behavior

The library should:

- connect to the supplied WebSocket URL
- reconnect automatically after disconnects
- expose connection status

Suggested accessors:

```java
boolean connected()
String connectionStatus()
```

## Message Model

The library should use the existing browser/server message shapes where possible.

Expected incoming events:

- `touch`
- `slider`
- `button`
- `key`
- `motion`

The current server may need one addition for this design:

- a way for non-browser clients to subscribe to normalized event output

Two possible approaches:

### Option A: dedicated WebSocket route for sketch/library clients

Example:

- `/sketch-ws`

This route would send normalized event messages to the Processing library.

### Option B: reuse `/ws` with a client role

Example:

- client sends a hello message like:

```json
{ "type": "client-role", "role": "sketch-listener" }
```

Then the server broadcasts normalized event messages to that connection.

I recommend Option A because it is clearer and easier to teach.

## Required Server Support

The current app already gathers browser events centrally.

To support this library, the server will likely need:

1. a normalized outbound event stream for sketch clients
2. session lifecycle messages
3. slider/button/key/motion/touch forwarding

This is much smaller than rewriting the whole app.

## First-Pass Event Semantics

### Touch

Touch events should be sent as normalized coordinates:

- `x` in `0..1`
- `y` in `0..1`

### Slider

Slider values should be sent as normalized floats:

- `0..1`

### Button

Buttons should be sent as discrete press events.

### Keyboard

Keyboard should send:

- `key`
- `keyCode`
- `action` (`pressed` / `released`)

### Motion

Motion should send:

- `alpha`
- `beta`
- `gamma`
- `ax`
- `ay`
- `az`
- `magnitude`

This should match the current app's motion event model as closely as possible.

## First-Pass Sketch Examples

### Example 1: Compatibility-style mouse sketch

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

### Example 2: Compatibility-style motion sketch

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
  ellipse(x, y, 60 + orchestra.compatMagnitude() * 20, 60 + orchestra.compatMagnitude() * 20);
}
```

### Example 3: Native multi-user sketch

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
    MotionState motion = orchestra.motion(sessionId);
    if (touch == null) {
      continue;
    }

    float x = touch.xNormalized * width;
    float y = touch.yNormalized * height;
    float size = 30;
    if (motion != null) {
      size += motion.magnitude * 20;
    }

    ellipse(x, y, size, size);
  }
}
```

## First-Pass Class List

Suggested library classes:

- `OrchestraInput`
- `OrchestraConfig`
- `TouchState`
- `MotionState`
- `KeyState`
- `SliderState` or plain map-based slider access

Optional internal classes:

- `OrchestraWebSocketClient`
- `CompatibilityState`
- `SessionState`

## Recommended First-Pass Constraints

To keep this reliable:

- no audio support in v1
- no OSC support in v1
- no attempt to overwrite Processing internals like `mouseX` directly
- no automatic multi-user sketch rewriting
- no attempt to support every browser/mobile keyboard quirk

## Likely Follow-Up Phases

### Phase 2

- richer compatibility helpers
- button-consume utilities
- stronger session lifecycle handling

### Phase 3

- optional audio support
- optional OSC bridge
- optional direct sketch-to-sketch or tool-to-sketch interoperability

## Recommendation

The best first version is:

- a standard Processing library
- WebSocket-based
- compatible with the current controller server
- includes touch, buttons, sliders, keyboard, and motion
- supports both:
  - compatibility mode
  - native multi-user access

That should give much better direct integration with standard PDE sketches than continuing to rely only on source conversion.
