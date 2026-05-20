# PDE To ProcessingSketch Script Specification

## Purpose

This document defines a small migration tool that helps move a sketch from the Processing IDE format into this project's `ProcessingSketch.java` format.

The tool is not meant to fully solve the integration problem. Its job is to:

- preserve as much visual code as possible
- generate a usable project-side starter class
- mark places where a student must make design decisions
- prepare the code for a second step where a student or an AI coding agent helps resolve those decisions

## Goal

Input:

- a simple Processing IDE sketch, usually one `.pde` file

Output:

- a generated Java class shaped like this project's `ProcessingSketch.java`
- a migration report
- clear TODO notes for the parts that require human choices

## Non-Goals

The first version should not try to:

- fully integrate every sketch automatically
- convert every possible multi-tab Processing project
- understand every Processing library
- decide automatically how local input should become browser/session input
- decide automatically how audio or motion should affect the sketch

This is a migration assistant, not a full translator.

## Expected Use

The expected workflow is:

1. a student creates or pastes a sketch from the Processing IDE
2. the script generates a starter `ProcessingSketch` version
3. the script also generates TODO notes and a migration report
4. the student, teacher, or an AI coding agent resolves those TODO notes
5. the student tests and refines the result inside this project

## Input Assumptions

Version 1 should assume:

- one main `.pde` file
- basic Processing syntax
- standard Processing functions such as `setup()`, `draw()`, `mousePressed()`, `keyPressed()`
- top-level variables and helper functions

Version 1 may reject or warn on:

- multiple PDE tabs
- custom libraries
- unusual preprocessing tricks
- advanced Java syntax inside the PDE sketch

## Output Files

The script should produce:

1. a generated Java file
   suggested name:
   - `ProcessingSketch.generated.java`

2. a migration notes file
   suggested name:
   - `pde-migration-report.md`

3. optional prompt helper file for AI follow-up
   suggested name:
   - `pde-migration-ai-prompts.md`

## Core Conversion Rules

### 1. Wrap The PDE Sketch In A Java Class

The tool should generate:

- `public class ProcessingSketch extends PApplet`

It should preserve the project's required constructor shape:

- `EventQueue`
- `AudioBuffer`
- width
- height
- `DebugConfig`
- `MotionConfig`

It should also generate:

- `settings()`
- `setup()`
- `draw()`
- `runSketch()`

### 2. Convert Global Variables To Class Fields

Top-level PDE variables should become Java class fields.

Example:

```java
float circleX = 100;
int count = 0;
```

becomes:

```java
private float circleX = 100;
private int count = 0;
```

### 3. Preserve Helper Functions

Top-level helper functions should become class methods.

Example:

```java
void drawStar(float x, float y) {
  ellipse(x, y, 10, 10);
}
```

becomes:

```java
private void drawStar(float x, float y) {
    ellipse(x, y, 10, 10);
}
```

### 4. Convert Processing Lifecycle Functions

The script should recognize and preserve:

- `setup()`
- `draw()`
- `settings()`
- input handlers such as:
  - `mousePressed()`
  - `mouseDragged()`
  - `mouseReleased()`
  - `keyPressed()`
  - `keyReleased()`

They should be converted into Java override methods where appropriate.

### 5. Preserve Visual Logic First

The script should prioritize preserving drawing and animation logic over trying to over-integrate.

If a decision is unclear, the script should:

- keep the visual code
- add a TODO comment
- report the unresolved choice in the migration report

When possible, the script should prefer preserving the original local behavior in the first conversion rather than removing it immediately.

## Integration Rules

### Constructor And Project Fields

The generated class should include the standard project fields:

- `eventQueue`
- `audioBuffer`
- `sketchWidth`
- `sketchHeight`
- `debugConfig`
- `motionConfig`

The generated `draw()` should include the normal project pattern, even if some methods are placeholders:

```java
background(0);
processEvents();
processAudio();
drawGeneratedSketch();
```

This is important because it keeps the generated file close to the shape of the real project sketch.

### Generated Draw Structure

Instead of placing all imported PDE drawing logic directly in `draw()`, the script should move it into a helper such as:

