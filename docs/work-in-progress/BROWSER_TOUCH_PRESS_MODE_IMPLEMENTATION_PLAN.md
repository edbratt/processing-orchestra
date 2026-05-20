# Browser Touch Press Mode Implementation Plan

This plan covers the next constrained converter mode:

- `--mode browser-touch-press`

The goal is to automate the simple migration from local `mousePressed()` behavior to browser touch events without expanding the converter into a general mouse-callback rewrite tool.

## Scope

This mode only targets:

- simple one-file PDE sketches
- sketches with `mousePressed()`
- no drag-style mouse callbacks

## Milestone 1: Add Mode Parsing

- [x] Extend the CLI so it accepts `--mode browser-touch-press`
- [x] Store the selected mode in the sketch model
- [x] Keep current modes unchanged

## Milestone 2: Detect Safe mousePressed Usage

- [x] Detect whether `mousePressed()` exists
- [x] Detect whether other mouse handlers exist
- [x] Add a safe-case predicate such as `canApplyBrowserTouchPressSafely()`

Success criteria:
- `03-helper-trails` is marked safe
- sketches with drag-style handlers are not

## Milestone 3: Generate Stored Touch Fields

- [x] Add `touchX` and `touchY` to the generated sketch in the safe case

## Milestone 4: Generate Touch-Aware processEvents()

- [x] Read `touch` events from `EventQueue`
- [x] Update `touchX` and `touchY`
- [x] Call a generated helper like `handleBrowserTouchPress()`

## Milestone 5: Convert mousePressed() Into A Helper

- [x] Do not emit the original local `mousePressed()` override in the safe case
- [x] Generate a helper method driven by browser touch
- [x] Inside the helper, create compatibility locals:
  - `float mouseX = touchX * width;`
  - `float mouseY = touchY * height;`
- [x] Reuse the original `mousePressed()` body as much as possible

## Milestone 6: Report What Was Changed

- [x] Update the migration report to say when browser-touch-press mapping was applied
- [x] Add notes when mouse handlers were preserved unchanged

## Milestone 7: Add Focused Tests

- [x] Add a fixture that exercises `browser-touch-press`
- [x] Verify generated output includes:
  - `touchX`
  - `touchY`
  - touch-driven `processEvents()`
  - a generated browser-touch-press helper
- [x] Verify the generated sketch compiles

## Recommended First Trial

Use:

- `samples/pde-converter-trials/03-helper-trails.pde`

## Deliberate Limits

Do not do these in the first version:

- [x] convert drag semantics
- [x] rewrite helper methods
- [x] infer multi-user behavior

## Expected Outcome

After this mode is implemented, a sketch like `03-helper-trails` should:

- convert into a browser-touch-triggered version of the old `mousePressed()` behavior
- compile in the project
- keep its helper-method structure intact
