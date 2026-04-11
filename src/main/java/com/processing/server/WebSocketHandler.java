/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import java.io.StringReader;
import java.util.Collections;

import io.helidon.common.buffers.BufferData;
import io.helidon.websocket.WsListener;
import io.helidon.websocket.WsSession;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

public class WebSocketHandler implements WsListener {
    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());

    private final SessionManager sessionManager;
    private final EventQueue eventQueue;
    private final AudioBuffer audioBuffer;
    private final int bufferSize;
    private final DebugConfig debugConfig;
    private String sessionId;
    private int binaryMessageCount = 0;

    public WebSocketHandler(SessionManager sessionManager, EventQueue eventQueue, AudioBuffer audioBuffer, int bufferSize, DebugConfig debugConfig) {
        this.sessionManager = sessionManager;
        this.eventQueue = eventQueue;
        this.audioBuffer = audioBuffer;
        this.bufferSize = bufferSize;
        this.debugConfig = debugConfig;
    }

    @Override
    public void onOpen(WsSession session) {
        this.sessionId = sessionManager.createSession();
        JsonObject welcome = JSON.createObjectBuilder()
            .add("type", "session")
            .add("sessionId", sessionId)
            .add("sampleRate", audioBuffer.getSampleRate())
            .add("channels", audioBuffer.getChannels())
            .add("bufferSize", bufferSize)
            .build();
        session.send(welcome.toString(), true);
        if (debugConfig.isLogging()) {
            System.out.println("WebSocket opened for session: " + sessionId.substring(0, 8));
        }
    }

    @Override
    public void onMessage(WsSession session, String message, boolean last) {
        try (JsonReader reader = Json.createReader(new StringReader(message))) {
            JsonObject json = reader.readObject();
            String messageType = json.getString("type", "");

            switch (messageType) {
                case "audio-config" -> handleAudioConfig(json, session);
                case "touch", "slider", "button" -> handleControlEvent(json);
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
        JsonObject result = JSON.createObjectBuilder()
            .add("type", "audio-config-ack")
            .add("sampleRate", audioBuffer.getSampleRate())
            .add("channels", audioBuffer.getChannels())
            .build();
        session.send(result.toString(), true);
    }

    private void handleControlEvent(JsonObject json) {
        String eventType = json.getString("type", "");

        float value = getFloat(json, "value", 0.0);
        float x = getFloat(json, "x", 0.0);
        float y = getFloat(json, "y", 0.0);

        UserInputEvent event = new UserInputEvent(
            sessionId,
            eventType,
            json.getString("controlId", ""),
            value,
            x,
            y,
            System.currentTimeMillis()
        );

        eventQueue.push(event);
    }

    private float getFloat(JsonObject json, String key, double defaultValue) {
        try {
            if (json.containsKey(key)) {
                JsonValue val = json.get(key);
                if (val instanceof jakarta.json.JsonNumber num) {
                    return (float) num.doubleValue();
                }
            }
            return (float) defaultValue;
        } catch (Exception e) {
            return (float) defaultValue;
        }
    }

    @Override
    public void onClose(WsSession session, int status, String reason) {
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
        if (sessionId != null) {
            sessionManager.removeSession(sessionId);
            audioBuffer.clearSession(sessionId);
            System.err.println("WebSocket error for session " + sessionId.substring(0, 8) + ": " + t.getMessage());
        }
    }
}