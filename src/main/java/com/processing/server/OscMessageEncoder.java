/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

final class OscMessageEncoder {
    private OscMessageEncoder() {
    }

    static byte[] encode(String address, Object... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeOscString(out, address);
        writeOscString(out, typeTag(args));
        for (Object arg : args) {
            writeArgument(out, arg);
        }
        return out.toByteArray();
    }

    private static String typeTag(Object[] args) {
        StringBuilder tag = new StringBuilder(",");
        for (Object arg : args) {
            if (arg instanceof Float || arg instanceof Double) {
                tag.append('f');
            } else if (arg instanceof Integer || arg instanceof Long) {
                tag.append('i');
            } else if (arg instanceof String) {
                tag.append('s');
            } else {
                throw new IllegalArgumentException("Unsupported OSC argument type: " + arg.getClass().getName());
            }
        }
        return tag.toString();
    }

    private static void writeArgument(ByteArrayOutputStream out, Object arg) {
        if (arg instanceof Float value) {
            writeInt(out, Float.floatToIntBits(value));
        } else if (arg instanceof Double value) {
            writeInt(out, Float.floatToIntBits(value.floatValue()));
        } else if (arg instanceof Integer value) {
            writeInt(out, value);
        } else if (arg instanceof Long value) {
            writeInt(out, value.intValue());
        } else if (arg instanceof String value) {
            writeOscString(out, value);
        } else {
            throw new IllegalArgumentException("Unsupported OSC argument type: " + arg.getClass().getName());
        }
    }

    private static void writeInt(ByteArrayOutputStream out, int value) {
        byte[] bytes = ByteBuffer.allocate(Integer.BYTES)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(value)
            .array();
        out.writeBytes(bytes);
    }

    private static void writeOscString(ByteArrayOutputStream out, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeBytes(bytes);
        out.write(0);
        int padding = paddingFor(bytes.length + 1);
        for (int i = 0; i < padding; i++) {
            out.write(0);
        }
    }

    private static int paddingFor(int lengthWithTerminator) {
        int remainder = lengthWithTerminator % 4;
        return remainder == 0 ? 0 : 4 - remainder;
    }
}
