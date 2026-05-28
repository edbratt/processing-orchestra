/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import processing.core.PApplet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AbandonedArt96 extends PApplet {
    private static final int NODE_COUNT = 15;
    private static final long USER_IDLE_MILLIS = 12000L;
    private static final float MIN_TOUCH_MARGIN_X = 0.05f;
    private static final float MIN_TOUCH_MARGIN_Y = 0.08f;
    private static final float MAX_TOUCH_MARGIN_X = 0.95f;
    private static final float MAX_TOUCH_MARGIN_Y = 0.92f;

    private final EventQueue eventQueue;
    private final int sketchWidth;
    private final int sketchHeight;
    private final MotionConfig motionConfig;
    private final Map<String, UserState> users = new HashMap<>();
    private PaletteLibrary.ColorPalette[] palettes;

    public AbandonedArt96(EventQueue eventQueue,
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
        size(sketchWidth, sketchHeight, JAVA2D);
    }

    @Override
    public void setup() {
        surface.setResizable(true);
        surface.setTitle("Processing Server - Abandoned Art Touch Motion");
        frameRate(24);
        smooth();
        colorMode(HSB, 360, 100, 100, 100);
        palettes = PaletteLibrary.defaults(this).toArray(PaletteLibrary.ColorPalette[]::new);
        background(0);
    }

    @Override
    public void draw() {
        processEvents();

        noStroke();
        fill(0, 0, 0, 10);
        rect(0, 0, width, height);

        pruneInactiveUsers();

        for (UserState state : users.values()) {
            drawUserNetwork(state);
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
        float clampedX = constrain(TouchNormalization.fromSignedRange(event.x()), MIN_TOUCH_MARGIN_X, MAX_TOUCH_MARGIN_X);
        float clampedY = constrain(TouchNormalization.fromSignedRange(event.y()), MIN_TOUCH_MARGIN_Y, MAX_TOUCH_MARGIN_Y);
        float dx = clampedX - state.touchX;
        float dy = clampedY - state.touchY;

        state.touchX = clampedX;
        state.touchY = clampedY;
        state.dragEnergy = constrain(lerp(state.dragEnergy, sqrt(dx * dx + dy * dy) * 9f, 0.8f), 0f, 1f);
        state.lastTouchMillis = millis();
    }

    private void handleMotionEvent(String sessionId, UserInputEvent event) {
        UserState state = users.computeIfAbsent(sessionId, this::createUserState);
        state.alpha = event.alpha();
        state.beta = event.beta();
        state.gamma = event.gamma();
        state.ax = event.ax();
        state.ay = event.ay();
        state.az = event.az();
        state.motionMagnitude = event.magnitude();

        float magnitudeDelta = max(0f, event.magnitude() - state.lastMotionMagnitude);
        state.lastMotionMagnitude = event.magnitude();

        float axisDelta = abs(event.ax() - state.lastAx)
            + abs(event.ay() - state.lastAy)
            + abs(event.az() - state.lastAz);
        state.lastAx = event.ax();
        state.lastAy = event.ay();
        state.lastAz = event.az();

        float axisShake = axisDelta / max(0.01f, motionConfig.getAccelerationClampG());
        float combinedShake = max(axisShake, magnitudeDelta);
        if (combinedShake > motionConfig.getShakeThresholdG()) {
            clearBackground();
            restartUser(state);
        }
    }

    private void handleButtonEvent(String sessionId, String controlId) {
        UserState state = users.computeIfAbsent(sessionId, this::createUserState);
        switch (controlId) {
            case "action1" -> {
                clearBackground();
                restartUser(state);
            }
            case "action2" -> state.spreadBias = constrain(state.spreadBias + 0.08f, 0f, 1f);
            case "action3" -> state.spreadBias = constrain(state.spreadBias - 0.08f, 0f, 1f);
            default -> {
            }
        }
    }

    private void drawUserNetwork(UserState state) {
        float centerX = (state.touchX + tiltOffsetX(state)) * width;
        float centerY = (state.touchY + tiltOffsetY(state)) * height;
        centerX = constrain(centerX, -40f, width + 40f);
        centerY = constrain(centerY, -40f, height + 40f);

        float spread = 20f
            + state.motionMagnitude * 22f
            + state.dragEnergy * 180f
            + state.spreadBias * 120f;
        float driftX = map(state.gamma, -motionConfig.getGammaClampDegrees(), motionConfig.getGammaClampDegrees(), -26f, 26f);
        float driftY = map(state.beta, -motionConfig.getBetaClampDegrees(), motionConfig.getBetaClampDegrees(), -26f, 26f);

        float newX = centerX + random(-spread, spread) + driftX;
        float newY = centerY + random(-spread, spread) + driftY;
        int radius = max(4, round(8 + state.count * 4 + state.motionMagnitude * 10f + state.dragEnergy * 14f));

        strokeWeight(1);
        stroke(state.strokeColor, 32);
        fill(state.fillColor, 16);
        ellipse(newX, newY, radius, radius);

        point(newX, newY);

        for (int i = 0; i < state.count; i++) {
            line(newX, newY, state.xArr[i], state.yArr[i]);
        }

        noStroke();
        fill(state.accentColor, 80);
        ellipse(newX, newY, 5, 5);

        if (state.count >= NODE_COUNT - 1) {
            restartUser(state);
        } else {
            state.xArr[state.count] = newX;
            state.yArr[state.count] = newY;
            state.count++;
        }

        state.dragEnergy *= 0.92f;
        state.spreadBias *= 0.985f;
    }

    private float tiltOffsetX(UserState state) {
        float normalized = state.gamma / max(1f, motionConfig.getGammaClampDegrees());
        return normalized * motionConfig.getTiltOffsetNormalized();
    }

    private float tiltOffsetY(UserState state) {
        float normalized = state.beta / max(1f, motionConfig.getBetaClampDegrees());
        return normalized * motionConfig.getTiltOffsetNormalized();
    }

    private void clearBackground() {
        background(0);
    }

    private void restartUser(UserState state) {
        state.count = 0;
        state.xArr = new float[NODE_COUNT];
        state.yArr = new float[NODE_COUNT];
    }

    private UserState createUserState(String sessionId) {
        UserState state = new UserState();
        state.touchX = 0.5f;
        state.touchY = 0.5f;
        state.palette = paletteForSession(sessionId);
        state.strokeColor = state.palette.colorForSession(sessionId);
        state.fillColor = state.palette.randomColor(this);
        state.accentColor = state.palette.accent();
        state.lastTouchMillis = millis();
        restartUser(state);
        return state;
    }

    private PaletteLibrary.ColorPalette paletteForSession(String sessionId) {
        return palettes[PaletteLibrary.stableIndex(sessionId, palettes.length)];
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

    private void drawHud() {
        fill(0, 0, 100, 85);
        textAlign(LEFT, TOP);
        text("Drag to steer growth. Tilt to bend it. Shake to clear and restart.", 16, 14);
        text("Users: " + users.size(), 16, 32);
    }

    private void drawCredit() {
        fill(0, 0, 100, 80);
        textAlign(RIGHT, BOTTOM);
        text("inspiration from: www.zenbullets.com", width - 16, height - 12);
    }

    public void runSketch() {
        String[] args = {this.getClass().getName()};
        PApplet.runSketch(args, this);
    }

    private static final class UserState {
        private float[] xArr;
        private float[] yArr;
        private int count;
        private float touchX;
        private float touchY;
        private float dragEnergy;
        private float spreadBias;
        private float alpha;
        private float beta;
        private float gamma;
        private float ax;
        private float ay;
        private float az;
        private float motionMagnitude;
        private float lastMotionMagnitude;
        private float lastAx;
        private float lastAy;
        private float lastAz;
        private long lastTouchMillis;
        private int strokeColor;
        private int fillColor;
        private int accentColor;
        private PaletteLibrary.ColorPalette palette;
    }
}
