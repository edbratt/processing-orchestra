/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

public record PitchConfig(
    boolean enabled,
    String detector,
    float minLevel,
    float minConfidence,
    float minFrequencyHz,
    float maxFrequencyHz,
    float yinThreshold,
    int emitIntervalMs,
    int noteSwitchStreak,
    float smoothingAlpha
) {
    public PitchConfig {
        detector = detector == null || detector.isBlank() ? "yin" : detector.trim().toLowerCase();
        minLevel = clamp(minLevel, 0f, 1f);
        minConfidence = clamp(minConfidence, 0f, 1f);
        minFrequencyHz = Math.max(1f, minFrequencyHz);
        maxFrequencyHz = Math.max(minFrequencyHz, maxFrequencyHz);
        yinThreshold = clamp(yinThreshold, 0.01f, 0.99f);
        emitIntervalMs = Math.max(0, emitIntervalMs);
        noteSwitchStreak = Math.max(1, noteSwitchStreak);
        smoothingAlpha = clamp(smoothingAlpha, 0f, 1f);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
