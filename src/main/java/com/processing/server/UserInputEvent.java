/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;

public record UserInputEvent(
    String sessionId,
    String eventType,
    String controlId,
    float value,
    float x,
    float y,
    long timestamp
) {
    public static UserInputEvent fromJson(jakarta.json.JsonObject json) {
        return new UserInputEvent(
            json.getString("sessionId", ""),
            json.getString("eventType", ""),
            json.getString("controlId", ""),
            (float) json.getJsonNumber("value").doubleValue(),
            (float) json.getJsonNumber("x").doubleValue(),
            (float) json.getJsonNumber("y").doubleValue(),
            json.getJsonNumber("timestamp").longValue()
        );
    }
}