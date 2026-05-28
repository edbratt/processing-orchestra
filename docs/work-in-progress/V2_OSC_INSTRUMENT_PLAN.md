# V2 OSC Instrument Plan

## Goal

V2 separates browser control surfaces, server-side interpretation, and visual performers.
Users choose an instrument when they connect. The server processes each session independently,
maps sessions into one OSC stream at most, combines multiple sessions where configured, and
sends OSC to performer sketches listening on known ports.

## Vocabulary

- Instrument: a named JavaScript control panel plus server-side processor.
- Session assignment: a connected user name, selected instrument, and optional OSC stream id.
- OSC stream: a logical output bus with destination host/port and message contract.
- Performer: a Processing sketch or other receiver that listens to an OSC stream.

## Target Flow

```text
browser instrument UI
  -> WebSocket events/audio/motion
  -> per-session instrument processor
  -> stream aggregator
  -> OSC output service
  -> performer sketch
```

## First Prototype Slice

The first slice is intentionally small and is now implemented on branch
`v2-osc-instruments`:

- Add opt-in `output.mode=osc` so the server can run without a local Processing sketch.
- Add a basic OSC output service with no external OSC dependency.
- Map browser touch to Lee's walker signal:
  - `/walker/signal/x` float in `[-1..1]`
  - `/walker/signal/y` float in `[-1..1]`
- Map browser action buttons to Lee's trigger messages:
  - `/test/ping` string `"ping"`, int count
  - `/event/ping` int count
- Send walker data to a performer port, default `12000`.
- Optionally mirror walker data and event pings to the vectorscope port, default `12001`.

This is enough to run the server as a browser-to-OSC bridge and drive Lee's
`spiral_walker` and `lissajous_display` sketches externally.

## Current Stream Prototype

The second slice adds named streams and per-session stream assignment.

Default compatibility config keeps the original behavior:

```yaml
output:
  mode: "osc"

osc:
  default-stream: "spiral-main"
  streams: ""
  host: "127.0.0.1"
  walker-port: 12000
  vectorscope-port: 12001
  mirror-vectorscope: true
  fps: 60
  debug:
    logging: false
    sample-limit: 100
```

For multiple explicit streams, set `osc.streams` to a comma-separated id list:

```yaml
osc:
  default-stream: "spiral-main"
  streams: "spiral-main,gravity-orbit,vectorscope"
  stream:
    spiral-main:
      host: "127.0.0.1"
      port: 12000
      contract: "walker-trigger"
    gravity-orbit:
      host: "127.0.0.1"
      port: 12002
      contract: "orchestra-session-v1"
    vectorscope:
      host: "127.0.0.1"
      port: 12001
      contract: "walker-monitor"
      mirror-source: "spiral-main"
```

Browser sessions can choose a stream with the OSC stream field or with
`?streamId=gravity-orbit`. Each session maps to at most one primary stream.
The browser also exposes an instrument selector. For the current prototype:

- `full-orchestra`: touch/buttons/Space plus browser-audio clap pings.
- `touch-walker`: touch controls walker XY; buttons and Space send pings.
- `audio-clap`: browser-audio claps send pings.
- `tilt-walker`: phone tilt controls walker XY.
- `shake-ping`: phone shake sends pings.
- `motion-orchestra`: phone tilt controls walker XY; shake sends pings.

The selected instrument controls which browser control groups remain visible.
`full-orchestra` shows every control enabled by the active sketch/controller config;
limited instruments only show the preconfigured feature set for that instrument.
You can preselect an instrument with `?instrumentId=audio-clap`.

Current aggregation behavior:

- Browser touch is interpreted per session as walker XY.
- On each OSC pump tick, latest walker positions for sessions mapped to the same stream are averaged.
- Browser action buttons, Space, and touch-surface double-tap emit trigger events into the assigned stream.
- Browser audio streams are processed independently per session with a quick clap detector:
  - PCM chunks are drained per session in the OSC loop.
  - Peak amplitude above `0.22` emits a ping, with a `350ms` debounce.
  - A low floor of `0.05` tracks clap onset/reset, following the shape of Lee's `clap_trigger`.
  - Dominant frequency is estimated from PCM chunks and converted to pitch classes such as `A`, `A#`, `B`, and `C`.
  - Pitch events are throttled to at most one repeated note every `250ms`, while note changes emit immediately.
