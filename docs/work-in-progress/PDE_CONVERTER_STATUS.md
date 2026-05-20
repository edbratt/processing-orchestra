# PDE Converter Status

This note captures where the PDE-to-`ProcessingSketch` converter work stands now, what is working, and what the most likely next steps are.

## Current Status

The converter is still intentionally limited, but it is no longer just a raw wrapper-and-report tool.

It now supports a narrow, predictable workflow for simple one-file sketches:

- read one simple `.pde` file
- detect top-level fields
- detect simple top-level helper types
- detect top-level methods
- detect `setup()`
- detect `draw()`
- preserve `size(..., renderer)` from `setup()` into generated `settings()`
- detect common mouse and keyboard handlers
- detect simple local input usage such as:
  - `mouseX`
  - `mouseY`
  - `pmouseX`
  - `pmouseY`
  - `key`
  - `keyCode`
- preserve local mouse and keyboard behavior where possible
- generate:
  - `ProcessingSketchGenerated.java`
  - `pde-migration-report.md`
- support a small set of conservative browser-mapping modes for common cases

This is still a constrained teaching tool. It is not trying to solve general Processing-to-server conversion.

## What Is Working

The converter code lives under:

- [src/main/java/com/processing/server/tools/pde](../../src/main/java/com/processing/server/tools/pde)

The core pieces are:

- [PdeToProcessingSketchMain.java](../../src/main/java/com/processing/server/tools/pde/PdeToProcessingSketchMain.java)
- [PdeStructureScanner.java](../../src/main/java/com/processing/server/tools/pde/PdeStructureScanner.java)
- [ProcessingSketchRenderer.java](../../src/main/java/com/processing/server/tools/pde/ProcessingSketchRenderer.java)
- [MigrationReportRenderer.java](../../src/main/java/com/processing/server/tools/pde/MigrationReportRenderer.java)

The current command shape is:

```text
java -cp target/classes com.processing.server.tools.pde.PdeToProcessingSketchMain <input.pde> [--mode <mode>] [--output-dir <dir>]
```

Current supported modes:

- default mode
- `browser-touch`
- `browser-keyboard`
- `browser-touch-press`
- `browser-touch-drag`

Current behavior:

- the scanner prints a clear summary to the console
- simple top-level helper type declarations are captured and emitted as nested classes
- unbalanced braces are rejected
- a starter `ProcessingSketchGenerated.java` is written
- a short `pde-migration-report.md` is written

## What The Generated Sketch Is Good For

The generated sketch is good for:

- simple standalone Processing IDE sketches
- preserving the original drawing logic
- preserving local mouse and keyboard behavior where possible
- giving the student a project-integrated starting point that builds against this repo

It is not meant to:

- fully integrate browser controls
- fully integrate multi-user state
- automatically integrate audio or motion into the generated sketch
- handle mixed or ambiguous mouse semantics automatically

It can now automatically map a few narrow, well-understood cases:

- direct `mouseX` / `mouseY` position usage via `browser-touch`
- simple `keyPressed()` logic via `browser-keyboard`
- simple `mousePressed()` logic via `browser-touch-press`
- simple `mouseDragged()` logic via `browser-touch-drag`

## Verified Fixture Sketches

The current verified simple fixtures are:

- [bouncing-circle.pde](../../src/test/resources/pde-fixtures/bouncing-circle.pde)
- [mouse-follow.pde](../../src/test/resources/pde-fixtures/mouse-follow.pde)
- [keyboard-toggle.pde](../../src/test/resources/pde-fixtures/keyboard-toggle.pde)
- [helper-drawing.pde](../../src/test/resources/pde-fixtures/helper-drawing.pde)
- [browser-touch-safe.pde](../../src/test/resources/pde-fixtures/browser-touch-safe.pde)
- [helper-trails.pde](../../src/test/resources/pde-fixtures/helper-trails.pde)
- [drag-paint.pde](../../src/test/resources/pde-fixtures/drag-paint.pde)
- [top-level-class-p3d.pde](../../src/test/resources/pde-fixtures/top-level-class-p3d.pde)

