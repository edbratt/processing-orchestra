/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

final class AudioFeatureAnalyzer {
    static final float MIN_DETECTION_LEVEL = 0.035f;
    static final float MIN_FREQUENCY_HZ = 85f;
    static final float MAX_FREQUENCY_HZ = 1200f;
    private static final float MIN_CORRELATION_SCORE = 0.18f;

    private AudioFeatureAnalyzer() {
    }

    static AudioAnalysisResult analyze(byte[] data,
                                       int channels,
                                       int sampleRate,
                                       float gainFactor,
                                       float previousFrequency) {
        if (data == null || data.length == 0) {
            return new AudioAnalysisResult(0f, previousFrequency);
        }

        int frameSizeBytes = Math.max(1, channels) * 2;
        int frameCount = data.length / frameSizeBytes;
        if (frameCount < 32) {
            return new AudioAnalysisResult(0f, previousFrequency);
        }

        float[] samples = new float[frameCount];
        float sumAbs = 0f;
        float mean = 0f;

        for (int frame = 0; frame < frameCount; frame++) {
            int frameOffset = frame * frameSizeBytes;
            float mixedSample = 0f;
            for (int channel = 0; channel < channels; channel++) {
                int offset = frameOffset + channel * 2;
                short rawSample = (short) ((data[offset] & 0xFF) | (data[offset + 1] << 8));
                mixedSample += rawSample / 32768.0f;
            }

            float normalizedSample = mixedSample / Math.max(1, channels);
            normalizedSample = Math.max(-1f, Math.min(1f, normalizedSample * gainFactor));
            samples[frame] = normalizedSample;
            sumAbs += Math.abs(normalizedSample);
            mean += normalizedSample;
        }

        float level = sumAbs / frameCount;
        if (level < MIN_DETECTION_LEVEL) {
            return new AudioAnalysisResult(level, previousFrequency);
        }

        mean /= frameCount;
        for (int i = 0; i < frameCount; i++) {
            samples[i] -= mean;
        }

        int minLag = Math.max(2, Math.round(sampleRate / MAX_FREQUENCY_HZ));
        int maxLag = Math.min(frameCount - 4, Math.round(sampleRate / MIN_FREQUENCY_HZ));
        if (minLag >= maxLag) {
            return new AudioAnalysisResult(level, previousFrequency);
        }

        float bestScore = -Float.MAX_VALUE;
        int bestLag = -1;

        for (int lag = minLag; lag <= maxLag; lag++) {
            float correlation = 0f;
            float energyA = 0f;
            float energyB = 0f;
            int limit = frameCount - lag;

            for (int i = 0; i < limit; i++) {
                float sampleA = samples[i];
                float sampleB = samples[i + lag];
                correlation += sampleA * sampleB;
                energyA += sampleA * sampleA;
                energyB += sampleB * sampleB;
            }

            if (energyA <= 0.000001f || energyB <= 0.000001f) {
                continue;
            }

            float normalizedCorrelation =
                (float) (correlation / Math.sqrt((double) energyA * energyB));
            if (normalizedCorrelation > bestScore) {
                bestScore = normalizedCorrelation;
                bestLag = lag;
            }
        }

        if (bestLag < 0 || bestScore < MIN_CORRELATION_SCORE) {
            return new AudioAnalysisResult(level, previousFrequency);
        }

        float dominantFrequency = sampleRate / (float) bestLag;
        return new AudioAnalysisResult(level, dominantFrequency);
    }

    static final class AudioAnalysisResult {
        private final float level;
        private final float dominantFrequency;

        AudioAnalysisResult(float level, float dominantFrequency) {
            this.level = level;
            this.dominantFrequency = dominantFrequency;
        }

        float level() {
            return level;
        }

        float dominantFrequency() {
            return dominantFrequency;
        }
    }
}
