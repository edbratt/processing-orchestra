/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import java.util.Collections;

import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;

public class InputService implements HttpService {
    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());
    
    private final SessionManager sessionManager;
    private final EventQueue eventQueue;
    private final AudioBuffer audioBuffer;

    public InputService(SessionManager sessionManager, EventQueue eventQueue) {
        this.sessionManager = sessionManager;
        this.eventQueue = eventQueue;
        this.audioBuffer = null;
    }

    public InputService(SessionManager sessionManager, EventQueue eventQueue, AudioBuffer audioBuffer) {
        this.sessionManager = sessionManager;
        this.eventQueue = eventQueue;
        this.audioBuffer = audioBuffer;
    }

    @Override
    public void routing(HttpRules rules) {
        rules
            .post("/event", this::handleEvent)
            .post("/session", this::createSession)
            .delete("/session/{id}", this::removeSession)
            .get("/status", this::getStatus);
    }

    private void handleEvent(ServerRequest req, ServerResponse res) {
        JsonObject json = req.content().as(JsonObject.class);
        UserInputEvent event = UserInputEvent.fromJson(json);
        if (sessionManager.isActive(event.sessionId())) {
            eventQueue.push(event);
            res.status(Status.OK_200).send("{\"status\":\"accepted\"}");
        } else {
            JsonObject error = JSON.createObjectBuilder()
                .add("error", "invalid session")
                .build();
            res.status(Status.UNAUTHORIZED_401).send(error);
        }
    }

    private void createSession(ServerRequest req, ServerResponse res) {
        String sessionId = sessionManager.createSession();
        JsonObject result = JSON.createObjectBuilder()
            .add("sessionId", sessionId)
            .build();
        res.status(Status.CREATED_201).send(result);
    }

    private void removeSession(ServerRequest req, ServerResponse res) {
        String sessionId = req.path().pathParameters().get("id");
        if (sessionId != null && sessionManager.isActive(sessionId)) {
            sessionManager.removeSession(sessionId);
            res.status(Status.NO_CONTENT_204).send();
        } else {
            res.status(Status.NOT_FOUND_404).send();
        }
    }

    private void getStatus(ServerRequest req, ServerResponse res) {
        var builder = JSON.createObjectBuilder()
            .add("activeSessions", sessionManager.getActiveSessionCount())
            .add("queueSize", eventQueue.size());
        
        if (audioBuffer != null) {
            builder.add("audioStreams", audioBuffer.getActiveSessionCount())
                   .add("audioSampleRate", audioBuffer.getSampleRate())
                   .add("audioChannels", audioBuffer.getChannels());
        }
        
        res.status(Status.OK_200).send(builder.build());
    }
}