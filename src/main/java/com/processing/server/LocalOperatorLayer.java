/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import processing.core.PApplet;
import processing.event.MouseEvent;

final class LocalOperatorLayer {
    interface Adapter {
        Iterable<String> sessionIds();
        float[] position(String sessionId);
        float selectionRadiusPixels(String sessionId);
        String label(String sessionId);
        float audioLevel(String sessionId);
        float dominantFrequency(String sessionId);
        boolean supportsDominantFrequency();
        float speed(String sessionId);
        float sizeValue(String sessionId);
        void setSizeValue(String sessionId, float value);
        float gainValue(String sessionId);
        void setGainValue(String sessionId, float value);
        void setTargetPosition(String sessionId, float normalizedX, float normalizedY, boolean snapImmediately);
        void scatterAll();
        void recenterAll();
    }

    private String selectedSessionId;
    private boolean localDragging;
    private boolean showDebugHud = true;
    private boolean showNames = true;
    private boolean showVelocityVectors;
    private boolean showAttractionLines;
    private boolean physicsPaused;
    private boolean slowMotion;

    boolean showNames() {
        return showNames;
    }

    boolean showVelocityVectors() {
        return showVelocityVectors;
    }

    boolean showAttractionLines() {
        return showAttractionLines;
    }

    float physicsTimeScale() {
        if (physicsPaused) {
            return 0f;
        }
        return slowMotion ? 0.35f : 1f;
    }

    String selectedSessionId() {
        return selectedSessionId;
    }

    void clearSelectionIfMatches(String sessionId) {
        if (sessionId != null && sessionId.equals(selectedSessionId)) {
            selectedSessionId = null;
            localDragging = false;
        }
    }

    void mousePressed(PApplet app, Adapter adapter) {
        selectNearestUser(app, adapter, app.mouseX, app.mouseY);
        localDragging = selectedSessionId != null;
        if (localDragging) {
            dragSelectedUserTo(app, adapter, app.mouseX, app.mouseY);
        }
    }

    void mouseDragged(PApplet app, Adapter adapter) {
        if (localDragging) {
            dragSelectedUserTo(app, adapter, app.mouseX, app.mouseY);
        }
    }

    void mouseReleased() {
        localDragging = false;
    }

    void mouseWheel(PApplet app, MouseEvent event, Adapter adapter) {
        if (selectedSessionId == null) {
            return;
        }

        float delta = -event.getCount() * 0.04f;
        if (app.keyPressed && app.keyCode == PApplet.SHIFT) {
            adapter.setSizeValue(selectedSessionId, clamp01(adapter.sizeValue(selectedSessionId) + delta));
        } else if (app.keyPressed && app.keyCode == PApplet.CONTROL) {
            adapter.setGainValue(selectedSessionId, clamp01(adapter.gainValue(selectedSessionId) + delta));
        }
    }

    void keyPressed(char key, Adapter adapter) {
        switch (key) {
            case 'D' -> showDebugHud = !showDebugHud;
            case 'N' -> showNames = !showNames;
            case 'V' -> showVelocityVectors = !showVelocityVectors;
            case 'L' -> showAttractionLines = !showAttractionLines;
            case 'P' -> physicsPaused = !physicsPaused;
            case 'S' -> slowMotion = !slowMotion;
            case 'R' -> adapter.scatterAll();
            case 'C' -> adapter.recenterAll();
            default -> {
            }
        }
    }

    void drawSelectionOverlay(PApplet app, Adapter adapter, float strokeAlpha) {
        if (selectedSessionId == null) {
            return;
        }

        float[] pos = adapter.position(selectedSessionId);
        if (pos == null) {
            return;
        }

        float overlaySize = adapter.selectionRadiusPixels(selectedSessionId) * 2f + 10f;
        app.stroke(48, 18, 100, strokeAlpha);
        app.strokeWeight(2.5f);
        app.noFill();
        app.ellipse(pos[0] * app.width, pos[1] * app.height, overlaySize, overlaySize);
    }

