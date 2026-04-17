/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import processing.core.PApplet;
import processing.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GravityOrbitGradientSketch extends PApplet {
    private static final float DEFAULT_SIZE = 0.5f;
    private static final float DEFAULT_SPEED = 0.5f;
    private static final float DEFAULT_GAIN = 0.5f;
    private static final String DEFAULT_USER_LABEL = "Guest";
    private static final float MIN_CORE_SCALE = 0.82f;
    private static final float MAX_CORE_LEVEL_SCALE = 1.65f;
    private static final float BASE_ATTRACTION_FORCE = 0.0012f;
    private static final float BASE_ORBIT_FORCE = 0.00055f;
    private static final float BASE_SHAKE_IMPULSE = 0.028f;
    private static final float VELOCITY_DAMPING = 0.92f;
    private static final float MAX_VELOCITY = 0.022f;
    private static final int BACKGROUND_FIELD_STEP = 16;

    private final EventQueue eventQueue;
    private final AudioBuffer audioBuffer;
    private final int sketchWidth;
    private final int sketchHeight;
    private final DebugConfig debugConfig;
    private final MotionConfig motionConfig;

    private final Map<String, float[]> userPositions = new HashMap<>();
    private final Map<String, float[]> userTargetPositions = new HashMap<>();
    private final Map<String, float[]> userVelocities = new HashMap<>();
    private final Map<String, float[]> userColors = new HashMap<>();
    private final Map<String, Float> userBaseHues = new HashMap<>();
    private final Map<String, float[]> userAudioLevels = new HashMap<>();
    private final Map<String, Float> userDominantFrequencies = new HashMap<>();
    private final Map<String, Float> userSpeedColorLevels = new HashMap<>();
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
            float coreSize = coreSizePixels(sessionId);
            float ringSize = max(coreSize * 1.25f, baseSizePixels(sessionId) * ringScale(sessionId));
            return max(coreSize, ringSize) * 0.5f + 4f;
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
            return userDominantFrequencies.getOrDefault(sessionId, 0f);
        }

        @Override
        public boolean supportsDominantFrequency() {
            return true;
        }

        @Override
        public float speed(String sessionId) {
            float[] velocity = userVelocities.get(sessionId);
            return velocity == null ? 0f : sqrt(velocity[0] * velocity[0] + velocity[1] * velocity[1]);
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
            float[] position = userPositions.get(sessionId);
            float[] velocity = userVelocities.get(sessionId);
            if (targetPos == null || position == null || velocity == null) {
                return;
            }
            targetPos[0] = normalizedX;
            targetPos[1] = normalizedY;
            if (snapImmediately) {
                position[0] = normalizedX;
                position[1] = normalizedY;
                velocity[0] = 0f;
                velocity[1] = 0f;
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

    private float globalAudioLevel = 0f;
    private float smoothedGlobalAudioLevel = 0f;
    private int audioFrameCount = 0;
    private int motionFrameCount = 0;

    public GravityOrbitGradientSketch(EventQueue eventQueue,
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
        surface.setTitle("Processing Server - Gravity Orbit Gradient Sketch");
        frameRate(60);
        background(0);
        colorMode(HSB, 360, 100, 100, 100);
        if (debugConfig.isLogging()) {
            println("Gravity orbit gradient sketch initialized: " + sketchWidth + "x" + sketchHeight);
        }
    }

    @Override
    public void draw() {
        background(210, 18, 10, 100);
        processEvents();
        processAudio();
        updateUserPhysics(localOperatorLayer.physicsTimeScale());
        updateUserSpeedColorLevels();
        updateUserVisualState();
        drawAttractionGradientBackground();
        drawUsers();
        drawInteractionLines();
        drawVelocityVectors();
        localOperatorLayer.drawSelectionOverlay(this, localOperatorAdapter, 100f);
        localOperatorLayer.drawLocalHud(this, localOperatorAdapter, 70f, 100f);
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
                if (!userPositions.containsKey(sessionId)) {
                    initializeUser(sessionId);
                }
                float[] targetPos = userTargetPositions.get(sessionId);
                targetPos[0] = event.x();
                targetPos[1] = event.y();
            }
            case "slider" -> {
                if (!userPositions.containsKey(sessionId)) {
                    initializeUser(sessionId);
                }
                if ("sizeSlider".equals(event.controlId())) {
                    userSizes.put(sessionId, constrain(event.value(), 0, 1));
                } else if ("speedSlider".equals(event.controlId())) {
                    userSpeeds.put(sessionId, constrain(event.value(), 0, 1));
                } else if ("gainSlider".equals(event.controlId())) {
                    userGains.put(sessionId, constrain(event.value(), 0, 1));
                }
            }
            case "button" -> {
                if (!userPositions.containsKey(sessionId)) {
                    initializeUser(sessionId);
                }
                handleButtonEvent(sessionId, event.controlId());
            }
            case "motion" -> {
                if (!userPositions.containsKey(sessionId)) {
                    initializeUser(sessionId);
                }
                handleMotionEvent(sessionId, event);
            }
            case "key" -> {
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
            default -> {
            }
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
            userPulseBoosts.put(sessionId, max(userPulseBoosts.getOrDefault(sessionId, 0f), burstBoost));
            userHueVelocities.put(sessionId,
                userHueVelocities.getOrDefault(sessionId, 0f) + random(-6f, 6f) * max(0.4f, normalizedShake));
            applyShakeImpulse(sessionId, normalizedShake);
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
        globalAudioLevel = 0f;
        int totalSessions = 0;

        for (String sessionId : audioBuffer.getActiveSessionIds()) {
            if (!userPositions.containsKey(sessionId)) {
                initializeUser(sessionId);
            }

            float level = updateAudioFeatures(sessionId);
            userAudioLevels.put(sessionId, new float[]{level});

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
        userVelocities.put(sessionId, new float[]{0f, 0f});
        float baseHue = random(360);
        userColors.put(sessionId, new float[]{baseHue, random(26f, 42f), random(78f, 90f)});
        userBaseHues.put(sessionId, baseHue);
        userSizes.put(sessionId, DEFAULT_SIZE);
        userDominantFrequencies.put(sessionId, 0f);
        userSpeedColorLevels.put(sessionId, 0f);
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
        userVelocities.remove(sessionId);
        userColors.remove(sessionId);
        userBaseHues.remove(sessionId);
        userAudioLevels.remove(sessionId);
        userDominantFrequencies.remove(sessionId);
        userSpeedColorLevels.remove(sessionId);
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
        float minDistance = 0.15f;
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

    private float updateAudioFeatures(String sessionId) {
        byte[] data = audioBuffer.poll(sessionId);
        float previousLevel = userAudioLevels.getOrDefault(sessionId, new float[]{0f})[0];
        float previousFrequency = userDominantFrequencies.getOrDefault(sessionId, 0f);
        if (data == null || data.length == 0) {
            return previousLevel * 0.9f;
        }

        float gainValue = userGains.getOrDefault(sessionId, DEFAULT_GAIN);
        float gainFactor = pow(2.0f, (gainValue - 0.5f) * 4.0f);
        AudioFeatureAnalyzer.AudioAnalysisResult analysis = AudioFeatureAnalyzer.analyze(
            data,
            audioBuffer.getChannels(),
            audioBuffer.getSampleRate(),
            gainFactor,
            previousFrequency
        );
        userDominantFrequencies.put(sessionId, analysis.dominantFrequency());

        if (debugConfig.isAudioLogging()
            && analysis.level() >= AudioFeatureAnalyzer.MIN_DETECTION_LEVEL
            && audioFrameCount < debugConfig.getAudioSampleLimit()) {
            audioFrameCount++;
            println("Processing audio data: " + data.length + " bytes, session: " + sessionId.substring(0, 8));
            println("Audio features for " + sessionId.substring(0, 8)
                + ": level=" + nf(analysis.level(), 0, 3)
                + ", frequency=" + nf(analysis.dominantFrequency(), 0, 1) + "Hz");
        }
        return analysis.level();
    }

    private void updateUserPhysics(float timeScale) {
        if (userPositions.isEmpty() || timeScale <= 0f) {
            return;
        }

        List<String> sessionIds = new ArrayList<>(userPositions.keySet());
        Map<String, float[]> forces = new HashMap<>();
        for (String sessionId : sessionIds) {
            float[] force = new float[]{0f, 0f};
            applyAnchorForce(sessionId, force);
            forces.put(sessionId, force);
        }

        for (int i = 0; i < sessionIds.size(); i++) {
            String sessionA = sessionIds.get(i);
            float[] posA = userPositions.get(sessionA);
            if (posA == null) {
                continue;
            }
            for (int j = i + 1; j < sessionIds.size(); j++) {
                String sessionB = sessionIds.get(j);
                float[] posB = userPositions.get(sessionB);
                if (posB == null) {
                    continue;
                }

                float dx = posB[0] - posA[0];
                float dy = posB[1] - posA[1];
                float distanceSq = dx * dx + dy * dy;
                if (distanceSq < 0.000001f) {
                    dx = random(-0.01f, 0.01f);
                    dy = random(-0.01f, 0.01f);
                    distanceSq = dx * dx + dy * dy;
                }

                float distance = sqrt(distanceSq);
                float nx = dx / distance;
                float ny = dy / distance;
                float minDistance = outerRadiusNormalized(sessionA) + outerRadiusNormalized(sessionB);
                float pairAttraction = 0.5f * (attractionModifier(sessionA) + attractionModifier(sessionB));
                float gravityStrength = BASE_ATTRACTION_FORCE * pairAttraction * max(distance - minDistance, 0f) * timeScale;
                float orbitStrength = BASE_ORBIT_FORCE * pairAttraction * timeScale;
                float orbitDirection = orbitDirection(sessionA, sessionB);

                float[] forceA = forces.get(sessionA);
                float[] forceB = forces.get(sessionB);
                forceA[0] += nx * gravityStrength + (-ny * orbitStrength * orbitDirection);
                forceA[1] += ny * gravityStrength + (nx * orbitStrength * orbitDirection);
                forceB[0] -= nx * gravityStrength + (-ny * orbitStrength * orbitDirection);
                forceB[1] -= ny * gravityStrength + (nx * orbitStrength * orbitDirection);

                if (distance < minDistance) {
                    float overlap = minDistance - distance;
                    float correctionX = nx * overlap * 0.5f;
                    float correctionY = ny * overlap * 0.5f;
                    posA[0] -= correctionX;
                    posA[1] -= correctionY;
                    posB[0] += correctionX;
                    posB[1] += correctionY;
                    clampToBounds(sessionA, posA, userVelocities.get(sessionA));
                    clampToBounds(sessionB, posB, userVelocities.get(sessionB));
                }
            }
        }

        for (String sessionId : sessionIds) {
            float[] pos = userPositions.get(sessionId);
            float[] velocity = userVelocities.computeIfAbsent(sessionId, key -> new float[]{0f, 0f});
            float[] force = forces.get(sessionId);
            if (pos == null || force == null) {
                continue;
            }

            velocity[0] = (velocity[0] + force[0]) * VELOCITY_DAMPING;
            velocity[1] = (velocity[1] + force[1]) * VELOCITY_DAMPING;
            limitVelocity(velocity);

            pos[0] += velocity[0] * timeScale;
            pos[1] += velocity[1] * timeScale;
            clampToBounds(sessionId, pos, velocity);
        }
    }

    private void updateUserSpeedColorLevels() {
        if (userPositions.isEmpty()) {
            return;
        }

        for (Map.Entry<String, float[]> entry : userVelocities.entrySet()) {
            String sessionId = entry.getKey();
            float[] velocity = entry.getValue();
            float speed = sqrt(velocity[0] * velocity[0] + velocity[1] * velocity[1]);
            float normalizedSpeed = constrain(speed / MAX_VELOCITY, 0f, 1f);
            float smoothedSpeed = lerp(
                userSpeedColorLevels.getOrDefault(sessionId, 0f),
                normalizedSpeed,
                0.28f
            );
            userSpeedColorLevels.put(sessionId, smoothedSpeed);
        }
    }

    private void updateUserVisualState() {
        for (Map.Entry<String, float[]> entry : userPositions.entrySet()) {
            String sessionId = entry.getKey();
            float[] color = userColors.getOrDefault(sessionId, new float[]{0f, 50f, 100f});
            float level = userAudioLevels.getOrDefault(sessionId, new float[]{0f})[0];
            float pulseBoost = userPulseBoosts.getOrDefault(sessionId, 0f);
            float hueVelocity = userHueVelocities.getOrDefault(sessionId, 0f);
            float speedValue = userSpeeds.getOrDefault(sessionId, DEFAULT_SPEED);
            float speedLevel = userSpeedColorLevels.getOrDefault(sessionId, 0f);
            float animationSpeed = map(speedValue, 0f, 1f, 0.85f, 2.8f);
            float decay = map(speedValue, 0f, 1f, 0.035f, 0.18f);
            float dominantFrequency = userDominantFrequencies.getOrDefault(sessionId, 0f);
            float fallbackHue = userBaseHues.getOrDefault(sessionId, color[0]);
            float frequencyHue = dominantFrequency > 0f ? frequencyToHue(dominantFrequency) : fallbackHue;

            hueVelocity = lerp(hueVelocity, 0f, decay);
            pulseBoost = lerp(pulseBoost, 0f, decay);

            color[0] = (frequencyHue + hueVelocity * animationSpeed + speedLevel * 54f + 360f) % 360f;
            color[1] = constrain(24f + level * 18f + speedLevel * 58f, 0f, 100f);
            color[2] = constrain(72f + level * 20f + speedLevel * 26f, 0f, 100f);

            userHueVelocities.put(sessionId, hueVelocity);
            userPulseBoosts.put(sessionId, pulseBoost);
        }
    }

    private void applyAnchorForce(String sessionId, float[] force) {
        float[] pos = userPositions.get(sessionId);
        float[] target = userTargetPositions.get(sessionId);
        if (pos == null || target == null) {
            return;
        }
        float speedValue = userSpeeds.getOrDefault(sessionId, DEFAULT_SPEED);
        float anchorStrength = map(speedValue, 0f, 1f, 0.008f, 0.02f);
        force[0] += (target[0] - pos[0]) * anchorStrength;
        force[1] += (target[1] - pos[1]) * anchorStrength;
    }

    private void applyShakeImpulse(String sessionId, float normalizedShake) {
        float[] velocity = userVelocities.computeIfAbsent(sessionId, key -> new float[]{0f, 0f});
        float[] direction = directionAwayFromOthers(sessionId);
        velocity[0] += direction[0] * BASE_SHAKE_IMPULSE * normalizedShake;
        velocity[1] += direction[1] * BASE_SHAKE_IMPULSE * normalizedShake;
        limitVelocity(velocity);
    }

    private float[] directionAwayFromOthers(String sessionId) {
        float[] origin = userPositions.get(sessionId);
        if (origin == null || userPositions.size() <= 1) {
            return directionFromCenter(origin);
        }

        float otherX = 0f;
        float otherY = 0f;
        int count = 0;
        for (Map.Entry<String, float[]> entry : userPositions.entrySet()) {
            if (entry.getKey().equals(sessionId)) {
                continue;
            }
            otherX += entry.getValue()[0];
            otherY += entry.getValue()[1];
            count++;
        }

        if (count == 0) {
            return directionFromCenter(origin);
        }

        float dx = origin[0] - (otherX / count);
        float dy = origin[1] - (otherY / count);
        float magnitude = sqrt(dx * dx + dy * dy);
        if (magnitude < 0.0001f) {
            return directionFromCenter(origin);
        }
        return new float[]{dx / magnitude, dy / magnitude};
    }

    private float[] directionFromCenter(float[] origin) {
        if (origin == null) {
            float angle = random(TWO_PI);
            return new float[]{cos(angle), sin(angle)};
        }
        float dx = origin[0] - 0.5f;
        float dy = origin[1] - 0.5f;
        float magnitude = sqrt(dx * dx + dy * dy);
        if (magnitude < 0.0001f) {
            float angle = random(TWO_PI);
            return new float[]{cos(angle), sin(angle)};
        }
        return new float[]{dx / magnitude, dy / magnitude};
    }

    private float attractionModifier(String sessionId) {
        float[] motion = userMotion.getOrDefault(sessionId, new float[]{0f, 0f, 0f, 0f, 0f, 0f, 0f});
        float betaComponent = motionConfig.getBetaClampDegrees() <= 0f
            ? 0f
            : motion[1] / motionConfig.getBetaClampDegrees();
        float gammaComponent = motionConfig.getGammaClampDegrees() <= 0f
            ? 0f
            : motion[2] / motionConfig.getGammaClampDegrees();
        float shakeComponent = motionConfig.getMagnitudeClampG() <= 0f
            ? 0f
            : motion[6] / motionConfig.getMagnitudeClampG();
        return constrain(1.0f + betaComponent * 0.85f + gammaComponent * 0.35f - shakeComponent * 0.45f, 0.2f, 2.2f);
    }

    private float orbitDirection(String sessionA, String sessionB) {
        return ((sessionA.hashCode() ^ sessionB.hashCode()) & 1) == 0 ? 1f : -1f;
    }

    private void limitVelocity(float[] velocity) {
        float speed = sqrt(velocity[0] * velocity[0] + velocity[1] * velocity[1]);
        if (speed > MAX_VELOCITY) {
            float scale = MAX_VELOCITY / speed;
            velocity[0] *= scale;
            velocity[1] *= scale;
        }
    }

    private void clampToBounds(String sessionId, float[] pos, float[] velocity) {
        float radius = outerRadiusNormalized(sessionId);
        float minX = radius;
        float maxX = 1f - radius;
        float minY = radius;
        float maxY = 1f - radius;

        if (pos[0] < minX) {
            pos[0] = minX;
            if (velocity != null) {
                velocity[0] *= -0.35f;
            }
        } else if (pos[0] > maxX) {
            pos[0] = maxX;
            if (velocity != null) {
                velocity[0] *= -0.35f;
            }
        }

        if (pos[1] < minY) {
            pos[1] = minY;
            if (velocity != null) {
                velocity[1] *= -0.35f;
            }
        } else if (pos[1] > maxY) {
            pos[1] = maxY;
            if (velocity != null) {
                velocity[1] *= -0.35f;
            }
        }
    }

    private float outerRadiusNormalized(String sessionId) {
        float coreSize = coreSizePixels(sessionId);
        float pulseSize = max(coreSize * 1.25f, baseSizePixels(sessionId) * ringScale(sessionId));
        float radiusPixels = max(coreSize, pulseSize) * 0.5f + 4f;
        return radiusPixels / min(width, height);
    }

    private float baseSizePixels(String sessionId) {
        return map(userSizes.getOrDefault(sessionId, DEFAULT_SIZE), 0f, 1f, 20f, 90f);
    }

    private float coreSizePixels(String sessionId) {
        float level = userAudioLevels.getOrDefault(sessionId, new float[]{0f})[0];
        return baseSizePixels(sessionId) * (MIN_CORE_SCALE + level * MAX_CORE_LEVEL_SCALE);
    }

    private float ringScale(String sessionId) {
        float pulseBoost = userPulseBoosts.getOrDefault(sessionId, 0f);
        return 1.5f * (1f + pulseBoost * 1.35f);
    }

    private float frequencyToHue(float frequency) {
        float clamped = constrain(
            frequency,
            AudioFeatureAnalyzer.MIN_FREQUENCY_HZ,
            AudioFeatureAnalyzer.MAX_FREQUENCY_HZ
        );
        float minLog = log(AudioFeatureAnalyzer.MIN_FREQUENCY_HZ);
        float maxLog = log(AudioFeatureAnalyzer.MAX_FREQUENCY_HZ);
        float normalized = (log(clamped) - minLog) / max(0.0001f, maxLog - minLog);
        return normalized * 360f;
    }

    private void drawAttractionGradientBackground() {
        if (userPositions.size() < 2) {
            return;
        }

        noStroke();
        for (int y = 0; y < height; y += BACKGROUND_FIELD_STEP) {
            for (int x = 0; x < width; x += BACKGROUND_FIELD_STEP) {
                float[] sample = sampleAttractionField(
                    (x + BACKGROUND_FIELD_STEP * 0.5f) / width,
                    (y + BACKGROUND_FIELD_STEP * 0.5f) / height
                );
                float strength = sample[0];
                if (strength < 0.03f) {
                    continue;
                }

                float hue = sample[1];
                float saturation = 18f + strength * 24f;
                float brightness = 16f + strength * 26f;
                float alpha = 20f + strength * 34f;
                fill(hue, saturation, brightness, alpha);
                rect(x, y, BACKGROUND_FIELD_STEP + 1, BACKGROUND_FIELD_STEP + 1);
            }
        }
    }

    private float[] sampleAttractionField(float sampleX, float sampleY) {
        List<String> sessionIds = new ArrayList<>(userPositions.keySet());
        float totalWeight = 0f;
        float hueX = 0f;
        float hueY = 0f;

        for (int i = 0; i < sessionIds.size(); i++) {
            String sessionA = sessionIds.get(i);
            float[] posA = userPositions.get(sessionA);
            float[] colorA = userColors.get(sessionA);
            if (posA == null || colorA == null) {
                continue;
            }
            for (int j = i + 1; j < sessionIds.size(); j++) {
                String sessionB = sessionIds.get(j);
                float[] posB = userPositions.get(sessionB);
                float[] colorB = userColors.get(sessionB);
                if (posB == null || colorB == null) {
                    continue;
                }

                float dx = posB[0] - posA[0];
                float dy = posB[1] - posA[1];
                float distanceSq = dx * dx + dy * dy;
                if (distanceSq < 0.000001f) {
                    continue;
                }

                float distance = sqrt(distanceSq);
                float minDistance = outerRadiusNormalized(sessionA) + outerRadiusNormalized(sessionB);
                float pairAttraction = 0.5f * (attractionModifier(sessionA) + attractionModifier(sessionB));
                float gravityStrength = BASE_ATTRACTION_FORCE * pairAttraction * max(distance - minDistance, 0f);
                float normalizedGravity = constrain(gravityStrength * 1800f, 0f, 1f);
                if (normalizedGravity <= 0f) {
                    continue;
                }

                float t = projectionFactor(sampleX, sampleY, posA[0], posA[1], posB[0], posB[1]);
                float closestX = lerp(posA[0], posB[0], t);
                float closestY = lerp(posA[1], posB[1], t);
                float corridorDistance = dist(sampleX, sampleY, closestX, closestY);
                float corridorWeight = 1f / (1f + corridorDistance * 10f);
                float endpointWeight = 0.5f * (
                    1f / (1f + dist(sampleX, sampleY, posA[0], posA[1]) * 6f)
                    + 1f / (1f + dist(sampleX, sampleY, posB[0], posB[1]) * 6f)
                );
                float influence = normalizedGravity * corridorWeight * endpointWeight;
                if (influence <= 0.001f) {
                    continue;
                }

                float blendedHue = lerpAngle(colorA[0], colorB[0], t);
                float radians = radians(blendedHue);
                hueX += cos(radians) * influence;
                hueY += sin(radians) * influence;
                totalWeight += influence;
            }
        }

        if (totalWeight <= 0f) {
            return new float[]{0f, 0f};
        }

        float hue = (degrees(atan2(hueY, hueX)) + 360f) % 360f;
        float strength = constrain(totalWeight * 3.2f, 0f, 1f);
        return new float[]{strength, hue};
    }

    private float projectionFactor(float px, float py, float ax, float ay, float bx, float by) {
        float abx = bx - ax;
        float aby = by - ay;
        float lengthSq = abx * abx + aby * aby;
        if (lengthSq < 0.000001f) {
            return 0.5f;
        }
        float apx = px - ax;
        float apy = py - ay;
        return constrain((apx * abx + apy * aby) / lengthSq, 0f, 1f);
    }

    private float lerpAngle(float startHue, float endHue, float amount) {
        float delta = ((endHue - startHue + 540f) % 360f) - 180f;
        return (startHue + delta * amount + 360f) % 360f;
    }

    private void drawUsers() {
        for (Map.Entry<String, float[]> entry : userPositions.entrySet()) {
            String sessionId = entry.getKey();
            float[] pos = entry.getValue();
            float[] color = userColors.getOrDefault(sessionId, new float[]{0f, 50f, 100f});
            float coreSize = coreSizePixels(sessionId);
            float pulseSize = max(coreSize * 1.25f, baseSizePixels(sessionId) * ringScale(sessionId));

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
                textSize(constrain(coreSize * 0.2f, 10f, 18f));
                String name = userNames.getOrDefault(sessionId, "").trim();
                String label = name.isEmpty() ? DEFAULT_USER_LABEL : name;
                text(label, pos[0] * width, pos[1] * height);
            }
        }
    }

    private void drawInteractionLines() {
        if (!localOperatorLayer.showAttractionLines() || userPositions.size() < 2) {
            return;
        }

        List<String> sessionIds = new ArrayList<>(userPositions.keySet());
        stroke(190, 20, 75, 55);
        strokeWeight(1.2f);
        for (int i = 0; i < sessionIds.size(); i++) {
            String sessionA = sessionIds.get(i);
            float[] posA = userPositions.get(sessionA);
            if (posA == null) {
                continue;
            }
            for (int j = i + 1; j < sessionIds.size(); j++) {
                String sessionB = sessionIds.get(j);
                float[] posB = userPositions.get(sessionB);
                if (posB == null) {
                    continue;
                }
                line(posA[0] * width, posA[1] * height, posB[0] * width, posB[1] * height);
            }
        }
    }

    private void drawVelocityVectors() {
        if (!localOperatorLayer.showVelocityVectors()) {
            return;
        }

        stroke(55, 30, 100, 100);
        strokeWeight(2);
        for (Map.Entry<String, float[]> entry : userVelocities.entrySet()) {
            float[] pos = userPositions.get(entry.getKey());
            float[] velocity = entry.getValue();
            if (pos == null || velocity == null) {
                continue;
            }
            float startX = pos[0] * width;
            float startY = pos[1] * height;
            line(startX, startY, startX + velocity[0] * width * 18f, startY + velocity[1] * height * 18f);
        }
    }

    private void scatterAllUsers() {
        for (String sessionId : new ArrayList<>(userPositions.keySet())) {
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
        for (String sessionId : new ArrayList<>(userPositions.keySet())) {
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
        float meterHeight = 20f;
        float meterX = (width - meterWidth) / 2f;
        float meterY = height - 40f;

        fill(30, 50, 30);
        noStroke();
        rect(meterX, meterY, meterWidth, meterHeight, 5);

        float levelWidth = meterWidth * smoothedGlobalAudioLevel;
        float hue = map(smoothedGlobalAudioLevel, 0f, 1f, 120f, 0f);
        fill(hue, 80, 100);
        rect(meterX, meterY, levelWidth, meterHeight, 5);

        fill(255);
        textAlign(CENTER, CENTER);
        textSize(12);
        text("Global Audio Level", width / 2f, meterY - 10f);

        int activeSessions = audioBuffer.getActiveSessionCount();
        if (activeSessions > 0) {
            textSize(10);
            fill(200);
            text(activeSessions + " audio stream" + (activeSessions != 1 ? "s" : "") + " active",
                    width / 2f, meterY + meterHeight + 15f);
        }
    }

    public void runSketch() {
        String[] args = {this.getClass().getName()};
        PApplet.runSketch(args, this);
    }
}
