/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.helidon.common.buffers.BufferData;
import io.helidon.json.JsonObject;
import io.helidon.json.JsonParser;
import io.helidon.json.JsonValue;
import io.helidon.websocket.WsCloseCodes;
import io.helidon.websocket.WsListener;
import io.helidon.websocket.WsSession;

public class WebSocketHandler implements WsListener {
    private static final Set<WsSession> OPEN_SESSIONS = ConcurrentHashMap.newKeySet();

    private final SessionManager sessionManager;
    private final EventQueue eventQueue;
    private final AudioBuffer audioBuffer;
    private final int bufferSize;
    private final DebugConfig debugConfig;
    private final MotionConfig motionConfig;
    private String sessionId;
    private int binaryMessageCount = 0;
    private int motionMessageCount = 0;

    public WebSocketHandler(SessionManager sessionManager,
                            EventQueue eventQueue,
                            AudioBuffer audioBuffer,
                            int bufferSize,
                            DebugConfig debugConfig,
                            MotionConfig motionConfig) {
        this.sessionManager = sessionManager;
        this.eventQueue = eventQueue;
        this.audioBuffer = audioBuffer;
        this.bufferSize = bufferSize;
        this.debugConfig = debugConfig;
        this.motionConfig = motionConfig;
    }

    @Override
    public void onOpen(WsSession session) {
        OPEN_SESSIONS.add(session);
        this.sessionId = sessionManager.createSession();
        JsonObject welcome = JsonValue.objectBuilder()
            .set("type", "session")
            .set("sessionId", sessionId)
            .set("sampleRate", audioBuffer.getSampleRate())
            .set("channels", audioBuffer.getChannels())
            .set("bufferSize", bufferSize)
            .set("motionUpdateHz", motionConfig.getUpdateHz())
            .set("motionBetaClampDegrees", motionConfig.getBetaClampDegrees())
            .set("motionGammaClampDegrees", motionConfig.getGammaClampDegrees())
            .set("motionAlphaClampDegrees", motionConfig.getAlphaClampDegrees())
            .set("motionAccelerationClampG", motionConfig.getAccelerationClampG())
            .set("motionMagnitudeClampG", motionConfig.getMagnitudeClampG())
            .build();
        session.send(welcome.toString(), true);
        if (debugConfig.isLogging()) {
            System.out.println("WebSocket opened for session: " + sessionId.substring(0, 8));
        }
    }

    @Override
    public void onMessage(WsSession session, String message, boolean last) {
        try {
            JsonObject json = JsonParser.create(message).readJsonObject();
            String messageType = json.stringValue("type", "");

            switch (messageType) {
                case "audio-config" -> handleAudioConfig(json, session);
                case "touch", "slider", "button" -> handleControlEvent(json);
                case "motion" -> handleMotionEvent(json);
                default -> session.send("{\"error\":\"unknown message type\"}", true);
            }
        } catch (Exception e) {
            session.send("{\"error\":\"invalid message\"}", true);
        }
    }

