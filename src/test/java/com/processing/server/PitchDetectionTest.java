/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PitchDetectionTest {
    private static final int SAMPLE_RATE = 44100;
    private static final PitchConfig CONFIG = new PitchConfig(
        true,
        "yin",
        0.02f,
        0.12f,
        65f,
        1600f,
        0.12f,
        250,
        2,
        0.35f
    );

    @Test
    void detectsA4FromSineWave() {
        PitchDetector detector = new YinPitchDetector(CONFIG);
        PitchDetectionResult result = detector.detect(sineWavePcm(440f, 0.7f, 4096), 1, SAMPLE_RATE);

        assertTrue(result.detected(), "expected detector to identify a stable pitch");
        assertEquals("A", PitchUtils.midiToPitchClass(PitchUtils.frequencyToMidi(result.frequencyHz())));
        assertEquals("A4", PitchUtils.midiToNoteName(PitchUtils.frequencyToMidi(result.frequencyHz())));
        assertEquals(69, PitchUtils.frequencyToMidi(result.frequencyHz()));
        assertTrue(Math.abs(result.frequencyHz() - 440f) < 5f, "expected frequency near A4");
        assertTrue(result.confidence() > 0.5f, "expected meaningful confidence");
    }

    @Test
    void suppressesSilence() {
        PitchDetector detector = new YinPitchDetector(CONFIG);
        PitchDetectionResult result = detector.detect(new byte[4096], 1, SAMPLE_RATE);

        assertNull(new PitchTracker().update(result, CONFIG, 0L));
    }

    @Test
    void holdsTheCurrentNoteUntilTheSwitchIsStable() {
        PitchTracker tracker = new PitchTracker();
        PitchDetector detector = new YinPitchDetector(CONFIG);

        PitchDetectionResult a4 = detector.detect(sineWavePcm(440f, 0.7f, 4096), 1, SAMPLE_RATE);
        PitchDetectionResult b4 = detector.detect(sineWavePcm(493.88f, 0.7f, 4096), 1, SAMPLE_RATE);

        PitchEmission first = tracker.update(a4, CONFIG, 0L);
        PitchEmission unstableSwitch = tracker.update(b4, CONFIG, 75L);
        PitchEmission stableSwitch = tracker.update(b4, CONFIG, 150L);

        assertNotNull(first);
        assertEquals("A4", first.note());
        assertNull(unstableSwitch);
        assertNotNull(stableSwitch);
        assertEquals("B4", stableSwitch.note());
    }

    private byte[] sineWavePcm(float frequencyHz, float amplitude, int sampleCount) {
        byte[] data = new byte[sampleCount * 2];
        for (int i = 0; i < sampleCount; i++) {
            double phase = (2.0 * Math.PI * frequencyHz * i) / SAMPLE_RATE;
            short sample = (short) Math.round(Math.sin(phase) * amplitude * Short.MAX_VALUE);
            int offset = i * 2;
            data[offset] = (byte) (sample & 0xFF);
            data[offset + 1] = (byte) ((sample >>> 8) & 0xFF);
        }
        return data;
    }
}
