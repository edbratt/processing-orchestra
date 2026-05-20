/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import io.helidon.http.Status;
import io.helidon.json.JsonArray;
import io.helidon.json.JsonObject;
import io.helidon.json.JsonValue;
import java.util.ArrayList;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

public class InputService implements HttpService {
    private final SessionManager sessionManager;
    private final EventQueue eventQueue;
    private final AudioBuffer audioBuffer;
    private final ControllerConfig controllerConfig;

    public InputService(SessionManager sessionManager, EventQueue eventQueue) {
        this.sessionManager = sessionManager;
        this.eventQueue = eventQueue;
        this.audioBuffer = null;
        this.controllerConfig = ControllerConfig.forSketch("com.processing.server.ProcessingSketch");
    }

    public InputService(SessionManager sessionManager, EventQueue eventQueue, AudioBuffer audioBuffer) {
        this(sessionManager, eventQueue, audioBuffer, ControllerConfig.forSketch("com.processing.server.ProcessingSketch"));
    }

    public InputService(SessionManager sessionManager,
                        EventQueue eventQueue,
                        AudioBuffer audioBuffer,
                        ControllerConfig controllerConfig) {
        this.sessionManager = sessionManager;
        this.eventQueue = eventQueue;
        this.audioBuffer = audioBuffer;
        this.controllerConfig = controllerConfig;
    }

    @Override
    public void routing(HttpRules rules) {
        rules
            .post("/event", this::handleEvent)
            .post("/session", this::createSession)
            .delete("/session/{id}", this::removeSession)
            .get("/controller", this::getController)
            .get("/status", this::getStatus);
    }

    private void handleEvent(ServerRequest req, ServerResponse res) {
        JsonObject json = req.content().as(JsonObject.class);
        UserInputEvent event = UserInputEvent.fromJson(json);
        if (sessionManager.isActive(event.sessionId())) {
            eventQueue.push(event);
            res.status(Status.OK_200).send(JsonValue.objectBuilder()
                .set("status", "accepted")
                .build());
        } else {
            JsonObject error = JsonValue.objectBuilder()
                .set("error", "invalid session")
                .build();
            res.status(Status.UNAUTHORIZED_401).send(error);
        }
    }

    private void createSession(ServerRequest req, ServerResponse res) {
        String sessionId = sessionManager.createSession();
        JsonObject result = JsonValue.objectBuilder()
            .set("sessionId", sessionId)
            .build();
        res.status(Status.CREATED_201).send(result);
    }

    private void removeSession(ServerRequest req, ServerResponse res) {
        String sessionId = req.path().pathParameters().get("id");
        if (sessionId != null && sessionManager.isActive(sessionId)) {
            sessionManager.removeSession(sessionId);
            if (audioBuffer != null) {
                audioBuffer.clearSession(sessionId);
            }
            eventQueue.push(new UserInputEvent(sessionId, "session-ended", "", "", System.currentTimeMillis()));
            res.status(Status.NO_CONTENT_204).send();
        } else {
            res.status(Status.NOT_FOUND_404).send();
        }
    }

    private void getController(ServerRequest req, ServerResponse res) {
        res.status(Status.OK_200).send(controllerConfig.toJson());
    }

    private void getStatus(ServerRequest req, ServerResponse res) {
        var builder = JsonValue.objectBuilder()
            .set("activeSessions", sessionManager.getActiveSessionCount())
            .set("queueSize", eventQueue.size());

        var sessions = new ArrayList<JsonValue>();
        for (SessionManager.SessionInfo session : sessionManager.snapshotSessions().values()) {
            sessions.add(JsonValue.objectBuilder()
                .set("sessionId", session.sessionId())
                .set("createdAt", session.createdAt())
                .set("name", session.name())
                .set("lastSeenAt", session.lastSeenAt())
                .build());
        }
        builder.set("sessions", JsonArray.create(sessions));
        
        if (audioBuffer != null) {
            builder.set("audioStreams", audioBuffer.getActiveSessionCount())
                   .set("audioSampleRate", audioBuffer.getSampleRate())
                   .set("audioChannels", audioBuffer.getChannels());
        }
        
        res.status(Status.OK_200).send(builder.build());
    }
}
