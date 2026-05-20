# Browser Touch Drag Mode Implementation Plan

This plan covers the next constrained converter mode:

- `--mode browser-touch-drag`

The goal is to automate the simple migration from local `mouseDragged()` behavior to browser touch events without expanding the converter into a general mouse-callback rewrite tool.

## Scope

This mode only targets:

- simple one-file PDE sketches
- sketches with `mouseDragged()`
- no press/release mouse callbacks

## Milestone 1: Add Mode Parsing

- [x] Extend the CLI so it accepts `--mode browser-touch-drag`
- [x] Store the selected mode in the sketch model
- [x] Keep current modes unchanged

## Milestone 2: Detect Safe mouseDragged Usage

- [x] Detect whether `mouseDragged()` exists
- [x] Detect whether other mouse handlers exist
- [x] Add a safe-case predicate such as `canApplyBrowserTouchDragSafely()`

Success criteria:
- `04-drag-paint` is marked safe
- sketches with press/release-style handlers are not

## Milestone 3: Generate Stored Touch Fields

- [x] Add `touchX` and `touchY` to the generated sketch in the safe case

## Milestone 4: Generate Touch-Aware processEvents()

- [x] Read `touch` events from `EventQueue`
- [x] Update `touchX` and `touchY`
- [x] Call a generated helper like `handleBrowserTouchDrag()`

## Milestone 5: Convert mouseDragged() Into A Helper

- [x] Do not emit the original local `mouseDragged()` override in the safe case
- [x] Generate a helper method driven by browser touch
- [x] Inside the helper, create compatibility locals:
  - `float mouseX = touchX * width;`
  - `float mouseY = touchY * height;`
- [x] Reuse the original `mouseDragged()` body as much as possible

## Milestone 6: Report What Was Changed

- [x] Update the migration report to say when browser-touch-drag mapping was applied
- [x] Add notes when mouse handlers were preserved unchanged

## Milestone 7: Add Focused Tests

- [x] Add a fixture that exercises `browser-touch-drag`
- [x] Verify generated output includes:
  - `touchX`
  - `touchY`
  - touch-driven `processEvents()`
  - a generated browser-touch-drag helper
- [x] Verify the generated sketch compiles

## Recommended First Trial

Use:

- `samples/pde-converter-trials/04-drag-paint.pde`

## Deliberate Limits

Do not do these in the first version:

- [x] convert press/release semantics
- [x] rewrite helper methods
- [x] infer multi-user behavior

## Expected Outcome

After this mode is implemented, a sketch like `04-drag-paint` should:

- convert into a browser-touch-driven version of the old `mouseDragged()` behavior
- compile in the project
- keep its helper-method structure intact
