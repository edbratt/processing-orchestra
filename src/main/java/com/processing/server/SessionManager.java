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

    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        sessions.put(sessionId, new SessionInfo(sessionId, now, "", now));
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

    public record SessionInfo(String sessionId, long createdAt, String name, long lastSeenAt) {
        SessionInfo withName(String updatedName) {
            return new SessionInfo(sessionId, createdAt, updatedName, System.currentTimeMillis());
        }

        SessionInfo withLastSeenAt(long updatedLastSeenAt) {
            return new SessionInfo(sessionId, createdAt, name, updatedLastSeenAt);
        }
    }
}
