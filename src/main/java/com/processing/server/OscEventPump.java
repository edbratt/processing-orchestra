/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OscEventPump implements Runnable {
    private static final float CLAP_PRE_THRESHOLD = 0.05f;
    private static final float CLAP_LIVE_THRESHOLD = 0.22f;
    private static final int CLAP_DEBOUNCE_MS = 350;
    private static final float MOTION_BETA_CLAMP_DEGREES = 60f;
    private static final float MOTION_GAMMA_CLAMP_DEGREES = 60f;
    private static final float MOTION_SHAKE_THRESHOLD_G = 1.2f;
    private static final int MOTION_SHAKE_DEBOUNCE_MS = 350;
    private static final int PITCH_EMIT_INTERVAL_MS = 250;
    private static final String[] NOTE_NAMES = {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    private final EventQueue eventQueue;
    private final SessionManager sessionManager;
    private final AudioBuffer audioBuffer;
    private final AudioConfig audioConfig;
    private final OscOutputService oscOutputService;
    private final String defaultStreamId;
    private final Map<String, WalkerState> walkerStatesBySession = new HashMap<>();
    private final Map<String, Integer> pingCountsByStream = new HashMap<>();
    private final Map<String, ClapState> clapStatesBySession = new HashMap<>();
    private final Map<String, Long> lastMotionPingMsBySession = new HashMap<>();
    private final Map<String, Float> lastMotionMagnitudeBySession = new HashMap<>();
    private final Map<String, MotionControlState> motionControlsBySession = new HashMap<>();
    private final Map<String, SessionSnapshot> joinedSessionsBySession = new HashMap<>();
    private final Map<String, PitchState> pitchStatesBySession = new HashMap<>();

    public OscEventPump(EventQueue eventQueue,
                        SessionManager sessionManager,
                        AudioBuffer audioBuffer,
                        AudioConfig audioConfig,
                        OscOutputService oscOutputService,
                        String defaultStreamId) {
        this.eventQueue = eventQueue;
        this.sessionManager = sessionManager;
        this.audioBuffer = audioBuffer;
        this.audioConfig = audioConfig;
        this.oscOutputService = oscOutputService;
        this.defaultStreamId = defaultStreamId;
    }

    @Override
    public void run() {
        Map<String, StreamFrame> framesByStream = new HashMap<>();
        UserInputEvent event;
        while ((event = eventQueue.poll()) != null) {
            process(event, framesByStream);
        }
        processAudio(framesByStream);
        flush(framesByStream);
    }

    private void process(UserInputEvent event, Map<String, StreamFrame> framesByStream) {
        if ("session-ended".equals(event.eventType())) {
            sendSessionLeave(event.sessionId());
            walkerStatesBySession.remove(event.sessionId());
            clapStatesBySession.remove(event.sessionId());
            lastMotionPingMsBySession.remove(event.sessionId());
            lastMotionMagnitudeBySession.remove(event.sessionId());
            motionControlsBySession.remove(event.sessionId());
            pitchStatesBySession.remove(event.sessionId());
            return;
        }

        String streamId = streamIdFor(event.sessionId());
        String instrumentId = instrumentIdFor(event.sessionId());
        ensureSessionJoined(event.sessionId(), streamId, instrumentId);
        switch (event.eventType()) {
            case "touch" -> {
                if (usesSurfaceControls(instrumentId)) {
                    updateWalker(event, streamId, framesByStream);
                }
            }
            case "button" -> {
                if (usesSurfaceControls(instrumentId)) {
                    addTrigger(framesByStream, streamId, event.sessionId(), triggerKind(event.controlId(), "button"));
                }
            }
            case "key" -> {
                if (usesSurfaceControls(instrumentId) && "pressed".equals(event.keyAction()) && isSpace(event.keyText())) {
                    addTrigger(framesByStream, streamId, event.sessionId(), "space");
                }
            }
            case "slider" -> updateMotionControl(event);
            case "motion" -> processMotion(event, streamId, instrumentId, framesByStream);
            default -> {
            }
        }
    }

    private void updateMotionControl(UserInputEvent event) {
        MotionControlState state = motionControlsBySession.computeIfAbsent(
            event.sessionId(),
            ignored -> new MotionControlState()
        );
        float value = event.value();
        switch (event.controlId()) {
            case "motionXTrim" -> state.xTrim = clamp(value, 0.25f, 3f);
            case "motionYTrim" -> state.yTrim = clamp(value, 0.25f, 3f);
            case "motionXOffset" -> state.xOffset = clamp(value, -1f, 1f);
            case "motionYOffset" -> state.yOffset = clamp(value, -1f, 1f);
            case "motionShakeThreshold" -> state.shakeThreshold = clamp(value, 0.2f, 3f);
            default -> {
            }
        }
    }

    private void updateWalker(UserInputEvent event, String streamId, Map<String, StreamFrame> framesByStream) {
        float x = clamp(event.x(), 0f, 1f) * 2f - 1f;
        float y = 1f - clamp(event.y(), 0f, 1f) * 2f;
        updateWalkerState(event.sessionId(), streamId, framesByStream, x, y);
    }

    private void processMotion(UserInputEvent event,
                               String streamId,
                               String instrumentId,
                               Map<String, StreamFrame> framesByStream) {
        if (usesMotionTilt(instrumentId)) {
            MotionControlState controls = motionControlsBySession.computeIfAbsent(
                event.sessionId(),
                ignored -> new MotionControlState()
            );
            float x = clamp((event.gamma() / MOTION_GAMMA_CLAMP_DEGREES) * controls.xTrim + controls.xOffset, -1f, 1f);
            float y = clamp((-event.beta() / MOTION_BETA_CLAMP_DEGREES) * controls.yTrim + controls.yOffset, -1f, 1f);
            ensureSessionJoined(event.sessionId(), streamId, instrumentId);
            updateWalkerState(event.sessionId(), streamId, framesByStream, x, y);
        }

        if (usesMotionShake(instrumentId)) {
            MotionControlState controls = motionControlsBySession.computeIfAbsent(
                event.sessionId(),
                ignored -> new MotionControlState()
            );
            Float previousMagnitude = lastMotionMagnitudeBySession.put(event.sessionId(), event.magnitude());
            if (previousMagnitude == null) {
                return;
            }
            float motionDelta = Math.abs(event.magnitude() - previousMagnitude);
            long now = event.timestamp() > 0 ? event.timestamp() : System.currentTimeMillis();
            long lastPingMs = lastMotionPingMsBySession.getOrDefault(event.sessionId(), 0L);
            if (motionDelta >= controls.shakeThreshold && now - lastPingMs > MOTION_SHAKE_DEBOUNCE_MS) {
                lastMotionPingMsBySession.put(event.sessionId(), now);
                ensureSessionJoined(event.sessionId(), streamId, instrumentId);
                addTrigger(framesByStream, streamId, event.sessionId(), "shake");
            }
        }
    }

    private void updateWalkerState(String sessionId,
                                   String streamId,
                                   Map<String, StreamFrame> framesByStream,
                                   float x,
                                   float y) {
        walkerStatesBySession.put(sessionId, new WalkerState(x, y));
        framesByStream.computeIfAbsent(streamId, ignored -> new StreamFrame()).walkerChanged = true;
        oscOutputService.sendSessionPosition(streamId, sessionId, x, y);
    }

    private void flush(Map<String, StreamFrame> framesByStream) {
        for (Map.Entry<String, StreamFrame> entry : framesByStream.entrySet()) {
            String streamId = entry.getKey();
            StreamFrame frame = entry.getValue();
            if (frame.walkerChanged) {
                sendAggregatedWalker(streamId);
            }
            for (Trigger trigger : frame.triggers) {
                int count = pingCountsByStream.merge(streamId, 1, Integer::sum);
                oscOutputService.sendTrigger(streamId, trigger.kind(), count);
                oscOutputService.sendSessionTrigger(streamId, trigger.sessionId(), trigger.kind(), count);
            }
        }
    }

    private void processAudio(Map<String, StreamFrame> framesByStream) {
        long now = System.currentTimeMillis();
        for (String sessionId : audioBuffer.getActiveSessionIds()) {
            if (!usesAudioClap(instrumentIdFor(sessionId))) {
                drainAudio(sessionId);
                continue;
            }
            String streamId = streamIdFor(sessionId);
            ensureSessionJoined(sessionId, streamId, instrumentIdFor(sessionId));
            byte[] chunk;
            while ((chunk = audioBuffer.poll(sessionId)) != null) {
                float amplitude = peakAmplitude(chunk);
                if (detectClap(sessionId, amplitude, now)) {
                    addTrigger(framesByStream, streamId, sessionId, "clap");
                }
                processPitch(sessionId, streamId, chunk, now);
            }
        }
    }

    private void drainAudio(String sessionId) {
        while (audioBuffer.poll(sessionId) != null) {
            // Discard audio for instruments that do not currently use it.
        }
    }

    private boolean detectClap(String sessionId, float amplitude, long now) {
        ClapState state = clapStatesBySession.computeIfAbsent(sessionId, ignored -> new ClapState());
        if (amplitude > CLAP_PRE_THRESHOLD) {
            state.inClap = true;
            state.currentPeak = Math.max(state.currentPeak, amplitude);
            if (amplitude > CLAP_LIVE_THRESHOLD && now - state.lastPingMs > CLAP_DEBOUNCE_MS) {
                state.lastPingMs = now;
                return true;
            }
            return false;
        }

        if (state.inClap) {
            state.inClap = false;
            state.currentPeak = 0f;
        }
        return false;
    }

    private float peakAmplitude(byte[] data) {
        float peak = 0f;
        for (int i = 0; i + 1 < data.length; i += 2) {
            short sample = (short) ((data[i] & 0xFF) | (data[i + 1] << 8));
            peak = Math.max(peak, Math.abs(sample / 32768.0f));
        }
        return peak;
    }

    private void processPitch(String sessionId, String streamId, byte[] chunk, long now) {
        PitchState state = pitchStatesBySession.computeIfAbsent(sessionId, ignored -> new PitchState());
        AudioFeatureAnalyzer.AudioAnalysisResult analysis = AudioFeatureAnalyzer.analyze(
            chunk,
            audioConfig.getChannels(),
            audioConfig.getSampleRate(),
            1f,
            state.previousFrequencyHz
        );
        state.previousFrequencyHz = analysis.dominantFrequency();
        Pitch pitch = pitchFromFrequency(analysis.dominantFrequency());
        if (pitch == null || analysis.level() < AudioFeatureAnalyzer.MIN_DETECTION_LEVEL) {
            return;
        }
        boolean changed = pitch.midiNote() != state.lastMidiNote;
        boolean due = now - state.lastEmittedAtMs >= PITCH_EMIT_INTERVAL_MS;
        if (!changed && !due) {
            return;
        }
        state.lastMidiNote = pitch.midiNote();
        state.lastEmittedAtMs = now;
        oscOutputService.sendPitch(streamId, pitch.note(), pitch.midiNote(), pitch.frequencyHz(), analysis.level());
        oscOutputService.sendSessionPitch(streamId, sessionId, pitch.note(), pitch.midiNote(), pitch.frequencyHz(), analysis.level());
    }

    private Pitch pitchFromFrequency(float frequencyHz) {
        if (frequencyHz <= 0f) {
            return null;
        }
        int midi = Math.round(69f + 12f * (float) (Math.log(frequencyHz / 440f) / Math.log(2)));
        int noteIndex = Math.floorMod(midi, NOTE_NAMES.length);
        return new Pitch(NOTE_NAMES[noteIndex], midi, frequencyHz);
    }

    private void sendAggregatedWalker(String streamId) {
        float sumX = 0f;
        float sumY = 0f;
        int count = 0;
        for (Map.Entry<String, WalkerState> entry : walkerStatesBySession.entrySet()) {
            SessionManager.SessionInfo session = sessionManager.getSession(entry.getKey());
            if (session == null || !streamId.equals(session.streamId())) {
                continue;
            }
            WalkerState state = entry.getValue();
            sumX += state.x();
            sumY += state.y();
            count++;
        }
        if (count > 0) {
            oscOutputService.sendWalker(streamId, sumX / count, sumY / count);
        }
    }

    private String streamIdFor(String sessionId) {
        SessionManager.SessionInfo session = sessionManager.getSession(sessionId);
        if (session == null || session.streamId().isBlank()) {
            return defaultStreamId;
        }
        return session.streamId();
    }

    private String instrumentIdFor(String sessionId) {
        SessionManager.SessionInfo session = sessionManager.getSession(sessionId);
        if (session == null || session.instrumentId().isBlank()) {
            return "full-orchestra";
        }
        return session.instrumentId();
    }

    private boolean usesSurfaceControls(String instrumentId) {
        return "full-orchestra".equals(instrumentId) || "touch-walker".equals(instrumentId);
    }

    private boolean usesAudioClap(String instrumentId) {
        return "full-orchestra".equals(instrumentId) || "audio-clap".equals(instrumentId);
    }

    private boolean usesMotionTilt(String instrumentId) {
        return "full-orchestra".equals(instrumentId)
            || "tilt-walker".equals(instrumentId)
            || "motion-orchestra".equals(instrumentId);
    }

    private boolean usesMotionShake(String instrumentId) {
        return "full-orchestra".equals(instrumentId)
            || "shake-ping".equals(instrumentId)
            || "motion-orchestra".equals(instrumentId);
    }

    private boolean isSpace(String keyText) {
        return " ".equals(keyText) || "Space".equals(keyText) || "Spacebar".equals(keyText);
    }

    private void addTrigger(Map<String, StreamFrame> framesByStream, String streamId, String sessionId, String kind) {
        framesByStream.computeIfAbsent(streamId, ignored -> new StreamFrame()).triggers.add(new Trigger(sessionId, kind));
    }

    private String triggerKind(String controlId, String fallback) {
        if (controlId == null || controlId.isBlank()) {
            return fallback;
        }
        return controlId;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void ensureSessionJoined(String sessionId, String streamId, String instrumentId) {
        SessionManager.SessionInfo session = sessionManager.getSession(sessionId);
        String name = session == null ? "" : session.name();
        SessionSnapshot previous = joinedSessionsBySession.get(sessionId);
        SessionSnapshot current = new SessionSnapshot(streamId, name, instrumentId);
        if (current.equals(previous)) {
            return;
        }
        if (previous != null && !previous.streamId().equals(streamId)) {
            oscOutputService.sendSessionLeave(previous.streamId(), sessionId);
        }
        joinedSessionsBySession.put(sessionId, current);
        oscOutputService.sendSessionJoin(streamId, sessionId, name, instrumentId);
    }

    private void sendSessionLeave(String sessionId) {
        SessionSnapshot previous = joinedSessionsBySession.remove(sessionId);
        if (previous != null) {
            oscOutputService.sendSessionLeave(previous.streamId(), sessionId);
        }
    }

    private static final class StreamFrame {
        private boolean walkerChanged;
        private final List<Trigger> triggers = new ArrayList<>();
    }

    private record WalkerState(float x, float y) {
    }

    private record Trigger(String sessionId, String kind) {
    }

    private record SessionSnapshot(String streamId, String name, String instrumentId) {
    }

    private record Pitch(String note, int midiNote, float frequencyHz) {
    }

    private static final class ClapState {
        private boolean inClap;
        private float currentPeak;
        private long lastPingMs;
    }

    private static final class MotionControlState {
        private float xTrim = 1f;
        private float yTrim = 1f;
        private float xOffset = 0f;
        private float yOffset = 0f;
        private float shakeThreshold = MOTION_SHAKE_THRESHOLD_G;
    }

    private static final class PitchState {
        private float previousFrequencyHz;
        private int lastMidiNote = Integer.MIN_VALUE;
        private long lastEmittedAtMs;
    }
}
