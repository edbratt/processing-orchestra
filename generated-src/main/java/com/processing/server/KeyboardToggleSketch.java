package com.processing.server;

import processing.core.PApplet;

public class KeyboardToggleSketch extends PApplet {
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
    // TODO: Local keyboard input was preserved. Decide later whether it should stay local or become browser keys, buttons, or sliders.

    public KeyboardToggleSketch(EventQueue eventQueue,
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
        surface.setTitle("Processing Server - Keyboard Toggle Sketch");
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
        // text("Space toggles ring, arrows change color", width / 2f, height - 24);
        text("Browser keys: Space toggles ring, arrows change color", width / 2f, height - 24);
    }

   // Wire into keyboard events from the input queue
    private void processEvents() {
      while (!eventQueue.isEmpty()) {
          UserInputEvent event = eventQueue.poll();
          if (event == null) {
              break;
          }

          if (!"key".equals(event.eventType())) {
              continue;
          }

          if (!"pressed".equals(event.keyAction())) {
              continue;
          }

          handleBrowserKey(event);
      }
    }

    // Add helper method to map from the input queue to the actions we want to take
    private void handleBrowserKey(UserInputEvent event) {
      String keyText = event.keyText();
      int keyCodeValue = event.keyCode();

      if (" ".equals(keyText) || "Space".equals(keyText) || "Spacebar".equals(keyText) || keyCodeValue == 32) {
          ringVisible = !ringVisible;
          return;
      }

      if ("ArrowLeft".equals(keyText) || keyCodeValue == 37) {
          hueValue = (hueValue + 345) % 360;
      } else if ("ArrowRight".equals(keyText) || keyCodeValue == 39) {
          hueValue = (hueValue + 15) % 360;
      }
    }



    private void processAudio() {
        // TODO: Integrate AudioBuffer processing here if this sketch later needs browser audio.
    }

/*     @Override
    public void keyPressed() {
        if (key == ' ') {
            ringVisible = !ringVisible;
        }

        if (keyCode == LEFT) {
            hueValue = (hueValue + 345) % 360;
        } else if (keyCode == RIGHT) {
            hueValue = (hueValue + 15) % 360;
        }
    } */

    public void runSketch() {
        String[] args = {this.getClass().getName()};
        PApplet.runSketch(args, this);
    }
}
