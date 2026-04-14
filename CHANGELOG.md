# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- HTTPS launch scripts using `run-https.ps1` and `run-https.sh`.
- Runtime HTTPS overlay configuration in `config/application-https.yaml`, enabled with `-Dapp.config=...`.
- Browser disconnect and clean shutdown notifications, including a final `server-shutdown` WebSocket message.
- Audio gain control in the browser UI and sketch processing path.
- Additional sketch controls and effects for `Burst`, `Spin Color`, and `Scatter`.

### Changed
- Build packaging now produces an executable shaded jar for `java -jar`.
- Default `application.yaml` now starts local HTTP only; HTTPS is enabled through the overlay config instead of manual YAML edits.
- TLS keystore generation now writes `keystore.p12` to the project root so HTTPS certificates can be regenerated without rebuilding.
- Run scripts now discover the newest packaged jar automatically.
- Documentation was updated to reflect the HTTP/HTTPS launch split, external keystore workflow, and current UI behavior.

### Fixed
- WebSocket handling now uses a dedicated handler per connection.
- WebSocket routing is available on both the default HTTP listener and the optional TLS listener.
- Session, control, and audio interactions now work correctly over HTTPS.

## [0.1.0] - 2026-04-10

### Added
- Multi-user WebSocket server with Helidon 4.4
- Browser-based UI with touch area, sliders, and buttons
- Binary WebSocket audio streaming (PCM 16-bit, 44.1kHz stereo)
- Per-user audio-reactive visualization with Processing
- HTTPS/TLS support with self-signed CA certificate generation
- Session management with unique IDs
- Configurable debug logging
- Cross-platform scripts (PowerShell and Bash)
- Comprehensive documentation (README, ARCHITECTURE, CUSTOMIZATION)

### Security
- Keystore generation scripts (not included in repo)
- Self-signed CA approach for development/testing