```java
private void drawGeneratedSketch() {
    // migrated PDE drawing logic
}
```

This keeps the runtime flow readable and leaves room for the existing project hooks.

### Placeholder Methods

If they do not already exist, the script should generate placeholders for:

- `processEvents()`
- `processAudio()`
- `initializeUser(String sessionId)`

These placeholders should include TODO notes explaining what still needs to be decided.

## TODO Detection Rules

The script should explicitly detect patterns that require a human decision.

### Local Mouse Input

If the PDE sketch uses:

- `mouseX`
- `mouseY`
- `pmouseX`
- `pmouseY`
- `mousePressed`
- mouse event handlers

the script should add TODO notes such as:

```java
// TODO: Local mouse input was preserved for the first conversion.
// Decide later whether to keep it, mirror it in the browser client, or replace it with browser touch/controller input.
// Also decide whether this should use one shared position or per-session positions.
```

### Local Keyboard Input

If the PDE sketch uses:

- `key`
- `keyCode`
- `keyPressed()`
- `keyReleased()`

the script should add TODO notes such as:

```java
// TODO: Local keyboard behavior was preserved for the first conversion.
// Decide later whether it should stay local to the server machine, be mirrored in the browser,
// or be replaced by browser buttons, sliders, or key events.
```

### Single-User State

If the PDE sketch has one set of variables controlling one object, and that object is likely to become multi-user, the script should add a TODO such as:

```java
// TODO: Decide whether this state should remain global or become per-session state.
```

### Audio

If the PDE sketch already contains audio-like logic, or if the student chooses an audio-reactive mode, the script should add:

```java
// TODO: Decide how browser audio from AudioBuffer should affect this sketch.
// Possible hook points: processAudio(), calculateAudioLevel(), drawGeneratedSketch().
```

### Motion

If the student chooses a motion-reactive mode, the script should add:

```java
// TODO: Decide how phone motion should affect this sketch.
// Possible hook points: handleEvent(...), handleMotionEvent(...), drawGeneratedSketch().
```

## Migration Modes

The tool should support a small set of explicit modes.

### 1. Visual-Only

Goal:

- get the sketch running inside the project with minimal structural change

Behavior:

- preserve the visual code
- preserve local mouse and keyboard input where possible
- do not attempt multi-user state conversion
- generate warnings where local input exists

### 2. Multi-User Ready

Goal:

- prepare the sketch to use session-based state

Behavior:

- generate suggested per-session maps for key visual state
- preserve local input initially unless the student chooses to remove it
- add TODO notes where one-object variables probably need to become per-session

### 3. Audio-Reactive Ready

Goal:

- prepare the sketch to consume levels from `AudioBuffer`

Behavior:

- generate `processAudio()` placeholder
- preserve existing local interaction unless it conflicts with generated code
- generate suggested hook comments for audio-driven parameters

### 4. Motion-Reactive Ready

Goal:

- prepare the sketch to consume phone motion data

Behavior:

- generate `handleEvent(...)` and `handleMotionEvent(...)` placeholders if needed
- preserve existing local interaction unless it conflicts with generated code
- add motion integration TODO notes

### 5. Preserve-Local-Input

Goal:

- keep the converted sketch usable with its original Processing mouse and keyboard interaction while still moving it into the project structure

Behavior:

- preserve local mouse handlers and local keyboard handlers where possible
- preserve direct references such as `mouseX`, `mouseY`, or key-based behavior where they still compile cleanly inside `ProcessingSketch`
- mark those sections as transitional
- add TODO notes describing browser-mapping options for a later step

## Migration Report Contents

The generated `pde-migration-report.md` should include:

### Summary

- source file name
- selected migration mode
- number of global variables found
- number of helper methods found
- which Processing lifecycle methods were found

### Detected Input Patterns

- mouse usage
- keyboard usage
- any existing audio-like logic
- any obvious one-user assumptions

### Preserved Local Interaction

- which local mouse behaviors were kept
- which local keyboard behaviors were kept
- where those behaviors now live in the generated class
- whether the script detected any likely conflict between local and future browser-driven control

