# Browser Keyboard Mode Implementation Plan

This plan covers the next constrained converter mode:

- `--mode browser-keyboard`

The goal is to automate the simple, common migration from local Processing keyboard input to browser keyboard events without expanding the converter into a general rewrite tool.

## Scope

This mode only targets:

- simple one-file PDE sketches
- clear use of `keyPressed()`
- sketches where browser keyboard input is the obvious first integration step

## Milestone 1: Add Mode Parsing

- [ ] Extend the CLI so it accepts `--mode browser-keyboard`
- [ ] Store the selected mode in the internal sketch model
- [ ] Keep the current default behavior unchanged when no mode is passed

Success criteria:
- running without `--mode` behaves exactly as it does now
- running with `--mode browser-keyboard` is visible in the migration report

## Milestone 2: Detect Safe Keyboard Usage

- [ ] Detect whether the sketch has `keyPressed()`
- [ ] Detect whether the sketch also has `keyReleased()`
- [ ] Detect use of `key` and `keyCode`
- [ ] Add simple flags to the model such as:
  - `hasKeyPressedHandler`
  - `hasKeyReleasedHandler`
  - `canApplyBrowserKeyboardSafely`

Success criteria:
- `02-keyboard-toggle` is marked safe
- sketches with `keyReleased()` are flagged for cautious handling

## Milestone 3: Generate Keyboard-Aware processEvents()

- [ ] When `browser-keyboard` is active and safe, generate a `processEvents()` loop
- [ ] Read `key` events from `eventQueue`
- [ ] Only process `event.keyAction() == "pressed"` in v1
- [ ] Call a generated helper such as `handleBrowserKeyPressed(event)`

Success criteria:
- `02-keyboard-toggle` gets a usable `processEvents()`
- generated code remains small and readable

## Milestone 4: Convert keyPressed() Into A Helper

- [ ] Do not emit the original local `keyPressed()` override in the safe case
- [ ] Generate a helper method that receives `UserInputEvent`
- [ ] Inside that helper, create compatibility locals:
  - `char key`
  - `int keyCode`
  - arrow-key constants
- [ ] Reuse the original `keyPressed()` body as much as possible

Success criteria:
- the generated code stays close to the original
- the original keyboard intent is easy to recognize

## Milestone 5: Report What Was Changed

- [ ] Update the migration report to say when browser-keyboard mapping was applied
- [ ] Add notes when local keyboard handlers were preserved unchanged
- [ ] Add notes when some keyboard usage was left untouched

Success criteria:
- the report makes the converter’s choices explicit
- a student can see whether the result is browser-keyboard driven or still local

## Milestone 6: Add Focused Tests

- [ ] Add or reuse a fixture that exercises `browser-keyboard` on a simple sketch like `02-keyboard-toggle`
- [ ] Verify generated output includes:
  - keyboard-aware `processEvents()`
  - a browser-key helper method
  - no local `keyPressed()` override in the safe case
- [ ] Add a cautious-case test later for a sketch with `keyReleased()`

Success criteria:
- the new mode is covered by tests
- simple browser-keyboard conversion stays stable over time

## Recommended First Trial

Use:

- `samples/pde-converter-trials/02-keyboard-toggle.pde`

That sketch already proved the intended manual migration pattern.

## Deliberate Limits

Do not do these in the first version:

- [ ] rewrite `keyReleased()` into browser held-key state logic
- [ ] infer button or slider replacements
- [ ] mix in touch, audio, or motion logic
- [ ] rewrite the whole sketch around per-session maps

## Expected Outcome

After this mode is implemented, a simple sketch like `02-keyboard-toggle` should:

- convert directly into a browser-keyboard-driven sketch
- compile in the project
- require much less manual editing than it does today
