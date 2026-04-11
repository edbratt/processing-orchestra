/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import processing.core.PApplet;
import java.util.HashMap;
import java.util.Map;

public class ProcessingSketch extends PApplet {
    private final EventQueue eventQueue;
    private final AudioBuffer audioBuffer;
    private final int sketchWidth;
    private final int sketchHeight;
    private final DebugConfig debugConfig;
    
    private final Map<String, float[]> userPositions = new HashMap<>();
    private final Map<String, float[]> userColors = new HashMap<>();
    private final Map<String, float[]> userAudioLevels = new HashMap<>();
    
    private float globalAudioLevel = 0;
    private float smoothedGlobalAudioLevel = 0;
    private int audioFrameCount = 0;
    
    public ProcessingSketch(EventQueue eventQueue, AudioBuffer audioBuffer, int width, int height, DebugConfig debugConfig) {
        this.eventQueue = eventQueue;
        this.audioBuffer = audioBuffer;
        this.sketchWidth = width;
        this.sketchHeight = height;
        this.debugConfig = debugConfig;
    }

    @Override
    public void settings() {
        size(sketchWidth, sketchHeight, JAVA2D);
    }

    @Override
    public void setup() {
        surface.setResizable(false);
        surface.setTitle("Processing Server - Multi-User Canvas");
        frameRate(60);
        background(0);
        colorMode(HSB, 360, 100, 100);
        if (debugConfig.isLogging()) {
            println("Processing sketch initialized: " + sketchWidth + "x" + sketchHeight);
        }
    }

    @Override
    public void draw() {
        background(0);
        
        processEvents();
        
        processAudio();
        
        drawUsers();
        
        drawAudioMeter();
    }

    private void processEvents() {
        while (!eventQueue.isEmpty()) {
            UserInputEvent event = eventQueue.poll();
            if (event != null) {
                handleEvent(event);
            }
        }
    }

    private void handleEvent(UserInputEvent event) {
        String sessionId = event.sessionId();
        
        switch (event.eventType()) {
            case "touch" -> {
                if (!userPositions.containsKey(sessionId)) {
                    initializeUser(sessionId);
                }
                float[] pos = userPositions.get(sessionId);
                pos[0] = event.x();
                pos[1] = event.y();
            }
            case "slider" -> {
                if (!userColors.containsKey(sessionId)) {
                    initializeUser(sessionId);
                }
                float[] color = userColors.get(sessionId);
                color[1] = map(event.value(), 0, 1, 20, 100);
            }
            case "button" -> {
                if (!userColors.containsKey(sessionId)) {
                    initializeUser(sessionId);
                }
                float[] color = userColors.get(sessionId);
                color[0] = (color[0] + 30) % 360;
            }
        }
    }

    private void processAudio() {
        globalAudioLevel = 0;
        int totalSessions = 0;
        
        for (String sessionId : audioBuffer.getActiveSessionIds()) {
            float level = calculateAudioLevel(sessionId);
            userAudioLevels.put(sessionId, new float[]{level});
            
            if (!userPositions.containsKey(sessionId)) {
                initializeUser(sessionId);
            }
            
            globalAudioLevel += level;
            totalSessions++;
        }
        
        if (totalSessions > 0) {
            globalAudioLevel /= totalSessions;
        }
        
        smoothedGlobalAudioLevel = lerp(smoothedGlobalAudioLevel, globalAudioLevel, 0.3f);
    }
    
    private void initializeUser(String sessionId) {
        float[] position = findNonOverlappingPosition();
        userPositions.put(sessionId, position);
        userColors.put(sessionId, new float[]{random(360), 70, 100});
        
        if (debugConfig.isLogging()) {
            println("Initialized user " + sessionId.substring(0, 8) + " at position (" 
                    + nf(position[0], 0, 2) + ", " + nf(position[1], 0, 2) + ")");
        }
    }
    
