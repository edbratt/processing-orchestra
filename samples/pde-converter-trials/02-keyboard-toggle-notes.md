# 02 Keyboard Toggle Notes

## Purpose

This sketch is meant to exercise keyboard handling.

It tests:

- `keyPressed()`
- use of `key`
- use of `keyCode`
- simple state toggling from the keyboard

## Why it is useful

The project now has two keyboard paths:

- local Processing keyboard input when the sketch window has focus
- browser keyboard events over WebSocket when the browser page has focus

This sketch gives you a clean way to discuss that choice.

## What should convert cleanly

- global variables
- `setup()`
- `draw()`
- the `keyPressed()` method itself

## What should stay a manual decision

- whether to preserve only local keyboard behavior
- whether to add browser `key` event handling
- whether to support both keyboard paths

## Good conversion outcome

A good result would:

- keep the existing local `keyPressed()` behavior
- note that the sketch could also be mapped to browser `key` events
- avoid guessing which keyboard path is the right one

## Tutorial

This tutorial walks through the full workflow for the keyboard sample:

1. run the base converter
2. inspect the default output
3. run the converter again with `browser-keyboard`
4. compare that automated result to the manual conversion steps
5. rebuild and run the sketch in the app

## Goal

Replace local `keyPressed()` handling with browser `key` events that flow through the existing `EventQueue`.

## Step 1: Run the base converter first

```powershell
java -cp target\classes com.processing.server.tools.pde.PdeToProcessingSketchMain .\samples\pde-converter-trials\02-keyboard-toggle.pde --output-dir .\target\pde-output\02-keyboard-toggle
```

This writes:

- `target/pde-output/02-keyboard-toggle/ProcessingSketchGenerated.java`
- `target/pde-output/02-keyboard-toggle/pde-migration-report.md`

## Step 2: Review the base output

What to look for:

- the local `keyPressed()` method should still be present
- the ring and hue logic should remain recognizable
- the report should explain that browser keyboard mapping is still a decision

## Step 3: Run the mode-specific converter

```powershell
java -cp target\classes com.processing.server.tools.pde.PdeToProcessingSketchMain .\samples\pde-converter-trials\02-keyboard-toggle.pde --mode browser-keyboard --output-dir .\target\pde-output\02-keyboard-toggle-browser-keyboard
```

This writes:

- `target/pde-output/02-keyboard-toggle-browser-keyboard/ProcessingSketchGenerated.java`
- `target/pde-output/02-keyboard-toggle-browser-keyboard/pde-migration-report.md`

What this mode should add:

- key-aware `processEvents()`
- a generated helper that reuses the old `keyPressed()` body
- browser-keyboard TODO/report text

## Step 4: Place reviewed runnable output in the generated source tree

Raw converter output stays under `target/pde-output/...`.

If you want a reviewed runnable class in the app, keep it under:

- `generated-src/main/java/com/processing/server/`

For this sample, the reviewed runnable version is:

- `generated-src/main/java/com/processing/server/KeyboardToggleBrowserKeySketch.java`

## Step 5: Manual conversion steps

If you want to do the conversion manually instead of using the mode, work from the preserved local version.

In `processing-server/generated-src/main/java/com/processing/server/KeyboardToggleSketch.java`:

### 1. Leave the visual drawing code alone

You do not need to change `drawGeneratedSketch()`.

It already does the right thing:

- draws the circle
- draws the ring when enabled
- uses `hueValue` and `ringVisible`

### 2. Replace the empty `processEvents()` with keyboard event handling

```java
private void processEvents() {
    while (!eventQueue.isEmpty()) {
        UserInputEvent event = eventQueue.poll();
        if (event == null) {
            break;
        }

        if (!"key".equals(event.eventType())) {
            continue;
        }

        if (!"pressed".equals(event.keyAction())) {
            continue;
        }

        handleBrowserKey(event);
    }
}
```

### 3. Add a helper method for the browser key behavior

```java
private void handleBrowserKey(UserInputEvent event) {
    String keyText = event.keyText();
    int keyCodeValue = event.keyCode();

    if (" ".equals(keyText) || "Space".equals(keyText) || "Spacebar".equals(keyText) || keyCodeValue == 32) {
        ringVisible = !ringVisible;
        return;
    }

    if ("ArrowLeft".equals(keyText) || keyCodeValue == 37) {
        hueValue = (hueValue + 345) % 360;
    } else if ("ArrowRight".equals(keyText) || keyCodeValue == 39) {
        hueValue = (hueValue + 15) % 360;
    }
}
```

### 4. Remove or comment out the local `keyPressed()` override

If you want browser-only keyboard control, delete the local `keyPressed()` method.

If you want both local and browser keyboard paths during testing, you can keep it temporarily.

### 5. Update the on-screen label

```java
text("Browser keys: Space toggles ring, arrows change color", width / 2f, height - 24);
```

### 6. Update the TODO comment

Replace the old local-keyboard TODO with:

```java
// Browser keyboard events now drive this sketch through EventQueue.
```

## Result

After this manual change:

- keyboard input works from the browser page when it has focus
- the Processing window no longer needs focus for the sketch controls
- the behavior stays almost exactly the same as the original PDE sketch

## Step 6: Rebuild and run in the app

Rebuild:

```powershell
mvn package -DskipTests
```

Run the reviewed browser-keyboard version:

```powershell
java "-Dprocessing.sketch-class=com.processing.server.KeyboardToggleBrowserKeySketch" -jar .\target\processing-server-1.0-SNAPSHOT.jar
```

Open:

- `http://localhost:8080/`

Click the browser page so it has focus, then test:

- `Space` toggles the ring
- left arrow changes hue backward
- right arrow changes hue forward

## Compare the results

After you try both the base conversion and the browser-keyboard conversion, compare:

- what the base converter preserved
- what the `browser-keyboard` mode added automatically
- whether the generated mode result matches the manual edit closely enough for teaching use
