/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

public class DebugConfig {
    private final boolean logging;

    public DebugConfig(boolean logging) {
        this.logging = logging;
    }

    public boolean isLogging() {
        return logging;
    }
}