# Browser Keyboard Mode Spec

This document defines the next constrained converter mode:

- `--mode browser-keyboard`

The goal is to reduce manual editing for simple sketches that use local Processing keyboard input in ways that naturally map to the existing browser keyboard protocol.

## Purpose

`browser-keyboard` is meant for simple PDE sketches that:

- use `keyPressed()`
- read `key` and/or `keyCode`
- should become browser-controlled with the smallest predictable change

This mode is based on the successful manual migration of `02-keyboard-toggle`.

## Design Goal

Make a narrow, useful best guess.

This mode should:

- convert obvious local keyboard usage into browser keyboard event usage
- preserve the original sketch structure as much as possible
- avoid guessing anything about multi-user behavior

This mode should not:

- invent buttons or sliders automatically
- rewrite the whole sketch architecture
- guess audio, motion, or touch mappings
- attempt per-session user maps

## Input Pattern

The first target is a sketch where:

- there is a simple `keyPressed()` handler
- `key` and `keyCode` are used in direct conditionals
- there is no matching `keyReleased()` logic that needs stateful browser handling yet

Example:

```java
void keyPressed() {
  if (key == ' ') {
    ringVisible = !ringVisible;
  }

  if (keyCode == LEFT) {
    hueValue = (hueValue + 345) % 360;
  } else if (keyCode == RIGHT) {
    hueValue = (hueValue + 15) % 360;
  }
}
```

## Generated Behavior

When `--mode browser-keyboard` is used, the converter should:

1. Generate `processEvents()` so it reads `key` events from `EventQueue`
2. Only react to `action == "pressed"` in v1
3. Replace the local `keyPressed()` override with a helper method driven by `UserInputEvent`
4. Preserve the original conditional body as much as possible

Example generated shape:

```java
private void processEvents() {
    while (!eventQueue.isEmpty()) {
        UserInputEvent event = eventQueue.poll();
        if (event == null) {
            break;
        }

        if ("key".equals(event.eventType()) && "pressed".equals(event.keyAction())) {
            handleBrowserKeyPressed(event);
        }
    }
}
```

```java
private void handleBrowserKeyPressed(UserInputEvent event) {
    char key = event.keyText().isEmpty() ? '\0' : event.keyText().charAt(0);
    int keyCode = event.keyCode();
    final int LEFT = 37;
    final int UP = 38;
    final int RIGHT = 39;
    final int DOWN = 40;

    // original keyPressed body continues here
}
```

## Scope Rules

Apply the mode only when the pattern is simple enough.

Safe cases:

- one `keyPressed()` handler
- no `keyReleased()` handler
- straightforward `key` / `keyCode` conditionals

Unsafe cases:

- `keyReleased()` logic that implies held-key state
- multiple keyboard handlers that depend on continuous local key state
- unusual Processing keyboard APIs or timing assumptions

In unsafe cases, the converter should:

- preserve local keyboard behavior
- emit a report note that `browser-keyboard` was not applied fully

## Output Expectations

The generated sketch should:

- compile in the project
- no longer depend on local Processing-window keyboard focus for the converted parts
- respond to browser keyboard input with minimal additional editing

The migration report should:

- state that browser-keyboard mapping was applied
- list any preserved local keyboard handlers
- clearly say if keyboard usage was left untouched

## Non-Goals

This mode does not:

- add multi-user support
- add per-session maps
- add touch, audio, or motion integration
- infer button or slider replacements

## Success Criteria

`browser-keyboard` is successful when a simple sketch like `02-keyboard-toggle`:

- converts with minimal or no manual editing
- builds in the server project
- responds to browser keyboard input instead of local Processing-window keyboard focus
