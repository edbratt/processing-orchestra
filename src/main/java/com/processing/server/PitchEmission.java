/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

public record PitchEmission(String note, int midiNote, float frequencyHz, float level) {
}
