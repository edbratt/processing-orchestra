/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private static final String DEFAULT_SKETCH_CLASS = "com.processing.server.ProcessingSketch";
    private static final String OUTPUT_MODE_SKETCH = "sketch";
    private static final String OUTPUT_MODE_OSC = "osc";
    private static final long SESSION_STALE_AFTER_MILLIS = 30000;
    private static final long SESSION_REAPER_INTERVAL_MILLIS = 10000;

    private Main() {
    }

    public static void main(String[] args) {
        LogConfig.configureRuntime();

        Config config = loadConfig();
        Services.set(Config.class, config);

        EventQueue eventQueue = new EventQueue();
        String outputMode = normalizeOutputMode(config.get("output.mode").asString().orElse(OUTPUT_MODE_SKETCH));
        OscOutputConfig oscOutputConfig = loadOscOutputConfig(config, startsOsc(outputMode));
        SessionManager sessionManager = new SessionManager(oscOutputConfig.defaultStreamId());

        AudioConfig audioConfig = loadAudioConfig(config);
        MotionConfig motionConfig = loadMotionConfig(config);
        AudioBuffer audioBuffer = new AudioBuffer(
            config.get("audio.max-buffer-chunks").asInt().orElse(100),
            audioConfig.getSampleRate(),
            audioConfig.getChannels()
        );

        DebugConfig debugConfig = new DebugConfig(
            config.get("debug.logging").asBoolean().orElse(false),
            config.get("audio.debug.logging").asBoolean().orElse(false),
            config.get("audio.debug.sample-limit").asInt().orElse(5)
        );

        int width = config.get("processing.width").asInt().orElse(800);
        int height = config.get("processing.height").asInt().orElse(600);

        String sketchClassName = config.get("processing.sketch-class").asString().orElse(DEFAULT_SKETCH_CLASS);
        ControllerConfig controllerConfig = ControllerConfig.forSketch(sketchClassName);
        if (startsSketch(outputMode)) {
            startSketch(sketchClassName, eventQueue, audioBuffer, width, height, debugConfig, motionConfig);
        }
        ScheduledExecutorService sessionReaper = startSessionReaper(sessionManager, eventQueue, audioBuffer, debugConfig);
        OscRuntime oscRuntime = startsOsc(outputMode)
            ? startOscRuntime(eventQueue, sessionManager, audioBuffer, audioConfig, oscOutputConfig)
            : OscRuntime.disabled();

        InputService inputService = new InputService(
            sessionManager,
            eventQueue,
            audioBuffer,
            controllerConfig,
            oscOutputConfig
        );
        // Attach WebSocket handling to every listener we expose so the browser UI and sketch
        // stay in sync regardless of whether the page was loaded over HTTP or HTTPS.
        var serverBuilder = WebServer.builder()
            .config(config.get("server"))
            .routing(builder -> routing(builder, inputService))
            .addRouting(WsRouting.builder().endpoint("/ws",
                () -> new WebSocketHandler(sessionManager, eventQueue, audioBuffer, audioConfig.getBufferSize(), debugConfig, motionConfig)));

        if (config.get("server.sockets." + TLS_SOCKET_NAME).exists()) {
            serverBuilder.routing(TLS_SOCKET_NAME, builder -> routing(builder, inputService))
                .putSocket(TLS_SOCKET_NAME, socket -> socket
                    .config(config.get("server.sockets." + TLS_SOCKET_NAME))
                    .addRouting(WsRouting.builder().endpoint("/ws",
                        () -> new WebSocketHandler(sessionManager, eventQueue, audioBuffer, audioConfig.getBufferSize(), debugConfig, motionConfig))));
        }

        WebServer server = serverBuilder.build().start();
        Runtime.getRuntime().addShutdownHook(new Thread(
            () -> {
                WebSocketHandler.broadcastShutdown("Processing Server is shutting down.");
                sessionReaper.shutdownNow();
                oscRuntime.close();
            },
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
        System.out.println("Motion config: " + motionConfig.getUpdateHz() + "Hz, clamp beta "
                + motionConfig.getBetaClampDegrees() + "°, gamma "
                + motionConfig.getGammaClampDegrees() + "°, magnitude "
                + motionConfig.getMagnitudeClampG() + "g");
        System.out.println("Sketch class: " + sketchClassName);
        System.out.println("Output mode: " + outputMode);
        if (oscRuntime.enabled()) {
            System.out.println("OSC output: " + oscRuntime.description());
        }
        System.out.println("Debug logging: " + (debugConfig.isLogging() ? "enabled" : "disabled"));
        System.out.println("Audio debug logging: " + (debugConfig.isAudioLogging() ? "enabled" : "disabled"));
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

    private static MotionConfig loadMotionConfig(Config config) {
        return new MotionConfig(
            config.get("motion.update-hz").asInt().orElse(20),
            config.get("motion.clamp.alpha-degrees").asDouble().orElse(180.0).floatValue(),
            config.get("motion.clamp.beta-degrees").asDouble().orElse(60.0).floatValue(),
            config.get("motion.clamp.gamma-degrees").asDouble().orElse(60.0).floatValue(),
            config.get("motion.clamp.acceleration-g").asDouble().orElse(3.0).floatValue(),
            config.get("motion.clamp.magnitude-g").asDouble().orElse(4.0).floatValue(),
            config.get("motion.mapping.tilt-offset-normalized").asDouble().orElse(0.12).floatValue(),
            config.get("motion.mapping.shake-threshold-g").asDouble().orElse(0.6).floatValue(),
            config.get("motion.mapping.shake-burst-scale").asDouble().orElse(1.8).floatValue(),
            config.get("motion.debug.logging").asBoolean().orElse(false),
            config.get("motion.debug.sample-limit").asInt().orElse(5)
        );
    }

    private static OscOutputConfig loadOscOutputConfig(Config config, boolean enabled) {
        OscOutputConfig defaults = OscOutputConfig.defaults(enabled);
        int fps = Math.max(1, config.get("osc.fps").asInt().orElse(defaults.fps()));
        OscDebugConfig oscDebugConfig = new OscDebugConfig(
            config.get("osc.debug.logging").asBoolean().orElse(defaults.debugConfig().logging()),
            config.get("osc.debug.sample-limit").asInt().orElse(defaults.debugConfig().sampleLimit())
        );
        String defaultStreamId = config.get("osc.default-stream").asString().orElse(defaults.defaultStreamId());
        String streamIds = config.get("osc.streams").asString().orElse("");
        if (streamIds.isBlank()) {
            String host = config.get("osc.host").asString().orElse("127.0.0.1");
            int walkerPort = config.get("osc.walker-port").asInt().orElse(12000);
            int vectorscopePort = config.get("osc.vectorscope-port").asInt().orElse(12001);
            boolean mirrorVectorscope = config.get("osc.mirror-vectorscope").asBoolean().orElse(true);
            List<OscStreamConfig> streams = new ArrayList<>();
            streams.add(new OscStreamConfig(defaultStreamId, host, walkerPort, OscStreamConfig.CONTRACT_WALKER_TRIGGER));
            if (mirrorVectorscope) {
                streams.add(new OscStreamConfig(
                    "vectorscope",
                    host,
                    vectorscopePort,
                    OscStreamConfig.CONTRACT_WALKER_MONITOR,
                    defaultStreamId
                ));
            }
            return new OscOutputConfig(enabled, defaultStreamId, List.copyOf(streams), fps, oscDebugConfig);
        }

        List<OscStreamConfig> streams = new ArrayList<>();
        for (String rawId : streamIds.split(",")) {
            String id = rawId.trim();
            if (id.isBlank()) {
                continue;
            }
            String path = "osc.stream." + id + ".";
            streams.add(new OscStreamConfig(
                id,
                config.get(path + "host").asString().orElse("127.0.0.1"),
                config.get(path + "port").asInt().orElse(12000),
                config.get(path + "contract").asString().orElse(OscStreamConfig.CONTRACT_WALKER_TRIGGER),
                config.get(path + "mirror-source").asString().orElse("")
            ));
        }
        if (streams.isEmpty()) {
            streams = defaults.streams();
        }
        return new OscOutputConfig(
            enabled,
            defaultStreamId,
            List.copyOf(streams),
            fps,
            oscDebugConfig
        );
    }

    private static boolean startsSketch(String outputMode) {
        return OUTPUT_MODE_SKETCH.equals(outputMode);
    }

    private static boolean startsOsc(String outputMode) {
        return OUTPUT_MODE_OSC.equals(outputMode);
    }

    private static String normalizeOutputMode(String outputMode) {
        String normalized = outputMode == null ? OUTPUT_MODE_SKETCH : outputMode.trim().toLowerCase();
        if (OUTPUT_MODE_SKETCH.equals(normalized) || OUTPUT_MODE_OSC.equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("Unsupported output.mode: " + outputMode
            + " (expected '" + OUTPUT_MODE_SKETCH + "' or '" + OUTPUT_MODE_OSC + "')");
    }

    private static OscRuntime startOscRuntime(EventQueue eventQueue,
                                              SessionManager sessionManager,
                                              AudioBuffer audioBuffer,
                                              AudioConfig audioConfig,
                                              OscOutputConfig config) {
        OscOutputService oscOutputService = new OscOutputService(config);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "osc-event-pump");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleAtFixedRate(
            new OscEventPump(eventQueue, sessionManager, audioBuffer, audioConfig, oscOutputService, config.defaultStreamId()),
            0,
            Math.max(1, 1000 / config.fps()),
            TimeUnit.MILLISECONDS);
        return new OscRuntime(true, executor, oscOutputService, describeOsc(config));
    }

    private static String describeOsc(OscOutputConfig config) {
        StringBuilder description = new StringBuilder("default=").append(config.defaultStreamId()).append(" streams=");
        for (int i = 0; i < config.streams().size(); i++) {
            OscStreamConfig stream = config.streams().get(i);
            if (i > 0) {
                description.append(", ");
            }
            description.append(stream.id()).append("->").append(stream.host()).append(":").append(stream.port())
                .append("[").append(stream.contract()).append("]");
            if (!stream.mirrorSourceId().isBlank()) {
                description.append("<-").append(stream.mirrorSourceId());
            }
        }
        return description.toString();
    }

    private static void startSketch(String sketchClassName,
                                    EventQueue eventQueue,
                                    AudioBuffer audioBuffer,
                                    int width,
                                    int height,
                                    DebugConfig debugConfig,
                                    MotionConfig motionConfig) {
        try {
            Class<?> sketchClass = Class.forName(sketchClassName);
            if (!processing.core.PApplet.class.isAssignableFrom(sketchClass)) {
                throw new IllegalArgumentException("Sketch class must extend processing.core.PApplet: " + sketchClassName);
            }

            Constructor<?> constructor = sketchClass.getConstructor(
                EventQueue.class,
                AudioBuffer.class,
                int.class,
                int.class,
                DebugConfig.class,
                MotionConfig.class
            );
            Object sketch = constructor.newInstance(eventQueue, audioBuffer, width, height, debugConfig, motionConfig);
            sketchClass.getMethod("runSketch").invoke(sketch);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to start sketch class " + sketchClassName, e);
        }
    }

    private static ScheduledExecutorService startSessionReaper(SessionManager sessionManager,
                                                               EventQueue eventQueue,
                                                               AudioBuffer audioBuffer,
                                                               DebugConfig debugConfig) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "session-reaper");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleAtFixedRate(() -> reapExpiredSessions(sessionManager, eventQueue, audioBuffer, debugConfig),
            SESSION_REAPER_INTERVAL_MILLIS,
            SESSION_REAPER_INTERVAL_MILLIS,
            TimeUnit.MILLISECONDS);
        return executor;
    }

    private static void reapExpiredSessions(SessionManager sessionManager,
                                            EventQueue eventQueue,
                                            AudioBuffer audioBuffer,
                                            DebugConfig debugConfig) {
        long now = System.currentTimeMillis();
        for (SessionManager.SessionInfo session : sessionManager.findExpiredSessions(SESSION_STALE_AFTER_MILLIS, now).values()) {
            sessionManager.removeSession(session.sessionId());
            audioBuffer.clearSession(session.sessionId());
            eventQueue.push(new UserInputEvent(session.sessionId(), "session-ended", "", "", now));
            if (debugConfig.isLogging()) {
                System.out.println("Reaped stale session: " + session.sessionId().substring(0, 8)
                    + " lastSeenAt=" + session.lastSeenAt()
                    + " ageMs=" + (now - session.lastSeenAt()));
                System.out.println("Stale session cleanup completed for " + session.sessionId().substring(0, 8)
                    + " (event queued, session removed, audio cleared)");
            }
        }
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

    private record OscRuntime(boolean enabled,
                              ScheduledExecutorService executor,
                              OscOutputService service,
                              String description) implements AutoCloseable {
        private static OscRuntime disabled() {
            return new OscRuntime(false, null, null, "");
        }

        @Override
        public void close() {
            if (executor != null) {
                executor.shutdownNow();
            }
            if (service != null) {
                service.close();
            }
        }
    }
}
