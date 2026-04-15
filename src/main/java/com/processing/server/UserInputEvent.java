/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import io.helidon.json.JsonObject;

public record UserInputEvent(
    String sessionId,
    String eventType,
    String controlId,
    float value,
    float x,
    float y,
    float alpha,
    float beta,
    float gamma,
    float ax,
    float ay,
    float az,
    float magnitude,
    long timestamp
) {
    public UserInputEvent(String sessionId,
                          String eventType,
                          String controlId,
                          float value,
                          float x,
                          float y,
                          long timestamp) {
        this(sessionId, eventType, controlId, value, x, y, 0f, 0f, 0f, 0f, 0f, 0f, 0f, timestamp);
    }

    public static UserInputEvent fromJson(JsonObject json) {
        return new UserInputEvent(
            json.stringValue("sessionId", ""),
            json.stringValue("eventType", ""),
            json.stringValue("controlId", ""),
            (float) json.doubleValue("value", 0.0),
            (float) json.doubleValue("x", 0.0),
            (float) json.doubleValue("y", 0.0),
            (float) json.doubleValue("alpha", 0.0),
            (float) json.doubleValue("beta", 0.0),
            (float) json.doubleValue("gamma", 0.0),
            (float) json.doubleValue("ax", 0.0),
            (float) json.doubleValue("ay", 0.0),
            (float) json.doubleValue("az", 0.0),
            (float) json.doubleValue("magnitude", 0.0),
            json.numberValue("timestamp").map(number -> number.longValue()).orElse(0L)
        );
    }
}
