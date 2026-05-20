# PDE To ProcessingSketch Implementation Plan

This checklist keeps the migration tool narrow and predictable. The goal is to make simple sketches work reliably, not to support every possible Processing IDE project.

## Scope Guardrails

- [x] Support one `.pde` file only in v1
- [x] Support simple top-level variables
- [x] Support simple helper functions
- [x] Support `setup()`, `draw()`, and common mouse/keyboard handlers
- [x] Preserve local interaction where possible
- [x] Generate explicit TODO notes instead of guessing complicated integration choices
- [x] Reject or warn on unsupported inputs early
- [ ] Do not attempt multi-tab sketch conversion in v1
- [x] Do not attempt broad automatic browser mapping in v1
- [ ] Do not attempt automatic multi-user conversion in v1
- [ ] Do not attempt automatic audio or motion integration in v1

## Milestone 1: Minimal Scanner

- [x] Create a small Java CLI entry point
- [x] Read one `.pde` file from disk
- [x] Detect top-level variable declarations
- [x] Detect top-level methods
- [x] Detect `setup()`
- [x] Detect `draw()`
- [x] Detect common mouse handlers
- [x] Detect common keyboard handlers
- [x] Detect simple local-input usage such as `mouseX`, `mouseY`, `pmouseX`, `pmouseY`, `key`, and `keyCode`
- [x] Print a clear console summary
- [x] Fail clearly on missing file or unsupported input
- [x] Test against a few tiny fixture sketches

## Milestone 2: Generate Starter Java Output

- [x] Generate starter Java output
- [x] Preserve top-level PDE variables as class fields
- [x] Preserve helper methods
- [x] Preserve `setup()`
- [x] Preserve sketch drawing logic inside a helper such as `drawGeneratedSketch()`
- [x] Generate the project constructor shape
- [x] Generate `settings()`
- [x] Generate `draw()`
- [x] Generate `runSketch()`
- [x] Preserve local mouse and keyboard handlers where possible
- [x] Add TODO comments at unresolved migration points
- [x] Keep generated output readable and close to compilable for simple sketches

## Milestone 3: Generate Migration Report

- [x] Generate `pde-migration-report.md`
- [x] Summarize what the scanner found
- [x] List preserved local mouse behavior
- [x] List preserved local keyboard behavior
- [x] List unresolved TODO decisions
- [x] Suggest the next manual review step
- [x] Keep the report simple and low in jargon

## Milestone 4: Add Constrained Modes

- [ ] Add `visual-only` mode
- [ ] Add `preserve-local-input` mode
- [x] Add narrow browser mapping only for proven safe cases
- [x] Keep mode behavior small and understandable
- [x] Do not add risky automatic rewrites in these modes
- [ ] Leave `multi-user`, `audio-reactive`, and `motion-reactive` as report/TODO suggestions only for now

Implemented conservative mapping modes so far:

- [x] `browser-touch`
- [x] `browser-keyboard`
- [x] `browser-touch-press`
- [x] `browser-touch-drag`

Next likely documentation step:

- [ ] Make each sample tutorial self-contained, including:
  - base conversion command
  - mode-specific conversion command
  - reviewed-generated-class placement
  - rebuild and launch steps

## Milestone 5: Follow-Up Decision Support

- [ ] Classify TODO items by decision type
- [ ] Support student-led follow-up
- [ ] Support AI coding agent follow-up
- [ ] Preserve existing integration decisions by default
- [ ] Ask only short, concrete follow-up questions
- [ ] Only ask new conversion questions when a new control or behavior is introduced

## Suggested Java Structure

- [x] `PdeToProcessingSketchMain`
- [x] `PdeStructureScanner`
- [x] `PdeSketchModel`
- [x] `PdeMethod`
- [x] `PdeField`
- [ ] `MigrationTodo`
- [x] `ProcessingSketchRenderer`
- [x] `MigrationReportRenderer`

## First Build Target

- [x] Reach a working scanner before attempting renderer work
- [x] Confirm the scanner is stable on tiny sketches before generating Java output
- [x] Keep the first generated output focused on simple sketches only
