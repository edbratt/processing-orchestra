/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AudioBuffer {
    private final Map<String, ConcurrentLinkedQueue<byte[]>> sessionBuffers = new ConcurrentHashMap<>();
    private final int maxChunksPerSession;
    private final int sampleRate;
    private final int channels;

    public AudioBuffer(int maxChunksPerSession, int sampleRate, int channels) {
        this.maxChunksPerSession = maxChunksPerSession;
        this.sampleRate = sampleRate;
        this.channels = channels;
    }

    public void push(String sessionId, byte[] audioData) {
        sessionBuffers.computeIfAbsent(sessionId, k -> new ConcurrentLinkedQueue<>());
        ConcurrentLinkedQueue<byte[]> queue = sessionBuffers.get(sessionId);
        queue.offer(audioData);

        while (queue.size() > maxChunksPerSession) {
            queue.poll();
        }
    }

    public byte[] poll(String sessionId) {
        ConcurrentLinkedQueue<byte[]> queue = sessionBuffers.get(sessionId);
        return queue != null ? queue.poll() : null;
    }

    public boolean hasData(String sessionId) {
        ConcurrentLinkedQueue<byte[]> queue = sessionBuffers.get(sessionId);
        return queue != null && !queue.isEmpty();
    }

    public int getQueueSize(String sessionId) {
        ConcurrentLinkedQueue<byte[]> queue = sessionBuffers.get(sessionId);
        return queue != null ? queue.size() : 0;
    }

    public void clearSession(String sessionId) {
        sessionBuffers.remove(sessionId);
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getChannels() {
        return channels;
    }

    public int getActiveSessionCount() {
        return sessionBuffers.size();
    }
    
    public java.util.Set<String> getActiveSessionIds() {
        return sessionBuffers.keySet();
    }
}