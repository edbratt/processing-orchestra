/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import processing.core.PApplet;

public class StarterSketch extends PApplet {
    private final EventQueue eventQueue;
    private final int sketchWidth;
    private final int sketchHeight;
    private float lineX = 0;
    private float speed = 2.5f;
    private float markerX = 0.5f;
    private float markerY = 0.5f;
    private float markerSize = 48f;
    private String lastKey = "";
    private PaletteLibrary.ColorPalette palette;
    private int lineColor;
    private int markerColor;
    private int textColor;

    public StarterSketch(EventQueue eventQueue,
                         AudioBuffer audioBuffer,
                         int width,
                         int height,
                         DebugConfig debugConfig,
                         MotionConfig motionConfig) {
        this.eventQueue = eventQueue;
        this.sketchWidth = width;
        this.sketchHeight = height;
    }

    @Override
    public void settings() {
        size(sketchWidth, sketchHeight, JAVA2D);
    }

    @Override
    public void setup() {
        surface.setResizable(true);
        surface.setTitle("Processing Server - Starter Sketch");
        strokeWeight(6);
        palette = PaletteLibrary.randomDefault(this);
        lineColor = palette.accent();
        markerColor = palette.colorAt(1);
        textColor = palette.foreground();
    }

    @Override
    public void draw() {
        processEvents();
        background(palette.background());

        stroke(lineColor);
        line(lineX, 0, lineX, height);

        noStroke();
        fill(markerColor);
        ellipse(markerX * width, markerY * height, markerSize, markerSize);

        fill(textColor);
        textAlign(LEFT, TOP);
        text("Touch moves the circle. Arrows/WASD move it. Space reverses the line.", 16, 16);
        text("Last key: " + (lastKey.isBlank() ? "-" : lastKey), 16, 34);

        lineX += speed;
        if (lineX > width || lineX < 0) {
            speed *= -1;
        }
    }

    private void processEvents() {
        while (!eventQueue.isEmpty()) {
            UserInputEvent event = eventQueue.poll();
            if (event == null) {
                break;
            }

            if ("touch".equals(event.eventType())) {
                markerX = constrain(event.x(), 0.05f, 0.95f);
                markerY = constrain(event.y(), 0.1f, 0.9f);
            } else if ("slider".equals(event.eventType()) && "sizeSlider".equals(event.controlId())) {
                markerSize = map(constrain(event.value(), 0f, 1f), 0f, 1f, 20f, 120f);
            } else if ("key".equals(event.eventType()) && "pressed".equals(event.keyAction())) {
                handleKeyEvent(event);
            }
        }
    }

    private void handleKeyEvent(UserInputEvent event) {
        String key = event.keyText();
        lastKey = key == null || key.isBlank() ? Integer.toString(event.keyCode()) : key;
        switch (key) {
            case "ArrowLeft", "a", "A" -> markerX = constrain(markerX - 0.05f, 0.05f, 0.95f);
            case "ArrowRight", "d", "D" -> markerX = constrain(markerX + 0.05f, 0.05f, 0.95f);
            case "ArrowUp", "w", "W" -> markerY = constrain(markerY - 0.05f, 0.1f, 0.9f);
            case "ArrowDown", "s", "S" -> markerY = constrain(markerY + 0.05f, 0.1f, 0.9f);
            case " ", "Space", "Spacebar" -> {
                speed *= -1f;
                palette = PaletteLibrary.randomDefault(this);
                lineColor = palette.accent();
                markerColor = palette.randomColor(this);
                textColor = palette.foreground();
            }
            default -> {
            }
        }
    }

    public void runSketch() {
        String[] args = {this.getClass().getName()};
        PApplet.runSketch(args, this);
    }
}
