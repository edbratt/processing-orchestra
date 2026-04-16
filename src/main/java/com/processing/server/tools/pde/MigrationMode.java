package com.processing.server.tools.pde;

enum MigrationMode {
    DEFAULT,
    BROWSER_TOUCH,
    BROWSER_KEYBOARD,
    BROWSER_TOUCH_PRESS,
    BROWSER_TOUCH_DRAG;

    static MigrationMode fromCli(String value) {
        return switch (value) {
            case "browser-touch" -> BROWSER_TOUCH;
            case "browser-keyboard" -> BROWSER_KEYBOARD;
            case "browser-touch-press" -> BROWSER_TOUCH_PRESS;
            case "browser-touch-drag" -> BROWSER_TOUCH_DRAG;
            default -> throw new IllegalArgumentException("Unsupported mode: " + value);
        };
    }
}