### TODO List

- all unresolved integration decisions

### Suggested Next Step

This section should recommend which file and methods to inspect next.

Example:

- Start in `ProcessingSketch.generated.java`
- Review TODO notes in `drawGeneratedSketch()`
- Decide whether `circleX` and `circleY` should become per-session state

## Follow-Up Decision Support

The script should prepare for a second step where a student or an AI coding agent resolves the TODO notes.

To support that, the script should classify each TODO into a small number of decision types:

- input source decision
- global vs per-session state decision
- audio mapping decision
- motion mapping decision
- local keyboard vs browser control decision
- preserve-local-input vs browser-mapping decision

## Suggested AI Question Patterns

If an AI coding agent is used for the follow-up step, it should ask short questions like:

- Should this sketch react to one shared position or one position per connected user?
- Should mouse-driven movement become touch input from the browser?
- Should keyboard controls stay local, or should they become browser buttons/sliders?
- Should audio affect size, color, motion, or background effects?
- Should phone tilt affect position, color, or another visual property?

These questions should be:

- concrete
- low in jargon
- tied directly to one TODO note

Equivalent manual prompts for a student or teacher should also be easy to read in the migration report.

## Preserve Existing Integration Decisions

If the student is working from an already converted `ProcessingSketch.java`, the follow-up AI should assume that existing integration choices were made on purpose unless the student says otherwise.

That means:

- keep existing event-routing decisions intact
- keep existing per-session state structure intact
- keep existing audio and motion integration intact
- avoid reopening earlier migration questions unless the current change requires it

The AI should only ask new conversion-style questions when:

- the student adds a brand-new control or event type
- the student introduces a new kind of visual state that clearly needs a global-versus-per-session decision
- the student wants to replace an existing integration choice

Suggested prompt rule for the AI layer:

```text
If a converted ProcessingSketch.java already exists, preserve its current integration decisions by default.
Do not ask the student to re-decide earlier event, session, audio, or motion wiring unless the new change introduces a control or behavior that was not part of the original conversion.
```

## Mapping Local Input Into The Browser Client

Local input in the original PDE sketch should not always be mapped into the browser automatically. The first conversion should usually preserve local interaction first, then offer browser-mapping choices as a second step.

### Mouse Input

Mouse-based behavior is often a good candidate for browser mapping.

Reasonable future mappings include:

- `mouseX`, `mouseY` -> touch area or pointer area in `index.html`
- `mousePressed()` -> pointer down or touch start
- `mouseDragged()` -> touch drag or pointer move
- `mouseReleased()` -> pointer up or touch end

The script should report these as suggestions, not as forced conversions.

### Keyboard Input

Keyboard input can be mapped into the browser, but it requires a design choice.

Possible future mappings include:

- direct browser key events
- buttons in the browser UI
- sliders or toggles for stateful controls

The script should treat keyboard mapping as optional and should not assume that direct browser key input is always the best choice.

Default recommendation:

- preserve local keyboard behavior in the first conversion
- ask later whether that behavior should stay local, become browser keys, or become explicit browser controls

## Command Shape

Suggested command:

```text
pde-to-processing-sketch <input.pde> --mode visual-only
```

Optional flags:

- `--mode visual-only`
- `--mode multi-user`
- `--mode audio-reactive`
- `--mode motion-reactive`
- `--mode preserve-local-input`
- `--output-dir <dir>`

## Version 1 Success Criteria

Version 1 is successful if it can:

1. accept a simple one-file PDE sketch
2. generate a compilable starter class or near-compilable starter class
3. preserve most drawing code
4. preserve local mouse/keyboard behavior when requested
5. generate useful TODO notes
6. generate a migration report that a student can understand
7. support a student-led or AI-assisted follow-up step without the student needing to explain the whole project from scratch

## Recommended Next Step

After approving this spec, the next implementation step should be:

1. write a minimal script that handles one-file sketches
2. test it on very small sketches first
3. only then add mode-specific TODO generation
4. after that, draft the AI follow-up prompts that turn the migration report into a short question flow
