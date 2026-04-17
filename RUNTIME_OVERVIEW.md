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

There is now also a small local operator layer inside the Processing-side runtime. That layer handles mouse and keyboard input in the Processing window itself without going through the browser or the WebSocket path.

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
- `LocalOperatorLayer`

At runtime, `Main.java` can also start a different sketch class if `processing.sketch-class` is set. That means the runtime wiring stays the same even when the visual sketch implementation changes.

These are not just random classes. They are the core runtime building blocks of the app.

A few Java words appear often in this explanation:

- `object`
  a specific thing created while the program is running, such as one `WebSocketHandler` for one browser connection
- `map`
  a container that stores data as key-value pairs, like a labeled set of boxes where one label points to one value
- `queue`
  a container that holds items in waiting order, like a line of people waiting their turn
- `record`
  a compact Java way to store a small bundle of related data values together

## The Runtime Pieces and Their Jobs

### `SessionManager.java`

File:

- [SessionManager.java](/C:/Users/ed/dev/processing-server/src/main/java/com/processing/server/SessionManager.java)

What it does:

- keeps track of connected browser sessions
- gives each client a stable identity
- stores session metadata such as a browser-provided display name
- tracks when the session was last seen
- removes session data when a client disconnects or becomes stale

Why it matters:

The sketch needs to know which circle or audio stream belongs to which browser client. `SessionManager` makes that possible.

How it stores that:

`SessionManager` uses a `map`, which means a container storing key-value pairs. In this case:

- the key is the `sessionId`
- the value is a small `SessionInfo` record containing:
  - the same `sessionId`
  - the time the session was created
  - the saved display name
  - the last time the server heard from that session

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

How it stores that:

`AudioBuffer` also uses a `map`, but here each session ID points to its own audio `queue`, which is a waiting line of audio chunks for that one browser client.

Good beginner idea:

If `EventQueue` is the control-message inbox, `AudioBuffer` is the audio inbox.

### `WebSocketHandler.java`

File:

- [WebSocketHandler.java](/C:/Users/ed/dev/processing-server/src/main/java/com/processing/server/WebSocketHandler.java)

What it does:

- receives the real-time WebSocket connection from the browser
- handles JSON control messages
- handles binary audio frames
- handles browser heartbeats
- handles explicit browser close requests
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

### `LocalOperatorLayer.java`

File:

- [LocalOperatorLayer.java](/C:/Users/ed/dev/processing-server/src/main/java/com/processing/server/LocalOperatorLayer.java)

What it does:

- handles Processing-window mouse and keyboard input that should stay local to the machine running the sketch
- manages local selection state
- supports dragging a selected user, wheel-based parameter changes, and a local HUD
- keeps pause and slow-motion state for sketches that use it

Why it matters:

This layer separates two different ideas cleanly:

- browser client interaction that belongs to remote users
- local operator interaction that belongs to the person running the sketch window

That separation reduces confusion because local Processing-window shortcuts do not have to be mixed into the browser event protocol.

### `index.html`

File:

- [index.html](/C:/Users/ed/dev/processing-server/src/main/resources/static/index.html)

What it does:

- defines the browser UI
- opens the WebSocket connection
- watches touch, sliders, buttons, motion sensors, and microphone input
- sends those inputs to the server
- sends session metadata updates such as the saved display name
- sends periodic heartbeat messages while connected
- sends a best-effort session close notification on page teardown

Why it matters:

The browser page is not the main visualization. It is the remote control panel and sensor/input source.

## How Launch Scripts Affect Runtime

The common launch scripts are:

- `run.ps1`
- `run.sh`
- `run-https.ps1`
- `run-https.sh`

At runtime, those scripts now do two useful things before Java starts:

1. they can pass extra Java system properties such as `-Dprocessing.sketch-class=...`
2. they can auto-build the packaged jar when source files are newer than the current jar

That means the runtime entry point is still `Main.java`, but many users now reach it through a script that may first rebuild the jar and then pass launch-time properties into the Java process.

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

While the page stays open, the browser now also sends:

- `session-meta` messages when the user saves a display name
- `heartbeat` messages on a timer so the server can keep `lastSeenAt` fresh

When the page is leaving, the browser tries two cleanup paths:

- an explicit `session-close` WebSocket message
- a best-effort `DELETE /api/session/{id}` request

## How One Browser Client Is Represented At Runtime

