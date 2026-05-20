# Processing IDE Integration Guide

This guide shows how to use the Processing IDE to develop a sketch first, then move that sketch into this project so it can work with the browser controller, audio input, and phone motion input.

The goal is to keep the process simple:

1. build the visual idea in the Processing IDE
2. move it into `ProcessingSketch.java`
3. connect it to the controller inputs already used by this project

## Table of Contents

- [Why Start In The Processing IDE](#why-start-in-the-processing-ide)
- [What Changes When You Move Into This Project](#what-changes-when-you-move-into-this-project)
- [Stage 1: Start With A Very Simple Sketch](#stage-1-start-with-a-very-simple-sketch)
- [Stage 2: Replace Local Mouse Input With Controller Input](#stage-2-replace-local-mouse-input-with-controller-input)
- [Stage 3: Add Per-User State](#stage-3-add-per-user-state)
- [Stage 4: Add Slider Control](#stage-4-add-slider-control)
- [Stage 5: Add Audio Reactivity](#stage-5-add-audio-reactivity)
- [Stage 6: Add Motion Input](#stage-6-add-motion-input)
- [Migration Checklist](#migration-checklist)
- [Rules Of Thumb](#rules-of-thumb)

## Why Start In The Processing IDE

The Processing IDE is a good place to begin because it gives you a very fast way to experiment.

You can:

- change shapes, colors, and movement quickly
- test visual ideas without thinking about the server
- learn the drawing logic before adding browser controls

This is especially useful for students. It lets you focus on the visual part first.

## What Changes When You Move Into This Project

A simple Processing IDE sketch is usually one file with:

- some variables at the top
- `setup()`
- `draw()`
- maybe local mouse or keyboard input

In this project, the sketch becomes part of a larger application.

That means:

- the sketch lives in `src/main/java/com/processing/server/ProcessingSketch.java`
- it still uses `settings()`, `setup()`, and `draw()`
- it must keep the constructor and `runSketch()`
- input now comes from browser clients instead of only the local mouse and keyboard

The important shift is this:

- in the Processing IDE, your sketch usually reads local input directly
- in this project, your sketch reads stored input that came through the server

For keyboard input, there are now two possible paths:

- browser keyboard input:
  the browser page sends `key` events over WebSocket when that page has focus
- local Processing keyboard input:
  a sketch can still use `keyPressed()` and `keyReleased()` when the Processing window itself has focus

So if a converted sketch still preserves local keyboard logic, that local logic only works while the Processing window is active. The browser keyboard protocol only works while the browser page is active.

## Stage 1: Start With A Very Simple Sketch

Contents:
- [Processing IDE Version](#processing-ide-version)
- [Project Version](#project-version)
- [What Changed](#what-changed)

### Processing IDE Version

Start with a sketch that only draws and animates one shape.

```java
float circleX = 200;
float circleY = 200;
float speed = 2;

void setup() {
  size(400, 400);
  noStroke();
}

void draw() {
  background(0);

  circleX += speed;
  if (circleX > width - 40 || circleX < 40) {
    speed *= -1;
  }

  fill(0, 200, 255);
  ellipse(circleX, circleY, 80, 80);
}
```

This is a good first step because it teaches the basic Processing pattern:

- set things up once
- update values every frame
- draw the current frame

### Project Version

In this project, the same idea becomes part of `ProcessingSketch.java`.

```java
public class ProcessingSketch extends PApplet {
    private float circleX = 200;
    private float circleY = 200;
    private float speed = 2;

    @Override
    public void settings() {
        size(sketchWidth, sketchHeight, JAVA2D);
    }

    @Override
    public void setup() {
        noStroke();
    }

    @Override
    public void draw() {
        background(0);

        circleX += speed;
        if (circleX > width - 40 || circleX < 40) {
            speed *= -1;
        }

        fill(180, 80, 100);
        ellipse(circleX, circleY, 80, 80);
    }
}
```

### What Changed

- the Processing IDE variables became class fields
- the sketch now lives inside a Java class
- `setup()` and `draw()` still work the same way

At this stage, the sketch is not yet using browser input.

---

## Stage 2: Replace Local Mouse Input With Controller Input

Contents:
- [Processing IDE Version](#processing-ide-version-1)
- [Project Version](#project-version-1)
- [What Changed](#what-changed-1)

### Processing IDE Version

A student might next write a sketch that follows the local mouse:

```java
float circleSize = 60;

void setup() {
  size(400, 400);
  noStroke();
}

void draw() {
  background(20);
  fill(255, 120, 80);
  ellipse(mouseX, mouseY, circleSize, circleSize);
}
```

### Project Version

In this project, the browser sends touch data through the controller. The sketch should read stored user positions instead of `mouseX` and `mouseY`.

```java
private void drawUsers() {
    for (Map.Entry<String, float[]> entry : userPositions.entrySet()) {
        String sessionId = entry.getKey();
        float[] pos = entry.getValue();

        float x = pos[0] * sketchWidth;
        float y = pos[1] * sketchHeight;

        fill(20, 80, 100);
        noStroke();
        ellipse(x, y, 60, 60);
    }
}
```

And the touch position is updated earlier from browser events:

```java
private void handleEvent(UserInputEvent event) {
    String sessionId = event.sessionId();

    switch (event.eventType()) {
        case "touch" -> {
            if (!userPositions.containsKey(sessionId)) {
                initializeUser(sessionId);
            }
            float[] targetPos = userTargetPositions.get(sessionId);
            targetPos[0] = event.x();
            targetPos[1] = event.y();
        }
    }
}
```

### What Changed

- one local mouse pointer became many browser users
- `mouseX` and `mouseY` were replaced by stored user positions
- positions are tied to each browser session

---

## Stage 3: Add Per-User State

Contents:
- [Processing IDE Version](#processing-ide-version-2)
- [Project Version](#project-version-2)
- [What Changed](#what-changed-2)

### Processing IDE Version

A simple sketch might use one shared color value:

```java
float hueValue = 0;

void setup() {
  size(400, 400);
  colorMode(HSB, 360, 100, 100);
}

void draw() {
  background(0);
  hueValue = (hueValue + 1) % 360;
  fill(hueValue, 80, 100);
  ellipse(width / 2, height / 2, 100, 100);
}
```

### Project Version

In this project, each user usually needs separate state. A color becomes one value per session instead of one value for the whole sketch.

```java
private final Map<String, float[]> userColors = new HashMap<>();
```

Then when a new user is initialized:

```java
private void initializeUser(String sessionId) {
    float[] position = findNonOverlappingPosition();
    userPositions.put(sessionId, position);
    userTargetPositions.put(sessionId, position.clone());
    userColors.put(sessionId, new float[]{random(360), 70, 100});
}
```

And later in drawing:

```java
float[] color = userColors.getOrDefault(sessionId, new float[]{0, 70, 100});
fill(color[0], color[1], color[2]);
ellipse(x, y, 80, 80);
```

### What Changed

- one sketch-wide variable became per-user state
- the sketch keeps that state by session ID
- this is how many users can each have their own visual behavior

---

## Stage 4: Add Slider Control

Contents:
- [Processing IDE Version](#processing-ide-version-3)
- [Project Version](#project-version-3)
- [What Changed](#what-changed-3)

### Processing IDE Version

In a standalone sketch, you may just hardcode a size value:

```java
float circleSize = 50;
```

### Project Version

In this project, that value can come from the browser UI.

Store the size for each user:

```java
private final Map<String, Float> userSizes = new HashMap<>();
```

Update the value from slider events:

```java
case "slider" -> {
    if (!userPositions.containsKey(sessionId)) {
        initializeUser(sessionId);
    }
    if ("sizeSlider".equals(event.controlId())) {
        userSizes.put(sessionId, constrain(event.value(), 0, 1));
    }
}
```

Use it while drawing:

```java
float sizeValue = userSizes.getOrDefault(sessionId, 0.5f);
float baseSize = map(sizeValue, 0, 1, 20, 90);
ellipse(x, y, baseSize, baseSize);
```

### What Changed

- a fixed number became a browser-controlled value
- the browser sends the slider event
- the sketch stores that value and uses it when drawing

---

## Stage 5: Add Audio Reactivity

Contents:
- [Project Hook Points](#project-hook-points)
- [Example Pattern](#example-pattern)
- [What Changed](#what-changed-4)

### Project Hook Points

Audio does not usually exist in a simple Processing IDE sketch unless you build it yourself.

In this project, audio already comes from browser clients and is stored for the sketch.

The main places to work with it are:

- `processAudio()`
- `calculateAudioLevel(String sessionId)`
- `drawUsers()`

### Example Pattern

```java
private void processAudio() {
    for (String sessionId : audioBuffer.getActiveSessionIds()) {
        float level = calculateAudioLevel(sessionId);
        userAudioLevels.put(sessionId, new float[]{level});

        if (!userPositions.containsKey(sessionId)) {
            initializeUser(sessionId);
        }
    }
}
```

Then in drawing:

```java
float[] audioLevel = userAudioLevels.getOrDefault(sessionId, new float[]{0});
float audioScale = 1 + audioLevel[0] * 2;
float pulseSize = baseSize * audioScale;

noFill();
stroke(color[0], color[1], color[2]);
ellipse(x, y, pulseSize, pulseSize);
```

### What Changed

- the sketch no longer reads local sound directly
- it reads stored audio data that came through the server
- each user can have a different audio level

---

## Stage 6: Add Motion Input

Contents:
- [Project Hook Points](#project-hook-points-1)
- [Example Pattern](#example-pattern-1)
- [What Changed](#what-changed-5)

### Project Hook Points

Phone motion is also already integrated into this project.

The main places to work with it are:

- `handleEvent(UserInputEvent event)`
- `handleMotionEvent(String sessionId, UserInputEvent event)`
- `drawUsers()`

### Example Pattern

Motion events are routed like this:

```java
case "motion" -> {
    if (!userPositions.containsKey(sessionId)) {
        initializeUser(sessionId);
    }
    handleMotionEvent(sessionId, event);
}
```

And later used in drawing:

```java
float[] motion = userMotion.getOrDefault(sessionId, new float[]{0f, 0f, 0f, 0f, 0f, 0f, 0f});

float gammaOffset = map(
    motion[2],
    -motionConfig.getGammaClampDegrees(),
    motionConfig.getGammaClampDegrees(),
    -motionConfig.getTiltOffsetNormalized(),
    motionConfig.getTiltOffsetNormalized()
);
```

### What Changed

- motion does not come from the local keyboard or mouse
- it arrives as structured browser data
- the sketch stores it and uses it as another visual input

---

## Migration Checklist

When moving a sketch from the Processing IDE into this project:

1. Move sketch-global variables into class fields.
2. Keep the Processing lifecycle methods:
   - `settings()`
   - `setup()`
   - `draw()`
3. Preserve the constructor and `runSketch()`.
4. Decide which parts are:
   - purely visual logic
   - per-user state
   - controller-driven behavior
5. Replace local input like `mouseX`, `mouseY`, or key state with:
   - stored event-driven values
   - per-session maps
   - audio from `AudioBuffer`
   - motion from `handleMotionEvent(...)`
   - browser `key` events if you want keyboard control from the web page
6. Keep event handling separate from drawing:
   - update stored state first
   - draw from stored state later

## Rules Of Thumb

- use the Processing IDE first when you are inventing the visual idea
- move into this project when you want browser control, multi-user input, audio, or motion
- start by integrating the visual part first
- add sliders, buttons, audio, and motion one step at a time
- do not rewrite the server code just to change the look of the sketch
- decide explicitly whether keyboard behavior should stay local to the Processing window, move to the browser page, or support both

The best workflow is:

1. prototype the visual behavior simply
2. move it into `ProcessingSketch.java`
3. connect touch, sliders, or buttons
4. add audio and motion only if the sketch needs them