There is also an older unsupported fixture still present in test resources:

- [unsupported-top-level-class.pde](../../src/test/resources/pde-fixtures/unsupported-top-level-class.pde)

That fixture is now historical for this status note. Simple helper classes are supported by the current converter.

Tests for this work live in:

- [PdeStructureScannerTest.java](../../src/test/java/com/processing/server/tools/pde/PdeStructureScannerTest.java)
- [ProcessingSketchRendererTest.java](../../src/test/java/com/processing/server/tools/pde/ProcessingSketchRendererTest.java)
- [MigrationReportRendererTest.java](../../src/test/java/com/processing/server/tools/pde/MigrationReportRendererTest.java)
- [GeneratedSketchCompilationTest.java](../../src/test/java/com/processing/server/tools/pde/GeneratedSketchCompilationTest.java)

## Current Constraints

These constraints are intentional:

- only one `.pde` file is supported
- only simple sketches are the target
- only simple top-level helper-type declarations are intended to work reliably
- local input is preserved instead of being aggressively rewritten
- TODO notes are still used instead of broad automatic design decisions
- generated output is a starter class, not a final integrated sketch
- browser mapping is only attempted for narrow safe cases

This is important: the current converter is meant to be reliable for simple cases, not broad for complex cases.

## What The Report Currently Provides

The generated `pde-migration-report.md` currently includes:

- a summary of what was found
- preserved local interaction
- unresolved TODO decisions
- a suggested next step

This is enough to support:

- manual follow-up by a student or teacher
- or a later AI-assisted question flow

## Immediate Next Steps

The next few steps should stay practical and documentation-driven:

1. Make each sample tutorial self-contained.
   For each sample notes file, add the missing repeated steps:
   - run the core converter in default mode first
   - review the basic generated output
   - run the converter again with the specific mode used by that tutorial
   - copy or promote the reviewed class into the separated generated source tree when needed
   - rebuild and launch the sketch in the app

2. Keep the sample notes aligned with the actual workflow.
   The notes should match the current project structure:
   - raw converter output in `target/pde-output/...`
   - reviewed generated sketches in `generated-src/main/java/...`
   - app launch via `processing.sketch-class`

3. Try the converter on a few real student sketches.
   This is still the best way to find the next real gap.

4. Use those student sketches to decide the next constrained mode.
   The most likely next technical targets are:
   - sketches that mix `mousePressed()` and `mouseDragged()`
   - sketches that use `pmouseX` / `pmouseY`
   - sketches that need a clearer “preserve local input only” path

5. Build the follow-up decision support from milestone 5.
   This would help a student, teacher, or AI coding agent resolve TODOs without reopening earlier decisions.

## Completed Conservative Modes

These modes are now implemented and tested:

- `browser-touch`
- `browser-keyboard`
- `browser-touch-press`
- `browser-touch-drag`

Each one is intentionally narrow and should only fire in a clear safe case.

## What We Should Not Do Yet

These are likely to create complexity faster than value:

- automatic browser mapping for all mouse input
- automatic keyboard-to-browser conversion
- automatic multi-user state conversion
- automatic audio-reactive or motion-reactive conversion
- support for complex multi-tab PDE projects
- support for arbitrary Java-style PDE type structures beyond the current simple helper-type path
- automatic handling of mixed press/drag/release sketches unless a narrow safe case is proven first

Those can wait until the simple cases have been exercised with real sketches.

## Practical Recommendation

Before building more converter features, use the current tool and the tutorial notes on a small handful of real sketches and answer these questions:

1. Does the scanner recognize the structure correctly?
2. Does the generated class still reflect the original sketch clearly?
3. Does the generated class compile?
4. Are the TODO notes understandable?
5. Are students more helped by preserving local behavior first, or do they immediately want browser-mapped behavior?
6. Are the sample tutorials self-contained enough that a student can repeat the workflow without outside help?

The answers to those questions should drive the next feature work.