- Browser motion streams are processed independently per session for motion instruments:
  - `(gamma / 60) * xTrim + xOffset` maps to walker X.
  - `(-beta / 60) * yTrim + yOffset` maps to walker Y.
  - acceleration magnitude delta above the per-session shake threshold emits a ping, with a `350ms` debounce.
  - The browser prefers acceleration without gravity when available, then falls back to acceleration including gravity.
  - Motion tuning controls are exposed in the browser:
    - X Trim: `0.25x..3.00x`, horizontal slider.
    - X Offset: `-1.00..1.00`, horizontal slider.
    - Y Trim: `0.25x..3.00x`, vertical slider.
    - Y Offset: `-1.00..1.00`, vertical slider.
    - Shake Threshold: `0.20g..3.00g`, horizontal slider.
- Triggers maintain independent counters per stream.
- Streams with `mirror-source` receive mirrored walker and ping output from their source stream.

Current OSC contracts:

- `walker-trigger`: Lee compatibility output for `spiral_walker`.
  - `/walker/signal/x` float `[-1..1]`
  - `/walker/signal/y` float `[-1..1]`
  - `/test/ping` string `"ping"`, int count
- `walker-monitor`: Lee vectorscope compatibility output.
  - `/walker/signal/x` float `[-1..1]`
  - `/walker/signal/y` float `[-1..1]`
  - `/event/ping` int count
- `orchestra-input-v1`: general v2 performer output.
  - `/input/position` float x, float y in `[-1..1]`
  - `/input/trigger` string kind, int count
  - `/input/pitch` string note, int midiNote, float frequencyHz, float level
- `orchestra-session-v1`: session-aware v2 performer output.
  - `/session/join` string sessionId, string name, string instrumentId
  - `/session/leave` string sessionId
  - `/session/position` string sessionId, float x, float y in `[-1..1]`
  - `/session/trigger` string sessionId, string kind, int count
  - `/session/pitch` string sessionId, string note, int midiNote, float frequencyHz, float level

Enable OSC success logging with:

```powershell
.\run.ps1 -Properties "-Doutput.mode=osc -Dosc.debug.logging=true -Dosc.debug.sample-limit=200"
```

Example log lines:

```text
OSC -> spiral-main 127.0.0.1:12000 /walker/signal/x 0.25
OSC -> spiral-main 127.0.0.1:12000 /test/ping "ping" 3
OSC -> gravity-orbit 127.0.0.1:12002 /session/position "7f3a1c22-..." 0.25 -0.4
OSC -> gravity-orbit 127.0.0.1:12002 /session/trigger "7f3a1c22-..." "surfaceDoubleTap" 4
OSC -> gravity-orbit 127.0.0.1:12002 /session/pitch "7f3a1c22-..." "A#" 70 466.16 0.12
OSC -> vectorscope 127.0.0.1:12001 /event/ping 3
```

This is still a prototype bridge, but the branch now has the core v2 concepts in place:

- Browser users can choose an instrument and OSC stream when they connect.
- The server stores the selected name, instrument, and stream on the session.
- Instrument selection controls which browser controls are visible.
- The OSC pump applies the current per-session processors for touch, buttons, keyboard, audio clap, tilt, and shake.
- Stream aggregation combines compatible session output before sending OSC.

## Later Phases

The running backlog for ideas not yet implemented lives in [V2 OSC Backlog](V2_OSC_BACKLOG.md).

1. Instrument definitions
   - Move the hardcoded instrument list into configuration or a schema file.
   - Replace `ControllerConfig` feature booleans with a richer control schema.
   - Define each JavaScript control panel and matching server-side processor explicitly.

2. Stream assignment UI and management
   - Keep the current one-primary-stream-per-session rule.
   - Add clearer server status and operator controls for remapping sessions during a performance.
   - Add validation for unknown stream ids and unavailable performer destinations.

3. Per-session processors
   - Move the prototype touch, motion, button, keyboard, and audio interpretation into named instrument processor classes.
   - Add richer audio processors beyond clap/onset detection.

4. Aggregation policies
   - Latest per session.
   - Average XY.
   - Trigger fan-in.
   - Leader/solo assignment.

5. Performer contracts
   - Continue separating compatibility contracts such as `walker-trigger` from general contracts such as `orchestra-input-v1` and session-aware contracts such as `orchestra-session-v1`.
   - Document receiver expectations and provide sample listener sketches.

## Design Constraint

Do not collapse instrument, stream, and performer into one concept:

- A user plays an instrument.
- A session is assigned to an OSC stream.
- A performer listens to that stream.
