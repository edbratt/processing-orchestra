/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    private final String defaultStreamId;

    public SessionManager() {
        this("spiral-main");
    }

    public SessionManager(String defaultStreamId) {
        this.defaultStreamId = sanitizeId(defaultStreamId, "spiral-main");
    }

    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        sessions.put(sessionId, new SessionInfo(sessionId, now, "", "", defaultStreamId, now));
        return sessionId;
    }

    public boolean isActive(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }

    public SessionInfo getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public void updateSessionName(String sessionId, String name) {
        sessions.computeIfPresent(sessionId, (id, info) -> info.withName(sanitizeName(name)));
    }

    public void updateSessionMetadata(String sessionId, String name, String instrumentId, String streamId) {
        sessions.computeIfPresent(sessionId, (id, info) -> info.withMetadata(
            sanitizeName(name),
            sanitizeId(instrumentId, info.instrumentId()),
            sanitizeId(streamId, info.streamId())
        ));
    }

    public void touchSession(String sessionId) {
        sessions.computeIfPresent(sessionId, (id, info) -> info.withLastSeenAt(System.currentTimeMillis()));
    }

    public Map<String, SessionInfo> findExpiredSessions(long staleAfterMillis, long now) {
        return sessions.entrySet().stream()
            .filter(entry -> now - entry.getValue().lastSeenAt() > staleAfterMillis)
            .collect(ConcurrentHashMap::new,
                (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                ConcurrentHashMap::putAll);
    }

    public Map<String, SessionInfo> snapshotSessions() {
        return Map.copyOf(sessions);
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }

    private String sanitizeName(String name) {
        if (name == null) {
            return "";
        }
        String trimmed = name.trim();
        return trimmed.length() > 40 ? trimmed.substring(0, 40) : trimmed;
    }

    private String sanitizeId(String id, String fallback) {
        if (id == null || id.isBlank()) {
            return fallback == null ? "" : fallback;
        }
        String trimmed = id.trim();
        StringBuilder sanitized = new StringBuilder();
        for (int i = 0; i < trimmed.length() && sanitized.length() < 64; i++) {
            char ch = trimmed.charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == '-' || ch == '_' || ch == '.') {
                sanitized.append(ch);
            }
        }
        return sanitized.isEmpty() ? (fallback == null ? "" : fallback) : sanitized.toString();
    }

    public record SessionInfo(String sessionId,
                              long createdAt,
                              String name,
                              String instrumentId,
                              String streamId,
                              long lastSeenAt) {
        SessionInfo withName(String updatedName) {
            return new SessionInfo(sessionId, createdAt, updatedName, instrumentId, streamId, System.currentTimeMillis());
        }

        SessionInfo withMetadata(String updatedName, String updatedInstrumentId, String updatedStreamId) {
            return new SessionInfo(
                sessionId,
                createdAt,
                updatedName,
                updatedInstrumentId,
                updatedStreamId,
                System.currentTimeMillis()
            );
        }

        SessionInfo withLastSeenAt(long updatedLastSeenAt) {
            return new SessionInfo(sessionId, createdAt, name, instrumentId, streamId, updatedLastSeenAt);
        }
    }
}
