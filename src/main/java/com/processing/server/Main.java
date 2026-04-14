/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import java.util.List;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.logging.common.LogConfig;
import io.helidon.service.registry.Services;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.websocket.WsRouting;

public final class Main {
    private static final String APP_CONFIG_PROPERTY = "app.config";
    private static final String DEFAULT_CONFIG_RESOURCE = "application.yaml";
    private static final String TLS_SOCKET_NAME = "tls";

    private Main() {
    }

    public static void main(String[] args) {
        LogConfig.configureRuntime();

        Config config = loadConfig();
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

        InputService inputService = new InputService(sessionManager, eventQueue, audioBuffer);
        // Attach WebSocket handling to every listener we expose so the browser UI and sketch
        // stay in sync regardless of whether the page was loaded over HTTP or HTTPS.
        var serverBuilder = WebServer.builder()
            .config(config.get("server"))
            .routing(builder -> routing(builder, inputService))
            .addRouting(WsRouting.builder().endpoint("/ws",
                () -> new WebSocketHandler(sessionManager, eventQueue, audioBuffer, audioConfig.getBufferSize(), debugConfig)));

        if (config.get("server.sockets." + TLS_SOCKET_NAME).exists()) {
            serverBuilder.routing(TLS_SOCKET_NAME, builder -> routing(builder, inputService))
                .putSocket(TLS_SOCKET_NAME, socket -> socket
                    .config(config.get("server.sockets." + TLS_SOCKET_NAME))
                    .addRouting(WsRouting.builder().endpoint("/ws",
                        () -> new WebSocketHandler(sessionManager, eventQueue, audioBuffer, audioConfig.getBufferSize(), debugConfig))));
        }

        WebServer server = serverBuilder.build().start();
        Runtime.getRuntime().addShutdownHook(new Thread(
            () -> WebSocketHandler.broadcastShutdown("Processing Server is shutting down."),
            "processing-server-shutdown"));

        logSocket(server, WebServer.DEFAULT_SOCKET_NAME, "Local HTTP", "localhost");
        if (config.get("server.sockets." + TLS_SOCKET_NAME).exists()) {
            logSocket(server, TLS_SOCKET_NAME, "LAN HTTPS", "localhost");
            System.out.println("LAN HTTPS can also be reached with your machine hostname or LAN IP if covered by the certificate.");
        } else {
            System.out.println("LAN HTTPS: disabled");
            System.out.println("To enable HTTPS for other devices, generate keystore.p12 and run with -Dapp.config=<https-config-file> or use the HTTPS launch script.");
        }
        System.out.println("Audio config: " + audioConfig.getSampleRate() + "Hz, "
                + audioConfig.getChannels() + " channel(s), buffer "
                + audioConfig.getBufferSize() + " samples - " + audioConfig.getDescription());
        System.out.println("Debug logging: " + (debugConfig.isLogging() ? "enabled" : "disabled"));
    }

    private static Config loadConfig() {
        String externalConfigPath = System.getProperty(APP_CONFIG_PROPERTY);
        if (externalConfigPath == null || externalConfigPath.isBlank()) {
            return Config.create();
        }

        return Config.builder()
            .sources(List.of(
                () -> ConfigSources.environmentVariables(),
                () -> ConfigSources.systemProperties().build(),
                () -> ConfigSources.file(externalConfigPath).build(),
                () -> ConfigSources.classpath(DEFAULT_CONFIG_RESOURCE).build()))
            .build();
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
        builder.register("/api", inputService);
    }

    private static void logSocket(WebServer server, String socketName, String label, String displayHost) {
        boolean tlsEnabled = WebServer.DEFAULT_SOCKET_NAME.equals(socketName)
            ? server.hasTls()
            : server.hasTls(socketName);
        int port = WebServer.DEFAULT_SOCKET_NAME.equals(socketName)
            ? server.port()
            : server.port(socketName);
        String httpProtocol = tlsEnabled ? "https" : "http";
        String wsProtocol = tlsEnabled ? "wss" : "ws";

        System.out.println(label + " UI: " + httpProtocol + "://" + displayHost + ":" + port + "/");
        System.out.println(label + " WebSocket: " + wsProtocol + "://" + displayHost + ":" + port + "/ws");
        System.out.println(label + " REST API: " + httpProtocol + "://" + displayHost + ":" + port + "/api/");
    }
}