    private float[] findNonOverlappingPosition() {
        float minDistance = 0.15f; // Minimum distance between user centers (15% of canvas)
        int maxAttempts = 50;
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            float x = random(0.1f, 0.9f);
            float y = random(0.15f, 0.85f);
            
            boolean overlaps = false;
            for (float[] existingPos : userPositions.values()) {
                float dx = x - existingPos[0];
                float dy = y - existingPos[1];
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                
                if (distance < minDistance) {
                    overlaps = true;
                    break;
                }
            }
            
            if (!overlaps || attempt == maxAttempts - 1) {
                return new float[]{x, y};
            }
        }
        
        return new float[]{random(0.1f, 0.9f), random(0.15f, 0.85f)};
    }

    private float calculateAudioLevel(String sessionId) {
        byte[] data = audioBuffer.poll(sessionId);
        if (data == null || data.length == 0) {
            float[] existing = userAudioLevels.get(sessionId);
            return existing != null ? existing[0] * 0.9f : 0;
        }
        
        if (debugConfig.isLogging()) {
            audioFrameCount++;
            if (audioFrameCount <= 5) {
                println("Processing audio data: " + data.length + " bytes, session: " + sessionId.substring(0, 8));
            }
        }
        
        float sum = 0;
        int channels = audioBuffer.getChannels();
        int samples = data.length / 2;
        
        for (int i = 0; i < samples; i++) {
            short sample = (short) ((data[i * 2] & 0xFF) | (data[i * 2 + 1] << 8));
            sum += Math.abs(sample) / 32768.0f;
        }
        
        float level = sum / samples;
        return level;
    }

    private void drawUsers() {
        for (Map.Entry<String, float[]> entry : userPositions.entrySet()) {
            String sessionId = entry.getKey();
            float[] pos = entry.getValue();
            float[] color = userColors.getOrDefault(sessionId, new float[]{0, 50, 100});
            float[] audioLevel = userAudioLevels.getOrDefault(sessionId, new float[]{0});
            
            float baseSize = 30;
            float audioScale = 1 + audioLevel[0] * 2;
            float size = baseSize * audioScale;
            
            fill(color[0], color[1], color[2]);
            noStroke();
            ellipse(pos[0] * sketchWidth, pos[1] * sketchHeight, size, size);
            
            float pulseSize = size * 1.5f * audioScale;
            stroke(color[0], color[1] * 0.5f, color[2] * 0.5f);
            strokeWeight(2);
            noFill();
            ellipse(pos[0] * sketchWidth, pos[1] * sketchHeight, pulseSize, pulseSize);
            
            fill(0);
            noStroke();
            textAlign(CENTER, CENTER);
            textSize(10);
            String label = sessionId.substring(0, 4);
            text(label, pos[0] * sketchWidth, pos[1] * sketchHeight);
        }
    }

    private void drawAudioMeter() {
        float meterWidth = sketchWidth * 0.8f;
        float meterHeight = 20;
        float meterX = (sketchWidth - meterWidth) / 2;
        float meterY = sketchHeight - 40;
        
        fill(30, 50, 30);
        noStroke();
        rect(meterX, meterY, meterWidth, meterHeight, 5);
        
        float levelWidth = meterWidth * smoothedGlobalAudioLevel;
        float hue = map(smoothedGlobalAudioLevel, 0, 1, 120, 0);
        fill(hue, 80, 100);
        rect(meterX, meterY, levelWidth, meterHeight, 5);
        
        fill(255);
        textAlign(CENTER, CENTER);
        textSize(12);
        text("Global Audio Level", sketchWidth / 2, meterY - 10);
        
        int activeSessions = audioBuffer.getActiveSessionCount();
        if (activeSessions > 0) {
            textSize(10);
            fill(200);
            text(activeSessions + " audio stream" + (activeSessions != 1 ? "s" : "") + " active", 
                 sketchWidth / 2, meterY + meterHeight + 15);
        }
    }

    public void runSketch() {
        String[] args = {this.getClass().getName()};
        PApplet.runSketch(args, this);
    }
}