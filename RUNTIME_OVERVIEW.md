# Runtime Overview

This document explains how the application is put together at runtime, using the actual source files in this repository as the guide. It is written for readers who are new to Java or object-oriented programming and want enough context to understand what they are seeing in the code.

## Big Picture

At runtime, this application is two things working together:

1. a Helidon web server
2. a Processing sketch running on the same machine

The browser client is the input side. It collects touch, slider, button, audio, and motion input from users.

The Java server is the middle layer. It receives those inputs, sorts them into the right shared structures, and keeps track of which browser session each input came from.

The Processing sketch is the output side. It reads the shared state and draws the final visual result on the server machine.

If you want the shortest possible description, it is this:

- `index.html` collects input
- Helidon receives it
- helper classes store it
- `ProcessingSketch.java` turns it into visuals

## Where the Program Starts

The application starts in:

- [Main.java](/C:/Users/ed/dev/processing-server/src/main/java/com/processing/server/Main.java)

`Main.java` is the organizer for the whole runtime. Its main jobs are:

- load configuration
- create the shared helper objects
- start the Processing sketch
- start the Helidon server

When you read `Main.java`, think of it as the place where the main runtime pieces are created and connected together.

The important objects created there are:

- `SessionManager`
- `EventQueue`
- `AudioBuffer`
- `ProcessingSketch`
- `WebSocketHandler`
- `InputService`

These are not just random classes. They are the core runtime building blocks of the app.

## The Runtime Pieces and Their Jobs

### `SessionManager.java`

File:

- [SessionManager.java](/C:/Users/ed/dev/processing-server/src/main/java/com/processing/server/SessionManager.java)

What it does:

- keeps track of connected browser sessions
- gives each client a stable identity
- removes session data when a client disconnects

Why it matters:

The sketch needs to know which circle or audio stream belongs to which browser client. `SessionManager` makes that possible.

Good beginner question:

- "How does the server tell one browser user from another?"

The answer starts here.

### `EventQueue.java`

File:

- [EventQueue.java](/C:/Users/ed/dev/processing-server/src/main/java/com/processing/server/EventQueue.java)

What it does:

- stores control events until the sketch is ready to process them

Examples of events:

- touch events
- slider changes
- button presses
- phone motion events

Why it matters:

The browser can send messages at any time. The sketch should not try to redraw itself in the middle of receiving those messages. Instead, events go into `EventQueue`, and the sketch reads them later during its normal frame loop.

Good beginner idea:

Think of `EventQueue` as the sketch's inbox.

### `AudioBuffer.java`

File:

- [AudioBuffer.java](/C:/Users/ed/dev/processing-server/src/main/java/com/processing/server/AudioBuffer.java)

What it does:

- stores incoming audio chunks for each connected session

Why it matters:

Audio is different from small control messages. It arrives as a continuous stream of binary data. `AudioBuffer` gives the sketch a place to read that audio from when it is ready.

Good beginner idea:

If `EventQueue` is the control-message inbox, `AudioBuffer` is the audio inbox.

### `WebSocketHandler.java`

File:

- [WebSocketHandler.java](/C:/Users/ed/dev/processing-server/src/main/java/com/processing/server/WebSocketHandler.java)

What it does:

- receives the real-time WebSocket connection from the browser
- handles JSON control messages
- handles binary audio frames
- pushes control data into `EventQueue`
- pushes audio data into `AudioBuffer`

Why it matters:

This is the class where real-time browser input first enters the Java side of the system.

Good beginner question:

- "Where do browser messages arrive?"

The answer is mostly here.

### `InputService.java`

File:

- [InputService.java](/C:/Users/ed/dev/processing-server/src/main/java/com/processing/server/InputService.java)

What it does:

- handles normal HTTP API requests
- serves status-style endpoints such as `/api/status`

Why it matters:

Not everything needs a live WebSocket. Some things are better handled as normal request/response HTTP endpoints. This class is the simpler, non-real-time side of the server.

### `ProcessingSketch.java`

File:

- [ProcessingSketch.java](/C:/Users/ed/dev/processing-server/src/main/java/com/processing/server/ProcessingSketch.java)

What it does:

- runs the Processing drawing window
- reads events from `EventQueue`
- reads audio from `AudioBuffer`
- keeps track of per-user state
- updates animations
- draws the final frame

