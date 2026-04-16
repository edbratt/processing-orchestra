# PDE Converter Trial Sketches

These sketches are small, controlled examples for trying the PDE converter before real student work is available.

Each sketch is paired with a short notes file that explains:

- what the sketch is testing
- what should convert cleanly
- what should remain a manual decision after conversion

## Files

- `01-mouse-follow.pde`
- `01-mouse-follow-notes.md`
- `02-keyboard-toggle.pde`
- `02-keyboard-toggle-notes.md`
- `03-helper-trails.pde`
- `03-helper-trails-notes.md`
- `04-drag-paint.pde`
- `04-drag-paint-notes.md`

## Suggested order

1. `01-mouse-follow.pde`
2. `02-keyboard-toggle.pde`
3. `03-helper-trails.pde`
4. `04-drag-paint.pde`

## How to run the converter

```powershell
java -cp target/classes com.processing.server.tools.pde.PdeToProcessingSketchMain .\samples\pde-converter-trials\01-mouse-follow.pde --output-dir .\target\pde-output\01-mouse-follow
```

Replace the input file and output directory for the other sketches.

## What to review after each conversion

1. Did the generated `ProcessingSketchGenerated.java` compile cleanly?
2. Did the visual logic come across clearly?
3. Were local input handlers preserved where expected?
4. Did the TODO notes point to real design decisions instead of guessing?
5. Would a student understand the generated output and the migration report?
