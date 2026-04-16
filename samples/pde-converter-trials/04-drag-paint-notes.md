# 04 Drag Paint Notes

## Purpose

This sketch is the first drag-style trial in the set.

It tests:

- top-level fields
- helper methods
- `setup()`
- `draw()`
- local mouse input through `mouseDragged()`

## Why it is useful

This is the smallest clean example of a sketch where the important local behavior is not a click and not just reading `mouseX` in `draw()`.

That makes it a good fit for a narrow drag-oriented converter mode.

## What should convert cleanly

- field declarations
- helper methods
- `setup()`
- `draw()`
- `mouseDragged()`

## What should stay a manual decision

- whether drag behavior should remain local
- whether browser touch events should fully replace drag behavior
- whether the sketch should later become multi-user

## Good conversion outcome

A good result would:

- preserve the helper-method structure
- preserve the simple brush logic
- map browser touch events into the old drag behavior with minimal editing

## Tutorial

This tutorial walks through the full workflow for the drag-style mouse sketch:

1. run the base converter
2. inspect the default output
3. run the converter again with `browser-touch-drag`
4. compare that automated result to the manual conversion steps
5. rebuild and run the sketch in the app

## Goal

Replace local `mouseDragged()` behavior with browser touch events that continuously update the brush position and color.

## Step 1: Run the base converter first

```powershell
java -cp target\classes com.processing.server.tools.pde.PdeToProcessingSketchMain .\samples\pde-converter-trials\04-drag-paint.pde --output-dir .\target\pde-output\04-drag-paint
```

This writes:

- `target/pde-output/04-drag-paint/ProcessingSketchGenerated.java`
- `target/pde-output/04-drag-paint/pde-migration-report.md`

## Step 2: Review the base output

What to look for:

- the helper-method structure should still be visible
- local `mouseDragged()` should still be present
- the report should explain that browser drag mapping is still a decision

## Step 3: Run the mode-specific converter

```powershell
java -cp target\classes com.processing.server.tools.pde.PdeToProcessingSketchMain .\samples\pde-converter-trials\04-drag-paint.pde --mode browser-touch-drag --output-dir .\target\pde-output\04-drag-paint-browser-touch-drag
```

This writes:

- `target/pde-output/04-drag-paint-browser-touch-drag/ProcessingSketchGenerated.java`
- `target/pde-output/04-drag-paint-browser-touch-drag/pde-migration-report.md`

What this mode should add:

- `touchX` / `touchY`
- touch-aware `processEvents()`
- a generated `handleBrowserTouchDrag()` helper

## Step 4: Place reviewed runnable output in the generated source tree

Raw converter output stays under `target/pde-output/...`.

If you want a reviewed runnable class in the app, keep it under:

- `generated-src/main/java/com/processing/server/`

For this sample, the reviewed runnable version is:

- `generated-src/main/java/com/processing/server/DragPaintBrowserTouchDragSketch.java`

## Step 5: Manual conversion steps

If you want to do the conversion manually instead of using the mode, work from the preserved local version.

In `processing-server/generated-src/main/java/com/processing/server/DragPaintSketch.java`:

### 1. Leave the helper methods alone

You do not need to rewrite:

- `fadeBackground()`
- `drawBrush()`
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
        handleBrowserTouchDrag();
    }
}
```

### 4. Replace the local `mouseDragged()` override with a helper

Delete the local `mouseDragged()` method and add:

```java
private void handleBrowserTouchDrag() {
    float browserMouseX = touchX * width;
    float browserMouseY = touchY * height;

    brushX = browserMouseX;
    brushY = browserMouseY;
    brushHue = (brushHue + 12) % 360;
}
```

### 5. Update the label

```java
text("Drag in the browser to paint", width / 2f, height - 24);
```

### 6. Update the TODO comment

```java
// Browser touch events now trigger the old mouseDragged behavior through EventQueue.
```

## Result

After this manual change:

- the helper-method structure stays intact
- the brush still moves and changes color continuously
- browser touch now drives the old drag behavior
- local Processing-window dragging is no longer required

## Step 6: Rebuild and run in the app

Rebuild:

```powershell
mvn package -DskipTests
```

Run the reviewed browser-touch-drag version:

```powershell
java "-Dprocessing.sketch-class=com.processing.server.DragPaintBrowserTouchDragSketch" -jar .\target\processing-server-1.0-SNAPSHOT.jar
```

Open:

- `http://localhost:8080/`

Drag in the browser touch area.

Expected behavior:

- the brush follows the browser drag position
- the hue changes as drag events are handled
- the fading paint effect continues to work

## Compare the results

After you try both the base conversion and the browser-touch-drag conversion, compare:

- what the base converter preserved
- what the `browser-touch-drag` mode added automatically
- whether the generated mode result matches the manual edit closely enough for teaching use
