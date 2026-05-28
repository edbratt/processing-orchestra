# V2 OSC Backlog

This is the running list of v2 ideas that have been discussed but are not fully implemented. Keep this list focused on browser instruments, OSC streams, aggregation, and performer-sketch behavior.

## Current Prototype Baseline

Implemented on `v2-osc-instruments`:

- Browser users can choose a name, instrument, and OSC stream.
- Each browser session maps to at most one primary OSC stream.
- Multiple sessions can feed the same OSC stream.
- OSC-only mode can run without opening a local Processing graphics sketch.
- Touch maps to walker XY.
- Buttons and Space emit ping events.
- Double-tap on the touch surface emits a ping event.
- Browser audio has a quick clap-to-ping detector.
- Motion instruments can map tilt to walker XY and shake to ping.
- Motion tuning sliders exist for X/Y trim, X/Y offset, and shake threshold.
- OSC send logging can be enabled with `-Dosc.debug.logging=true`.
- `orchestra-input-v1` streams emit general `/input/position` and `/input/trigger` messages.
- `orchestra-session-v1` streams emit per-session join, leave, position, and trigger messages.
- Audio-capable instruments emit pitch-class events derived from dominant frequency analysis.

## Clap / Ping Detection

- [ ] Move clap detector constants into configuration:
  - peak threshold
  - reset floor
  - debounce time
  - optional per-instrument defaults
- [ ] Add browser controls for audio clap sensitivity.
- [x] Add OSC pitch-class events derived from browser audio.
- [ ] Add browser controls for pitch detection sensitivity and minimum level.
- [ ] Add smoother pitch tracking for singing and sustained instruments.
- [ ] Implement the single-note live instrument plan in [V2 Single-Note Pitch Plan](V2_SINGLE_NOTE_PITCH_PLAN.md).
- [ ] Track short-term noise floor per session so quiet rooms and noisy rooms behave differently.
- [ ] Detect onset using a rising-edge energy delta instead of only absolute peak amplitude.
- [ ] Add a short refractory window after each ping to reduce double triggers.
- [ ] Add optional visual/debug feedback in the browser when the server detects a clap ping.
- [ ] Log why a clap did or did not trigger when OSC debug logging is enabled at a higher level.
- [ ] Support different ping payloads per performer contract, not only `/test/ping` and `/event/ping`.

## Motion Instruments

- [ ] Revisit shake detection after more phone testing:
  - use acceleration delta
  - optionally ignore gravity-including samples when true acceleration is available
  - expose threshold presets for desk, handheld, and active movement
- [x] Add double-tap on the touch surface as a ping source.
- [ ] Consider single surface tap as a separate ping source if performers need it.
- [ ] Add per-axis motion thresholding for gestures such as flick left/right or lift/drop.
- [ ] Add smoothing for tilt walker output to reduce jitter.
- [ ] Add configurable dead zones around neutral tilt.
- [ ] Add calibration actions:
  - set current phone pose as neutral
  - reset trim/offset to defaults
- [ ] Support combining multiple motion streams for one OSC stream:
  - average tilt
  - latest active phone wins
  - max shake intensity wins
  - split X from one player and Y from another

## Stream Aggregation

- [ ] Promote aggregation behavior from code defaults into stream configuration.
- [ ] Support aggregation policies:
  - average XY
  - latest per session
  - most recent active session wins
  - weighted average
  - trigger fan-in
  - leader/solo session
  - round-robin session
  - section/group aggregation
- [ ] Add operator controls to solo one client session into a stream.
- [ ] Add mute controls per session and per stream.
- [ ] Add stream-level gain/sensitivity for audio-derived events.
- [ ] Add stream-level trim/offset for walker XY output.
- [ ] Add session grouping so several users can act as one section before feeding a performer.
- [ ] Define how stale sessions decay or disappear from stream aggregation.

## Instruments And Control Panels

- [ ] Move the hardcoded instrument list into configuration or a schema file.
- [ ] Define each instrument as:
  - browser control panel
  - enabled input features
  - server-side processor
  - compatible OSC contracts
  - default stream assignment
- [ ] Add a richer JavaScript instrument-control schema instead of simple feature booleans.
- [ ] Add instrument-specific control labels and ranges.
- [ ] Allow URL preselection for performance setup, while still allowing in-page selection.
- [ ] Add an operator view of connected users, selected instruments, and selected streams.

## Performer Sketch Contracts

- [ ] Document stable OSC contracts:
  - `walker-trigger`
  - `walker-monitor`
  - `orchestra-input-v1`
  - `orchestra-session-v1`
  - future capability contracts
- [ ] Document expected OSC addresses, argument types, ranges, and timing.
- [ ] Add sample performer startup notes for Lee's sketches.
- [ ] Add a receiver test sketch that prints every OSC message for a stream.
- [ ] Add versioning for OSC contracts so performer sketches can reject unsupported messages.
- [x] Add a session-aware contract so performer sketches can receive session ids and display names.
- [ ] Decide which future performer sketches should use session-aware output versus aggregate output.

## Multi-Performer Orchestration

- [ ] Allow one input stream to mirror to multiple performer ports.
- [ ] Allow one performer to receive multiple named streams.
- [ ] Add routing presets for common setups:
  - one group controls one performer
  - several groups control separate performers
  - one soloist controls a primary performer while ensemble controls a secondary performer
- [ ] Add start/stop health checks for performer destinations where possible.
- [ ] Add a simple monitor showing last OSC send time per stream.

## Configuration And Runtime Control

- [ ] Move more OSC settings out of packaged `application.yaml` into external runtime config.
- [ ] Add validation errors for unknown stream ids, duplicate ports, and invalid contracts.
- [ ] Add `/api/orchestra` details for aggregation policies and performer contracts.
- [ ] Add `/api/status` details for stream membership and last emitted OSC events.
- [ ] Add debug levels:
  - connection/session only
  - generated event summaries
  - full OSC message logging
  - detector internals

## Open Design Questions

- [ ] Should browser events be converted to generic OSC-like events immediately per session, then aggregated, or should raw session state be aggregated first and converted afterward?
- [ ] Should stream aggregation be configured by the stream, by the performer contract, or by the selected instrument?
- [ ] Should audio processors run only for audio-capable instruments, or always run and let the instrument decide whether to emit events?
- [ ] Should solo/mute be controlled by an operator page, by performer sketches over OSC, or by privileged browser clients?
- [ ] Should vectorscope-style monitor streams receive raw session events, aggregated stream output, or both?
