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
    private final OscOutputConfig oscOutputConfig;

    public InputService(SessionManager sessionManager, EventQueue eventQueue) {
        this.sessionManager = sessionManager;
        this.eventQueue = eventQueue;
        this.audioBuffer = null;
        this.controllerConfig = ControllerConfig.forSketch("com.processing.server.ProcessingSketch");
        this.oscOutputConfig = OscOutputConfig.defaults(false);
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
        this.oscOutputConfig = OscOutputConfig.defaults(false);
    }

    public InputService(SessionManager sessionManager,
                        EventQueue eventQueue,
                        AudioBuffer audioBuffer,
                        ControllerConfig controllerConfig,
                        OscOutputConfig oscOutputConfig) {
        this.sessionManager = sessionManager;
        this.eventQueue = eventQueue;
        this.audioBuffer = audioBuffer;
        this.controllerConfig = controllerConfig;
        this.oscOutputConfig = oscOutputConfig;
    }

    @Override
    public void routing(HttpRules rules) {
        rules
            .post("/event", this::handleEvent)
            .post("/session", this::createSession)
            .delete("/session/{id}", this::removeSession)
            .get("/controller", this::getController)
            .get("/orchestra", this::getOrchestra)
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

    private void getOrchestra(ServerRequest req, ServerResponse res) {
        var streams = new ArrayList<JsonValue>();
        for (OscStreamConfig stream : oscOutputConfig.streams()) {
            streams.add(JsonValue.objectBuilder()
                .set("id", stream.id())
                .set("label", stream.id() + " (" + stream.contract() + " : " + stream.port() + ")")
                .set("host", stream.host())
                .set("port", stream.port())
                .set("contract", stream.contract())
                .build());
        }

        var instruments = new ArrayList<JsonValue>();
        instruments.add(instrument(
            "full-orchestra",
            "Full Orchestra",
            "Touch, buttons, keyboard, motion, and audio clap pings.",
            true,
            true,
            true,
            true,
            true,
            true,
            true
        ));
        instruments.add(instrument(
            "touch-walker",
            "Touch Walker",
            "Touch controls walker XY; buttons and Space send pings.",
            true,
            false,
            false,
            true,
            false,
            false,
            true
        ));
        instruments.add(instrument(
            "audio-clap",
            "Audio Clap",
            "Browser audio claps send pings.",
            false,
            false,
            false,
            false,
            true,
            false,
            false
        ));
        instruments.add(instrument(
            "tilt-walker",
            "Tilt Walker",
            "Phone tilt controls walker XY.",
            false,
            false,
            false,
            false,
            false,
            true,
            false
        ));
        instruments.add(instrument(
            "shake-ping",
            "Shake Ping",
            "Phone shake sends pings.",
            false,
            false,
            false,
            false,
            false,
            true,
            false
        ));
        instruments.add(instrument(
            "motion-orchestra",
            "Motion Orchestra",
            "Phone tilt controls walker XY; shake sends pings.",
            false,
            false,
            false,
            false,
            false,
            true,
            false
        ));

        JsonObject result = JsonValue.objectBuilder()
            .set("defaultStreamId", oscOutputConfig.defaultStreamId())
            .set("defaultInstrumentId", "full-orchestra")
            .set("streams", JsonArray.create(streams))
            .set("instruments", JsonArray.create(instruments))
            .build();
        res.status(Status.OK_200).send(result);
    }

    private JsonValue instrument(String id,
                                 String label,
                                 String description,
                                 boolean touch,
                                 boolean sizeSlider,
                                 boolean speedSlider,
                                 boolean actionButtons,
                                 boolean audio,
                                 boolean motion,
                                 boolean keyboard) {
        JsonObject features = JsonValue.objectBuilder()
            .set("touch", touch)
            .set("sizeSlider", sizeSlider)
            .set("speedSlider", speedSlider)
            .set("actionButtons", actionButtons)
            .set("audio", audio)
            .set("motion", motion)
            .set("keyboard", keyboard)
            .build();

        return JsonValue.objectBuilder()
            .set("id", id)
            .set("label", label)
            .set("description", description)
            .set("features", features)
            .build();
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
                .set("instrumentId", session.instrumentId())
                .set("streamId", session.streamId())
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