One important idea for reading this code is that a browser client is not represented by one big `Client` object.

Instead, one browser client is represented in a distributed way across several runtime objects and data containers, mostly tied together by one shared `sessionId` string.

For one connected browser client, the runtime picture looks like this:

- one Helidon `WsSession`
  this is Helidon's object for the actual open WebSocket connection
- one `WebSocketHandler` object
  this handler belongs to that connection and stores the session ID for it
- one `SessionInfo` record inside `SessionManager`
  this is the server's official session registry entry
- zero or more `UserInputEvent` records in `EventQueue`
  each event carries that same `sessionId`
- zero or more queued audio chunks in `AudioBuffer`
  stored under that `sessionId`
- one bundle of sketch-side user state in `ProcessingSketch`
  spread across several maps, all keyed by that `sessionId`

So the main idea is:

- there is not one big client object
- there is one shared session ID
- that session ID connects the networking side, the event side, the audio side, and the sketch side

## What Happens When A New Session Is Created

The most important runtime path is in:

- [WebSocketHandler.java](/C:/Users/ed/dev/processing-server/src/main/java/com/processing/server/WebSocketHandler.java)

When the browser opens `/ws`, Helidon creates a new `WsSession` for that live socket connection.

At the same time, the application uses a new `WebSocketHandler` object for that connection. This is important because the app no longer reuses one shared mutable handler. Each connection gets its own handler instance.

Then, inside `WebSocketHandler.onOpen(...)`, this happens:

1. the `WsSession` is added to `OPEN_SESSIONS`
2. `SessionManager.createSession()` is called
3. `SessionManager` generates a new UUID session ID
4. `SessionManager` stores a new `SessionInfo` record in its internal map
5. the `WebSocketHandler` stores that session ID in its own `sessionId` field
6. the server sends a welcome JSON message back to the browser

That welcome message includes:

- `type: "session"`
- `sessionId`
- saved session name
- audio settings
- motion settings

So the browser learns, "this is my session ID," from the server as soon as the WebSocket opens.

## What Gets Created Immediately, And What Gets Created Later

This is a useful distinction for students.

### Created immediately when the WebSocket opens

- a Helidon `WsSession`
- a `WebSocketHandler` object for that connection
- a `SessionInfo` record in `SessionManager`

### Not created immediately

- sketch-side visual state in `ProcessingSketch`
- audio queues in `AudioBuffer`
- queued `UserInputEvent` records

Those later pieces only appear when the browser actually starts sending data that needs them.

So the session exists first, and the rest of the user state appears lazily, meaning only when it becomes necessary.

## How Control Messages Represent A Client

Touch, slider, button, and motion inputs all become `UserInputEvent` records.

A `UserInputEvent` record is a small bundle of data that carries things like:

- `sessionId`
- event type
- control ID
- values such as `x`, `y`, `value`, or motion fields
- timestamp

So the queue does not store a big client object. It stores many small event records, each tagged with the session they belong to.

This means the client is represented in control flow mostly as:

- a session ID attached to each event record

## How Audio Represents A Client

Audio is stored differently.

In `AudioBuffer`, the app keeps a map where:

- the key is the `sessionId`
- the value is that client's audio queue

That means each client's audio stream is represented by its own entry in `AudioBuffer`, keyed by session ID.

Again, there is no large audio-client object. It is:

- session ID
- mapped to queued audio chunks

## How The Sketch Represents A Client

The Processing sketch has the richest client state, but even there it is not stored as one user object.

Instead, `ProcessingSketch` uses several maps keyed by `sessionId`.

Examples include:

- `userPositions`
- `userTargetPositions`
- `userColors`
- `userAudioLevels`
- `userSizes`
- `userSpeeds`
- `userGains`
- `userPulseBoosts`
- `userHueVelocities`
- `userMotion`
- `userLastMotionMagnitudes`

So one browser client becomes a bundle of entries across many maps.

You can think of this as:

- one user
- many labeled boxes of state
- all tied together by the same session ID

## When The Sketch Creates A New User

The sketch does not create a user entry during `WebSocketHandler.onOpen(...)`.

Instead, it waits until that session first does something that matters to the sketch, such as:

- sending a touch event
- moving a slider
- pressing a button
- sending a motion event
- or contributing audio

At that point, `ProcessingSketch` checks whether it already has state for that session. If not, it calls `initializeUser(sessionId)`.

