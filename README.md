# Processing Server

[![CI](https://github.com/edbratt/processing-orchestra/actions/workflows/ci.yml/badge.svg)](https://github.com/edbratt/processing-orchestra/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Processing Server is a multi-user browser controller for Processing sketches running on the server machine. Multiple clients can connect at once and send touch, slider, button, keyboard, audio, and phone-motion input into a shared visual sketch.

## Quick Start

Requirements:

- Java 21 or newer
- Maven 3.9 or newer

Run locally:

```powershell
.\run.ps1
```

Then open:

- `http://localhost:8080`

Run a different included sketch:

```powershell
.\run.ps1 -Properties "-Dprocessing.sketch-class=com.processing.server.StarterSketch"
```

## Mobile and LAN Use

For phones, microphone input, or motion sensors from other devices on your network, use HTTPS:

```powershell
.\create-keystore.ps1
.\run-https.ps1
```

Then open the HTTPS address on the client device.

## Included Sketches

The included runtime sketches and converter trial samples are listed in [docs/sketches/samples-index.md](docs/sketches/samples-index.md).

Useful starting points:

- `com.processing.server.ProcessingSketch`
- `com.processing.server.StarterSketch`
- `com.processing.server.GravityOrbitSketch`
- `com.processing.server.GravityOrbitGradientSketch`
- `com.processing.server.AbandonedArt96`

## Documentation

Audience-specific guides:

- [Getting Started for Artists](docs/getting-started/artists.md)
- [Java Coding Guide](docs/java-coding/README.md)
- [Sketches and Conversion](docs/sketches/README.md)

Core references:

- [Documentation Index](docs/README.md)
- [Architecture](docs/architecture.md)
- [Top-Level Session Flow](docs/top-level-flow.md)
- [Customization Guide](docs/java-coding/customization-guide.md)
- [Runtime Overview](docs/java-coding/runtime-overview.md)

Work-in-progress notes:

- [docs/work-in-progress](docs/work-in-progress/README.md)

## Build and Test

```powershell
mvn test
```
