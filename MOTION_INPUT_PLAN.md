# Motion And Position Input Evaluation

## Summary

Two candidate new input streams were considered:
- phone motion data from accelerometer and orientation sensors
- phone position data

Recommendation:
- add motion and tilt input
- do not pursue browser geolocation for small-room position control

The motion path is practical with built-in browser APIs and only a modest amount of JavaScript. The position path is not useful for room-scale control in this project.

## Evaluation

### 1. Phone Motion / Accelerometer Input

This is a good fit for the project.

Why:
- modern mobile browsers expose motion and orientation events without needing a large custom library
- the data can be sent over the existing WebSocket channel as another JSON event type
- it fits the current interaction model, where each phone influences a shared visual sketch in real time

Best browser APIs to consider:
- `DeviceOrientationEvent`
- `DeviceMotionEvent`

Practical recommendation:
- start with orientation and tilt first
- consider raw acceleration later for shake or burst-style effects

Why orientation first:
- tilt is easier for users to understand and control
- it tends to map more directly to visual parameters
- it avoids some of the noise and interpretation complexity of raw acceleration

Notes:
- motion and orientation access typically requires HTTPS for remote/mobile usage
- some browsers require explicit permission from a user gesture before motion events are delivered
- this still appears to be manageable with a small amount of browser code

### 2. Position Data

This is not a good fit if the goal is meaningful position within a small room.

Possible browser path:
- `navigator.geolocation.watchPosition()`

Why it is not recommended:
- browser geolocation accuracy is measured in meters, not room-scale precision
- that is generally too coarse to distinguish where someone is standing within a classroom, rehearsal space, or small studio
- accelerometer-only dead reckoning would drift too quickly to be useful
- true indoor positioning would require a different class of solution such as BLE, UWB, AR, or computer vision

Conclusion:
- browser geolocation may be interesting for broad outdoor or campus-scale experiences
- it is not useful here as a reliable “where is this phone in the room” input stream

## Recommended Product Direction

Add one new optional motion stream over the existing WebSocket connection.

Suggested progression:
1. Add phone tilt and orientation input.
2. Map tilt to simple visual behaviors.
3. Add optional motion/acceleration later for shake-like triggers.
4. Do not add browser geolocation for indoor positioning.

## Suggested Data Model

Add a new WebSocket JSON message type:

```json
{
  "type": "motion",
  "controlId": "deviceMotion",
  "beta": 12.5,
  "gamma": -8.2,
  "alpha": 180.0,
  "timestamp": 1710000000000
}
```

Possible later extension for acceleration:

```json
{
  "type": "motion",
  "controlId": "deviceMotion",
  "ax": 0.12,
  "ay": 0.44,
  "az": 0.98,
  "magnitude": 1.08,
  "timestamp": 1710000000000
}
```

## Suggested Initial Mappings In The Sketch

Simple first-pass mappings:
- `gamma` left/right tilt -> horizontal drift or hue shift
- `beta` forward/back tilt -> vertical drift or audio-ring thickness
- motion magnitude -> burst intensity or transient pulse
- shake gesture -> trigger a stronger short-lived effect

The goal should be to make the new stream obviously expressive without overloading the UI or the Processing sketch with too many controls at once.

## Work List

- [ ] Add a browser UI control such as `Enable Motion`.
- [ ] Add browser-side permission handling for motion/orientation access.
- [ ] Detect whether the client supports `DeviceOrientationEvent`.
- [ ] If needed, detect whether explicit permission is required and request it from a user gesture.
- [ ] Add a small browser module that listens for `deviceorientation` events.
- [ ] Normalize or clamp incoming orientation values into a predictable range for the server.
- [ ] Throttle outgoing motion messages to a reasonable frequency such as 15 to 30 Hz.
- [ ] Send motion data over the existing WebSocket as JSON.
- [ ] Extend the server-side event model to represent motion input.
- [ ] Update `WebSocketHandler.java` to route `type: "motion"` messages.
- [ ] Update `ProcessingSketch.java` to store per-session motion state.
- [ ] Map tilt to one or two initial visual behaviors in the sketch.
- [ ] Verify that motion works over HTTPS on actual phone clients.
- [ ] Add a fallback or status message when motion permissions are denied or unsupported.
- [ ] Document the new motion input path in `README.md` and `ARCHITECTURE.md`.

## Explicit Non-Goals For Now

- [ ] Do not add browser geolocation for indoor room position control.
- [ ] Do not add Generic Sensor `Accelerometer` API support unless there is a clear compatibility advantage later.
- [ ] Do not build a large custom JavaScript sensor abstraction layer.
- [ ] Do not attempt indoor positioning via BLE, UWB, AR, or computer vision in this phase.

## Notes For Future Reconsideration

Position data could be reconsidered later only if the project moves toward:
- larger outdoor experiences
- installation work with dedicated infrastructure
- explicit indoor positioning hardware or AR support

For the current project, motion and tilt are the correct next step.
