/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

public class MotionConfig {
    private final int updateHz;
    private final float alphaClampDegrees;
    private final float betaClampDegrees;
    private final float gammaClampDegrees;
    private final float accelerationClampG;
    private final float magnitudeClampG;
    private final float tiltOffsetNormalized;
    private final float shakeThresholdG;
    private final float shakeBurstScale;
    private final boolean debugLogging;
    private final int debugSampleLimit;

    public MotionConfig(int updateHz,
                        float alphaClampDegrees,
                        float betaClampDegrees,
                        float gammaClampDegrees,
                        float accelerationClampG,
                        float magnitudeClampG,
                        float tiltOffsetNormalized,
                        float shakeThresholdG,
                        float shakeBurstScale,
                        boolean debugLogging,
                        int debugSampleLimit) {
        this.updateHz = updateHz;
        this.alphaClampDegrees = alphaClampDegrees;
        this.betaClampDegrees = betaClampDegrees;
        this.gammaClampDegrees = gammaClampDegrees;
        this.accelerationClampG = accelerationClampG;
        this.magnitudeClampG = magnitudeClampG;
        this.tiltOffsetNormalized = tiltOffsetNormalized;
        this.shakeThresholdG = shakeThresholdG;
        this.shakeBurstScale = shakeBurstScale;
        this.debugLogging = debugLogging;
        this.debugSampleLimit = debugSampleLimit;
    }

    public int getUpdateHz() {
        return updateHz;
    }

    public float getAlphaClampDegrees() {
        return alphaClampDegrees;
    }

    public float getBetaClampDegrees() {
        return betaClampDegrees;
    }

    public float getGammaClampDegrees() {
        return gammaClampDegrees;
    }

    public float getAccelerationClampG() {
        return accelerationClampG;
    }

    public float getMagnitudeClampG() {
        return magnitudeClampG;
    }

    public float getTiltOffsetNormalized() {
        return tiltOffsetNormalized;
    }

    public float getShakeThresholdG() {
        return shakeThresholdG;
    }

    public float getShakeBurstScale() {
        return shakeBurstScale;
    }

    public boolean isDebugLogging() {
        return debugLogging;
    }

    public int getDebugSampleLimit() {
        return debugSampleLimit;
    }
}
