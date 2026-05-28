/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import processing.core.PApplet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AbandonedArt90Sketch extends PApplet {
    private static final long USER_IDLE_MILLIS = 12000L;
    private static final int POINT_COUNT = 180;
    private static final int SPHERE_COUNT = 8;

    private final EventQueue eventQueue;
    private final int sketchWidth;
    private final int sketchHeight;
    private final MotionConfig motionConfig;
    private final Map<String, UserState> users = new HashMap<>();
    private PaletteLibrary.ColorPalette[] palettes;

    public AbandonedArt90Sketch(EventQueue eventQueue,
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
        surface.setTitle("Processing Server - Abandoned Art 90");
        smooth();
        frameRate(30);
        colorMode(HSB, 360, 100, 100, 100);
        palettes = PaletteLibrary.defaults(this).toArray(PaletteLibrary.ColorPalette[]::new);
        background(255);
    }

    @Override
    public void draw() {
        processEvents();
        pruneInactiveUsers();

        background(255);
        lights();

        for (UserState state : users.values()) {
            updateUserState(state);
            drawUserGraphic(state);
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
        state.targetX = constrain(TouchNormalization.fromSignedRange(event.x()), 0.08f, 0.92f);
        state.targetY = constrain(TouchNormalization.fromSignedRange(event.y()), 0.12f, 0.88f);
        state.lastInputMillis = millis();
    }

    private void handleMotionEvent(String sessionId, UserInputEvent event) {
        UserState state = users.computeIfAbsent(sessionId, this::createUserState);
        state.beta = event.beta();
        state.gamma = event.gamma();
        state.motionMagnitude = event.magnitude();
        state.lastInputMillis = millis();
    }

    private void handleButtonEvent(String sessionId, String controlId) {
        UserState state = users.computeIfAbsent(sessionId, this::createUserState);
        switch (controlId) {
            case "action1" -> restartUser(state);
            case "action2" -> state.rotationVelocity += 0.01f;
            case "action3" -> state.rotationVelocity -= 0.01f;
            default -> {
            }
        }
        state.lastInputMillis = millis();
    }

    private void updateUserState(UserState state) {
        state.maxNoise += 0.01f + state.motionMagnitude * 0.01f;
        state.maxRadius = noise(state.maxNoise) * (80f + state.sizeScale * 140f);
        state.threshold = max(12f, state.maxRadius / 2.6f);

        for (SphereState sphere : state.spheres) {
            sphere.radNoise += 0.01f + state.motionMagnitude * 0.008f;
            sphere.radius = 45f + state.sizeScale * 35f + noise(sphere.radNoise) * state.maxRadius;
        }

        for (PointState point : state.points) {
            SphereState sphere = state.spheres[point.sphereIndex];
            point.x = sphere.radius * cos(point.s) * sin(point.t);
            point.y = sphere.radius * sin(point.s) * sin(point.t);
            point.z = sphere.radius * cos(point.t);
        }

        float gammaNormalized = state.gamma / max(1f, motionConfig.getGammaClampDegrees());
        float betaNormalized = state.beta / max(1f, motionConfig.getBetaClampDegrees());

        state.horizontalOffset = gammaNormalized * width * 0.18f;
        state.sizeScale = constrain(1f - betaNormalized * 0.55f, 0.45f, 1.8f);
        state.currentX = lerp(state.currentX, state.targetX * width, 0.22f);
        state.currentY = lerp(state.currentY, state.targetY * height, 0.22f);
        state.rotation += 0.01f + state.rotationVelocity + gammaNormalized * 0.01f;
        state.rotationVelocity *= 0.96f;
    }

    private void drawUserGraphic(UserState state) {
        pushMatrix();
        translate(state.currentX + state.horizontalOffset, state.currentY, 0);
        rotateY(state.rotation);
        rotateX((state.beta / max(1f, motionConfig.getBetaClampDegrees())) * 0.3f);

        for (int i = 0; i < state.points.length; i++) {
            PointState fromPoint = state.points[i];
            stroke(state.strokeColor, 42);
            noFill();
            for (int j = i + 1; j < state.points.length; j++) {
                PointState toPoint = state.points[j];
                float diff = dist(fromPoint.x, fromPoint.y, fromPoint.z, toPoint.x, toPoint.y, toPoint.z);
                if (diff < state.threshold) {
                    line(fromPoint.x, fromPoint.y, fromPoint.z, toPoint.x, toPoint.y, toPoint.z);
                }
            }
        }

        noStroke();
        fill(state.accentColor, 18);
        sphere(max(10f, state.sizeScale * 8f));
        popMatrix();
    }

    private void restartUser(UserState state) {
        state.maxNoise = random(1);
        for (int i = 0; i < state.spheres.length; i++) {
            state.spheres[i] = new SphereState(random(10));
        }
        for (int i = 0; i < state.points.length; i++) {
            state.points[i] = new PointState(random(TWO_PI), random(TWO_PI), (int) random(state.spheres.length));
        }
    }

    private UserState createUserState(String sessionId) {
        UserState state = new UserState();
        state.palette = paletteForSession(sessionId);
        state.strokeColor = state.palette.colorForSession(sessionId);
        state.accentColor = state.palette.accent();
        state.targetX = 0.5f;
        state.targetY = 0.5f;
        state.currentX = state.targetX * width;
        state.currentY = state.targetY * height;
        state.sizeScale = 1f;
        state.spheres = new SphereState[SPHERE_COUNT];
        state.points = new PointState[POINT_COUNT];
        state.lastInputMillis = millis();
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
            if (now - entry.getValue().lastInputMillis > USER_IDLE_MILLIS) {
                iterator.remove();
            }
        }
    }

    private void drawHud() {
        camera();
        hint(DISABLE_DEPTH_TEST);
        fill(0, 0, 15, 80);
        textAlign(LEFT, TOP);
        text("Each client has one colored graphic. Drag moves it. Tilt forward/back changes size. Tilt left/right shifts it.", 16, 14);
        text("Users: " + users.size(), 16, 32);
        hint(ENABLE_DEPTH_TEST);
    }

    private void drawCredit() {
        camera();
        hint(DISABLE_DEPTH_TEST);
        fill(0, 0, 15, 80);
        textAlign(RIGHT, BOTTOM);
        text("inspiration from: www.zenbullets.com", width - 16, height - 12);
        hint(ENABLE_DEPTH_TEST);
    }

    public void runSketch() {
        String[] args = {this.getClass().getName()};
        PApplet.runSketch(args, this);
    }

    private static final class UserState {
        private SphereState[] spheres;
        private PointState[] points;
        private float targetX;
        private float targetY;
        private float currentX;
        private float currentY;
        private float beta;
        private float gamma;
        private float motionMagnitude;
        private float sizeScale;
        private float horizontalOffset;
        private float maxNoise;
        private float maxRadius;
        private float threshold;
        private float rotation;
        private float rotationVelocity;
        private long lastInputMillis;
        private int strokeColor;
        private int accentColor;
        private PaletteLibrary.ColorPalette palette;
    }

    private static final class SphereState {
        private float radNoise;
        private float radius;

        private SphereState(float radNoise) {
            this.radNoise = radNoise;
        }
    }

    private static final class PointState {
        private final float s;
        private final float t;
        private final int sphereIndex;
        private float x;
        private float y;
        private float z;

        private PointState(float s, float t, int sphereIndex) {
            this.s = s;
            this.t = t;
            this.sphereIndex = sphereIndex;
        }
    }
}
