/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

public record OscDebugConfig(boolean logging, int sampleLimit) {
    public boolean shouldLog(int messageCount) {
        return logging && (sampleLimit <= 0 || messageCount <= sampleLimit);
    }
}
