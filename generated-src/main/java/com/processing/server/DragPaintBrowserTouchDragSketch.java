package com.processing.server;

import processing.core.PApplet;

public class DragPaintBrowserTouchDragSketch extends PApplet {
    private final EventQueue eventQueue;
    private final AudioBuffer audioBuffer;
    private final int sketchWidth;
    private final int sketchHeight;
    private final DebugConfig debugConfig;
    private final MotionConfig motionConfig;

    private float brushX = 260;
    private float brushY = 180;
    private float brushHue = 20;
    private float brushSize = 36;
    private float touchX = 0.5f;
    private float touchY = 0.5f;

    // Browser touch events now trigger the old mouseDragged behavior.

    public DragPaintBrowserTouchDragSketch(EventQueue eventQueue,
                                           AudioBuffer audioBuffer,
                                           int width,
                                           int height,
                                           DebugConfig debugConfig,
                                           MotionConfig motionConfig) {
        this.eventQueue = eventQueue;
        this.audioBuffer = audioBuffer;
        this.sketchWidth = width;
        this.sketchHeight = height;
        this.debugConfig = debugConfig;
        this.motionConfig = motionConfig;
    }

    @Override
    public void settings() {
        size(sketchWidth, sketchHeight, JAVA2D);
    }

    @Override
    public void setup() {
        colorMode(HSB, 360, 100, 100, 100);
        background(220, 20, 10);
        noStroke();
    }

    @Override
    public void draw() {
        processEvents();
        processAudio();
        drawGeneratedSketch();
    }

    private void drawGeneratedSketch() {
        fadeBackground();
        drawBrush();
        drawLabel();
    }

    private void processEvents() {
        while (!eventQueue.isEmpty()) {
            UserInputEvent event = eventQueue.poll();
            if (event == null) {
                break;
            }

            if ("touch".equals(event.eventType())) {
                touchX = constrain(event.x(), 0f, 1f);
                touchY = constrain(event.y(), 0f, 1f);
                handleBrowserTouchDrag();
            }
        }
    }

    private void processAudio() {
        // This sketch does not use browser audio.
    }

    private void handleBrowserTouchDrag() {
        float mouseX = touchX * width;
        float mouseY = touchY * height;

        brushX = mouseX;
        brushY = mouseY;
        brushHue = (brushHue + 12) % 360;
    }

    private void fadeBackground() {
        fill(220, 20, 10, 10);
        rect(0, 0, width, height);
    }

    private void drawBrush() {
        fill(brushHue, 80, 100, 90);
        ellipse(brushX, brushY, brushSize, brushSize);
    }

    private void drawLabel() {
        fill(0, 0, 100, 85);
        textAlign(CENTER, CENTER);
        text("Drag in the browser to paint", width / 2f, height - 24);
    }

    public void runSketch() {
        String[] args = {this.getClass().getName()};
        PApplet.runSketch(args, this);
    }
}
