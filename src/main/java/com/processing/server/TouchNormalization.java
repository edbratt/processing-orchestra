/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

final class TouchNormalization {
    private TouchNormalization() {
    }

    static float fromSignedRange(float value) {
        return Math.max(0f, Math.min(1f, (value + 1f) * 0.5f));
    }
}
