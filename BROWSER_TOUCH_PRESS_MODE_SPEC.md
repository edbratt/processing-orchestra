# Browser Touch Press Mode Spec

This document defines the next constrained converter mode:

- `--mode browser-touch-press`

The goal is to reduce manual editing for simple sketches that use `mousePressed()` as a click-like trigger and can reasonably treat browser touch events as the replacement trigger.

## Purpose

`browser-touch-press` is meant for simple PDE sketches that:

- use `mousePressed()`
- use `mouseX` and `mouseY` inside that handler
- should become browser-controlled with the smallest predictable change

This mode is based on the manual migration of `03-helper-trails`.

## Design Goal

Make a narrow, useful best guess.

This mode should:

- preserve helper-method structure
- preserve drawing logic
- replace the local `mousePressed()` callback with a browser-touch-triggered helper

This mode should not:

- rewrite drag semantics
- infer multi-user behavior
- redesign helper-method structure
- guess audio, motion, or keyboard mappings

## Safe Case

Apply this mode only when:

- `mousePressed()` exists
- `mouseDragged()`, `mouseReleased()`, and `mouseMoved()` do not exist
- `pmouseX` and `pmouseY` are not used

## Generated Behavior

When `--mode browser-touch-press` is used, the converter should:

1. Add stored normalized touch fields:

```java
private float touchX = 0.5f;
private float touchY = 0.5f;
```

2. Generate `processEvents()` so it:

- reads `touch` events from `EventQueue`
- updates `touchX` and `touchY`
- calls a generated helper such as `handleBrowserTouchPress()`

3. Replace the local `mousePressed()` override with a helper method driven by browser touch

4. Inside that helper, provide compatibility locals:

```java
float mouseX = touchX * width;
float mouseY = touchY * height;
```

so the original body can stay visually familiar

## Output Expectations

The generated sketch should:

- compile in the project
- no longer depend on Processing-window mouse clicks for the converted parts
- respond to browser touch input with minimal additional editing

The migration report should:

- state that browser-touch-press mapping was applied
- list any preserved local mouse handlers if the mode could not be applied

## Non-Goals

This mode does not:

- convert drag behavior
- add multi-user state
- add keyboard, audio, or motion integration

## Success Criteria

`browser-touch-press` is successful when a sketch like `03-helper-trails`:

- converts with minimal or no manual editing
- builds in the project
- responds to browser touch input by triggering the old `mousePressed()` behavior
