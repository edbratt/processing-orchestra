# V2 Single-Note Pitch Plan

## Goal

Add better pitch detection for monophonic live instruments and voice in v2 OSC mode.

The first target is a single dominant note per browser audio session. This should improve the current lightweight autocorrelation pitch estimate while keeping the OSC event model simple:

```text
/input/pitch      note, midiNote, frequencyHz, level
/session/pitch    sessionId, note, midiNote, frequencyHz, level
```

## Scope

In scope:

- One note at a time per browser client session.
- Voice, flute, trumpet, clarinet, saxophone, violin single-note lines, monophonic synth, whistling, and similar sources.
- Server-side detection from the existing PCM audio stream.
- OSC output only; no raw audio forwarded to performer sketches.
- Configurable detector choice and thresholds.

Out of scope for this first pass:

- Chord detection.
- Multiple simultaneous notes from one client.
- Full transcription.
- Key/scale inference.
- Beat tracking.

## Recommended Detector

This branch now uses a Java-native YIN-style detector behind a `PitchDetector` abstraction. That keeps the runtime self-contained while still leaving room to swap in TarsosDSP later if we want the library-specific implementations.

Candidate algorithms:

- `YIN`: good general-purpose monophonic pitch detection.
- Existing simple autocorrelation: keep as fallback and for comparison.

Recommended initial default:

```yaml
osc:
  pitch:
    enabled: true
    detector: "yin"
    min-level: 0.035
    emit-interval-ms: 250
    min-frequency-hz: 65
    max-frequency-hz: 1600
```

## Runtime Behavior

For each audio-capable session:

1. Drain PCM chunks in the OSC event pump.
2. Run clap/onset detection as today.
3. Run pitch detection on the same chunk.
4. Convert detected frequency to nearest MIDI note.
5. Convert MIDI note to pitch class: `C`, `C#`, `D`, ..., `A#`, `B`.
6. Emit a pitch event when:
   - the detected note changes, or
   - the same note remains stable and the repeat interval has elapsed.
7. Suppress pitch events when:
   - audio level is below `min-level`
   - pitch confidence is below detector threshold
   - frequency is outside configured min/max range

## OSC Contract

Aggregate stream:

```text
/input/pitch
  string note
  int midiNote
  float frequencyHz
  float level
```

Session-aware stream:

```text
/session/pitch
  string sessionId
  string note
  int midiNote
  float frequencyHz
  float level
```

Example:

```text
/session/pitch "7f3a1c22-..." "A#" 70 466.16 0.12
```

## Implementation Steps

1. Add pitch configuration.
   - Create `PitchConfig`.
   - Load `osc.pitch.*` values in `Main`.
   - Pass `PitchConfig` to `OscEventPump`.

2. Add detector abstraction.
   - Define a small interface such as:

```java
interface PitchDetector {
    PitchDetectionResult detect(byte[] pcm, int channels, int sampleRate);
}
```

   - Result fields:
     - detected/undetected
     - frequency Hz
     - confidence/probability if available
     - audio level

3. Keep current detector as fallback.
   - Wrap `AudioFeatureAnalyzer` behind the new interface.
   - Name it `simple-autocorrelation`.

4. Keep the detector swappable.
   - The current implementation uses a Java-native YIN detector.
   - If we later add TarsosDSP, wire it in behind the same interface and keep the existing fallback.

5. Add note conversion utility.
   - Frequency to nearest MIDI note.
   - MIDI note to pitch class.
   - Optional cents offset later.

6. Update `OscEventPump`.
   - Replace inline pitch logic with configured detector.
   - Preserve current throttling behavior.
   - Emit aggregate and session-aware pitch messages through `OscOutputService`.

7. Add tests.
   - Unit test note conversion.
   - Unit test threshold/range suppression.
   - Unit test OSC encoder output for `/input/pitch` and `/session/pitch`.
   - If practical, add a generated sine-wave fixture for A4 and C5.

8. Update browser UI later if needed.
   - Optional pitch detector status readout.
   - Optional sensitivity slider.
   - Optional note display for the user.

## Performer Sketch Behavior

Performer sketches should treat pitch as optional.

Examples:

- Gravity Orbit:
  - pitch changes collaborator color
  - pitch level changes orbiter brightness or pulse
- Lissajous:
  - pitch class changes phase or hue
  - octave changes amplitude or line weight
- Spiral Walker:
  - pitch changes spiral color
  - pitch changes growth rate or winding count

Sketches that do not use pitch should ignore `/input/pitch` and `/session/pitch`.

## Accuracy Notes

Single-note live pitch detection will be imperfect. Good defaults matter:

- A minimum level avoids random pitch from room noise.
- A confidence threshold avoids weak harmonic guesses.
- A frequency range avoids sub-bass rumble and high noise.
- Smoothing should be conservative so fast melodic movement still feels responsive.

For live use, the browser microphone and room acoustics will matter. Close mics and single sound sources will work much better than a phone across the room.

## Next Steps: Polyphonic Ideas

Polyphonic sources need a different approach. A single pitch detector will usually return one dominant pitch, not the notes in a chord.

Possible future event types:

```text
/input/chroma
  12 floats for C through B pitch-class energy

/session/chroma
  sessionId plus 12 floats for C through B

/session/harmony
  sessionId, root, quality, confidence

/session/onset
  sessionId, strength
```

Recommended exploration path:

1. Add chroma before chord names.
   - Chroma is more robust and lets performer sketches decide how to use pitch-class energy.
   - It avoids overclaiming exact chord labels.

2. Consider Essentia or a Python sidecar for richer analysis.
   - Better for chroma, harmonic analysis, and transcription-style features.
   - Heavier operational cost than Java-native TarsosDSP.

3. Keep polyphonic output optional.
   - Many performers only need energy, onset, or color mapping.
   - Do not require every stream to emit chroma/harmony.

4. Do not replace session pitch with chroma.
   - Keep `/session/pitch` for monophonic instruments.
   - Add `/session/chroma` for chords and ensembles.