Why it matters:

This is the visual heart of the application. If you want to know how the circles move, how audio affects the drawing, or how motion becomes a burst effect, this is the main file to study.

### `index.html`

File:

- [index.html](/C:/Users/ed/dev/processing-server/src/main/resources/static/index.html)

What it does:

- defines the browser UI
- opens the WebSocket connection
- watches touch, sliders, buttons, motion sensors, and microphone input
- sends those inputs to the server

Why it matters:

The browser page is not the main visualization. It is the remote control panel and sensor/input source.

## How Initialization Works

At startup, the order is roughly:

1. `Main.java` loads configuration.
2. `Main.java` creates the shared helper objects.
3. `Main.java` creates the `ProcessingSketch`.
4. `Main.java` starts the Processing sketch.
5. `Main.java` configures and starts Helidon.
6. Helidon begins serving the browser UI and listening for API and WebSocket traffic.

The important idea is that the sketch and the server live in the same Java process, but they have different jobs.

- Helidon handles incoming network traffic.
- Processing handles the drawing loop.

## How the Browser Connects

The browser files come from:

- [src/main/resources/static](/C:/Users/ed/dev/processing-server/src/main/resources/static)

When a user opens the page:

1. Helidon serves `index.html` and the rest of the static browser assets.
2. The JavaScript in `index.html` starts running.
3. The browser opens a WebSocket connection to `/ws`.
4. The server associates that connection with a session.

That session tracking is important because multiple people may be connected at once. The app needs to know which input belongs to which person.

## The Different Runtime Channels

The application uses three main communication paths.

### 1. Static Content

Purpose:

- send the browser page and front-end files to the client

Examples:

- `index.html`
- JavaScript
- CSS

Handled by:

- Helidon static content configuration in `Main.java`

### 2. REST API

Purpose:

- handle request/response endpoints such as status inspection

Example:

- `/api/status`

Handled by:

- `InputService.java`

### 3. WebSocket

Purpose:

- carry live real-time browser input

Endpoint:

- `/ws`

Handled by:

- `WebSocketHandler.java`

Important detail:

There is one WebSocket connection, but it carries two logical streams:

- JSON control messages
- binary audio data

That means one live channel is doing two kinds of jobs at once.

## How JSON Control Events Flow

Examples of JSON control events:

- touch movement
- slider changes
- button presses
- motion data from a phone

The path looks like this:

1. `index.html` detects input in the browser.
2. It sends a JSON message over `/ws`.
3. `WebSocketHandler.java` receives the message.
4. `WebSocketHandler.java` parses it into a `UserInputEvent`.
5. The event is pushed into `EventQueue.java`.
6. `ProcessingSketch.java` reads queued events during `draw()`.
7. The sketch updates the correct user's state.
8. The next frame reflects the change.

This is one of the most important ideas in the project.

The sketch does not directly react inside the WebSocket handler. Instead:

- network code receives data first
- queue code stores it
- sketch code applies it later during drawing

That separation keeps the program easier to reason about.

## How Audio Flows

Audio is different from JSON control input.

The path looks like this:

1. `index.html` captures microphone data in the browser.
2. The browser converts that data into binary audio frames.
3. Those binary frames are sent over `/ws`.
4. `WebSocketHandler.java` receives the binary frames.
5. The audio bytes are stored in `AudioBuffer.java`.
6. `ProcessingSketch.java` reads audio data from `AudioBuffer`.
7. The sketch calculates per-user audio levels.
8. Those levels affect the visuals.

So the runtime split is:

- control events go to `EventQueue`
- audio goes to `AudioBuffer`

This is a useful thing to notice in the code. The app intentionally uses different storage paths for different kinds of incoming data.

## How Motion Flows

Motion input from phones is treated more like control input than like audio.

The path looks like this:

1. `index.html` reads phone motion and orientation data.
2. The browser combines the latest sensor values into one `motion` event.
3. That event is sent as JSON over `/ws`.
4. `WebSocketHandler.java` receives it and clamps values to safe limits.
5. The event is pushed into `EventQueue.java`.
6. `ProcessingSketch.java` handles it during its normal event-processing step.
7. The sketch applies tilt and shake behavior to the visual state.

In the current sketch, this is used for things like:

- tilt changing the rendered position offset
- shake creating a burst-like effect

