# TODO

Future improvements and known limitations for the Processing Server project.

## Performance Testing

### Estimates

Some estimates can be made from architecture, but real testing would give definitive answers.

**Key limiting factors:**

| Factor | Constraint | Rough Estimate |
|--------|-----------|----------------|
| **Audio bandwidth** | 44.1kHz stereo 16-bit ≈ 176 KB/s per user | ~50 users = 8.8 MB/s (manageable on gigabit) |
| **WebSocket connections** | OS file descriptor limit (~1024 default) | ~1000 theoretical max |
| **Processing render** | 60 FPS × users (linear growth) | Depends on GPU/CPU |
| **Audio buffer queue** | Configurable (default 100 chunks) | Memory bound |

**Bottleneck analysis:**

1. **Network I/O** - Each user sends ~1.4 Mbps audio. 10 users = 14 Mbps inbound (fine for most networks). 100 users = 140 Mbps (needs good network).

2. **Memory** - Each audio buffer can queue up. Default config limits to 100 chunks. At 4KB/chunk stereo, that's ~400KB per user buffer.

3. **Processing loop** - `draw()` runs 60 FPS, processing all users. Linear complexity O(n). At 100+ users, frame drops may occur.

4. **WebSocket threads** - Helidon uses NIO, so thread-per-connection isn't the bottleneck.

**Rough estimates:**
- **Casual use (intermittent audio):** 50-100 users should work
- **All streaming audio continuously:** 20-30 users before CPU/network strain
- **Single powerful machine:** Maybe 50-75 with continuous streaming

### Testing Tasks

- [ ] Create load testing script with k6 or Artillery
- [ ] Simulate multiple WebSocket clients with audio streaming
- [ ] Monitor CPU, memory, network, and Processing frame rate
- [ ] Document actual user limits under various conditions
- [ ] Test on different hardware configurations

---

## Keystore Follow-Up

The keystore scripts now generate `keystore.p12` at the project root, export `processing-server-ca.cer`, and include `hostname.local` plus the current LAN IP in the SAN list.

Remaining follow-up:
- [ ] Test hostname-based SAN on Windows 10/11
- [ ] Test hostname-based SAN on macOS (mDNS/Bonjour)
- [ ] Test hostname-based SAN on Linux with avahi
- [ ] Document edge cases such as corporate networks blocking mDNS

---

## Feature Improvements

### Audio Enhancements

- [ ] Add configurable sample rate/channel options in application.yaml
- [ ] Implement audio compression (Opus codec) to reduce bandwidth
- [x] Add client-side audio level meter in the UI
- [ ] Support client muting/unmuting from server

### Video

 - [ ] Add video stream as an input to Processing sketch.

### Client enhancements

 - [x] Add accelerometer data (phone/tablet) as input to Processing sketch.

### Visualization Enhancements

- [ ] Make Processing sketch customizable via configuration
- [ ] Add user presence indicators (join/leave animations)
- [ ] Remove sketch-side visualization/state for sessions after their WebSocket connection closes
- [ ] Support multiple visualization modes
- [ ] Add server-side audio mixing option (currently per-user only)

### Protocol Improvements

- [ ] Add heartbeat/ping-pong for connection health
- [ ] Implement reconnection logic for dropped clients
- [ ] Add message acknowledgment for critical events
- [ ] Support binary protocol for lower overhead (currently JSON for control events)

### Security

- [ ] Add optional password/authentication for sessions
- [ ] Implement rate limiting on WebSocket messages
- [ ] Add CORS configuration for browser security
- [ ] Document how to use real certificates (Let's Encrypt, etc.)

---

## Documentation

- [ ] Add video demo/screenshot to README
- [ ] Create troubleshooting guide for common errors
- [ ] Document how to deploy on cloud (AWS, GCP, Azure)
- [ ] Add internationalization notes for non-English users

---

## Code Quality

- [ ] Add unit tests for core classes
- [ ] Add integration tests for WebSocket protocol
- [ ] Implement proper logging framework (SLF4J)
- [ ] Add connection pool limits and timeout configurations
- [ ] Review thread safety in shared components
