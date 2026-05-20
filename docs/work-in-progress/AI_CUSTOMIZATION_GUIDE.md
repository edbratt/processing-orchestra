# AI Customization Guide

This guide is for students using AI tools such as ChatGPT, Copilot, Cursor, or Claude to modify this project.

## Goal

Use AI to make focused changes without accidentally breaking the runtime structure of the app.

## Safe Edit Zones

Use these as the default boundaries:

- Visual behavior only:
  edit `src/main/java/com/processing/server/ProcessingSketch.java`
- Add or change browser controls:
  edit `src/main/resources/static/index.html` and `ProcessingSketch.java`
- Add a new event type:
  edit `index.html`, `WebSocketHandler.java`, `UserInputEvent.java`, and `ProcessingSketch.java`
- Change how audio affects visuals:
  usually edit `ProcessingSketch.java`
- Change raw incoming audio handling:
  edit `WebSocketHandler.java` or `AudioBuffer.java`
- Add diagnostics or status endpoints:
  edit `InputService.java`

## Runtime Contracts To Preserve

Tell the AI to preserve these unless your task is specifically about changing them:

- Keep the event flow:
  browser -> `WebSocketHandler` -> `EventQueue` -> `ProcessingSketch`
- Keep the audio flow:
  browser -> `WebSocketHandler` -> `AudioBuffer` -> `ProcessingSketch`
- Keep per-user state keyed by `sessionId`
- Keep `runSketch()` intact
- Keep the main `draw()` loop intact
- Do not change WebSocket message shapes unless adding a new protocol feature on purpose

## Good Prompt Shape

Use prompts with five parts:

1. Goal
   "Change tilt so it affects hue instead of position."
2. Edit scope
   "Only edit `ProcessingSketch.java`."
3. Preserve
   "Keep touch, audio, and per-session behavior working as they are."
4. Hook point
   "Make the change in `handleMotionEvent(...)` and `drawUsers()`."
5. Output style
   "Make the smallest patch and explain it briefly."

## Prompt Templates

### Visual change only

```text
Modify only `ProcessingSketch.java`.
Goal: make audio control ring thickness instead of size.
Preserve the current event flow, audio flow, and per-session behavior.
Prefer changing `processAudio()` and `drawUsers()`.
Show a small patch and explain the change briefly.
```

### Add a browser control

```text
Add one new browser slider called `opacitySlider`.
Edit only `src/main/resources/static/index.html` and `ProcessingSketch.java`.
Preserve the existing controls and WebSocket message format.
Handle the new slider in `handleEvent(...)` and use it in `drawUsers()`.
```

### Add a new event type

```text
Add a new event type called `pulse`.
Show the required changes in:
- `index.html`
- `WebSocketHandler.java`
- `UserInputEvent.java`
- `ProcessingSketch.java`
Preserve the existing touch, motion, and audio behavior.
```

### Ask for review

```text
Review this change for:
1. bugs
2. behavior regressions
3. performance problems
4. whether it preserves the EventQueue and AudioBuffer architecture
5. whether per-session behavior still works correctly
```

## What To Paste Into The AI

Do not start by pasting the whole repo.

Usually paste:

- the one file you want changed
- the one method you want changed
- a short description of the desired behavior

Especially useful methods to paste:

- `handleEvent(...)`
- `handleMotionEvent(...)`
- `processAudio()`
- `calculateAudioLevel(...)`
- `drawUsers()`

For browser work, paste:

- the relevant control markup
- the WebSocket send logic

## Edit Map By Task

- Change how circles look:
  `ProcessingSketch.java`
- Change how audio is interpreted visually:
  `ProcessingSketch.java`
- Change how motion is interpreted visually:
  `ProcessingSketch.java`
- Add a new UI widget:
  `index.html`, then `ProcessingSketch.java`
- Change incoming JSON handling:
  `WebSocketHandler.java`
- Change per-session audio buffering:
  `AudioBuffer.java`
- Add a status/debug API:
  `InputService.java`

## Common Failure Modes To Warn The AI About

Tell the AI not to:

- rewrite the whole sketch when a local change is enough
- remove `sessionId`-based behavior
- bypass `EventQueue` for control input
- bypass `AudioBuffer` for audio input
- change multiple unrelated files without a reason

## Short Student Advice

If the change is artistic, start in `ProcessingSketch.java`.

If the change is about what data arrives, start in `index.html` or `WebSocketHandler.java`.

If the AI proposes large architecture changes for a small behavior tweak, stop and ask for a smaller patch.
