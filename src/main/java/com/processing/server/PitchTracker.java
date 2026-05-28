/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

final class PitchTracker {
    private int activeMidiNote = Integer.MIN_VALUE;
    private int candidateMidiNote = Integer.MIN_VALUE;
    private int candidateStreak = 0;
    private long lastEmittedAtMs;
    private float smoothedFrequencyHz;

    PitchEmission update(PitchDetectionResult detection, PitchConfig config, long now) {
        if (detection == null || !detection.detected()) {
            return null;
        }
        if (detection.level() < config.minLevel() || detection.confidence() < config.minConfidence()) {
            return null;
        }
        int midiNote = PitchUtils.frequencyToMidi(detection.frequencyHz());
        if (midiNote == Integer.MIN_VALUE) {
            return null;
        }
        if (detection.frequencyHz() < config.minFrequencyHz() || detection.frequencyHz() > config.maxFrequencyHz()) {
            return null;
        }

        if (activeMidiNote == Integer.MIN_VALUE) {
            accept(midiNote, detection.frequencyHz(), now);
            return emit(detection.level());
        }

        if (midiNote == activeMidiNote) {
            resetCandidate();
            smoothFrequency(detection.frequencyHz(), config.smoothingAlpha());
            if (now - lastEmittedAtMs >= config.emitIntervalMs()) {
                lastEmittedAtMs = now;
                return emit(detection.level());
            }
            return null;
        }

        if (midiNote == candidateMidiNote) {
            candidateStreak++;
        } else {
            candidateMidiNote = midiNote;
            candidateStreak = 1;
        }

        if (candidateStreak < config.noteSwitchStreak()) {
            return null;
        }

        accept(midiNote, detection.frequencyHz(), now);
        return emit(detection.level());
    }

    private void accept(int midiNote, float frequencyHz, long now) {
        activeMidiNote = midiNote;
        candidateMidiNote = Integer.MIN_VALUE;
        candidateStreak = 0;
        smoothedFrequencyHz = frequencyHz;
        lastEmittedAtMs = now;
    }

    private void smoothFrequency(float frequencyHz, float smoothingAlpha) {
        if (smoothedFrequencyHz <= 0f) {
            smoothedFrequencyHz = frequencyHz;
            return;
        }
        smoothedFrequencyHz = (smoothingAlpha * frequencyHz) + ((1f - smoothingAlpha) * smoothedFrequencyHz);
    }

    private void resetCandidate() {
        candidateMidiNote = Integer.MIN_VALUE;
        candidateStreak = 0;
    }

    private PitchEmission emit(float level) {
        return new PitchEmission(
            PitchUtils.midiToNoteName(activeMidiNote),
            activeMidiNote,
            smoothedFrequencyHz,
            level
        );
    }
}
