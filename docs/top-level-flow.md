# Top-Level Session Flow

This diagram is the markdown-friendly version of the Draw.io source at [../top-level flow.drawio](../top-level%20flow.drawio).

The `.drawio` file remains the primary editable source. This Mermaid version is used here because VS Code's Markdown preview can fail to render Draw.io SVG exports that contain embedded HTML labels.

```mermaid
flowchart TD
    User["Browser client instrumentalist"] --> Browser["Browser UI<br/>index.html"]

    Browser -->|"loads app"| Static["Static assets<br/>HTTP or HTTPS"]
    Browser -->|"status/config"| API["REST API<br/>/api/status<br/>/api/controller<br/>/api/orchestra"]
    Browser -->|"name + instrument + stream"| Selector["Instrument selection<br/>OSC stream assignment"]
    Browser -->|"JSON controls"| WS["WebSocket /ws"]
    Browser -->|"binary microphone audio"| WS

    subgraph Server["Processing Server"]
        Static --> InputService["InputService"]
        API --> InputService
        WS --> Handler["WebSocketHandler"]
        Selector --> Handler
        Handler --> Sessions["SessionManager<br/>name, instrument, stream"]
        Handler --> Queue["EventQueue<br/>touch, sliders, buttons, motion"]
        Handler --> Audio["AudioBuffer<br/>per-session PCM audio"]
    end

    subgraph Output["Runtime Output"]
        Queue --> Sketch["Processing sketch mode<br/>shared visual canvas"]
        Audio --> Sketch
        Sessions --> Sketch
        Queue --> Osc["OSC-only mode<br/>stream event pump<br/>per-session processors"]
        Audio --> Osc
        Sessions --> Osc
    end

    Sketch --> Visual["Graphic output"]
    Osc --> Performer["Processing graphical-performer sketch<br/>OSC receiver on configured port"]
```

The older SVG export is still available at [assets/top-level-flow.svg](assets/top-level-flow.svg) when a renderer supports it.
