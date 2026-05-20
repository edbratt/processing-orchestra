# Samples Index

This index tracks sketches that are currently included in the `processing-server` project.

Scope:
- included in this repository now
- runnable or intended to be runnable from the app via `processing.sketch-class`

Excluded for now:
- external sketches reviewed from local copies of Matt Pearson's [100 Abandoned Artworks](http://abandonedart.org)
- work-in-progress candidate sketches not yet brought into this repo
- converter prospects that have not been promoted into project code

## Running a Sketch

The launch scripts use `processing.sketch-class` to choose which Processing sketch starts on the server. Pass the run property through the `-Properties` argument:

```powershell
.\run.ps1 -Properties "-Dprocessing.sketch-class=com.processing.server.StarterSketch"
```

Use the same property with HTTPS when testing from phones, microphone input, or motion sensors on other devices:

```powershell
.\run-https.ps1 -Properties "-Dprocessing.sketch-class=com.processing.server.StarterSketch"
```

The `Run property` column below contains the value to put inside the quoted `-Properties` argument. If no property is provided, the app uses the default sketch from `src/main/resources/application.yaml`.

## App Sketches

| Sketch class | Run property | File | Purpose | Status |
|---|---|---|---|---|
| `ProcessingSketch` | `-Dprocessing.sketch-class=com.processing.server.ProcessingSketch` | [ProcessingSketch.java](../../src/main/java/com/processing/server/ProcessingSketch.java) | Default multi-user sketch with touch, sliders, buttons, audio, motion, and local operator controls | Included |
| `StarterSketch` | `-Dprocessing.sketch-class=com.processing.server.StarterSketch` | [StarterSketch.java](../../src/main/java/com/processing/server/StarterSketch.java) | Minimal example sketch showing browser touch, slider, keyboard, and palette usage | Included |
| `GravityOrbitSketch` | `-Dprocessing.sketch-class=com.processing.server.GravityOrbitSketch` | [GravityOrbitSketch.java](../../src/main/java/com/processing/server/GravityOrbitSketch.java) | Physics-based multi-user orbit sketch with touch anchors and motion-modified behavior | Included |
| `GravityOrbitGradientSketch` | `-Dprocessing.sketch-class=com.processing.server.GravityOrbitGradientSketch` | [GravityOrbitGradientSketch.java](../../src/main/java/com/processing/server/GravityOrbitGradientSketch.java) | Gravity-orbit variant with attraction-field background rendering | Included |
| `AbandonedArt96` | `-Dprocessing.sketch-class=com.processing.server.AbandonedArt96` | [AbandonedArt96.java](../../src/main/java/com/processing/server/AbandonedArt96.java) | Adaptation of AbandonedArt 96 for touch/drag and motion input | Included |
| `AbandonedArt79Sketch` | `-Dprocessing.sketch-class=com.processing.server.AbandonedArt79Sketch` | [AbandonedArt79Sketch.java](../../src/main/java/com/processing/server/AbandonedArt79Sketch.java) | Adaptation of AbandonedArt 79 as per-user motion-oriented grid fields | Included |
| `AbandonedArt90Sketch` | `-Dprocessing.sketch-class=com.processing.server.AbandonedArt90Sketch` | [AbandonedArt90Sketch.java](../../src/main/java/com/processing/server/AbandonedArt90Sketch.java) | Adaptation of AbandonedArt 90 as per-user movable spherical line-cloud graphics | Included |

## PDE Converter Trial Samples

These are sample PDE inputs used to exercise the converter workflow. They are included in the repo as trial/sample material. Some generated Java outputs from these samples may also exist under `generated-src/`, but this table tracks the original PDE source fixtures.

| Sample | File | Purpose | Status |
|---|---|---|---|
| `01-mouse-follow` | [01-mouse-follow.pde](../../samples/pde-converter-trials/01-mouse-follow.pde) | Simple mouse-position sample for `browser-touch` testing | Included |
| `02-keyboard-toggle` | [02-keyboard-toggle.pde](../../samples/pde-converter-trials/02-keyboard-toggle.pde) | Keyboard sample for `browser-keyboard` testing | Included |
| `03-helper-trails` | [03-helper-trails.pde](../../samples/pde-converter-trials/03-helper-trails.pde) | Mouse-press sample for `browser-touch-press` testing | Included |
| `04-drag-paint` | [04-drag-paint.pde](../../samples/pde-converter-trials/04-drag-paint.pde) | Drag sample for `browser-touch-drag` testing | Included |

## Related Notes

- External AbandonedArt review work is tracked separately in [abandoned-art-converter-review.md](abandoned-art-converter-review.md).
- This file should only list sketches and trial samples that are already part of the repository.
