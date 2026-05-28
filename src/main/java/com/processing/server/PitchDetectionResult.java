/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

public record PitchDetectionResult(boolean detected,
                                   float frequencyHz,
                                   float confidence,
                                   float level) {
}
