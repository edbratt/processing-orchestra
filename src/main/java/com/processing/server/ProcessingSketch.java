/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import processing.core.PApplet;
import processing.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class ProcessingSketch extends PApplet {
    private static final float DEFAULT_SIZE = 0.5f;
    private static final float DEFAULT_SPEED = 0.5f;
    private static final float DEFAULT_GAIN = 0.5f;
    private static final String DEFAULT_USER_LABEL = "Guest";

    private final EventQueue eventQueue;
    private final AudioBuffer audioBuffer;
    private final int sketchWidth;
    private final int sketchHeight;
    private final DebugConfig debugConfig;
    private final MotionConfig motionConfig;
    
    private final Map<String, float[]> userPositions = new HashMap<>();
    // Touch events update the target position; draw() eases toward it so the speed slider can
    // control how quickly each user's visuals respond.
    private final Map<String, float[]> userTargetPositions = new HashMap<>();
    private final Map<String, float[]> userColors = new HashMap<>();
    private final Map<String, float[]> userAudioLevels = new HashMap<>();
    private final Map<String, Float> userSizes = new HashMap<>();
    private final Map<String, Float> userSpeeds = new HashMap<>();
    private final Map<String, Float> userGains = new HashMap<>();
    private final Map<String, Float> userPulseBoosts = new HashMap<>();
    private final Map<String, Float> userHueVelocities = new HashMap<>();
    private final Map<String, float[]> userMotion = new HashMap<>();
    private final Map<String, Float> userLastMotionMagnitudes = new HashMap<>();
    private final Map<String, Map<Integer, Boolean>> userKeyStates = new HashMap<>();
    private final Map<String, String> userNames = new HashMap<>();
    private final LocalOperatorLayer localOperatorLayer = new LocalOperatorLayer();
    private final LocalOperatorLayer.Adapter localOperatorAdapter = new LocalOperatorLayer.Adapter() {
        @Override
        public Iterable<String> sessionIds() {
            return userPositions.keySet();
        }

        @Override
        public float[] position(String sessionId) {
            return userPositions.get(sessionId);
        }

        @Override
        public float selectionRadiusPixels(String sessionId) {
            float baseSize = baseSizePixels(sessionId);
            float pulseSize = baseSize * ringScale(sessionId);
            return max(baseSize, pulseSize) * 0.5f + 4f;
        }

        @Override
        public String label(String sessionId) {
            String name = userNames.getOrDefault(sessionId, "").trim();
            return name.isEmpty() ? DEFAULT_USER_LABEL : name;
        }

        @Override
        public float audioLevel(String sessionId) {
            return userAudioLevels.getOrDefault(sessionId, new float[]{0f})[0];
        }

        @Override
        public float dominantFrequency(String sessionId) {
            return 0f;
        }

        @Override
        public boolean supportsDominantFrequency() {
            return false;
        }

        @Override
        public float speed(String sessionId) {
            float[] pos = userPositions.get(sessionId);
            float[] targetPos = userTargetPositions.get(sessionId);
            if (pos == null || targetPos == null) {
                return 0f;
            }
            float dx = targetPos[0] - pos[0];
            float dy = targetPos[1] - pos[1];
            return sqrt(dx * dx + dy * dy);
        }

        @Override
        public float sizeValue(String sessionId) {
            return userSizes.getOrDefault(sessionId, DEFAULT_SIZE);
        }

        @Override
        public void setSizeValue(String sessionId, float value) {
            userSizes.put(sessionId, value);
        }

        @Override
        public float gainValue(String sessionId) {
            return userGains.getOrDefault(sessionId, DEFAULT_GAIN);
        }

        @Override
        public void setGainValue(String sessionId, float value) {
            userGains.put(sessionId, value);
        }

        @Override
        public void setTargetPosition(String sessionId, float normalizedX, float normalizedY, boolean snapImmediately) {
            float[] targetPos = userTargetPositions.get(sessionId);
            float[] pos = userPositions.get(sessionId);
            if (targetPos == null || pos == null) {
                return;
            }
            targetPos[0] = normalizedX;
            targetPos[1] = normalizedY;
            if (snapImmediately) {
                pos[0] = normalizedX;
                pos[1] = normalizedY;
            }
        }

        @Override
        public void scatterAll() {
            scatterAllUsers();
        }

        @Override
        public void recenterAll() {
            recenterAllUsers();
        }
    };
    
    private float globalAudioLevel = 0;
    private float smoothedGlobalAudioLevel = 0;
    private int audioFrameCount = 0;
    private int motionFrameCount = 0;
    
    public ProcessingSketch(EventQueue eventQueue,
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
        surface.setResizable(true);
        surface.setTitle("Processing Server - Multi-User Canvas");
        frameRate(60);
        background(0);
        colorMode(HSB, 360, 100, 100);
        if (debugConfig.isLogging()) {
            println("Processing sketch initialized: " + sketchWidth + "x" + sketchHeight);
        }
    }

    @Override
    public void draw() {
        background(0);
        
        processEvents();
        
        processAudio();
        
        drawUsers();

        localOperatorLayer.drawSelectionOverlay(this, localOperatorAdapter, 100f);
        localOperatorLayer.drawLocalHud(this, localOperatorAdapter, 170f, 100f);
        
        drawAudioMeter();
    }

    private void processEvents() {
        while (!eventQueue.isEmpty()) {
            UserInputEvent event = eventQueue.poll();
            if (event != null) {
                handleEvent(event);
            }
        }
    }

    private void handleEvent(UserInputEvent event) {
        String sessionId = event.sessionId();
        
        switch (event.eventType()) {
            case "touch" -> {
                // Touch is the primary spatial input. It updates the persistent target position
                // that the draw loop eases toward on each frame.
                if (!userPositions.containsKey(sessionId)) {
                    initializeUser(sessionId);
                }
                float[] targetPos = userTargetPositions.get(sessionId);
                targetPos[0] = event.x();
                targetPos[1] = event.y();
            }
            case "slider" -> {
                // Sliders tune per-user visual and audio-response parameters without directly
                // moving the sketch objects.
                if (!userPositions.containsKey(sessionId)) {
                    initializeUser(sessionId);
                }
                if ("sizeSlider".equals(event.controlId())) {
                    userSizes.put(sessionId, constrain(event.value(), 0, 1));
                } else if ("speedSlider".equals(event.controlId())) {
                    userSpeeds.put(sessionId, constrain(event.value(), 0, 1));
                } else if ("gainSlider".equals(event.controlId())) {
                    // The UI sends a normalized 0..1 slider value; we turn that into a gain curve
                    // later when deriving audio amplitude from incoming PCM.
                    userGains.put(sessionId, constrain(event.value(), 0, 1));
                }
            }
            case "button" -> {
                // Buttons trigger short-lived visual actions such as burst, hue spin, and scatter.
                if (!userPositions.containsKey(sessionId)) {
                    initializeUser(sessionId);
                }
                handleButtonEvent(sessionId, event.controlId());
            }
            case "motion" -> {
                // Motion is an additive layer from phone sensors. Tilt offsets the rendered
                // position around the touch target, and shake intensity can inject burst energy.
                if (!userPositions.containsKey(sessionId)) {
                    initializeUser(sessionId);
                }
                handleMotionEvent(sessionId, event);
            }
            case "key" -> {
                // Keyboard events come from the focused browser page. They are available for
                // custom sketches, and the default sketch uses arrows/WASD and space as a small
                // built-in example.
                if (!userPositions.containsKey(sessionId)) {
                    initializeUser(sessionId);
                }
                handleKeyEvent(sessionId, event);
            }
            case "session-meta" -> {
                if ("name".equals(event.controlId())) {
                    userNames.put(sessionId, event.textValue());
                }
            }
            case "session-ended" -> removeUser(sessionId);
        }
    }

    private void handleKeyEvent(String sessionId, UserInputEvent event) {
        Map<Integer, Boolean> keyStates = userKeyStates.computeIfAbsent(sessionId, key -> new HashMap<>());
        boolean pressed = "pressed".equals(event.keyAction());
        keyStates.put(event.keyCode(), pressed);

        if (!pressed) {
            return;
        }

        float[] targetPos = userTargetPositions.get(sessionId);
        float step = 0.04f;

        switch (normalizeKey(event.keyText(), event.keyCode())) {
            case "ArrowLeft", "a", "A" -> targetPos[0] = constrain(targetPos[0] - step, 0.05f, 0.95f);
            case "ArrowRight", "d", "D" -> targetPos[0] = constrain(targetPos[0] + step, 0.05f, 0.95f);
            case "ArrowUp", "w", "W" -> targetPos[1] = constrain(targetPos[1] - step, 0.1f, 0.9f);
            case "ArrowDown", "s", "S" -> targetPos[1] = constrain(targetPos[1] + step, 0.1f, 0.9f);
            case " ", "Space", "Spacebar" -> userPulseBoosts.put(sessionId, max(userPulseBoosts.getOrDefault(sessionId, 0f), 0.8f));
            default -> {
            }
        }
    }

    private String normalizeKey(String keyText, int keyCode) {
        if (keyText != null && !keyText.isBlank()) {
            return keyText;
        }
        return switch (keyCode) {
            case 37 -> "ArrowLeft";
            case 38 -> "ArrowUp";
            case 39 -> "ArrowRight";
            case 40 -> "ArrowDown";
            case 32 -> " ";
            default -> "";
        };
    }

    private void handleMotionEvent(String sessionId, UserInputEvent event) {
        float[] previousMotion = userMotion.getOrDefault(sessionId, new float[]{0f, 0f, 0f, 0f, 0f, 0f, 0f});

        // Motion updates layer on top of touch targets rather than replacing them outright.
        userMotion.put(sessionId, new float[]{
            event.alpha(),
            event.beta(),
            event.gamma(),
            event.ax(),
            event.ay(),
            event.az(),
            event.magnitude()
        });

        float previousMagnitude = userLastMotionMagnitudes.getOrDefault(sessionId, 0f);
        float magnitudeDelta = max(0f, event.magnitude() - previousMagnitude);
        userLastMotionMagnitudes.put(sessionId, event.magnitude());

        float axisDelta = abs(event.ax() - previousMotion[3])
            + abs(event.ay() - previousMotion[4])
            + abs(event.az() - previousMotion[5]);
        float axisShake = axisDelta / max(0.01f, motionConfig.getAccelerationClampG());
        float combinedShakeSignal = max(axisShake, magnitudeDelta);
        float shakeIntensity = max(0f, combinedShakeSignal - motionConfig.getShakeThresholdG());
        if (shakeIntensity > 0f) {
            float normalizedShake = constrain(
                shakeIntensity / max(0.01f, motionConfig.getMagnitudeClampG() - motionConfig.getShakeThresholdG()),
                0f,
                1f
            );
            float burstBoost = normalizedShake * motionConfig.getShakeBurstScale();
            // A harder shake should look like a stronger burst, not a binary trigger.
            userPulseBoosts.put(sessionId, max(userPulseBoosts.getOrDefault(sessionId, 0f), burstBoost));
            userHueVelocities.put(sessionId,
                userHueVelocities.getOrDefault(sessionId, 0f) + random(-6f, 6f) * max(0.4f, normalizedShake));
        }

        if (motionConfig.isDebugLogging()) {
            motionFrameCount++;
            if (motionFrameCount <= motionConfig.getDebugSampleLimit()) {
                println("Motion stored for " + sessionId.substring(0, 8)
                        + ": beta=" + nf(event.beta(), 0, 2)
                        + ", gamma=" + nf(event.gamma(), 0, 2)
                        + ", magnitude=" + nf(event.magnitude(), 0, 2)
                        + ", magnitudeDelta=" + nf(magnitudeDelta, 0, 2)
                        + ", axisDelta=" + nf(axisDelta, 0, 2));
            }
        }
    }

    private void handleButtonEvent(String sessionId, String controlId) {
        switch (controlId) {
            case "action1" -> userPulseBoosts.put(sessionId, 1.0f);
            case "action2" -> userHueVelocities.put(sessionId, random(-12f, 12f));
            case "action3" -> {
                userTargetPositions.put(sessionId, findNonOverlappingPosition());
                userPulseBoosts.put(sessionId, max(userPulseBoosts.getOrDefault(sessionId, 0f), 0.45f));
            }
            default -> {
            }
        }
    }

    private void processAudio() {
        globalAudioLevel = 0;
        int totalSessions = 0;
        
        for (String sessionId : audioBuffer.getActiveSessionIds()) {
            // Audio is handled separately from JSON events. Each active audio stream contributes
            // per-user level data plus a shared global meter.
            float level = calculateAudioLevel(sessionId);
            userAudioLevels.put(sessionId, new float[]{level});
            
            if (!userPositions.containsKey(sessionId)) {
                initializeUser(sessionId);
            }
            
            globalAudioLevel += level;
            totalSessions++;
        }
        
        if (totalSessions > 0) {
            globalAudioLevel /= totalSessions;
        }
        
        smoothedGlobalAudioLevel = lerp(smoothedGlobalAudioLevel, globalAudioLevel, 0.3f);
    }
    
    private void initializeUser(String sessionId) {
        float[] position = findNonOverlappingPosition();
        userPositions.put(sessionId, position);
        userTargetPositions.put(sessionId, position.clone());
        userColors.put(sessionId, new float[]{random(360), 70, 100});
        userSizes.put(sessionId, DEFAULT_SIZE);
        userSpeeds.put(sessionId, DEFAULT_SPEED);
        userGains.put(sessionId, DEFAULT_GAIN);
        userPulseBoosts.put(sessionId, 0f);
        userHueVelocities.put(sessionId, 0f);
        userMotion.put(sessionId, new float[]{0f, 0f, 0f, 0f, 0f, 0f, 0f});
        userLastMotionMagnitudes.put(sessionId, 0f);
        userKeyStates.put(sessionId, new HashMap<>());
        userNames.putIfAbsent(sessionId, "");
        
        if (debugConfig.isLogging()) {
            println("Initialized user " + sessionId.substring(0, 8) + " at position (" 
                    + nf(position[0], 0, 2) + ", " + nf(position[1], 0, 2) + ")");
        }
    }

    private void removeUser(String sessionId) {
        localOperatorLayer.clearSelectionIfMatches(sessionId);
        userPositions.remove(sessionId);
        userTargetPositions.remove(sessionId);
        userColors.remove(sessionId);
        userAudioLevels.remove(sessionId);
        userSizes.remove(sessionId);
        userSpeeds.remove(sessionId);
        userGains.remove(sessionId);
        userPulseBoosts.remove(sessionId);
        userHueVelocities.remove(sessionId);
        userMotion.remove(sessionId);
        userLastMotionMagnitudes.remove(sessionId);
        userKeyStates.remove(sessionId);
        userNames.remove(sessionId);

        if (debugConfig.isLogging()) {
            println("Removed user state for " + sessionId.substring(0, 8));
        }
    }
    
    private float[] findNonOverlappingPosition() {
        float minDistance = 0.15f; // Minimum distance between user centers (15% of canvas)
        int maxAttempts = 50;
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            float x = random(0.1f, 0.9f);
            float y = random(0.15f, 0.85f);
            
            boolean overlaps = false;
            for (float[] existingPos : userPositions.values()) {
                float dx = x - existingPos[0];
                float dy = y - existingPos[1];
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                
                if (distance < minDistance) {
                    overlaps = true;
                    break;
                }
            }
            
            if (!overlaps || attempt == maxAttempts - 1) {
                return new float[]{x, y};
            }
        }
        
        return new float[]{random(0.1f, 0.9f), random(0.15f, 0.85f)};
    }

    private float calculateAudioLevel(String sessionId) {
        byte[] data = audioBuffer.poll(sessionId);
        if (data == null || data.length == 0) {
            float[] existing = userAudioLevels.get(sessionId);
            return existing != null ? existing[0] * 0.9f : 0;
        }
        
        if (debugConfig.isLogging()) {
            audioFrameCount++;
            if (audioFrameCount <= 5) {
                println("Processing audio data: " + data.length + " bytes, session: " + sessionId.substring(0, 8));
            }
        }
        
        float sum = 0;
        int samples = data.length / 2;
        // Map the normalized gain slider to a musically useful range around unity so the midpoint
        // feels like "leave the signal alone" instead of "50% volume".
        float gainValue = userGains.getOrDefault(sessionId, DEFAULT_GAIN);
        float gainFactor = pow(2.0f, (gainValue - 0.5f) * 4.0f);
        
        for (int i = 0; i < samples; i++) {
            short sample = (short) ((data[i * 2] & 0xFF) | (data[i * 2 + 1] << 8));
            float normalized = constrain((sample / 32768.0f) * gainFactor, -1.0f, 1.0f);
            sum += Math.abs(normalized);
        }
        
        float level = sum / samples;
        return level;
    }

    private void drawUsers() {
        for (Map.Entry<String, float[]> entry : userPositions.entrySet()) {
            String sessionId = entry.getKey();
            float[] pos = entry.getValue();
            float[] targetPos = userTargetPositions.getOrDefault(sessionId, pos);
            float[] color = userColors.getOrDefault(sessionId, new float[]{0, 50, 100});
            float[] audioLevel = userAudioLevels.getOrDefault(sessionId, new float[]{0});
            float sizeValue = userSizes.getOrDefault(sessionId, DEFAULT_SIZE);
            float pulseBoost = userPulseBoosts.getOrDefault(sessionId, 0f);
            float hueVelocity = userHueVelocities.getOrDefault(sessionId, 0f);
            float speedValue = userSpeeds.getOrDefault(sessionId, DEFAULT_SPEED);
            float[] motion = userMotion.getOrDefault(sessionId, new float[]{0f, 0f, 0f, 0f, 0f, 0f, 0f});

            // One slider drives both movement response and how quickly temporary effects settle.
            float updateSpeed = map(speedValue, 0, 1, 0.04f, 0.35f);
            float animationSpeed = map(speedValue, 0, 1, 0.85f, 2.8f);
            float decay = map(speedValue, 0, 1, 0.035f, 0.18f);
            // Motion tilt is applied as a bounded offset around the touch target so phone sensors
            // feel expressive without discarding the existing touch interaction model.
            float gammaOffset = map(
                motion[2],
                -motionConfig.getGammaClampDegrees(),
                motionConfig.getGammaClampDegrees(),
                -motionConfig.getTiltOffsetNormalized(),
                motionConfig.getTiltOffsetNormalized()
            );
            float betaOffset = map(
                motion[1],
                -motionConfig.getBetaClampDegrees(),
                motionConfig.getBetaClampDegrees(),
                -motionConfig.getTiltOffsetNormalized(),
                motionConfig.getTiltOffsetNormalized()
            );
            float displayX = constrain(targetPos[0] + gammaOffset, 0.05f, 0.95f);
            float displayY = constrain(targetPos[1] + betaOffset, 0.1f, 0.9f);

            pos[0] = lerp(pos[0], displayX, updateSpeed);
            pos[1] = lerp(pos[1], displayY, updateSpeed);

            hueVelocity = lerp(hueVelocity, 0, decay);
            color[0] = (color[0] + hueVelocity * animationSpeed + 360) % 360;
            userHueVelocities.put(sessionId, hueVelocity);

            pulseBoost = lerp(pulseBoost, 0, decay);
            userPulseBoosts.put(sessionId, pulseBoost);
            
            float baseSize = map(sizeValue, 0, 1, 20, 90);
            // The inner circle reflects the requested size; the outer ring stays proportional and
            // expands further with live audio and button-triggered pulse boosts.
            float audioScale = 1 + audioLevel[0] * 2 + pulseBoost * 1.8f;
            float coreSize = baseSize;
            float pulseSize = coreSize * (1.5f * audioScale);
            
            fill(color[0], color[1], color[2]);
            noStroke();
            ellipse(pos[0] * width, pos[1] * height, coreSize, coreSize);
            
            stroke(color[0], color[1] * 0.5f, color[2] * 0.5f);
            strokeWeight(2);
            noFill();
            ellipse(pos[0] * width, pos[1] * height, pulseSize, pulseSize);
            
            if (localOperatorLayer.showNames()) {
                fill(0);
                noStroke();
                textAlign(CENTER, CENTER);
                float labelTextSize = constrain(coreSize * 0.2f, 10f, 18f);
                textSize(labelTextSize);
                String name = userNames.getOrDefault(sessionId, "").trim();
                String label = name.isEmpty() ? DEFAULT_USER_LABEL : name;
                text(label, pos[0] * width, pos[1] * height);
            }
        }
    }

    private float baseSizePixels(String sessionId) {
        return map(userSizes.getOrDefault(sessionId, DEFAULT_SIZE), 0, 1, 20, 90);
    }

    private float ringScale(String sessionId) {
        float[] audioLevel = userAudioLevels.getOrDefault(sessionId, new float[]{0});
        float pulseBoost = userPulseBoosts.getOrDefault(sessionId, 0f);
        return 1.5f * (1 + audioLevel[0] * 2 + pulseBoost * 1.8f);
    }

    private void scatterAllUsers() {
        for (String sessionId : userPositions.keySet()) {
            userTargetPositions.put(sessionId, findNonOverlappingPosition());
            userPulseBoosts.put(sessionId, max(userPulseBoosts.getOrDefault(sessionId, 0f), 0.35f));
        }
    }

    private void recenterAllUsers() {
        int count = userPositions.size();
        if (count == 0) {
            return;
        }

        float radius = min(0.3f, 0.08f + count * 0.025f);
        int index = 0;
        for (String sessionId : userPositions.keySet()) {
            float angle = TWO_PI * index / count;
            float x = constrain(0.5f + cos(angle) * radius, 0.12f, 0.88f);
            float y = constrain(0.5f + sin(angle) * radius, 0.16f, 0.84f);
            float[] targetPos = userTargetPositions.get(sessionId);
            if (targetPos != null) {
                targetPos[0] = x;
                targetPos[1] = y;
            }
            index++;
        }
    }

    @Override
    public void mousePressed() {
        localOperatorLayer.mousePressed(this, localOperatorAdapter);
    }

    @Override
    public void mouseDragged() {
        localOperatorLayer.mouseDragged(this, localOperatorAdapter);
    }

    @Override
    public void mouseReleased() {
        localOperatorLayer.mouseReleased();
    }

    @Override
    public void mouseWheel(MouseEvent event) {
        localOperatorLayer.mouseWheel(this, event, localOperatorAdapter);
    }

    @Override
    public void keyPressed() {
        localOperatorLayer.keyPressed(key, localOperatorAdapter);
    }

    private void drawAudioMeter() {
        float meterWidth = width * 0.8f;
        float meterHeight = 20;
        float meterX = (width - meterWidth) / 2;
        float meterY = height - 40;
        
        fill(30, 50, 30);
        noStroke();
        rect(meterX, meterY, meterWidth, meterHeight, 5);
        
        float levelWidth = meterWidth * smoothedGlobalAudioLevel;
        float hue = map(smoothedGlobalAudioLevel, 0, 1, 120, 0);
        fill(hue, 80, 100);
        rect(meterX, meterY, levelWidth, meterHeight, 5);
        
        fill(255);
        textAlign(CENTER, CENTER);
        textSize(12);
        text("Global Audio Level", width / 2f, meterY - 10);
        
        int activeSessions = audioBuffer.getActiveSessionCount();
        if (activeSessions > 0) {
            textSize(10);
            fill(200);
            text(activeSessions + " audio stream" + (activeSessions != 1 ? "s" : "") + " active", 
                 width / 2f, meterY + meterHeight + 15);
        }
    }

    public void runSketch() {
        String[] args = {this.getClass().getName()};
        PApplet.runSketch(args, this);
    }
}