## How the Sketch Uses the Shared Data

`ProcessingSketch.java` is where the stored data becomes visible output.

During each frame, the sketch roughly does this:

1. read waiting control events from `EventQueue`
2. update per-user state from those events
3. read waiting audio chunks from `AudioBuffer`
4. calculate audio levels and smoothing values
5. update animation values
6. draw the current frame

This means the sketch is always reading from the shared helper objects that were created in `Main.java`.

The sketch does not own the network connections directly. Instead, it depends on:

- `EventQueue` for control events
- `AudioBuffer` for audio
- session-linked user state that is updated using session IDs

## How the Classes Are Hooked Together

Here is the runtime connection map in plain English.

### `Main.java` creates and wires the shared objects

`Main.java` is where:

- `SessionManager` is created
- `EventQueue` is created
- `AudioBuffer` is created
- `ProcessingSketch` is created
- `WebSocketHandler` is created with access to the shared runtime state
- `InputService` is created for HTTP endpoints

This is the "wiring" step.

### `WebSocketHandler.java` feeds the shared buffers

`WebSocketHandler.java` uses the shared runtime objects to sort incoming browser data:

- JSON control messages are turned into `UserInputEvent` objects and stored in `EventQueue`
- binary audio is stored in `AudioBuffer`
- session IDs are used so each browser client stays separate

### `InputService.java` exposes read-only or tool-style server information

`InputService.java` is connected into Helidon's routing so the browser or a developer can ask for information like status data.

### `ProcessingSketch.java` consumes the shared state

`ProcessingSketch.java` is the consumer of the shared runtime data.

It reads:

- events from `EventQueue`
- audio from `AudioBuffer`
- per-user identity/state keyed by session

Then it draws the final visual result.

## A Simple Way to Read the Source

If you are new to the codebase, read it in this order:

1. [Main.java](/C:/Users/ed/dev/processing-server/src/main/java/com/processing/server/Main.java)
   This shows how the application starts and how the major objects are connected.
2. [WebSocketHandler.java](/C:/Users/ed/dev/processing-server/src/main/java/com/processing/server/WebSocketHandler.java)
   This shows how live browser messages enter the Java side.
3. [SessionManager.java](/C:/Users/ed/dev/processing-server/src/main/java/com/processing/server/SessionManager.java)
   This shows how multiple users are kept separate.
4. [EventQueue.java](/C:/Users/ed/dev/processing-server/src/main/java/com/processing/server/EventQueue.java)
   This shows how control events wait for the sketch.
5. [AudioBuffer.java](/C:/Users/ed/dev/processing-server/src/main/java/com/processing/server/AudioBuffer.java)
   This shows how audio waits for the sketch.
6. [ProcessingSketch.java](/C:/Users/ed/dev/processing-server/src/main/java/com/processing/server/ProcessingSketch.java)
   This shows how the visual result is produced.
7. [index.html](/C:/Users/ed/dev/processing-server/src/main/resources/static/index.html)
   This shows what the browser is sending in the first place.

That reading order helps because it goes from system setup to input handling to final rendering.

## What a Beginner Should Notice in the Code

When reading the source, try to answer these questions:

1. Where does the application start?
   Look at `Main.java`.
2. Where does a browser connection get handled?
   Look at `WebSocketHandler.java`.
3. Where are users separated by session?
   Look at `SessionManager.java`.
4. Where do touch, slider, button, and motion events wait?
   Look at `EventQueue.java`.
5. Where does audio wait?
   Look at `AudioBuffer.java`.
6. Where does the actual drawing happen?
   Look at `ProcessingSketch.java`.
7. Where does the browser decide what to send?
   Look at `index.html`.

If you can answer those questions, you understand the runtime structure well enough to begin exploring the details.

## Teaching Summary

At runtime, the application works like this:

- `Main.java` starts everything and wires the main objects together
- Helidon serves the web page and accepts HTTP and WebSocket traffic
- `index.html` sends touch, sliders, buttons, motion, and audio from the browser
- `WebSocketHandler.java` sorts real-time incoming data into the right shared structures
- `EventQueue.java` stores control events
- `AudioBuffer.java` stores audio data
- `SessionManager.java` keeps users separate
- `ProcessingSketch.java` reads that shared data and draws the final result

That is the core runtime story of the project.
