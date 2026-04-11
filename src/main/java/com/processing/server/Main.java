/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import io.helidon.common.configurable.Resource;
import io.helidon.config.Config;
import io.helidon.logging.common.LogConfig;
import io.helidon.service.registry.Services;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.staticcontent.StaticContentService;
import io.helidon.webserver.websocket.WsRouting;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        LogConfig.configureRuntime();

        Config config = Config.create();
        Services.set(Config.class, config);

        EventQueue eventQueue = new EventQueue();
        SessionManager sessionManager = new SessionManager();

        AudioConfig audioConfig = loadAudioConfig(config);
        AudioBuffer audioBuffer = new AudioBuffer(
            config.get("audio.max-buffer-chunks").asInt().orElse(100),
            audioConfig.getSampleRate(),
            audioConfig.getChannels()
        );
        
        DebugConfig debugConfig = new DebugConfig(
            config.get("debug.logging").asBoolean().orElse(false)
        );

        int width = config.get("processing.width").asInt().orElse(800);
        int height = config.get("processing.height").asInt().orElse(600);

        ProcessingSketch sketch = new ProcessingSketch(eventQueue, audioBuffer, width, height, debugConfig);
        sketch.runSketch();

        InputService inputService = new InputService(sessionManager, eventQueue);
        WebSocketHandler wsHandler = new WebSocketHandler(sessionManager, eventQueue, audioBuffer, audioConfig.getBufferSize(), debugConfig);

        WebServer server = WebServer.builder()
                .config(config.get("server"))
                .tls(tls -> tls
                    .privateKey(key -> key
                        .keystore(store -> store
                            .passphrase("changeit")
                            .keystore(Resource.create("keystore.p12"))))
                    .privateKeyCertChain(key -> key
                        .keystore(store -> store
                            .passphrase("changeit")
                            .keystore(Resource.create("keystore.p12")))))
                .routing(builder -> routing(builder, inputService))
                .addRouting(WsRouting.builder().endpoint("/ws", () -> wsHandler))
                .build()
                .start();

        String protocol = server.hasTls() ? "https" : "http";
        String wsProtocol = server.hasTls() ? "wss" : "ws";
        System.out.println("Server started at " + protocol + "://localhost:" + server.port());
        System.out.println("WebSocket endpoint: " + wsProtocol + "://localhost:" + server.port() + "/ws");
        System.out.println("REST API: " + protocol + "://localhost:" + server.port() + "/api/");
        System.out.println("UI: " + protocol + "://localhost:" + server.port() + "/");
        if (server.hasTls()) {
            System.out.println("TLS enabled - connect from mobile at https://192.168.50.95:" + server.port());
        }
        System.out.println("Audio config: " + audioConfig.getSampleRate() + "Hz, " + 
                          audioConfig.getChannels() + " channel(s), buffer " + 
                          audioConfig.getBufferSize() + " samples - " + audioConfig.getDescription());
        System.out.println("Debug logging: " + (debugConfig.isLogging() ? "enabled" : "disabled"));
    }

    private static AudioConfig loadAudioConfig(Config config) {
        String mode = config.get("audio.mode").asString().orElse("high-quality-stereo");
        String basePath = "audio.modes." + mode;

        int sampleRate = config.get(basePath + ".sample-rate").asInt().orElse(44100);
        int channels = config.get(basePath + ".channels").asInt().orElse(2);
        int bufferSize = config.get("audio.buffer-size").asInt().orElse(4096);
        String description = config.get(basePath + ".description").asString().orElse(mode);

        return new AudioConfig(sampleRate, channels, bufferSize, description);
    }

    static void routing(HttpRouting.Builder builder, InputService inputService) {
        builder
            .register("/api", inputService)
            .register(StaticContentService.create("static"));
    }
}