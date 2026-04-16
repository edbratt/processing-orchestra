package com.processing.server;

import processing.core.PApplet;

public class KeyboardToggleBrowserKeySketch extends PApplet {
    private final EventQueue eventQueue;
    private final AudioBuffer audioBuffer;
    private final int sketchWidth;
    private final int sketchHeight;
    private final DebugConfig debugConfig;
    private final MotionConfig motionConfig;

    private float hueValue = 20;
    private float circleSize = 120;
    private boolean ringVisible = true;

    // Migration TODOs
    // TODO: Simple keyboard input was mapped to browser key events automatically.

    public KeyboardToggleBrowserKeySketch(EventQueue eventQueue,
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
        surface.setResizable(false);
        surface.setTitle("Processing Server - Keyboard Toggle Browser Key Sketch");
        colorMode(HSB, 360, 100, 100);
        strokeWeight(4);
    }

    @Override
    public void draw() {
        processEvents();
        processAudio();
        drawGeneratedSketch();
    }

    private void drawGeneratedSketch() {
        background(225, 20, 12);

        fill(hueValue, 80, 100);
        noStroke();
        ellipse(width / 2f, height / 2f, circleSize, circleSize);

        if (ringVisible) {
            noFill();
            stroke((hueValue + 180) % 360, 60, 100);
            ellipse(width / 2f, height / 2f, circleSize + 40, circleSize + 40);
        }

        fill(0, 0, 100);
        textAlign(CENTER, CENTER);
        text("Browser keys: space toggles ring, arrows change color", width / 2f, height - 24);
    }

    private void processEvents() {
        while (!eventQueue.isEmpty()) {
            UserInputEvent event = eventQueue.poll();
            if (event == null) {
                break;
            }

            if ("key".equals(event.eventType()) && "pressed".equals(event.keyAction())) {
                handleBrowserKeyPressed(event);
            }
        }
    }

    private void processAudio() {
        // TODO: Integrate AudioBuffer processing here if this sketch later needs browser audio.
    }

    private void handleBrowserKeyPressed(UserInputEvent event) {
        char key = event.keyText().isEmpty() ? '\0' : event.keyText().charAt(0);
        int keyCode = event.keyCode();
        final int LEFT = 37;
        final int UP = 38;
        final int RIGHT = 39;
        final int DOWN = 40;

        if (key == ' ') {
            ringVisible = !ringVisible;
        }

        if (keyCode == LEFT) {
            hueValue = (hueValue + 345) % 360;
        } else if (keyCode == RIGHT) {
            hueValue = (hueValue + 15) % 360;
        }
    }

    public void runSketch() {
        String[] args = {this.getClass().getName()};
        PApplet.runSketch(args, this);
    }
}
