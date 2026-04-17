# Gravity Orbit Sketch Tutorial

This tutorial explains how `GravityOrbitSketch` and `GravityOrbitGradientSketch` were built by starting from the existing `ProcessingSketch` and then modifying a few key parts of the interaction model.

The goal was not to invent a brand-new architecture. The goal was to keep the working multi-user sketch structure, then add the new behavior in a controlled way.

## The General Prompt

The general request was:

- draw one circle for each connected user
- let users move their circles with touch, drag, or mouse input
- make multiple circles pull toward each other
- make them feel more like orbiting bodies than simple dots sliding together
- use mobile shake to push a user farther away
- use motion events to increase or decrease attraction
- never let circles move closer than their outer ring radius
- keep the original sketch available, and put the new behavior in a separate sketch class
- later, add a second sketch variant with a soft attraction-based background gradient
- later, make circle color response more reactive by tying it to speed and allowing near-neon color ranges
- later, replace simple audio-level-only behavior with constrained dominant-frequency detection, holding the last detected frequency when input falls below threshold

That led to a practical implementation strategy:

1. Start with the existing `ProcessingSketch`.
2. Copy its event handling, drawing structure, and per-user state maps into a new class.
3. Keep the same constructor and `runSketch()` shape so the sketch can be selected with `processing.sketch-class`.
4. Replace the old "ease position toward target" logic with a small physics step that runs every frame.

## The Main Design Choice

The most important design decision was this:

Touch input still defines where a user wants their circle to be, but physics decides how the circle moves around that target.

That means the sketch keeps the original interaction feeling:

- the browser still sends touch and motion events in the same way
- each user still has a target position
- the draw loop still owns all visual state

But instead of immediately easing a circle straight to its target, the sketch now:

- stores velocity per user
- applies attraction between users
- adds a sideways orbit force
- applies shake as a repulsion impulse
- clamps positions so circles stay on screen
- separates overlapping circles so their outer rings do not cross

## The Two Variants

The two related sketches are:

- [GravityOrbitSketch.java](/C:/users/ed/dev/processing-server/src/main/java/com/processing/server/GravityOrbitSketch.java:1)
- [GravityOrbitGradientSketch.java](/C:/users/ed/dev/processing-server/src/main/java/com/processing/server/GravityOrbitGradientSketch.java:1)

The gradient version is intentionally a copy-and-modify variant rather than a big abstraction layer. That makes it easier for a learner to compare the two files directly and see what changed.

Most of the interaction model is shared. The gradient sketch adds:

- `drawAttractionGradientBackground()`
- `sampleAttractionField(...)`
- `projectionFactor(...)`
- `lerpAngle(...)`

Those methods create a soft background field using the same user positions and attraction relationships, but leave the main physics logic intact.

The two gravity sketches now also share a reusable local Processing-window control layer through:

- [LocalOperatorLayer.java](/C:/users/ed/dev/processing-server/src/main/java/com/processing/server/LocalOperatorLayer.java:1)

That means the local selection, drag, wheel, HUD, and pause/slow controls are no longer copied inline across the two files. Each sketch supplies an adapter that tells the shared helper how to read and update its own per-user state.

This tutorial is now the main place for the gravity-specific runtime details that would otherwise clutter the more general runtime overview. If you want the sketch-specific frame-loop behavior, color response changes, and gradient-background details, read this file before diving into the source.

## The Key Methods

The main interaction logic still lives in `GravityOrbitSketch`, and `GravityOrbitGradientSketch` reuses the same structure.

### `handleEvent(...)`

This method keeps the original event flow.

- `touch` updates `userTargetPositions`
- `slider` updates size, speed, and gain
- `motion` stores motion state and may trigger shake repulsion
- `session-meta` stores the browser-provided display name
- `session-ended` removes all state for that user

This is the core reason the new sketch could be built quickly from the original one: the server protocol did not need to change.

### `draw()`

The frame loop is still simple:

1. `processEvents()`
2. `processAudio()`
3. `updateUserPhysics()`
4. `updateUserSpeedColorLevels()`
5. `updateUserVisualState()`
6. `drawUsers()`
7. `drawAudioMeter()`

With the local operator layer added, both gravity sketches also include a few Processing-window-only rendering passes after the main user draw:

- local selection ring
- optional local velocity vectors
- optional local attraction lines
- local HUD

In the gradient variant, `draw()` is:

1. `processEvents()`
2. `processAudio()`
3. `updateUserPhysics()`
4. `updateUserSpeedColorLevels()`
5. `updateUserVisualState()`
6. `drawAttractionGradientBackground()`
7. `drawUsers()`
8. `drawAudioMeter()`

That structure matters. It keeps all mutation on the Processing thread and makes the new behavior easier to reason about.

### `updateUserPhysics()`

This is the main new method.

It does three jobs:

1. Apply an anchor force so each circle tends to move back toward the user’s touch target.
2. Compute pairwise forces between every pair of users.
3. Advance velocity and position, then clamp each circle back into the canvas bounds.

The attraction is not a strict physics simulation. It is a small, readable force model designed to feel good in a sketch:

