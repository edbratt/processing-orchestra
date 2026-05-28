/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class OscMessageEncoderTest {
    @Test
    void encodesFloatMessageWithPaddedAddressAndTypeTag() {
        byte[] packet = OscMessageEncoder.encode("/walker/signal/x", 0.5f);

        assertEquals("/walker/signal/x", oscString(packet, 0));
        assertEquals(",f", oscString(packet, 20));
        assertEquals(Float.floatToIntBits(0.5f), intAt(packet, 24));
        assertEquals(28, packet.length);
    }

    @Test
    void encodesStringAndIntegerArguments() {
        byte[] packet = OscMessageEncoder.encode("/test/ping", "ping", 7);

        assertEquals("/test/ping", oscString(packet, 0));
        assertEquals(",si", oscString(packet, 12));
        assertEquals("ping", oscString(packet, 16));
        assertEquals(7, intAt(packet, 24));
        assertEquals(28, packet.length);
    }

    @Test
    void usesBigEndianFloatBytes() {
        byte[] packet = OscMessageEncoder.encode("/x", 1.0f);
        byte[] expected = ByteBuffer.allocate(4)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(Float.floatToIntBits(1.0f))
            .array();

        assertArrayEquals(expected, new byte[]{packet[8], packet[9], packet[10], packet[11]});
    }

    private String oscString(byte[] packet, int offset) {
        int end = offset;
        while (end < packet.length && packet[end] != 0) {
            end++;
        }
        return new String(packet, offset, end - offset, StandardCharsets.UTF_8);
    }

    private int intAt(byte[] packet, int offset) {
        return ByteBuffer.wrap(packet, offset, 4).order(ByteOrder.BIG_ENDIAN).getInt();
    }
}
