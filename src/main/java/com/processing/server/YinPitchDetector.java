/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

final class YinPitchDetector implements PitchDetector {
    private final PitchConfig config;

    YinPitchDetector(PitchConfig config) {
        this.config = config;
    }

    @Override
    public PitchDetectionResult detect(byte[] data, int channels, int sampleRate) {
        PitchUtils.PitchSamples decoded = PitchUtils.decodePcm16Le(data, channels, 1f);
        float level = decoded.level();
        if (decoded.samples().length < 32 || level < config.minLevel()) {
            return new PitchDetectionResult(false, 0f, 0f, level);
        }

        float[] samples = decoded.samples().clone();
        removeMean(samples);

        int frameCount = samples.length;
        int minLag = Math.max(2, Math.round(sampleRate / config.maxFrequencyHz()));
        int maxLag = Math.min(frameCount - 4, Math.round(sampleRate / config.minFrequencyHz()));
        if (minLag >= maxLag) {
            return new PitchDetectionResult(false, 0f, 0f, level);
        }

        float[] difference = new float[maxLag + 1];
        for (int tau = 1; tau <= maxLag; tau++) {
            float sum = 0f;
            int limit = frameCount - tau;
            for (int i = 0; i < limit; i++) {
                float diff = samples[i] - samples[i + tau];
                sum += diff * diff;
            }
            difference[tau] = sum / limit;
        }

        float[] cmnd = new float[maxLag + 1];
        cmnd[0] = 1f;
        float runningSum = 0f;
        for (int tau = 1; tau <= maxLag; tau++) {
            runningSum += difference[tau];
            cmnd[tau] = runningSum > 0f ? difference[tau] * tau / runningSum : 1f;
        }

        int tau = chooseLag(cmnd, minLag, maxLag, config.yinThreshold());
        if (tau < 0) {
            return new PitchDetectionResult(false, 0f, 0f, level);
        }

        float refinedLag = refineLag(cmnd, tau);
        if (refinedLag <= 0f) {
            refinedLag = tau;
        }

        float frequencyHz = sampleRate / refinedLag;
        if (frequencyHz < config.minFrequencyHz() || frequencyHz > config.maxFrequencyHz()) {
            return new PitchDetectionResult(false, 0f, 0f, level);
        }

        float confidence = 1f - PitchUtils.clamp(cmnd[tau], 0f, 1f);
        if (confidence < config.minConfidence()) {
            return new PitchDetectionResult(false, 0f, confidence, level);
        }

        return new PitchDetectionResult(true, frequencyHz, confidence, level);
    }

    private void removeMean(float[] samples) {
        float mean = 0f;
        for (float sample : samples) {
            mean += sample;
        }
        mean /= samples.length;
        for (int i = 0; i < samples.length; i++) {
            samples[i] -= mean;
        }
    }

    private int chooseLag(float[] cmnd, int minLag, int maxLag, float threshold) {
        for (int tau = minLag; tau <= maxLag; tau++) {
            if (cmnd[tau] < threshold) {
                while (tau + 1 <= maxLag && cmnd[tau + 1] < cmnd[tau]) {
                    tau++;
                }
                return tau;
            }
        }

        int bestTau = -1;
        float bestValue = Float.MAX_VALUE;
        for (int tau = minLag; tau <= maxLag; tau++) {
            float value = cmnd[tau];
            if (value < bestValue) {
                bestValue = value;
                bestTau = tau;
            }
        }
        return bestValue < 0.45f ? bestTau : -1;
    }

    private float refineLag(float[] cmnd, int tau) {
        if (tau <= 1 || tau >= cmnd.length - 1) {
            return tau;
        }

        float y0 = cmnd[tau - 1];
        float y1 = cmnd[tau];
        float y2 = cmnd[tau + 1];
        float denominator = (2f * y1) - y0 - y2;
        if (Math.abs(denominator) < 0.000001f) {
            return tau;
        }
        float offset = 0.5f * (y0 - y2) / denominator;
        return tau + offset;
    }
}