- attraction increases when two circles are farther apart
- orbiting is created by adding a tangential force perpendicular to the line between two circles
- velocity is damped each frame so the system does not spin out of control

This method is where the "gravity-like" feeling comes from.

### `applyAnchorForce(...)`

This method makes touch interaction continue to matter.

Without it, circles would only drift under attraction and orbit forces. With it, a user can drag their circle to a new place and the system will treat that point like a preferred location.

That creates a better interaction model than either extreme:

- not purely direct manipulation
- not purely autonomous physics

### `handleMotionEvent(...)`

This method was adapted from the original sketch rather than replaced.

It still stores:

- `alpha`
- `beta`
- `gamma`
- acceleration axes
- motion magnitude

The new addition is that stronger shake events call `applyShakeImpulse(...)`.

That means motion is used in two different ways:

- continuous motion values influence attraction strength
- sharp shake events create a short repulsion burst

### `applyShakeImpulse(...)`

This method converts shake intensity into velocity.

Instead of teleporting the circle, it adds to the user’s current velocity. That is an important choice because it keeps the sketch feeling fluid and consistent with the rest of the motion system.

The current implementation pushes a user away from the average position of the other users. If there is only one user, it pushes away from the center of the canvas.

### `attractionModifier(...)`

This method is where motion affects the social behavior of the circles.

The current implementation uses:

- `beta` tilt
- `gamma` tilt
- motion magnitude

to scale pairwise attraction up or down inside a bounded range.

This was a pragmatic choice. It gives visible feedback from motion input without needing a more complicated physical model.

### `outerRadiusNormalized(...)`

This method supports the "do not overlap closer than the outer ring radius" requirement.

Instead of using only the solid inner circle, it computes a radius from the larger of:

- the core circle size
- the reactive outer ring size

That means spacing respects what the user actually sees on the canvas.

### `updateUserSpeedColorLevels()`

This method is one of the later refinements.

Originally, the color response was tied to attraction and closeness. That produced a visual effect, but it was not the effect we wanted. The sketch was then changed so color intensity is derived from actual movement speed instead.

The current implementation:

- reads each user velocity from `userVelocities`
- normalizes that speed against `MAX_VELOCITY`
- smooths the result into `userSpeedColorLevels`

That means:

- slow circles stay closer to their softer base palette
- fast circles move farther into a brighter, more saturated, near-neon range

This is a good example of iterative sketch development: keep the structure, swap the metric.

### `processAudio()`

This method changed substantially in the latest version.

Originally, the gravity sketches only measured average audio level. Now they do two things per session:

- compute a gated audio level
- estimate a constrained dominant frequency

The sketches still poll audio from `AudioBuffer`, but they now pass each buffer into:

- [AudioFeatureAnalyzer.java](/C:/users/ed/dev/processing-server/src/main/java/com/processing/server/AudioFeatureAnalyzer.java:1)

That helper:

- assumes the configured `2048` sample buffer
- works with the current audio config, which now defaults to `44.1kHz` mono
- searches only a bounded frequency range instead of doing an open-ended scan
- ignores quiet buffers below `MIN_DETECTION_LEVEL`
- keeps the previous detected frequency when the input is too quiet to trust

That "hold the last frequency" behavior was an intentional design choice. It avoids making the circle color flicker back to some neutral value whenever a user briefly stops making a strong sound.

### `updateUserVisualState()`

This method was added to keep the visual mapping separate from the force model.

It blends together three inputs:

- the dominant-frequency hue from the latest audio analysis
- the existing hue offset from motion and speed-based color reactivity
- the current audio level

In practice, that means:

- frequency now chooses the main hue family for the circle
- motion speed still adds the more electric, near-neon push
- louder audio increases the inner circle size

This was a cleaner way to add the new audio behavior than stuffing more rules into `drawUsers()`.

### `drawUsers()`

This method remains close to the original sketch:

- draw the inner filled circle
- draw the outer ring
- draw the user name inside the circle

The name comes from the browser-provided session metadata. If no name is available, the sketch falls back to `Guest`.

Later refinements in `drawUsers()` include:

- softer starting palette for new users
- speed-based hue/saturation/brightness response
- stronger active-color range so fast movement reads more clearly
- inner-circle sizing driven by current audio level
- an outer ring that no longer pulses from audio level

The current color model is:

- each user starts with a soft base palette
- dominant frequency provides the main hue used for the inner fill
- motion speed still shifts that hue and pushes saturation/brightness upward
- quiet buffers keep the last trusted frequency instead of resetting color

The current size model is:

- the inner filled circle grows and shrinks with audio intensity
- the outer ring stays in place as a framing element
- shake and button bursts can still affect the ring through the existing pulse-boost path, but audio level no longer drives that ring directly

That is still centered on a user-specific color identity, but it gives much more obvious feedback during motion.

### `LocalOperatorLayer`

This is a later refactor rather than the original sketch change, but it is worth calling out because it changed how the gravity sketches are organized.

Originally, local Processing-window controls were added directly inside the gravity sketch classes. That worked, but it duplicated the same logic in both files and would have made it awkward to port back to `ProcessingSketch`.

