/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

final class PitchUtils {
    private static final String[] NOTE_NAMES = {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    private PitchUtils() {
    }

    static PitchSamples decodePcm16Le(byte[] data, int channels, float gainFactor) {
        if (data == null || data.length == 0) {
            return new PitchSamples(new float[0], 0f);
        }

        int frameSizeBytes = Math.max(1, channels) * 2;
        int frameCount = data.length / frameSizeBytes;
        if (frameCount <= 0) {
            return new PitchSamples(new float[0], 0f);
        }

        float[] samples = new float[frameCount];
        float sumAbs = 0f;
        for (int frame = 0; frame < frameCount; frame++) {
            int frameOffset = frame * frameSizeBytes;
            float mixedSample = 0f;
            for (int channel = 0; channel < channels; channel++) {
                int offset = frameOffset + channel * 2;
                short rawSample = (short) ((data[offset] & 0xFF) | (data[offset + 1] << 8));
                mixedSample += rawSample / 32768.0f;
            }

            float normalizedSample = (mixedSample / Math.max(1, channels)) * gainFactor;
            normalizedSample = clamp(normalizedSample, -1f, 1f);
            samples[frame] = normalizedSample;
            sumAbs += Math.abs(normalizedSample);
        }

        return new PitchSamples(samples, sumAbs / frameCount);
    }

    static int frequencyToMidi(float frequencyHz) {
        if (!Float.isFinite(frequencyHz) || frequencyHz <= 0f) {
            return Integer.MIN_VALUE;
        }
        return Math.round(69f + 12f * (float) (Math.log(frequencyHz / 440f) / Math.log(2)));
    }

    static String midiToPitchClass(int midiNote) {
        return NOTE_NAMES[Math.floorMod(midiNote, NOTE_NAMES.length)];
    }

    static String midiToNoteName(int midiNote) {
        return midiToPitchClass(midiNote) + midiToOctave(midiNote);
    }

    static int midiToOctave(int midiNote) {
        return (midiNote / 12) - 1;
    }

    static float midiToFrequency(int midiNote) {
        return (float) (440.0 * Math.pow(2.0, (midiNote - 69) / 12.0));
    }

    static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    static final class PitchSamples {
        private final float[] samples;
        private final float level;

        PitchSamples(float[] samples, float level) {
            this.samples = samples;
            this.level = level;
        }

        float[] samples() {
            return samples;
        }

        float level() {
            return level;
        }
    }
}
