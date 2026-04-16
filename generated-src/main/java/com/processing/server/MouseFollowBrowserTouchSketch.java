package com.processing.server;

import processing.core.PApplet;

public class MouseFollowBrowserTouchSketch extends PApplet {
    private final EventQueue eventQueue;
    private final AudioBuffer audioBuffer;
    private final int sketchWidth;
    private final int sketchHeight;
    private final DebugConfig debugConfig;
    private final MotionConfig motionConfig;

    private float circleSize = 70;
    private float circleHue = 200;
    private float touchX = 0.5f;
    private float touchY = 0.5f;

    // Migration TODOs
    // TODO: Simple mouse-position usage was mapped to browser touch input automatically.

    public MouseFollowBrowserTouchSketch(EventQueue eventQueue,
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
        surface.setTitle("Processing Server - Mouse Follow Browser Touch Sketch");
        colorMode(HSB, 360, 100, 100);
        noStroke();
    }

    @Override
    public void draw() {
        processEvents();
        processAudio();
        drawGeneratedSketch();
    }

    private void drawGeneratedSketch() {
        background(230, 50, 15);

        float pulse = 1.0f + sin(frameCount * 0.08f) * 0.12f;

        fill(circleHue, 80, 100);
        ellipse(touchX * width, touchY * height, circleSize * pulse, circleSize * pulse);

        fill(0, 0, 100);
        textAlign(CENTER, CENTER);
        text("Touch or drag in the browser", width / 2f, height - 24);
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
            }
        }
    }

    private void processAudio() {
        // TODO: Integrate AudioBuffer processing here if this sketch later needs browser audio.
    }

    public void runSketch() {
        String[] args = {this.getClass().getName()};
        PApplet.runSketch(args, this);
    }
}