The current structure is cleaner:

- `LocalOperatorLayer` owns the shared local operator behavior
- each gravity sketch provides an adapter describing:
  - how to find users
  - how big selection should be
  - how to update selected size and gain
  - how to move a target position
  - how to scatter and re-center users
- each gravity sketch still owns its sketch-specific overlays such as attraction lines and velocity vectors

That is a good example of a healthy second-step refactor: first get the behavior working, then pull the shared layer into a reusable helper.

### `drawAttractionGradientBackground()`

This method only exists in `GravityOrbitGradientSketch`.

It paints a subtle background field before the circles are drawn. The field is sampled over a coarse grid so the implementation stays readable and reasonably efficient.

The important design choice is that the background pass is additive, not structural:

- it does not replace the orbit sketch
- it does not change user input
- it does not change the force computation
- it simply visualizes the relationship between users in the background

### `sampleAttractionField(...)`

This method computes one background sample point.

It:

- examines each pair of users
- estimates the strength of the attraction corridor between them
- interpolates hue between the two circle colors
- blends those contributions into one light background tone

That is why the gradient feels continuous instead of like isolated blobs around each circle.

## What Changed In The Gradient Version

If you want to compare the two files directly, the major changes to look for are:

- `draw()` includes `drawAttractionGradientBackground()`
- the sketch uses `colorMode(HSB, 360, 100, 100, 100)` so alpha can be controlled in the field
- `drawAttractionGradientBackground()` samples the full canvas
- `sampleAttractionField(...)` translates pairwise attraction into a color wash
- the live Processing `width` and `height` are used so the background and circles respond to window resizing

That makes `GravityOrbitGradientSketch` a useful study example: same interaction model, one extra rendering layer.

This is a good example of the overall approach: keep the working rendering structure, but swap in new state and behavior where needed.

## Why This Started From the Original Sketch

This sketch is a good example of a useful pattern for collaborative coding:

1. Start from a known-working sketch.
2. Keep the existing event model.
3. Keep the existing draw pipeline.
4. Add only the new per-user state you need.
5. Replace one behavior layer at a time.

In this case, the old behavior layer was mainly:

- touch target
- motion tilt offset
- direct easing toward the target

The new behavior layer became:

- touch target
- per-user velocity
- pairwise attraction
- orbit force
- shake impulse
- minimum-distance separation

That is often a better learning path than trying to write a new sketch from scratch.

## Suggestions For Further Modifications

### If You Use a Coding Agent

Good next prompts would be:

- "Reduce the attraction strength and make orbiting slower and smoother."
- "Change the dominant-frequency hue range so it favors only cool colors or warm colors."
- "Draw faint lines between users when they are pulling on each other."
- "Make only nearby users attract each other instead of all pairs."
- "Replace the current orbit direction logic with one based on user motion direction."
- "Add a visible debug overlay for velocity vectors, attraction strength, and minimum spacing."

Those are good agent tasks because they are local, testable changes to one sketch file.

### If You Want To Modify It Manually

A good manual learning path is:

1. Open `GravityOrbitSketch.java`.
2. Read `draw()` first so you understand the frame loop.
3. Read `handleEvent(...)` to see how input reaches sketch state.
4. Read `updateUserPhysics()` slowly and trace one pair of users on paper.
5. Change one constant at a time and rerun the sketch.

The most useful constants to tune first are:

- `BASE_ATTRACTION_FORCE`
- `BASE_ORBIT_FORCE`
- `BASE_SHAKE_IMPULSE`
- `VELOCITY_DAMPING`
- `MAX_VELOCITY`

Then, for the current color system:

- the base saturation/brightness chosen in `initializeUser(...)`
- the frequency-to-hue mapping in `frequencyToHue(...)`
- the speed normalization in `updateUserSpeedColorLevels()`
- the audio gate and frequency bounds in `AudioFeatureAnalyzer`

Then, for the gradient variant:

- `BACKGROUND_FIELD_STEP`
- the strength and falloff values in `sampleAttractionField(...)`

That is a good Java learning exercise because you can see how small numeric changes affect behavior without changing the overall program structure.

## A Few Good Manual Experiments

- Make attraction stronger but lower `MAX_VELOCITY` so the sketch feels heavy instead of chaotic.
- Remove orbit force temporarily and observe how much behavior came from attraction alone.
- Make shake affect only repulsion and not attraction, then compare the feel.
- Change `directionAwayFromOthers()` to push away from the nearest user instead of the group center.
- Add comments to `updateUserPhysics()` in your own words after you understand it.
- Compare `GravityOrbitSketch` and `GravityOrbitGradientSketch` side by side and mark only the methods that were added for the gradient effect.
- Change the speed-based color mapping so only the fastest movement becomes neon.

## Takeaway

The fastest path to this sketch was:

- keep the original multi-user sketch structure
- fork it into a new sketch class
- preserve the event and drawing pipeline
- add a small physics layer in a few focused methods
- make later visual experiments as additional variants instead of rewriting the original

That is a solid pattern for future custom sketches too. Start from a working baseline, then modify the key methods that control state update and rendering instead of rewriting everything at once.
