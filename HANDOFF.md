# Handoff Notes

## Current State

The project is in a working state for:

- local HTTP use on `http://localhost:8080/`
- optional LAN/mobile HTTPS use on `https://YOUR_IP:8443/` or `https://YOUR_HOSTNAME.local:8443/`
- browser-driven Processing sketch control
- browser microphone capture feeding audio-reactive visuals
- browser motion input from supported phones over HTTPS

The current browser/visual feature set includes:

- touch input
- sliders for size, speed, audio gain, and motion trim
- buttons for `Burst`, `Spin Color`, and `Scatter`
- microphone audio input
- phone motion input with tilt and shake behavior
- disconnect and shutdown notifications in the browser

## Main Decisions Made

### Runtime Config Model

We moved away from asking users to manually uncomment YAML blocks.

Current approach:
- base config lives in `src/main/resources/application.yaml`
- optional HTTPS overlay lives in `config/application-https.yaml`
- HTTPS is enabled at runtime with `-Dapp.config=config/application-https.yaml`
- `run.ps1` and `run.sh` start local HTTP
- `run-https.ps1` and `run-https.sh` start HTTP + HTTPS

Why:
- avoids YAML indentation mistakes
- keeps local startup simple
- keeps HTTPS optional

### Keystore Handling

We decided not to bundle the keystore into the jar.

Current approach:
- `keystore.p12` lives at the project root
- `processing-server-ca.cer` is exported alongside it
- `create-keystore.ps1` and `create-keystore.sh` generate both files
- regenerating the keystore does not require rebuilding the jar

Why:
- better user workflow
- easier certificate regeneration when the LAN IP changes

### Packaging

We kept `processing-server` as an executable shaded jar rather than reverting to the thinner Helidon example layout used by `processing-client`.

Current approach:
- app builds to `target/processing-server-1.0-SNAPSHOT.jar`
- users can launch with `java -jar`
- scripts discover and run the newest packaged jar automatically

Why:
- this app behaves more like a runnable desktop/server tool than a minimal framework example
- users should not have to manage `target/libs` or a custom classpath

### WebSocket Model

The app uses one shared `/ws` connection per browser client with multiple logical payload types:

- JSON control events
- JSON motion events
- binary audio frames

Important fixes made:
- `WebSocketHandler` must be created per connection, not reused as one mutable shared instance
- the `/ws` route must be attached on both the default HTTP listener and the optional TLS listener

This was the key fix for the earlier symptom where:
- the page loaded
- the browser showed `Session ID: Connecting...`
- the Processing sketch did not react

### Processing Window Startup

The sketch startup was not actually broken by the Helidon/config simplification work.

What we learned:
- the server starts quickly
- `runSketch()`, `settings()`, and `setup()` do execute
- the apparent startup problem came from the non-interactive tool launch environment, not the app itself
- when launched normally with `.\run.ps1`, the sketch window appears correctly

We removed the temporary forced window visibility/location tweak after confirming that.

## Functional Changes Added

### Control Semantics

- `Size` slider changes the core circle size
- the outer audio ring scales proportionally from that base size
- `Speed` changes movement responsiveness and effect decay speed

### Button Behavior

Button labels were updated and behavior clarified:
- `Burst`
- `Spin Color`
- `Scatter`

Those actions now create visible temporary effects in the sketch.

### Audio Gain

Added an `Audio Gain` slider in the browser UI.

Current behavior:
- slider attenuates or amplifies incoming audio before it affects visuals
- label is shown in dB-style terms rather than raw percent

### Motion Input

Added phone motion input over the existing WebSocket path.

Current behavior:
- browser has `Enable Motion` / `Stop Motion` controls
- browser requests motion/orientation permission when needed
- browser combines the latest orientation and acceleration data into a timed `motion` sample
- server clamps and queues motion samples using `MotionConfig`
- sketch uses tilt for positional offset and shake for burst-like effects
- browser-side `Motion Trim` changes motion sensitivity locally without requiring a rebuild

Important implementation details:
- motion update rate, clamp values, mapping values, and debug logging are configurable
- motion config currently lives in `src/main/resources/application.yaml`
- because that file is packaged into the jar, changing those settings requires a rebuild

