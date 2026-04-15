# Handoff Notes

## Current State

The project is in a working state for both local HTTP use and optional LAN/mobile HTTPS use.

What is working:
- Local HTTP UI on `http://localhost:8080/`
- Optional HTTPS UI on `https://YOUR_IP:8443/` or `https://YOUR_HOSTNAME.local:8443/`
- Processing sketch startup from the normal interactive run scripts
- Browser controls driving the Processing sketch
- Browser microphone capture feeding the server-side audio-reactive visuals
- Clean WebSocket shutdown notifications in the browser

## Main Decisions Made

### Runtime Config Model

We moved away from asking users to manually uncomment YAML blocks.

Current approach:
- Base config lives in `src/main/resources/application.yaml`
- Optional HTTPS overlay lives in `config/application-https.yaml`
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
- scripts discover and run the packaged jar

Why:
- this app behaves more like a runnable desktop/server tool than a minimal framework example
- users should not have to manage `target/libs` or a custom classpath

### WebSocket Model

The app uses one shared `/ws` connection per browser client with two logical streams:
- JSON control events
- binary audio frames

Important fix made:
- `WebSocketHandler` must be created per connection, not reused as one mutable shared instance

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

- `Size` slider now changes the core circle size
- outer audio ring scales proportionally from that base size
- `Speed` slider now changes movement responsiveness and effect decay speed

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

### Disconnect and Shutdown UX

Added client notification behavior for disconnects and shutdowns:
- server sends a final shutdown message before clean close
- browser shows a useful banner
- page scrolls back to the top so the message is visible

## Documentation Changes

Docs were updated to match the new system:
- `README.md`
- `ARCHITECTURE.md`
- `CHANGELOG.md`

Current doc direction:
- describe only the current supported workflow
- avoid historical/migration notes unless they are still operationally relevant
- use Mermaid for architecture diagrams instead of fragile text box diagrams

## Architecture Doc Status

`ARCHITECTURE.md` was rebuilt into a clean Markdown-only version.

It now includes:
- Mermaid architecture diagrams
- explicit distinction between structural architecture and runtime data flow
- component walkthrough for main server classes
- current HTTP/HTTPS config model
- current packaging and runtime model

One follow-up item remains:
- some Mermaid nodes may still be visually cramped depending on the renderer
- likely fix is to insert more line breaks into long node labels if GitHub preview still truncates text

## Verified Behavior

Verified during this round:
- build succeeds with `mvn clean package -DskipTests`
- local HTTP works
- HTTPS overlay works
- browser UI loads on both ports
- controls affect the Processing sketch
- audio path drives the visual response
- run scripts and HTTPS scripts work with the current packaging/config setup

## Things To Watch

### Untracked Local Files

Current local-only files that are not in the repo:
- `note.md`
- `processing-server-ca.cer`

We also added ignore rules for:
- `.codex*`
- `.tmp-*/`

### Certificate/IP Reality

The certificate must match the actual LAN identity in use.

Most recent real LAN IP discussed:
- `192.168.50.95`

If the network changes:
- regenerate the keystore
- trust the CA again on new client devices if needed

## Suggested Next Session Starting Points

Good candidates for the next session:
1. Tidy Mermaid node labels if GitHub still truncates text.
2. Decide whether `processing-server-ca.cer` should also be ignored in `.gitignore`.
3. Do a final polish pass on README and Architecture side by side for consistency.
4. Consider whether to add release tagging/versioning now that the workflow is stable.

## Recommended Restart Context

If resuming later, the shortest accurate summary is:

This repo now runs locally on HTTP by default and optionally on HTTPS using `-Dapp.config=config/application-https.yaml`. The keystore is external at the project root, not bundled into the jar. The app is packaged as an executable shaded jar. The important bug fix was making WebSocket handlers per-connection and wiring `/ws` on both default and TLS sockets. The Processing sketch, browser controls, audio gain, and shutdown messaging are all working. The main remaining polish item is Mermaid diagram readability.
