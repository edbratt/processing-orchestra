/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

public record OscStreamConfig(
    String id,
    String host,
    int port,
    String contract,
    String mirrorSourceId
) {
    public static final String CONTRACT_WALKER_TRIGGER = "walker-trigger";
    public static final String CONTRACT_WALKER_MONITOR = "walker-monitor";
    public static final String CONTRACT_ORCHESTRA_INPUT_V1 = "orchestra-input-v1";
    public static final String CONTRACT_ORCHESTRA_SESSION_V1 = "orchestra-session-v1";

    public OscStreamConfig(String id, String host, int port, String contract) {
        this(id, host, port, contract, "");
    }
}
