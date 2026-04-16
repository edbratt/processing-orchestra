package com.processing.server;

import processing.core.PApplet;

public class HelperTrailsBrowserTouchPressSketch extends PApplet {
    private final EventQueue eventQueue;
    private final AudioBuffer audioBuffer;
    private final int sketchWidth;
    private final int sketchHeight;
    private final DebugConfig debugConfig;
    private final MotionConfig motionConfig;

    private float ballX = 240;
    private float ballY = 160;
    private float velocityX = 2.8f;
    private float velocityY = 2.1f;
    private float trailHue = 180;
    private float touchX = 0.5f;
    private float touchY = 0.5f;

    // Browser touch events now trigger the old mousePressed behavior.

    public HelperTrailsBrowserTouchPressSketch(EventQueue eventQueue,
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
        background(225, 25, 8);
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
        updateBall();
        drawBall();
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
                handleBrowserTouchPress();
            }
        }
    }

    private void processAudio() {
        // This sketch does not use browser audio.
    }

    private void handleBrowserTouchPress() {
        float mouseX = touchX * width;
        float mouseY = touchY * height;

        ballX = mouseX;
        ballY = mouseY;
        trailHue = random(360);
    }

    private void fadeBackground() {
        fill(225, 25, 8, 12);
        rect(0, 0, width, height);
    }

    private void updateBall() {
        ballX += velocityX;
        ballY += velocityY;

        if (ballX > width - 28 || ballX < 28) {
            velocityX *= -1;
        }

        if (ballY > height - 28 || ballY < 28) {
            velocityY *= -1;
        }
    }

    private void drawBall() {
        fill(trailHue, 70, 100, 85);
        ellipse(ballX, ballY, 56, 56);
    }

    private void drawLabel() {
        fill(0, 0, 100, 85);
        textAlign(CENTER, CENTER);
        text("Touch in the browser to reposition and recolor", width / 2f, height - 24);
    }

    public void runSketch() {
        String[] args = {this.getClass().getName()};
        PApplet.runSketch(args, this);
    }
}
