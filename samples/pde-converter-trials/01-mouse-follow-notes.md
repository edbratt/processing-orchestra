# 01 Mouse Follow Notes

## Purpose

This is the simplest browser-controller-style sketch in the set.

It tests:

- top-level fields
- `setup()`
- `draw()`
- direct use of `mouseX` and `mouseY`

## Why it is useful

This is close to the existing touch controller model in the project.

That means the converter should be able to:

- preserve the visual behavior cleanly
- detect that local mouse input is present
- generate a useful TODO about replacing or supplementing `mouseX` and `mouseY` with browser touch data

## What should convert cleanly

- field declarations
- Processing lifecycle methods
- simple drawing code
- text drawing

## What should stay a manual decision

- whether to keep local mouse behavior
- whether to replace local mouse behavior with browser `touch` events
- whether one browser user should control the circle or whether the sketch should become multi-user

## Good conversion outcome

A good result would:

- preserve the sketch logic in readable form
- produce a small migration report
- mention that `mouseX` and `mouseY` are local Processing input and may need browser mapping later

## Tutorial

This tutorial walks through the full workflow for this sample:

1. run the base converter
2. inspect the default output
3. run the converter again with `browser-touch`
4. compare that automated result to the manual conversion steps
5. rebuild and run the sketch in the app

## Goal

Replace:

- `mouseX`
- `mouseY`

with position values that come from browser touch events in the shared `EventQueue`.

## Step 1: Run the base converter first

Use the default conversion first so you can see what the sketch looks like before any browser-specific mode is applied:

```powershell
java -cp target\classes com.processing.server.tools.pde.PdeToProcessingSketchMain .\samples\pde-converter-trials\01-mouse-follow.pde --output-dir .\target\pde-output\01-mouse-follow
```

This writes:

- `target/pde-output/01-mouse-follow/ProcessingSketchGenerated.java`
- `target/pde-output/01-mouse-follow/pde-migration-report.md`

## Step 2: Review the base output

Look at the generated Java and report first.

What to look for:

- the sketch structure should still be easy to recognize
- local `mouseX` / `mouseY` usage should still be present
- the report should explain that browser touch mapping is still a decision

## Step 3: Run the mode-specific converter

Now run the same sketch through the conservative browser-touch mode:

```powershell
java -cp target\classes com.processing.server.tools.pde.PdeToProcessingSketchMain .\samples\pde-converter-trials\01-mouse-follow.pde --mode browser-touch --output-dir .\target\pde-output\01-mouse-follow-browser-touch
```

This writes:

- `target/pde-output/01-mouse-follow-browser-touch/ProcessingSketchGenerated.java`
- `target/pde-output/01-mouse-follow-browser-touch/pde-migration-report.md`

What this mode should add:

- `touchX` / `touchY`
- touch-aware `processEvents()`
- replacement of `mouseX` / `mouseY` drawing usage with browser touch position

## Step 4: Place reviewed runnable output in the generated source tree

Raw converter output stays under `target/pde-output/...`.

If you want a reviewed runnable class in the app, keep it separate from the handwritten server code under:

- `generated-src/main/java/com/processing/server/`

For this sample, the reviewed runnable version is:

- `generated-src/main/java/com/processing/server/MouseFollowBrowserTouchSketch.java`

## Step 5: Manual conversion steps

If you want to do the conversion manually instead of using the mode, work from the preserved local version and make these edits.

In `processing-server/generated-src/main/java/com/processing/server/MouseFollowSketch.java`:

1. Add two fields to store the current normalized browser position.

```java
private float touchX = 0.5f;
private float touchY = 0.5f;
```

2. Update `processEvents()` so it reads `touch` events from `eventQueue`.

```java
private void processEvents() {
    while (!eventQueue.isEmpty()) {
        UserInputEvent event = eventQueue.poll();
        if (event == null) {
            break;
        }

        if ("touch".equals(event.eventType())) {
            touchX = constrain(event.x(), 0f, 1f);
            touchY = constrain(event.y(), 0f, 1f);
        }
    }
}
```

3. Update `drawGeneratedSketch()`.

Replace:

```java
ellipse(mouseX, mouseY, circleSize * pulse, circleSize * pulse);
```

with:

```java
ellipse(touchX * width, touchY * height, circleSize * pulse, circleSize * pulse);
```

4. Update the on-screen label if you want.

```java
text("Touch or drag in the browser", width / 2f, height - 24);
```

5. Remove or revise the old TODO comment, since the sketch is no longer using local mouse input for its main behavior.

## Result

Before:

- the circle followed the mouse inside the Processing window

After:

- the circle follows touch or drag input from the browser UI

What stays the same:

- the pulsing animation
- the color
- the overall single-circle behavior

## Step 6: Rebuild and run in the app

Rebuild:

```powershell
mvn package -DskipTests
```

Run the reviewed browser-touch version:

```powershell
java "-Dprocessing.sketch-class=com.processing.server.MouseFollowBrowserTouchSketch" -jar .\target\processing-server-1.0-SNAPSHOT.jar
```

Open:

- `http://localhost:8080/`

Then drag in the browser touch area.

Expected behavior:

- the circle moves from browser input, not from the local Processing-window mouse

## Compare the results

After you try both the base conversion and the browser-touch conversion, compare:

- what the base converter preserved
- what the `browser-touch` mode added automatically
- whether the generated mode result matches the manual edit closely enough for teaching use
