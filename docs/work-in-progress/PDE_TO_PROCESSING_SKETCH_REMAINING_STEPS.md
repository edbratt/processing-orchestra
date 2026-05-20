# PDE To ProcessingSketch Remaining Steps

This note captures the work that is still intentionally left for later after the current limited converter and its first set of conservative browser-mapping modes.

## Current Stopping Point

The tool now supports a narrow, predictable workflow:

- read one simple `.pde` file
- detect top-level fields and methods
- detect `setup()`, `draw()`, and common mouse/keyboard handlers
- preserve local interaction where possible
- generate:
  - `ProcessingSketchGenerated.java`
  - `pde-migration-report.md`
- build the generated starter class for a small set of simple fixtures
- apply a few narrow browser-mapping modes for proven safe cases

The current implemented browser-mapping modes are:

- `browser-touch`
- `browser-keyboard`
- `browser-touch-press`
- `browser-touch-drag`

This means the next “remaining steps” are no longer about first browser mapping. They are about documentation quality, mixed-case sketches, and better follow-up guidance.

## Near-Term Documentation Step

Before adding another converter mode, complete the tutorial pass on the sample notes files.

Each tutorial should be self-contained and include:

- how to run the base converter first
- how to review the initial generated output and report
- how to run the converter again with the mode used by that tutorial
- where reviewed generated classes belong
- how to rebuild
- how to launch the sketch with `processing.sketch-class`

This is likely the most valuable next step for teaching use.

## Milestone 4: Constrained Modes

The originally planned milestone 4 is now partially overtaken by real work.

The next remaining constrained-mode work should probably focus on one of these:

### `visual-only`

Purpose:

- keep the sketch as close as possible to the original PDE behavior

Expected effect:

- preserve local interaction
- preserve visual logic
- avoid extra migration assumptions

### `preserve-local-input`

Purpose:

- make local mouse and keyboard preservation an explicit requested mode instead of only the current default behavior

Expected effect:

- local input stays active where possible
- the report explicitly says which local behavior was preserved

### Possible next narrow mouse mode

If real sketches justify it, the next browser mode should be based on a proven safe case such as:

- combined `mousePressed()` + `mouseDragged()` sketches
- or a limited `pmouseX` / `pmouseY` use case

That should only happen after real examples show it is worth the added complexity.

### What this milestone should not do

- no automatic browser-control mapping
- no automatic multi-user state conversion
- no automatic audio-reactive conversion
- no automatic motion-reactive conversion

The goal is clearer behavior, not broader behavior.

## Milestone 5: Follow-Up Decision Support

This milestone is about making the generated TODOs easier to resolve after the converter has produced a starter class and report.

Expected additions:

- classify TODO items by type
- support follow-up by either:
  - the student
  - the teacher
  - an AI coding agent
- preserve existing integration decisions by default
- ask only short, concrete follow-up questions

Examples of follow-up question types:

- should this mouse-driven behavior stay local or later move to browser touch input?
- should this keyboard behavior stay local or become browser buttons or browser key input?
- should this sketch variable remain global or become per-session if the sketch later becomes multi-user?

## Recommended Constraint For Future Work

Keep the converter focused on simple, reliable output.

That means:

- prefer TODO notes over clever guesses
- prefer preserving local behavior over forcing browser integration
- prefer explicit warnings over partial support for complex sketches

If a sketch is too complex for the current tool, the tool should say so plainly.
