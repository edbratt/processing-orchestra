/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

final class SimpleAutocorrelationPitchDetector implements PitchDetector {
    private final PitchConfig config;

    SimpleAutocorrelationPitchDetector(PitchConfig config) {
        this.config = config;
    }

    @Override
    public PitchDetectionResult detect(byte[] data, int channels, int sampleRate) {
        AudioFeatureAnalyzer.AudioAnalysisResult analysis = AudioFeatureAnalyzer.analyze(
            data,
            channels,
            sampleRate,
            1f,
            0f
        );
        float frequencyHz = analysis.dominantFrequency();
        boolean detected = frequencyHz > 0f
            && analysis.level() >= config.minLevel()
            && frequencyHz >= config.minFrequencyHz()
            && frequencyHz <= config.maxFrequencyHz();
        float confidence = detected ? 0.3f : 0f;
        return new PitchDetectionResult(detected, frequencyHz, confidence, analysis.level());
    }
}
