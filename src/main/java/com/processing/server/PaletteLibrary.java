/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import processing.core.PApplet;

import java.util.List;

public final class PaletteLibrary {
    private PaletteLibrary() {
    }

    public static List<ColorPalette> defaults(PApplet applet) {
        return List.of(
            warm(applet),
            cool(applet),
            earthy(applet)
        );
    }

    public static ColorPalette randomDefault(PApplet applet) {
        List<ColorPalette> palettes = defaults(applet);
        return palettes.get((int) applet.random(palettes.size()));
    }

    public static ColorPalette defaultForSession(PApplet applet, String sessionId) {
        List<ColorPalette> palettes = defaults(applet);
        int index = stableIndex(sessionId, palettes.size());
        return palettes.get(index);
    }

    public static int stableIndex(String seed, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be greater than zero");
        }
        if (seed == null || seed.isBlank()) {
            return 0;
        }
        return Math.floorMod(seed.hashCode(), size);
    }

    public static ColorPalette warm(PApplet applet) {
        return new ColorPalette(
            "Warm",
            applet.color(32, 18, 12),
            applet.color(248, 232, 214),
            applet.color(234, 108, 55),
            new int[] {
                applet.color(196, 64, 36),
                applet.color(222, 116, 52),
                applet.color(237, 169, 72),
                applet.color(166, 58, 44),
                applet.color(247, 214, 167)
            }
        );
    }

    public static ColorPalette cool(PApplet applet) {
        return new ColorPalette(
            "Cool",
            applet.color(12, 24, 38),
            applet.color(228, 242, 248),
            applet.color(53, 168, 196),
            new int[] {
                applet.color(18, 76, 112),
                applet.color(27, 112, 153),
                applet.color(48, 165, 198),
                applet.color(118, 201, 214),
                applet.color(198, 235, 242)
            }
        );
    }

    public static ColorPalette earthy(PApplet applet) {
        return new ColorPalette(
            "Earthy",
            applet.color(28, 24, 18),
            applet.color(232, 223, 204),
            applet.color(128, 110, 66),
            new int[] {
                applet.color(78, 92, 46),
                applet.color(118, 96, 58),
                applet.color(156, 134, 88),
                applet.color(94, 66, 42),
                applet.color(201, 182, 144)
            }
        );
    }

    public static final class ColorPalette {
        private final String name;
        private final int background;
        private final int foreground;
        private final int accent;
        private final int[] colors;

        public ColorPalette(String name,
                            int background,
                            int foreground,
                            int accent,
                            int[] colors) {
            if (colors == null || colors.length == 0) {
                throw new IllegalArgumentException("colors must not be empty");
            }
            this.name = name;
            this.background = background;
            this.foreground = foreground;
            this.accent = accent;
            this.colors = colors.clone();
        }

        public String name() {
            return name;
        }

        public int background() {
            return background;
        }

        public int foreground() {
            return foreground;
        }

        public int accent() {
            return accent;
        }

        public int[] colors() {
            return colors.clone();
        }

        public int randomColor(PApplet applet) {
            return colors[(int) applet.random(colors.length)];
        }

        public int colorAt(int index) {
            return colors[Math.floorMod(index, colors.length)];
        }

        public int colorForSession(String sessionId) {
            return colorAt(stableIndex(sessionId, colors.length));
        }
    }
}
