# Browser Touch Drag Mode Spec

This document defines the next constrained converter mode:

- `--mode browser-touch-drag`

The goal is to reduce manual editing for simple sketches that use `mouseDragged()` as their main local input path and can reasonably treat browser touch events as the replacement drag stream.

## Purpose

`browser-touch-drag` is meant for simple PDE sketches that:

- use `mouseDragged()`
- use `mouseX` and `mouseY` inside that handler
- should become browser-controlled with the smallest predictable change

This mode is based on the manual migration of `04-drag-paint`.

## Design Goal

Make a narrow, useful best guess.

This mode should:

- preserve helper-method structure
- preserve drawing logic
- replace the local `mouseDragged()` callback with a browser-touch-driven helper

This mode should not:

- rewrite press or release semantics
- infer multi-user behavior
- redesign helper-method structure
- guess audio, motion, or keyboard mappings

## Safe Case

Apply this mode only when:

- `mouseDragged()` exists
- `mousePressed()`, `mouseReleased()`, and `mouseMoved()` do not exist
- `pmouseX` and `pmouseY` are not used

## Generated Behavior

When `--mode browser-touch-drag` is used, the converter should:

1. Add stored normalized touch fields:

```java
private float touchX = 0.5f;
private float touchY = 0.5f;
```

2. Generate `processEvents()` so it:

- reads `touch` events from `EventQueue`
- updates `touchX` and `touchY`
- calls a generated helper such as `handleBrowserTouchDrag()`

3. Replace the local `mouseDragged()` override with a helper driven by browser touch

4. Inside that helper, provide compatibility locals:

```java
float mouseX = touchX * width;
float mouseY = touchY * height;
```

so the original body can stay visually familiar

## Output Expectations

The generated sketch should:

- compile in the project
- no longer depend on Processing-window mouse dragging for the converted parts
- respond to browser touch drag input with minimal additional editing

The migration report should:

- state that browser-touch-drag mapping was applied
- list any preserved local mouse handlers if the mode could not be applied

## Non-Goals

This mode does not:

- convert press/release logic
- add multi-user state
- add keyboard, audio, or motion integration

## Success Criteria

`browser-touch-drag` is successful when a sketch like `04-drag-paint`:

- converts with minimal or no manual editing
- builds in the project
- responds to browser touch input by triggering the old `mouseDragged()` behavior
