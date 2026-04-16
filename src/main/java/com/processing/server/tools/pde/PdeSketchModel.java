package com.processing.server.tools.pde;

import java.nio.file.Path;
import java.util.List;

public record PdeSketchModel(
    Path sourcePath,
    String rawSource,
    List<PdeField> fields,
    List<PdeMethod> methods,
    List<String> unsupportedReasons,
    MigrationMode mode,
    boolean usesMouseX,
    boolean usesMouseY,
    boolean usesPmouseX,
    boolean usesPmouseY,
    boolean hasMouseHandlers,
    boolean hasKeyPressedHandler,
    boolean hasKeyReleasedHandler,
    boolean usesKey,
    boolean usesKeyCode
) {
    PdeSketchModel withMode(MigrationMode mode) {
        return new PdeSketchModel(
            sourcePath,
            rawSource,
            fields,
            methods,
            unsupportedReasons,
            mode,
            usesMouseX,
            usesMouseY,
            usesPmouseX,
            usesPmouseY,
            hasMouseHandlers,
            hasKeyPressedHandler,
            hasKeyReleasedHandler,
            usesKey,
            usesKeyCode
        );
    }

    boolean canApplyBrowserTouchSafely() {
        return mode == MigrationMode.BROWSER_TOUCH
            && (usesMouseX || usesMouseY)
            && !usesPmouseX
            && !usesPmouseY
            && !hasMouseHandlers;
    }

    boolean canApplyBrowserTouchPressSafely() {
        return mode == MigrationMode.BROWSER_TOUCH_PRESS
            && hasInputHandler("mousePressed")
            && !hasInputHandler("mouseDragged")
            && !hasInputHandler("mouseReleased")
            && !hasInputHandler("mouseMoved")
            && !usesPmouseX
            && !usesPmouseY;
    }

    boolean canApplyBrowserTouchDragSafely() {
        return mode == MigrationMode.BROWSER_TOUCH_DRAG
            && hasInputHandler("mouseDragged")
            && !hasInputHandler("mousePressed")
            && !hasInputHandler("mouseReleased")
            && !hasInputHandler("mouseMoved")
            && !usesPmouseX
            && !usesPmouseY;
    }

    boolean canApplyBrowserKeyboardSafely() {
        return mode == MigrationMode.BROWSER_KEYBOARD
            && hasKeyPressedHandler
            && !hasKeyReleasedHandler;
    }

    private boolean hasInputHandler(String name) {
        return methods.stream().anyMatch(method -> method.kind() == PdeMethodKind.INPUT_HANDLER && method.name().equals(name));
    }
}