### Disconnect and Shutdown UX

Added client notification behavior for disconnects and shutdowns:
- server sends a final shutdown message before clean close
- browser shows a useful banner
- page scrolls back to the top so the message is visible

## Documentation Changes

Docs were updated to match the current system:
- `README.md`
- `ARCHITECTURE.md`
- `CUSTOMIZATION.md`
- `CHANGELOG.md`
- `MOTION_INPUT_PLAN.md`
- `RUNTIME_OVERVIEW.md`

Current doc direction:
- describe only the current supported workflow
- avoid historical/migration notes unless they are still operationally relevant
- use Mermaid where it helps, but keep explanatory text near the diagrams
- add navigation with a top-level table of contents and local `Contents:` blocks for large docs

### New Docs Added

- `MOTION_INPUT_PLAN.md`
  evaluation of phone motion vs. geolocation/position, plus an implementation work list
- `RUNTIME_OVERVIEW.md`
  plain-English explanation of how the runtime is wired together, tied directly to the source files
- `HANDOFF.md`
  this ongoing resume point for future sessions

## Architecture Doc Status

`ARCHITECTURE.md` is in a much better state than before:

- Mermaid diagrams replace the old broken box layouts
- the main architecture vs. data-flow distinction is now explicit
- the diagrams show one WebSocket channel with separate conceptual JSON and binary audio streams
- descriptive detail from the old text diagrams was redistributed into the component sections
- a top-level TOC and section-local `Contents:` blocks were added

Remaining note:
- some Mermaid node labels may still benefit from additional line breaks if GitHub preview truncates text

## Verified Behavior

Verified during this round:
- build succeeds with `mvn clean package -DskipTests`
- local HTTP works
- HTTPS overlay works
- browser UI loads on both ports
- controls affect the Processing sketch
- audio path drives the visual response
- motion path sends from supported phones and affects the sketch
- run scripts and HTTPS scripts work with the current packaging/config setup

## Source Files That Matter Most

If resuming technical work, these are the main files to read first:

- `src/main/java/com/processing/server/Main.java`
- `src/main/java/com/processing/server/WebSocketHandler.java`
- `src/main/java/com/processing/server/ProcessingSketch.java`
- `src/main/java/com/processing/server/MotionConfig.java`
- `src/main/java/com/processing/server/UserInputEvent.java`
- `src/main/resources/application.yaml`
- `src/main/resources/static/index.html`

Useful support docs:

- `README.md`
- `ARCHITECTURE.md`
- `CUSTOMIZATION.md`
- `MOTION_INPUT_PLAN.md`
- `RUNTIME_OVERVIEW.md`

## Current Working Tree Notes

At the time of this handoff, the intentionally local files still outside the repo are:

- `note.md`
- `processing-server-ca.cer`

Ignore rules were added for:
- `.codex*`
- `.tmp-*/`

## Certificate/IP Reality

The certificate must match the actual LAN identity in use.

Most recent real LAN IP discussed:
- `192.168.50.95`

If the network changes:
- regenerate the keystore
- trust the CA again on new client devices if needed

## Suggested Next Session Starting Points

Good candidates for the next session:
1. Decide whether motion tuning should move out of `src/main/resources/application.yaml` into an external runtime config so rebuilds are not needed for mapping changes.
2. Tidy Mermaid node labels if GitHub still truncates text.
3. Decide whether `processing-server-ca.cer` should also be ignored in `.gitignore`.
4. Review the motion mapping in the sketch and decide whether tilt/shake should drive additional visual properties.
5. Commit and push the currently prepared docs/runtime changes if that has not already been done.

## Recommended Restart Context

If resuming later, the shortest accurate summary is:

This repo now runs locally on HTTP by default and optionally on HTTPS using `-Dapp.config=config/application-https.yaml`. The keystore is external at the project root, not bundled into the jar. The app is packaged as an executable shaded jar. The important bug fixes were making WebSocket handlers per-connection and wiring `/ws` on both default and TLS sockets. Browser controls, audio gain, motion input, and shutdown messaging are all working. The main remaining polish items are motion-config ergonomics, Mermaid readability, and commit/ignore cleanup around local certificate artifacts.
