/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

public class AudioConfig {
    private final int sampleRate;
    private final int channels;
    private final int bufferSize;
    private final String description;

    public AudioConfig(int sampleRate, int channels, int bufferSize, String description) {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.bufferSize = bufferSize;
        this.description = description;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getChannels() {
        return channels;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public String getDescription() {
        return description;
    }

    public int getBytesPerSample() {
        return 2;
    }

    public int getBytesPerFrame() {
        return channels * getBytesPerSample();
    }

    public int getBytesPerSecond() {
        return sampleRate * getBytesPerFrame();
    }
}