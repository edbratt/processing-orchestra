/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OscOutputService implements Closeable {
    private final DatagramSocket socket;
    private final Map<String, OscDestination> destinations;
    private final Map<String, List<String>> mirrorStreamIdsBySource;
    private final OscDebugConfig debugConfig;
    private int messageCount = 0;

    public OscOutputService(OscOutputConfig config) {
        try {
            this.socket = new DatagramSocket();
        } catch (SocketException e) {
            throw new IllegalStateException("Failed to create OSC UDP socket", e);
        }

        Map<String, OscDestination> loadedDestinations = new HashMap<>();
        Map<String, List<String>> loadedMirrors = new HashMap<>();
        for (OscStreamConfig stream : config.streams()) {
            try {
                loadedDestinations.put(stream.id(), new OscDestination(
                    stream.id(),
                    InetAddress.getByName(stream.host()),
                    stream.port(),
                    stream.contract()
                ));
                if (!stream.mirrorSourceId().isBlank()) {
                    loadedMirrors.merge(
                        stream.mirrorSourceId(),
                        List.of(stream.id()),
                        (left, right) -> {
                            java.util.ArrayList<String> merged = new java.util.ArrayList<>(left);
                            merged.addAll(right);
                            return List.copyOf(merged);
                        }
                    );
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to resolve OSC host for stream " + stream.id()
                    + ": " + stream.host(), e);
            }
        }
        this.destinations = Map.copyOf(loadedDestinations);
        this.mirrorStreamIdsBySource = Map.copyOf(loadedMirrors);
        this.debugConfig = config.debugConfig();
    }

    public void sendWalker(String streamId, float x, float y) {
        sendWalkerDirect(streamId, x, y);
        for (String mirrorStreamId : mirrorStreamIdsBySource.getOrDefault(streamId, List.of())) {
            sendWalkerDirect(mirrorStreamId, x, y);
        }
    }

    private void sendWalkerDirect(String streamId, float x, float y) {
        OscDestination destination = destinations.get(streamId);
        if (destination == null) {
            return;
        }
        if (OscStreamConfig.CONTRACT_ORCHESTRA_INPUT_V1.equals(destination.contract())) {
            send(destination, "/input/position", x, y);
        } else if (OscStreamConfig.CONTRACT_ORCHESTRA_SESSION_V1.equals(destination.contract())) {
            return;
        } else {
            send(destination, "/walker/signal/x", x);
            send(destination, "/walker/signal/y", y);
        }
    }

    public void sendTrigger(String streamId, String kind, int count) {
        sendTriggerDirect(streamId, kind, count);
        for (String mirrorStreamId : mirrorStreamIdsBySource.getOrDefault(streamId, List.of())) {
            sendTriggerDirect(mirrorStreamId, kind, count);
        }
    }

    private void sendTriggerDirect(String streamId, String kind, int count) {
        OscDestination destination = destinations.get(streamId);
        if (destination == null) {
            return;
        }
        if (OscStreamConfig.CONTRACT_ORCHESTRA_INPUT_V1.equals(destination.contract())) {
            send(destination, "/input/trigger", kind == null || kind.isBlank() ? "ping" : kind, count);
        } else if (OscStreamConfig.CONTRACT_ORCHESTRA_SESSION_V1.equals(destination.contract())) {
            return;
        } else if (OscStreamConfig.CONTRACT_WALKER_MONITOR.equals(destination.contract())) {
            send(destination, "/event/ping", count);
        } else {
            send(destination, "/test/ping", "ping", count);
        }
    }

    public void sendSessionJoin(String streamId, String sessionId, String name, String instrumentId) {
        sendSessionJoinDirect(streamId, sessionId, name, instrumentId);
        for (String mirrorStreamId : mirrorStreamIdsBySource.getOrDefault(streamId, List.of())) {
            sendSessionJoinDirect(mirrorStreamId, sessionId, name, instrumentId);
        }
    }

    private void sendSessionJoinDirect(String streamId, String sessionId, String name, String instrumentId) {
        OscDestination destination = destinations.get(streamId);
        if (destination == null || !OscStreamConfig.CONTRACT_ORCHESTRA_SESSION_V1.equals(destination.contract())) {
            return;
        }
        send(destination, "/session/join", clean(sessionId), clean(name), clean(instrumentId));
    }

    public void sendSessionLeave(String streamId, String sessionId) {
        sendSessionLeaveDirect(streamId, sessionId);
        for (String mirrorStreamId : mirrorStreamIdsBySource.getOrDefault(streamId, List.of())) {
            sendSessionLeaveDirect(mirrorStreamId, sessionId);
        }
    }

    private void sendSessionLeaveDirect(String streamId, String sessionId) {
        OscDestination destination = destinations.get(streamId);
        if (destination == null || !OscStreamConfig.CONTRACT_ORCHESTRA_SESSION_V1.equals(destination.contract())) {
            return;
        }
        send(destination, "/session/leave", clean(sessionId));
    }

    public void sendSessionPosition(String streamId, String sessionId, float x, float y) {
        sendSessionPositionDirect(streamId, sessionId, x, y);
        for (String mirrorStreamId : mirrorStreamIdsBySource.getOrDefault(streamId, List.of())) {
            sendSessionPositionDirect(mirrorStreamId, sessionId, x, y);
        }
    }

    private void sendSessionPositionDirect(String streamId, String sessionId, float x, float y) {
        OscDestination destination = destinations.get(streamId);
        if (destination == null || !OscStreamConfig.CONTRACT_ORCHESTRA_SESSION_V1.equals(destination.contract())) {
            return;
        }
        send(destination, "/session/position", clean(sessionId), x, y);
    }

    public void sendSessionTrigger(String streamId, String sessionId, String kind, int count) {
        sendSessionTriggerDirect(streamId, sessionId, kind, count);
        for (String mirrorStreamId : mirrorStreamIdsBySource.getOrDefault(streamId, List.of())) {
            sendSessionTriggerDirect(mirrorStreamId, sessionId, kind, count);
        }
    }

    private void sendSessionTriggerDirect(String streamId, String sessionId, String kind, int count) {
        OscDestination destination = destinations.get(streamId);
        if (destination == null || !OscStreamConfig.CONTRACT_ORCHESTRA_SESSION_V1.equals(destination.contract())) {
            return;
        }
        send(destination, "/session/trigger", clean(sessionId), clean(kind == null || kind.isBlank() ? "ping" : kind), count);
    }

    public void sendPitch(String streamId, String note, int midiNote, float frequencyHz, float level) {
        sendPitchDirect(streamId, note, midiNote, frequencyHz, level);
        for (String mirrorStreamId : mirrorStreamIdsBySource.getOrDefault(streamId, List.of())) {
            sendPitchDirect(mirrorStreamId, note, midiNote, frequencyHz, level);
        }
    }

    private void sendPitchDirect(String streamId, String note, int midiNote, float frequencyHz, float level) {
        OscDestination destination = destinations.get(streamId);
        if (destination == null || !OscStreamConfig.CONTRACT_ORCHESTRA_INPUT_V1.equals(destination.contract())) {
            return;
        }
        send(destination, "/input/pitch", clean(note), midiNote, frequencyHz, level);
    }

    public void sendSessionPitch(String streamId, String sessionId, String note, int midiNote, float frequencyHz, float level) {
        sendSessionPitchDirect(streamId, sessionId, note, midiNote, frequencyHz, level);
        for (String mirrorStreamId : mirrorStreamIdsBySource.getOrDefault(streamId, List.of())) {
            sendSessionPitchDirect(mirrorStreamId, sessionId, note, midiNote, frequencyHz, level);
        }
    }

    private void sendSessionPitchDirect(String streamId, String sessionId, String note, int midiNote, float frequencyHz, float level) {
        OscDestination destination = destinations.get(streamId);
        if (destination == null || !OscStreamConfig.CONTRACT_ORCHESTRA_SESSION_V1.equals(destination.contract())) {
            return;
        }
        send(destination, "/session/pitch", clean(sessionId), clean(note), midiNote, frequencyHz, level);
    }

    private String clean(String value) {
        return value == null ? "" : value;
    }

    private void send(OscDestination destination, String address, Object... args) {
        byte[] data = OscMessageEncoder.encode(address, args);
        DatagramPacket packet = new DatagramPacket(data, data.length, destination.host(), destination.port());
        try {
            socket.send(packet);
            logSent(destination, address, args);
        } catch (IOException e) {
            System.err.println("OSC send failed for " + address + " to "
                + destination.host().getHostAddress() + ":" + destination.port() + ": " + e.getMessage());
        }
    }

    private void logSent(OscDestination destination, String address, Object[] args) {
        messageCount++;
        if (!debugConfig.shouldLog(messageCount)) {
            return;
        }
        System.out.println("OSC -> " + destination.id() + " "
            + destination.host().getHostAddress() + ":" + destination.port()
            + " " + address + formatArgs(args));
        if (debugConfig.logging() && debugConfig.sampleLimit() > 0 && messageCount == debugConfig.sampleLimit()) {
            System.out.println("OSC debug sample limit reached (" + debugConfig.sampleLimit()
                + "); suppressing further OSC success logs.");
        }
    }

    private String formatArgs(Object[] args) {
        if (args.length == 0) {
            return "";
        }
        StringBuilder formatted = new StringBuilder();
        for (Object arg : args) {
            formatted.append(' ');
            if (arg instanceof String value) {
                formatted.append('"').append(value).append('"');
            } else {
                formatted.append(arg);
            }
        }
        return formatted.toString();
    }

    @Override
    public void close() {
        socket.close();
    }

    private record OscDestination(String id, InetAddress host, int port, String contract) {
    }
}
