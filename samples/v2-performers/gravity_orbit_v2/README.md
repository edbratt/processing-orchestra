# gravity_orbit_v2

Standalone Processing performer sketch for v2 OSC mode.

## Requirements

- Processing 4
- `oscP5` Processing library

## Run

1. Open `gravity_orbit_v2.pde` in the Processing IDE.
2. Install `oscP5` if needed.
3. Run the sketch. It listens on UDP port `12002`.
4. Start the server in OSC-only mode:

```powershell
.\run.ps1 -Properties "-Doutput.mode=osc -Dosc.debug.logging=true"
```

Select the `gravity-orbit` OSC stream in the browser. Use browser instrument `touch-walker`, `tilt-walker`, or `motion-orchestra` to move your own orbiter target. Use buttons, Space, double-tap, audio clap, or shake instruments to pulse your own orbiter.

## Behavior Changes From `GravityOrbitSketch`

The original Java `GravityOrbitSketch` runs inside the server and has access to per-session browser state. Each browser user can have their own position, velocity, size, audio level, motion state, name, and cleanup lifecycle.

This standalone performer primarily receives the session-aware `orchestra-session-v1` contract:

- `/session/join` with `sessionId`, display name, and instrument id
- `/session/leave` with `sessionId`
- `/session/position` with `sessionId`, x, and y
- `/session/trigger` with `sessionId`, trigger kind, and count
- `/session/pitch` with `sessionId`, note name, MIDI note, frequency, and level

The sketch maintains one orbiter per `sessionId`. Each collaborator's touch or tilt updates that orbiter's target position. Trigger events pulse and impulse only that collaborator's object. Pitch events change that collaborator's color and show the detected note near the orbiter.

It also accepts the general aggregate contract as a fallback:

- `/input/position`
- `/input/trigger`
- `/input/pitch`

And it accepts the older walker compatibility contract:

- `/walker/signal/x`
- `/walker/signal/y`
- `/test/ping`
- `/event/ping`

Compared with the in-process Java sketch, the behavior changes to:

- The performer owns its own physics state instead of reading Java `SessionManager` directly.
- Session-aware OSC creates/removes collaborator orbiters.
- Position messages move each collaborator's target.
- Trigger messages create pulse/impulse behavior for that collaborator's object.
- Audio and motion do not arrive as raw data; they arrive only as server-generated position, trigger, and pitch events.
