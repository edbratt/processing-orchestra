# Browser Touch Mode Implementation Plan

This plan covers the next constrained converter mode:

- `--mode browser-touch`

The goal is to automate the simple, common migration from local mouse position to browser touch position without expanding the converter into a general rewrite tool.

## Scope

This mode only targets:

- simple one-file PDE sketches
- clear uses of `mouseX` and `mouseY`
- sketches where replacing local pointer position with browser touch position is the obvious first integration step

## Milestone 1: Add Mode Parsing

- [ ] Extend the CLI so it accepts `--mode browser-touch`
- [ ] Store the selected mode in the internal sketch model
- [ ] Keep the current default behavior unchanged when no mode is passed

Success criteria:
- running without `--mode` behaves exactly as it does now
- running with `--mode browser-touch` is visible in the migration report

## Milestone 2: Detect Safe Mouse Position Usage

- [ ] Detect whether `mouseX` or `mouseY` are used in the sketch
- [ ] Distinguish simple position usage from more complicated local mouse-handler behavior
- [ ] Add a simple flag to the model such as:
  - `usesMousePosition`
  - `usesMouseHandlers`
  - `canApplyBrowserTouchSafely`

Success criteria:
- `01-mouse-follow` is marked safe
- sketches with `mousePressed()` or `mouseDragged()` are flagged for more cautious handling

## Milestone 3: Generate Stored Touch Fields

- [ ] When `browser-touch` is active and safe, generate:
  - `private float touchX = 0.5f;`
  - `private float touchY = 0.5f;`
- [ ] Only generate these fields once

Success criteria:
- generated sketch includes normalized stored touch position
- sketches without `browser-touch` do not get these fields

## Milestone 4: Generate Touch-Aware processEvents()

- [ ] Replace the placeholder `processEvents()` body with a simple touch-event loop
- [ ] Read from `eventQueue`
- [ ] Update `touchX` and `touchY` from `event.x()` and `event.y()`
- [ ] Constrain the normalized values to `0..1`

Success criteria:
- `01-mouse-follow` gets a usable `processEvents()`
- generated code remains small and readable

## Milestone 5: Replace Simple mouseX / mouseY Usage

- [ ] In safe cases, rewrite:
  - `mouseX` -> `touchX * width`
  - `mouseY` -> `touchY * height`
- [ ] Preserve the surrounding expression structure as much as possible
- [ ] Do not attempt deep semantic rewrites in v1

Success criteria:
- direct drawing-time pointer usage is replaced cleanly
- generated code still resembles the original sketch closely

## Milestone 6: Report What Was Changed

- [ ] Update the migration report to say when browser-touch mapping was applied
- [ ] Add notes when local mouse-handler methods were preserved unchanged
- [ ] Add notes when some `mouseX` / `mouseY` usage was left untouched

Success criteria:
- the report makes the converter’s choices explicit
- a student can see whether the result is fully browser-touch driven or only partially converted

## Milestone 7: Add Focused Tests

- [ ] Add a test fixture that exercises `browser-touch` on a simple sketch like `01-mouse-follow`
- [ ] Verify generated output includes:
  - `touchX`
  - `touchY`
  - touch-aware `processEvents()`
  - converted `mouseX` / `mouseY`
- [ ] Add a cautious-case test for a sketch with a local mouse handler

Success criteria:
- the new mode is covered by tests
- simple browser-touch conversion stays stable over time

## Recommended First Trial

Use:

- `samples/pde-converter-trials/01-mouse-follow.pde`

That sketch already proved the intended manual migration pattern.

## Deliberate Limits

Do not do these in the first version:

- [ ] rewrite `mousePressed()` into browser-touch callbacks
- [ ] infer multi-user state
- [ ] mix in audio or motion logic
- [ ] add new UI controls
- [ ] rewrite the whole sketch around per-session maps

## Expected Outcome

After this mode is implemented, a simple sketch like `01-mouse-follow` should:

- convert directly into a browser-touch-driven sketch
- compile in the project
- require much less manual editing than it does today
