# Getting Started for Artists

This guide is for people who want to run the project, try the included sketches, and work with browser-controlled multi-user visuals without needing to understand much Java first.

## What You Need

- Java JDK 21 or newer
- Maven 3.9 or newer
- Git

## 1. Get the Project

```powershell
cd C:\Users\yourname\Dev
git clone <repository-url> processing-server
cd processing-server
```

## 2. Run It Locally

```powershell
.\run.ps1
```

Then open:

- `http://localhost:8080`

This local HTTP mode is the fastest way to confirm the app is working on your machine.

## 3. Try the Included Sketches

Run a different sketch by passing `processing.sketch-class`:

```powershell
.\run.ps1 -Properties "-Dprocessing.sketch-class=com.processing.server.StarterSketch"
.\run.ps1 -Properties "-Dprocessing.sketch-class=com.processing.server.GravityOrbitSketch"
.\run.ps1 -Properties "-Dprocessing.sketch-class=com.processing.server.AbandonedArt96"
```

See the full list in [Samples Index](../sketches/samples-index.md).

## 4. Use More Than One Client

- Open multiple browser tabs on the same machine, or
- connect phones or tablets over your local network

For same-machine testing, plain HTTP is fine.

## 5. Enable Phones, Motion, and Microphone Over LAN

For phones and remote devices on your local network, use HTTPS:

```powershell
.\create-keystore.ps1
.\run-https.ps1
```

Then open the HTTPS URL shown by the script or use:

- `https://<your-ip>:8443`
- `https://<your-hostname>.local:8443`

HTTPS is important for:

- microphone access
- phone motion sensors
- reliable mobile browser behavior

## 6. Common First Experiments

- move users with touch or drag
- try multiple clients at once
- enable phone motion on a mobile browser
- switch between `ProcessingSketch`, `GravityOrbitSketch`, and the AbandonedArt adaptations

## Troubleshooting

### Windows Application Control and Processing DPI Helper

On Windows, Processing may look for a native display helper named `fenster.exe` while opening the sketch window. The PowerShell launch scripts set `java.library.path` to `C:\Windows\System32` by default so Processing does not accidentally find a blocked helper from another tool directory such as Chocolatey.

If you intentionally need a different native library path, pass it explicitly:

```powershell
.\run.ps1 -Properties "-Djava.library.path=C:\Your\Native\Path -Dprocessing.sketch-class=com.processing.server.StarterSketch"
```

## Where To Go Next

- If you want to understand the sketches and conversion workflow, read [Sketches and Conversion](../sketches/README.md).
- If you want to edit Java code, read [Java Coding Guide](../java-coding/README.md).
