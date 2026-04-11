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
        sessions.put(sessionId, new SessionInfo(sessionId, System.currentTimeMillis()));
        return sessionId;
    }

    public boolean isActive(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }

    public record SessionInfo(String sessionId, long createdAt) {}
}