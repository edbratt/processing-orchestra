/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import processing.core.PApplet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AbandonedArt79Sketch extends PApplet {
    private static final long USER_IDLE_MILLIS = 12000L;
    private static final int GRID_MIN = -45;
    private static final int GRID_MAX = 45;
    private static final int GRID_STEP = 6;

    private final EventQueue eventQueue;
    private final int sketchWidth;
    private final int sketchHeight;
    private final MotionConfig motionConfig;
    private final Map<String, UserState> users = new HashMap<>();
    private PaletteLibrary.ColorPalette[] palettes;

    public AbandonedArt79Sketch(EventQueue eventQueue,
                                AudioBuffer audioBuffer,
                                int width,
                                int height,
                                DebugConfig debugConfig,
                                MotionConfig motionConfig) {
        this.eventQueue = eventQueue;
        this.sketchWidth = width;
        this.sketchHeight = height;
        this.motionConfig = motionConfig;
    }

    @Override
    public void settings() {
        size(sketchWidth, sketchHeight, P3D);
    }

    @Override
    public void setup() {
        surface.setResizable(true);
        surface.setTitle("Processing Server - Abandoned Art 79");
        background(0);
        frameRate(24);
        sphereDetail(8);
        strokeWeight(0.5f);
        rectMode(CENTER);
        colorMode(HSB, 360, 100, 100, 100);
        palettes = PaletteLibrary.defaults(this).toArray(PaletteLibrary.ColorPalette[]::new);
    }

    @Override
    public void draw() {
        processEvents();
        pruneInactiveUsers();

        background(0);

        decayUserEnergy();
        for (UserState state : users.values()) {
            state.xstart += 0.01f + state.motionMagnitude * 0.018f + state.dragEnergy * 0.012f;
            state.ystart += 0.01f + state.motionMagnitude * 0.015f + state.dragEnergy * 0.014f;
            drawUserField(state);
        }
        drawHud();
        drawCredit();
    }

    private void processEvents() {
        while (!eventQueue.isEmpty()) {
            UserInputEvent event = eventQueue.poll();
            if (event == null) {
                break;
            }
            handleEvent(event);
        }
    }

    private void handleEvent(UserInputEvent event) {
        String sessionId = event.sessionId();
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        switch (event.eventType()) {
            case "touch" -> handleTouchEvent(sessionId, event);
            case "motion" -> handleMotionEvent(sessionId, event);
            case "button" -> handleButtonEvent(sessionId, event.controlId());
            case "session-ended" -> users.remove(sessionId);
            default -> {
            }
        }
    }

    private void handleTouchEvent(String sessionId, UserInputEvent event) {
        UserState state = users.computeIfAbsent(sessionId, this::createUserState);
        float nextX = constrain(event.x(), 0.08f, 0.92f);
        float nextY = constrain(event.y(), 0.08f, 0.92f);
        float dx = nextX - state.touchX;
        float dy = nextY - state.touchY;

        state.touchX = nextX;
        state.touchY = nextY;
        state.dragEnergy = constrain(lerp(state.dragEnergy, sqrt(dx * dx + dy * dy) * 10f, 0.75f), 0f, 1f);
        state.lastTouchMillis = millis();
    }

    private void handleMotionEvent(String sessionId, UserInputEvent event) {
        UserState state = users.computeIfAbsent(sessionId, this::createUserState);
        state.beta = event.beta();
        state.gamma = event.gamma();
        state.motionMagnitude = event.magnitude();
        state.lastTouchMillis = millis();

        float magnitudeDelta = max(0f, event.magnitude() - state.lastMotionMagnitude);
        state.lastMotionMagnitude = event.magnitude();
        if (magnitudeDelta > motionConfig.getShakeThresholdG()) {
            state.flashEnergy = 1f;
            state.xstart = random(10);
            state.ystart = random(10);
        }
    }

    private void handleButtonEvent(String sessionId, String controlId) {
        UserState state = users.computeIfAbsent(sessionId, this::createUserState);
        switch (controlId) {
            case "action1" -> {
                state.xstart = random(10);
                state.ystart = random(10);
                state.flashEnergy = 1f;
            }
            case "action2" -> state.depthBias = constrain(state.depthBias + 0.1f, 0f, 1f);
            case "action3" -> state.depthBias = constrain(state.depthBias - 0.1f, 0f, 1f);
            default -> {
            }
        }
    }

    private void drawUserField(UserState state) {
        float xnoise = state.xstart;
        float ynoise = state.ystart;

        pushMatrix();
        translate(width * state.touchX, height * state.touchY, 0);

        float forwardTilt = state.beta / max(1f, motionConfig.getBetaClampDegrees());
        float sidewaysTilt = state.gamma / max(1f, motionConfig.getGammaClampDegrees());

        rotateY(frameCount * (0.008f + state.dragEnergy * 0.02f) + sidewaysTilt * 0.35f);
        rotateZ(frameCount * 0.0035f + sidewaysTilt * 0.2f);
        rotateX(forwardTilt * 1.05f);

        for (int y = GRID_MIN; y <= GRID_MAX; y += GRID_STEP) {
            ynoise += 0.1f + state.motionMagnitude * 0.03f;
            xnoise = state.xstart;
            for (int x = GRID_MIN; x <= GRID_MAX; x += GRID_STEP) {
                xnoise += 0.1f + state.dragEnergy * 0.03f;
                float noiseFactor = max(0.02f, noise(xnoise, ynoise));
                drawPoint(x, y, noiseFactor, state);
            }
        }

        popMatrix();
    }

    private void drawPoint(float x, float y, float noiseFactor, UserState state) {
        pushMatrix();

        float depthBias = state.depthBias;
        float flashEnergy = state.flashEnergy;
        float saturation = 60f;

        translate(x * (1f / noiseFactor), y, -y * noiseFactor * (10f + depthBias * 24f));
        float edgeSize = noiseFactor * (15f + flashEnergy * 12f);
        float brightness = 48f + noiseFactor * 44f + flashEnergy * 18f;
        float alpha = 35f + noiseFactor * 45f + flashEnergy * 12f;

        noStroke();
        fill(adjustColor(state.baseColor, brightness, alpha), alpha);
        rect(0, 0, edgeSize, edgeSize / 2f);
        popMatrix();
    }

    private void decayUserEnergy() {
        for (UserState state : users.values()) {
            state.dragEnergy *= 0.94f;
            state.flashEnergy *= 0.9f;
            state.depthBias *= 0.995f;
        }
    }

    private void pruneInactiveUsers() {
        long now = millis();
        Iterator<Map.Entry<String, UserState>> iterator = users.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, UserState> entry = iterator.next();
            if (now - entry.getValue().lastTouchMillis > USER_IDLE_MILLIS) {
                iterator.remove();
            }
        }
    }

    private UserState createUserState(String sessionId) {
        UserState state = new UserState();
        state.touchX = 0.5f;
        state.touchY = 0.5f;
        state.palette = paletteForSession(sessionId);
        state.baseColor = state.palette.colorForSession(sessionId);
        state.xstart = random(10);
        state.ystart = random(10);
        state.lastTouchMillis = millis();
        return state;
    }

    private PaletteLibrary.ColorPalette paletteForSession(String sessionId) {
        return palettes[PaletteLibrary.stableIndex(sessionId, palettes.length)];
    }

    private int adjustColor(int baseColor, float brightness, float alpha) {
        return color(
            hue(baseColor),
            saturation(baseColor),
            constrain(brightness, 0f, 100f),
            constrain(alpha, 0f, 100f)
        );
    }

    private void drawHud() {
        camera();
        hint(DISABLE_DEPTH_TEST);
        fill(0, 0, 100, 85);
        textAlign(LEFT, TOP);
        text("Each user has a smaller grid. Touch places it. Drag energizes it. Forward/back tilt changes orientation.", 16, 14);
        text("Users: " + users.size(), 16, 32);
        hint(ENABLE_DEPTH_TEST);
    }

    private void drawCredit() {
        camera();
        hint(DISABLE_DEPTH_TEST);
        fill(0, 0, 100, 80);
        textAlign(RIGHT, BOTTOM);
        text("inspiration from: www.zenbullets.com", width - 16, height - 12);
        hint(ENABLE_DEPTH_TEST);
    }

    public void runSketch() {
        String[] args = {this.getClass().getName()};
        PApplet.runSketch(args, this);
    }

    private static final class UserState {
        private float touchX;
        private float touchY;
        private float beta;
        private float gamma;
        private float motionMagnitude;
        private float lastMotionMagnitude;
        private float dragEnergy;
        private float flashEnergy;
        private float depthBias;
        private float xstart;
        private float ystart;
        private long lastTouchMillis;
        private int baseColor;
        private PaletteLibrary.ColorPalette palette;
    }
}
