/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import io.helidon.json.JsonObject;
import io.helidon.json.JsonValue;

public final class ControllerConfig {
    private final String sketchClassName;
    private final String title;
    private final boolean touch;
    private final boolean sizeSlider;
    private final boolean speedSlider;
    private final boolean actionButtons;
    private final boolean audio;
    private final boolean motion;
    private final boolean keyboard;

    private ControllerConfig(Builder builder) {
        this.sketchClassName = builder.sketchClassName;
        this.title = builder.title;
        this.touch = builder.touch;
        this.sizeSlider = builder.sizeSlider;
        this.speedSlider = builder.speedSlider;
        this.actionButtons = builder.actionButtons;
        this.audio = builder.audio;
        this.motion = builder.motion;
        this.keyboard = builder.keyboard;
    }

    public static ControllerConfig forSketch(String sketchClassName) {
        String simpleName = simpleName(sketchClassName);
        Builder builder = builder(sketchClassName).title(titleFromClassName(simpleName));

        switch (simpleName) {
            case "StarterSketch" -> builder.touch().sizeSlider().keyboard();
            case "MouseFollowSketch", "MouseFollowBrowserTouchSketch",
                 "HelperTrailsBrowserTouchPressSketch", "DragPaintBrowserTouchDragSketch" -> builder.touch();
            case "KeyboardToggleSketch", "KeyboardToggleBrowserKeySketch" -> builder.keyboard();
            case "AbandonedArt79Sketch", "AbandonedArt90Sketch", "AbandonedArt96" -> builder.touch().motion().actions();
            case "ProcessingSketch", "GravityOrbitSketch", "GravityOrbitGradientSketch" -> builder
                .touch()
                .sizeSlider()
                .speedSlider()
                .actions()
                .audio()
                .motion()
                .keyboard();
            default -> builder
                .touch()
                .sizeSlider()
                .speedSlider()
                .actions()
                .audio()
                .motion()
                .keyboard();
        }

        return builder.build();
    }

    public JsonObject toJson() {
        JsonObject features = JsonValue.objectBuilder()
            .set("touch", touch)
            .set("sizeSlider", sizeSlider)
            .set("speedSlider", speedSlider)
            .set("actionButtons", actionButtons)
            .set("audio", audio)
            .set("motion", motion)
            .set("keyboard", keyboard)
            .build();

        return JsonValue.objectBuilder()
            .set("sketchClass", sketchClassName)
            .set("title", title)
            .set("features", features)
            .build();
    }

    private static Builder builder(String sketchClassName) {
        return new Builder(sketchClassName);
    }

    private static String simpleName(String sketchClassName) {
        int lastDot = sketchClassName.lastIndexOf('.');
        return lastDot >= 0 ? sketchClassName.substring(lastDot + 1) : sketchClassName;
    }

    private static String titleFromClassName(String simpleName) {
        return simpleName
            .replaceAll("([a-z])([A-Z])", "$1 $2")
            .replace(" Sketch", "")
            .trim();
    }

    private static final class Builder {
        private final String sketchClassName;
        private String title = "Processing Server UI";
        private boolean touch;
        private boolean sizeSlider;
        private boolean speedSlider;
        private boolean actionButtons;
        private boolean audio;
        private boolean motion;
        private boolean keyboard;

        private Builder(String sketchClassName) {
            this.sketchClassName = sketchClassName;
        }

        private Builder title(String title) {
            this.title = title;
            return this;
        }

        private Builder touch() {
            this.touch = true;
            return this;
        }

        private Builder sizeSlider() {
            this.sizeSlider = true;
            return this;
        }

        private Builder speedSlider() {
            this.speedSlider = true;
            return this;
        }

        private Builder actions() {
            this.actionButtons = true;
            return this;
        }

        private Builder audio() {
            this.audio = true;
            return this;
        }

        private Builder motion() {
            this.motion = true;
            return this;
        }

        private Builder keyboard() {
            this.keyboard = true;
            return this;
        }

        private ControllerConfig build() {
            return new ControllerConfig(this);
        }
    }
}
