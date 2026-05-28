/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

public interface PitchDetector {
    PitchDetectionResult detect(byte[] data, int channels, int sampleRate);
}
