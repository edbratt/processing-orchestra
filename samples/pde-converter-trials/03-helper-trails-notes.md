# 03 Helper Trails Notes

## Purpose

This sketch is a slightly more realistic trial.

It tests:

- multiple top-level fields
- several helper methods
- local mouse input through `mousePressed()`
- simple animation state

## Why it is useful

This is closer to the kinds of sketches students often write:

- drawing code split into helper methods
- local interaction
- a little animation
- a little state change

It is still small enough that the converter should remain predictable.

## What should convert cleanly

- field declarations
- helper methods
- `setup()`
- `draw()`
- `mousePressed()`

## What should stay a manual decision

- whether `mousePressed()` should remain local
- whether local mouse click should become browser touch input
- whether the sketch should stay single-object or grow into a multi-user sketch

## Good conversion outcome

A good result would:

- preserve the helper-method structure
- keep `mousePressed()` intact in the generated sketch
- mention that local mouse input can later be mapped to browser touch if desired

## Tutorial

This tutorial walks through the full workflow for the press-style mouse sketch:

1. run the base converter
2. inspect the default output
3. run the converter again with `browser-touch-press`
4. compare that automated result to the manual conversion steps
5. rebuild and run the sketch in the app

## Goal

Replace local `mousePressed()` behavior with browser touch events that trigger the same reposition-and-recolor logic.

## Step 1: Run the base converter first

```powershell
java -cp target\classes com.processing.server.tools.pde.PdeToProcessingSketchMain .\samples\pde-converter-trials\03-helper-trails.pde --output-dir .\target\pde-output\03-helper-trails
```

This writes:

- `target/pde-output/03-helper-trails/ProcessingSketchGenerated.java`
- `target/pde-output/03-helper-trails/pde-migration-report.md`

## Step 2: Review the base output

What to look for:

- the helper-method structure should still be visible
- local `mousePressed()` should still be present
- the report should explain that browser touch press mapping is still a decision

## Step 3: Run the mode-specific converter

```powershell
java -cp target\classes com.processing.server.tools.pde.PdeToProcessingSketchMain .\samples\pde-converter-trials\03-helper-trails.pde --mode browser-touch-press --output-dir .\target\pde-output\03-helper-trails-browser-touch-press
```

This writes:

- `target/pde-output/03-helper-trails-browser-touch-press/ProcessingSketchGenerated.java`
- `target/pde-output/03-helper-trails-browser-touch-press/pde-migration-report.md`

What this mode should add:

- `touchX` / `touchY`
- touch-aware `processEvents()`
- a generated `handleBrowserTouchPress()` helper

## Step 4: Place reviewed runnable output in the generated source tree

Raw converter output stays under `target/pde-output/...`.

If you want a reviewed runnable class in the app, keep it under:

- `generated-src/main/java/com/processing/server/`

For this sample, the reviewed runnable version is:

- `generated-src/main/java/com/processing/server/HelperTrailsBrowserTouchPressSketch.java`

## Step 5: Manual conversion steps

If you want to do the conversion manually instead of using the mode, work from the preserved local version.

In `processing-server/generated-src/main/java/com/processing/server/HelperTrailsSketch.java`:

### 1. Leave the helper methods alone

You do not need to rewrite:

- `fadeBackground()`
- `updateBall()`
- `drawBall()`
- `drawLabel()`

### 2. Add normalized browser touch position fields

```java
private float touchX = 0.5f;
private float touchY = 0.5f;
```

### 3. Replace the empty `processEvents()` with touch handling

```java
private void processEvents() {
    while (!eventQueue.isEmpty()) {
        UserInputEvent event = eventQueue.poll();
        if (event == null) {
            break;
        }

        if (!"touch".equals(event.eventType())) {
            continue;
        }

        touchX = constrain(event.x(), 0f, 1f);
        touchY = constrain(event.y(), 0f, 1f);
        handleBrowserTouchPress();
    }
}
```

### 4. Replace the local `mousePressed()` override with a helper

Delete the local `mousePressed()` method and add:

```java
private void handleBrowserTouchPress() {
    float browserMouseX = touchX * width;
    float browserMouseY = touchY * height;

    ballX = browserMouseX;
    ballY = browserMouseY;
    trailHue = random(360);
}
```

### 5. Update the label

```java
text("Touch in the browser to reposition and recolor", width / 2f, height - 24);
```

### 6. Update the TODO comment

```java
// Browser touch events now trigger the old mousePressed behavior through EventQueue.
```

## Result

After this manual change:

- the helper-method structure stays intact
- the animation still runs the same way
- browser touch now repositions and recolors the moving ball
- local Processing-window mouse input is no longer required

## Step 6: Rebuild and run in the app

Rebuild:

```powershell
mvn package -DskipTests
```

Run the reviewed browser-touch-press version:

```powershell
java "-Dprocessing.sketch-class=com.processing.server.HelperTrailsBrowserTouchPressSketch" -jar .\target\processing-server-1.0-SNAPSHOT.jar
```

Open:

- `http://localhost:8080/`

Drag or tap in the browser touch area.

Expected behavior:

- the moving ball jumps to the new browser-controlled position
- the hue changes each time a touch event is handled
- the trail effect continues to work

## Compare the results

After you try both the base conversion and the browser-touch-press conversion, compare:

- what the base converter preserved
- what the `browser-touch-press` mode added automatically
- whether the generated mode result matches the manual edit closely enough for teaching use
