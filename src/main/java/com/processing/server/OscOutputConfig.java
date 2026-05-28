/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import java.util.List;

public record OscOutputConfig(
    boolean enabled,
    String defaultStreamId,
    List<OscStreamConfig> streams,
    int fps,
    OscDebugConfig debugConfig
) {
    public static OscOutputConfig defaults(boolean enabled) {
        return new OscOutputConfig(
            enabled,
            "spiral-main",
            List.of(
                new OscStreamConfig("spiral-main", "127.0.0.1", 12000, OscStreamConfig.CONTRACT_WALKER_TRIGGER),
                new OscStreamConfig("vectorscope", "127.0.0.1", 12001, OscStreamConfig.CONTRACT_WALKER_MONITOR, "spiral-main")
            ),
            60,
            new OscDebugConfig(false, 100)
        );
    }
}