    @Override
    public void onMessage(WsSession session, BufferData buffer, boolean last) {
        if (sessionId == null) return;
        
        try {
            byte[] audioData = buffer.readBytes();
            audioBuffer.push(sessionId, audioData);
            
            if (debugConfig.isLogging()) {
                binaryMessageCount++;
                if (binaryMessageCount <= 5) {
                    System.out.println("Binary audio received: " + audioData.length + " bytes, session: " + sessionId.substring(0, 8));
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing binary audio data: " + e.getMessage());
        }
    }

    private void handleAudioConfig(JsonObject json, WsSession session) {
        JsonObject result = JsonValue.objectBuilder()
            .set("type", "audio-config-ack")
            .set("sampleRate", audioBuffer.getSampleRate())
            .set("channels", audioBuffer.getChannels())
            .build();
        session.send(result.toString(), true);
    }

    private void handleControlEvent(JsonObject json) {
        String eventType = json.stringValue("type", "");

        float value = getFloat(json, "value", 0.0);
        float x = getFloat(json, "x", 0.0);
        float y = getFloat(json, "y", 0.0);

        UserInputEvent event = new UserInputEvent(
            sessionId,
            eventType,
            json.stringValue("controlId", ""),
            value,
            x,
            y,
            System.currentTimeMillis()
        );

        eventQueue.push(event);
    }

    private void handleMotionEvent(JsonObject json) {
        float alpha = clampSignedAngle(getFloat(json, "alpha", 0.0), motionConfig.getAlphaClampDegrees());
        float beta = clampSignedAngle(getFloat(json, "beta", 0.0), motionConfig.getBetaClampDegrees());
        float gamma = clampSignedAngle(getFloat(json, "gamma", 0.0), motionConfig.getGammaClampDegrees());
        float ax = clampSignedValue(getFloat(json, "ax", 0.0), motionConfig.getAccelerationClampG());
        float ay = clampSignedValue(getFloat(json, "ay", 0.0), motionConfig.getAccelerationClampG());
        float az = clampSignedValue(getFloat(json, "az", 0.0), motionConfig.getAccelerationClampG());
        float magnitude = clampUnsignedValue(getFloat(json, "magnitude", 0.0), motionConfig.getMagnitudeClampG());
        long timestamp = json.numberValue("timestamp").map(number -> number.longValue()).orElse(System.currentTimeMillis());

        UserInputEvent event = new UserInputEvent(
            sessionId,
            "motion",
            json.stringValue("controlId", "deviceMotion"),
            0f,
            0f,
            0f,
            alpha,
            beta,
            gamma,
            ax,
            ay,
            az,
            magnitude,
            timestamp
        );

        eventQueue.push(event);

        if (motionConfig.isDebugLogging() && sessionId != null) {
            motionMessageCount++;
            if (motionMessageCount <= motionConfig.getDebugSampleLimit()) {
                System.out.println("Motion event " + motionMessageCount
                        + " for session " + sessionId.substring(0, 8)
                        + ": beta=" + beta
                        + ", gamma=" + gamma
                        + ", magnitude=" + magnitude);
            }
        }
    }

    private float getFloat(JsonObject json, String key, double defaultValue) {
        try {
            return (float) json.doubleValue(key, defaultValue);
        } catch (Exception e) {
            return (float) defaultValue;
        }
    }

    private float clampSignedAngle(float value, float maxAbsDegrees) {
        return clampSignedValue(value, maxAbsDegrees);
    }

    private float clampSignedValue(float value, float maxAbs) {
        return Math.max(-maxAbs, Math.min(maxAbs, value));
    }

    private float clampUnsignedValue(float value, float maxValue) {
        return Math.max(0f, Math.min(maxValue, value));
    }

    @Override
    public void onClose(WsSession session, int status, String reason) {
        OPEN_SESSIONS.remove(session);
        if (sessionId != null) {
            sessionManager.removeSession(sessionId);
            audioBuffer.clearSession(sessionId);
            if (debugConfig.isLogging()) {
                System.out.println("WebSocket closed for session: " + sessionId.substring(0, 8));
            }
        }
    }

    @Override
    public void onError(WsSession session, Throwable t) {
        OPEN_SESSIONS.remove(session);
        if (sessionId != null) {
            sessionManager.removeSession(sessionId);
            audioBuffer.clearSession(sessionId);
            System.err.println("WebSocket error for session " + sessionId.substring(0, 8) + ": " + t.getMessage());
        }
    }

    public static void broadcastShutdown(String reason) {
        JsonObject message = JsonValue.objectBuilder()
            .set("type", "server-shutdown")
            .set("reason", reason)
            .build();

        for (WsSession session : OPEN_SESSIONS) {
            try {
                session.send(message.toString(), true)
                    .close(WsCloseCodes.GOING_AWAY, reason);
            } catch (Exception ignored) {
                session.terminate();
            }
        }
        OPEN_SESSIONS.clear();
    }
}
