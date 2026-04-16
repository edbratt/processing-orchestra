# Browser Touch Mode Spec

This document defines the next constrained converter mode:

- `--mode browser-touch`

The goal is to reduce manual editing for simple sketches that use local mouse position in ways that naturally map to the existing browser touch controller.

## Purpose

`browser-touch` is meant for simple PDE sketches that:

- use `mouseX` and `mouseY` in drawing logic
- do not require complex multi-user behavior yet
- should become browser-controlled with the smallest predictable change

This mode is based on the first successful manual migration of `01-mouse-follow`.

## Design Goal

Make a narrow, useful best guess.

This mode should:

- convert obvious local mouse-position usage into browser touch-position usage
- preserve the original sketch structure as much as possible
- avoid guessing anything about multi-user behavior

This mode should not:

- invent new visual behavior
- rewrite the whole sketch architecture
- guess slider, button, audio, or motion mappings
- attempt per-session user maps

## Input Pattern

The first target is a sketch where:

- `mouseX` and `mouseY` appear in drawing code
- touch-like control is conceptually a direct replacement for local pointer position

Example:

```java
ellipse(mouseX, mouseY, 80, 80);
```

## Generated Behavior

When `--mode browser-touch` is used, the converter should:

1. Add stored normalized touch fields to the generated sketch
   Example:

```java
private float touchX = 0.5f;
private float touchY = 0.5f;
```

2. Generate `processEvents()` so it reads `touch` events from `EventQueue`

Example:

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

3. Replace direct `mouseX` and `mouseY` drawing usage with scaled browser-touch values

Example:

```java
ellipse(touchX * width, touchY * height, 80, 80);
```

4. Update migration TODOs to reflect that touch mapping was applied automatically

## Scope Rules

Apply the mode only when the pattern is simple enough.

Safe cases:

- direct use of `mouseX`
- direct use of `mouseY`
- arithmetic expressions that still clearly mean “pointer position”

Examples:

```java
ellipse(mouseX, mouseY, size, size);
line(mouseX, 0, mouseX, height);
float dx = mouseX - width / 2;
```

Unsafe cases:

- complicated local drag-state logic
- PDE mouse callbacks that imply more than “latest pointer position”
- sketches that use many local mouse APIs in interdependent ways

In unsafe cases, the converter should:

- preserve local behavior
- emit a report note that `browser-touch` was not applied fully

## Interaction With Local Mouse Handlers

If a sketch contains:

- `mousePressed()`
- `mouseDragged()`
- `mouseReleased()`

then `browser-touch` should not aggressively rewrite those methods in v1.

Instead:

- preserve them
- add report notes that the sketch still has local mouse-handler logic
- apply the simple `mouseX` / `mouseY` replacement only where it is clearly safe

## Output Expectations

The generated sketch should:

- compile in the project
- no longer depend on local `mouseX` / `mouseY` for the converted parts
- respond to browser touch input with minimal additional editing

The migration report should:

- state that browser-touch mapping was applied
- list any preserved local mouse handlers
- clearly say if any mouse usage was left untouched

## Non-Goals

This mode does not:

- add multi-user support
- add per-session maps
- add keyboard, audio, or motion integration
- decide whether the sketch should remain local-input compatible

## Success Criteria

`browser-touch` is successful when a simple sketch like `01-mouse-follow`:

- converts with minimal or no manual editing
- builds in the server project
- responds to browser touch input instead of local Processing-window mouse position
