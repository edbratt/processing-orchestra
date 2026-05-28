# OSC Event Reference

This page is the compact contract reference for v2 OSC performer sketches. Use it when writing a new Processing receiver or any other OSC listener that reacts to the current server output.

The contract is capability-based, not sketch-based. A performer may use any subset of the messages below and ignore the rest.

## Browser Event Classes

The web server currently turns these browser-side input classes into session state and OSC output:

- `touch`
  - surface interaction from mouse, touch, or pointer input
  - normalized browser range is `[-1..1]` for both axes on the client side
  - server-side legacy consumers still normalize into `0..1` where needed
- `button`
  - named actions such as `action1`, `action2`, `action3`, `surfaceDoubleTap`
- `key`
  - browser keyboard input such as `space`
- `slider`
  - control-panel values such as motion trim, motion offset, and shake threshold
- `motion`
  - device orientation and acceleration values such as `beta`, `gamma`, `ax`, `ay`, `az`, `magnitude`
- `audio`
  - per-session PCM audio chunks are consumed on the server for clap and pitch analysis

## General OSC Contracts

### `walker-trigger`

Lee compatibility stream for sketches that expect a walker signal and ping pulse.

- `/walker/signal/x` `float` in `[-1..1]`
- `/walker/signal/y` `float` in `[-1..1]`
- `/test/ping` `string kind`, `int count`

Typical use:
- `spiral_walker`
- simple Lee-style display sketches that only need a single walker and ping

### `walker-monitor`

Lee vectorscope compatibility stream.

- `/walker/signal/x` `float` in `[-1..1]`
- `/walker/signal/y` `float` in `[-1..1]`
- `/event/ping` `int count`

Typical use:
- `vectorscope`
- receiver sketches that only need a shared ping counter and walker coordinates

### `orchestra-input-v1`

General v2 performer stream for sketches that do not need per-session object identity.

- `/input/position` `float x`, `float y` in `[-1..1]`
- `/input/trigger` `string kind`, `int count`
- `/input/pitch` `string note`, `int midiNote`, `float frequencyHz`, `float level`

Typical use:
- shared-canvas performers
- sketches that react to the combined performance rather than per-user bodies

### `orchestra-session-v1`

Session-aware v2 performer stream for sketches that want one visual object per browser session.

- `/session/join` `string sessionId`, `string name`, `string instrumentId`
- `/session/leave` `string sessionId`
- `/session/position` `string sessionId`, `float x`, `float y` in `[-1..1]`
- `/session/trigger` `string sessionId`, `string kind`, `int count`
- `/session/pitch` `string sessionId`, `string note`, `int midiNote`, `float frequencyHz`, `float level`

Typical use:
- `gravity_orbit_v2`
- collaborative sketches where each participant should own one object

## Control IDs And Trigger Kinds

Current browser control names that can appear in OSC trigger or motion-related flows:

- `surfaceDoubleTap`
- `action1`
- `action2`
- `action3`
- `space`
- `clap`
- `shake`

Current motion control IDs:

- `motionXTrim`
- `motionYTrim`
- `motionXOffset`
- `motionYOffset`
- `motionShakeThreshold`

## Pitch Values

Pitch messages currently carry:

- note name with octave, for example `C4`, `A4`, `F#3`
- MIDI note number
- frequency in Hz
- signal level

Notes are emitted from the detected frequency and are intended to be stable enough for monophonic live instruments and voice.

## Notes For Performer Authors

- Ignore any OSC address you do not need.
- Treat unknown trigger kinds as generic pulses unless your sketch has a special behavior for them.
- `sessionId` is the stable key for per-user object mapping in session-aware performers.
- The current server does not require a performer to understand every contract at once. A sketch can listen to only `walker-trigger`, only `orchestra-input-v1`, or only `orchestra-session-v1`.

## Example Receiver Logic

```text
if address == "/session/join":
    create object for sessionId
if address == "/session/position":
    update object position
if address == "/session/trigger":
    pulse object
if address == "/session/pitch":
    color and label object by note
```

For a shared-canvas sketch, replace the session object map with a single shared target and ignore `sessionId`.
