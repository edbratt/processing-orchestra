/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

final class PitchDetectors {
    private PitchDetectors() {
    }

    static PitchDetector create(PitchConfig config) {
        String detector = config.detector();
        return switch (detector) {
            case "autocorrelation", "simple-autocorrelation" -> new SimpleAutocorrelationPitchDetector(config);
            case "yin", "tarsos-yin", "tarsos-mpm" -> new YinPitchDetector(config);
            default -> new YinPitchDetector(config);
        };
    }
}