That method creates the sketch-side default values for the new user, including:

- starting position
- target position
- color
- default size
- default speed
- default gain
- default pulse boost
- default hue velocity
- default motion data
- default last-motion magnitude

So the visual user is created lazily, not at the instant the network session opens.

## What The Sketch Does Each Frame After A User Exists

Once the sketch has created state for a session, that user's state is revisited on every Processing frame inside `draw()`.

For the default `ProcessingSketch`, the frame loop is roughly:

1. clear the background
2. drain `EventQueue`
3. apply those events to per-session maps
4. poll `AudioBuffer`
5. update per-user audio levels and shared audio smoothing
6. update temporary animation values such as hue velocity and pulse decay
7. draw each user's circles and label
8. draw the shared audio meter

So if you want to know "what happens repeatedly after a user exists?", the answer is:

- their stored maps are read every frame
- their motion/audio/effect values are updated every frame
- their current visual representation is redrawn every frame

That is a helpful mental model:

- session creation is lazy
- once created, user state is part of the draw loop on every frame
- different sketch classes share the same overall runtime wiring but can change what happens inside that frame loop

Sketch-specific variations belong in the sketch-specific documents. For the gravity-orbit examples, see:

- [GRAVITY_ORBIT_SKETCH_TUTORIAL.md](/C:/users/ed/dev/processing-server/GRAVITY_ORBIT_SKETCH_TUTORIAL.md:1)

## What Happens When A Client Disconnects

When the socket closes, errors, or the browser explicitly announces shutdown:

1. the `WsSession` is removed from `OPEN_SESSIONS`
2. `SessionManager.removeSession(sessionId)` removes the session record
3. `AudioBuffer.clearSession(sessionId)` removes queued audio for that session
4. `WebSocketHandler` pushes a `session-ended` event into `EventQueue`
5. `ProcessingSketch` handles that event and removes the session's sketch-side state and visualization

That means disconnect cleanup now happens across all three layers:

- `SessionManager`
- `AudioBuffer`
- `ProcessingSketch`

There is now also a fallback path for missed disconnects:

1. the browser keeps sending heartbeat messages while the page is alive
2. `SessionManager` updates `lastSeenAt` whenever traffic arrives
3. a background reaper in `Main.java` checks for sessions that have been silent for too long
4. stale sessions are removed from `SessionManager`
5. audio is cleared
6. a `session-ended` event is queued so the sketch removes the orphaned circle

This matters because browser/network disconnects are not always clean. The runtime no longer depends only on the WebSocket close callback being observed immediately.

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
- heartbeats
- session metadata updates
- explicit session close messages

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

It is also useful to remember that this file is the default visual consumer, not the only possible one. If `processing.sketch-class` points at one of the alternate sketch classes, the same runtime data sources are consumed by that alternate sketch instead.

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

Some sketches now also consult `LocalOperatorLayer` during their draw cycle for:

- pause or slow-motion state
- whether names and overlays should be drawn
- which user is locally selected

## Audio Debug Logging

There is now a separate audio-debug configuration path in addition to the general debug flag.

The two ideas are:

- `debug.logging`
  enables broader application and session-lifecycle debug output
- `audio.debug.logging`
  enables audio-analysis traces used by sketches that inspect buffered PCM audio in more detail

This matters because you may want to inspect audio analysis without turning on all other debug messages.

The related settings live in `application.yaml` under:

- `audio.debug.logging`
- `audio.debug.sample-limit`

In the current gravity-based sample sketches, the audio-debug trace only prints when the analyzed buffer level is above the configured detection threshold. Quiet buffers that just preserve the last detected frequency do not produce a log line.

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

### `LocalOperatorLayer.java` stays on the Processing side

`LocalOperatorLayer.java` does not participate in networking.

Its job is to sit entirely on the sketch side and interpret:

- mouse clicks in the Processing window
- mouse dragging in the Processing window
- wheel gestures with modifier keys
- a small set of local operator keyboard shortcuts

This is useful to notice because these local controls do not create `UserInputEvent` records and are not sent through `EventQueue`.

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
   This shows how the default visual result is produced.
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

And now, in the newer runtime:

- the browser sends heartbeats and best-effort close notifications
- `SessionManager` tracks `lastSeenAt`
- `Main.java` reaps stale sessions

That is the core runtime story of the project.