    void drawLocalHud(PApplet app, Adapter adapter, float panelAlpha, float textAlpha) {
        if (!showDebugHud) {
            return;
        }

        String selectedLabel = selectedSessionId == null
            ? "none"
            : adapter.label(selectedSessionId) + " (" + shortSessionId(selectedSessionId) + ")";

        float selectedLevel = selectedSessionId == null ? 0f : adapter.audioLevel(selectedSessionId);
        float selectedFrequency = selectedSessionId == null ? 0f : adapter.dominantFrequency(selectedSessionId);
        float selectedSpeed = selectedSessionId == null ? 0f : adapter.speed(selectedSessionId);

        String[] lines = new String[]{
            "Local Controls: D hud  N names  V vectors  L lines  P pause  S slow  R scatter  C center",
            "Shift+Wheel size  Ctrl+Wheel gain  Click select  Drag selected target",
            "Selected: " + selectedLabel,
            "Physics: " + (physicsPaused ? "paused" : (slowMotion ? "slow" : "live"))
                + " | names: " + onOff(showNames)
                + " | vectors: " + onOff(showVelocityVectors)
                + " | lines: " + onOff(showAttractionLines),
            "Selected audio: level=" + app.nf(selectedLevel, 0, 3)
                + " freq=" + frequencyStatus(adapter, selectedFrequency)
                + " speed=" + app.nf(selectedSpeed, 0, 4)
        };

        app.textAlign(PApplet.LEFT, PApplet.TOP);
        app.textSize(12);
        float panelX = 14f;
        float panelY = 14f;
        float lineHeight = 16f;
        float panelWidth = 0f;
        for (String lineText : lines) {
            panelWidth = Math.max(panelWidth, app.textWidth(lineText));
        }
        float panelHeight = 12f + lines.length * lineHeight;

        app.noStroke();
        app.fill(0, 0, 0, panelAlpha);
        app.rect(panelX - 8f, panelY - 8f, panelWidth + 16f, panelHeight + 8f, 8f);

        app.fill(0, 0, 100, textAlpha);
        for (int i = 0; i < lines.length; i++) {
            app.text(lines[i], panelX, panelY + i * lineHeight);
        }
    }

    private void selectNearestUser(PApplet app, Adapter adapter, float canvasX, float canvasY) {
        float bestDistanceSq = Float.MAX_VALUE;
        String bestSessionId = null;
        for (String sessionId : adapter.sessionIds()) {
            float[] pos = adapter.position(sessionId);
            if (pos == null) {
                continue;
            }
            float dx = canvasX - (pos[0] * app.width);
            float dy = canvasY - (pos[1] * app.height);
            float distanceSq = dx * dx + dy * dy;
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                bestSessionId = sessionId;
            }
        }

        if (bestSessionId == null) {
            selectedSessionId = null;
            return;
        }

        float selectionRadius = adapter.selectionRadiusPixels(bestSessionId) + 18f;
        selectedSessionId = bestDistanceSq <= selectionRadius * selectionRadius ? bestSessionId : null;
    }

    private void dragSelectedUserTo(PApplet app, Adapter adapter, float canvasX, float canvasY) {
        if (selectedSessionId == null) {
            return;
        }

        float radiusPixels = adapter.selectionRadiusPixels(selectedSessionId);
        float radiusX = radiusPixels / Math.max(1f, app.width);
        float radiusY = radiusPixels / Math.max(1f, app.height);
        float normalizedX = PApplet.constrain(canvasX / app.width, radiusX, 1f - radiusX);
        float normalizedY = PApplet.constrain(canvasY / app.height, radiusY, 1f - radiusY);
        adapter.setTargetPosition(selectedSessionId, normalizedX, normalizedY, physicsPaused);
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private String shortSessionId(String sessionId) {
        return sessionId == null ? "none" : sessionId.substring(0, Math.min(8, sessionId.length()));
    }

    private String onOff(boolean enabled) {
        return enabled ? "on" : "off";
    }

    private String frequencyStatus(Adapter adapter, float selectedFrequency) {
        if (!adapter.supportsDominantFrequency()) {
            return "n/a";
        }
        return selectedFrequency > 0f ? selectedFrequency + "Hz" : "held/none";
    }
}
