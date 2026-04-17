/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

public class DebugConfig {
    private final boolean logging;
    private final boolean audioLogging;
    private final int audioSampleLimit;

    public DebugConfig(boolean logging) {
        this(logging, false, 5);
    }

    public DebugConfig(boolean logging, boolean audioLogging, int audioSampleLimit) {
        this.logging = logging;
        this.audioLogging = audioLogging;
        this.audioSampleLimit = audioSampleLimit;
    }

    public boolean isLogging() {
        return logging;
    }

    public boolean isAudioLogging() {
        return audioLogging;
    }

    public int getAudioSampleLimit() {
        return audioSampleLimit;
    }
}
