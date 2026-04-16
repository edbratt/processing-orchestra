package com.processing.server.tools.pde;

import java.util.ArrayList;
import java.util.List;

final class MigrationReportRenderer {
    String render(PdeSketchModel model) {
        StringBuilder out = new StringBuilder();
        out.append("# PDE Migration Report").append(System.lineSeparator()).append(System.lineSeparator());
        out.append("## Summary").append(System.lineSeparator()).append(System.lineSeparator());
        out.append("- Source file: `").append(model.sourcePath().getFileName()).append("`").append(System.lineSeparator());
        out.append("- Fields found: ").append(model.fields().size()).append(System.lineSeparator());
        out.append("- Methods found: ").append(model.methods().size()).append(System.lineSeparator());
        out.append("- Lifecycle methods found: ").append(lifecycleSummary(model)).append(System.lineSeparator());
        out.append("- Generated file: `ProcessingSketchGenerated.java`").append(System.lineSeparator()).append(System.lineSeparator());

        out.append("## Preserved Local Interaction").append(System.lineSeparator()).append(System.lineSeparator());
        appendPreservedInteraction(out, model);

        out.append(System.lineSeparator()).append("## Unresolved TODO Decisions").append(System.lineSeparator()).append(System.lineSeparator());
        appendTodos(out, model);

        out.append(System.lineSeparator()).append("## Suggested Next Step").append(System.lineSeparator()).append(System.lineSeparator());
        out.append(nextStep(model)).append(System.lineSeparator());
        return out.toString();
    }

    private void appendPreservedInteraction(StringBuilder out, PdeSketchModel model) {
        boolean any = false;
        if (model.usesMouseX() || model.usesMouseY() || model.usesPmouseX() || model.usesPmouseY()
            || hasInputHandler(model, "mousePressed") || hasInputHandler(model, "mouseDragged")
            || hasInputHandler(model, "mouseReleased") || hasInputHandler(model, "mouseMoved")) {
            any = true;
            if (model.canApplyBrowserTouchSafely()) {
                out.append("- Simple mouse-position usage was mapped to browser touch input automatically.").append(System.lineSeparator());
            } else if (model.canApplyBrowserTouchPressSafely()) {
                out.append("- Simple mousePressed behavior was mapped to browser touch events automatically.").append(System.lineSeparator());
            } else if (model.canApplyBrowserTouchDragSafely()) {
                out.append("- Simple mouseDragged behavior was mapped to browser touch events automatically.").append(System.lineSeparator());
            } else {
                out.append("- Mouse input was preserved in the generated sketch where possible.").append(System.lineSeparator());
            }
        }
        if (model.usesKey() || model.usesKeyCode()
            || hasInputHandler(model, "keyPressed") || hasInputHandler(model, "keyReleased")) {
            any = true;
            if (model.canApplyBrowserKeyboardSafely()) {
                out.append("- Simple keyboard input was mapped to browser key events automatically.").append(System.lineSeparator());
            } else {
                out.append("- Keyboard input was preserved in the generated sketch where possible.").append(System.lineSeparator());
            }
        }
        if (!any) {
            out.append("- No local mouse or keyboard behavior was detected.").append(System.lineSeparator());
        }
    }

    private void appendTodos(StringBuilder out, PdeSketchModel model) {
        List<String> todos = collectTodos(model);
        if (todos.isEmpty()) {
            out.append("- No migration TODOs were detected for this simple sketch.").append(System.lineSeparator());
            return;
        }
        for (String todo : todos) {
            out.append("- ").append(todo).append(System.lineSeparator());
        }
    }

    private List<String> collectTodos(PdeSketchModel model) {
        List<String> todos = new ArrayList<>();
        if (model.canApplyBrowserTouchSafely()) {
            todos.add("Review the generated touch mapping and confirm that replacing local mouse position with browser touch is the right behavior for this sketch.");
        } else if (model.canApplyBrowserTouchPressSafely()) {
            todos.add("Review the generated touch-press mapping and confirm that replacing local mousePressed behavior with browser touch events is the right behavior for this sketch.");
        } else if (model.canApplyBrowserTouchDragSafely()) {
            todos.add("Review the generated touch-drag mapping and confirm that replacing local mouseDragged behavior with browser touch events is the right behavior for this sketch.");
        } else if (model.usesMouseX() || model.usesMouseY() || model.usesPmouseX() || model.usesPmouseY()) {
            todos.add("Decide whether the current mouse-driven behavior should stay local or later be mapped into browser touch or pointer input.");
        }
        if (model.mode() == MigrationMode.BROWSER_TOUCH && model.hasMouseHandlers()) {
            todos.add("Local mouse-handler methods were preserved. Review them manually if this sketch should become fully browser-touch driven.");
        }
        if (model.mode() == MigrationMode.BROWSER_TOUCH_PRESS && !model.canApplyBrowserTouchPressSafely() && model.hasMouseHandlers()) {
            todos.add("Local mouse-handler methods were preserved. Review them manually if this sketch should become browser-touch triggered.");
        }
        if (model.mode() == MigrationMode.BROWSER_TOUCH_DRAG && !model.canApplyBrowserTouchDragSafely() && model.hasMouseHandlers()) {
            todos.add("Local mouse-handler methods were preserved. Review them manually if this sketch should become browser-touch drag driven.");
        }
        if (model.canApplyBrowserKeyboardSafely()) {
            todos.add("Review the generated browser-keyboard mapping and confirm that replacing local keyboard focus with browser key events is the right behavior for this sketch.");
        } else if (model.usesKey() || model.usesKeyCode()
            || hasInputHandler(model, "keyPressed") || hasInputHandler(model, "keyReleased")) {
            todos.add("Decide whether the current keyboard behavior should stay local or later become browser keys, buttons, or sliders.");
        }
        if (model.mode() == MigrationMode.BROWSER_KEYBOARD && model.hasKeyReleasedHandler()) {
            todos.add("Local keyReleased logic was preserved. Review it manually if this sketch should become fully browser-keyboard driven.");
        }
        if (!model.fields().isEmpty()) {
            todos.add("Review sketch fields and decide whether any of them should eventually become per-session state for multiple users.");
        }
        todos.add("Review `processEvents()` and `processAudio()` in the generated sketch and leave them empty unless the sketch later needs browser control or browser audio.");
        return todos;
    }

    private String lifecycleSummary(PdeSketchModel model) {
        List<String> found = new ArrayList<>();
        if (findMethodByName(model, "settings") != null) {
            found.add("settings");
        }
        if (findMethodByName(model, "setup") != null) {
            found.add("setup");
        }
        if (findMethodByName(model, "draw") != null) {
            found.add("draw");
        }
        return found.isEmpty() ? "none" : String.join(", ", found);
    }

    private String nextStep(PdeSketchModel model) {
        if (model.canApplyBrowserTouchSafely()) {
            return "Open `ProcessingSketchGenerated.java`, confirm that browser touch replaced local mouse position correctly, then test the sketch in the app before making any further interaction changes.";
        }
        if (model.canApplyBrowserTouchPressSafely()) {
            return "Open `ProcessingSketchGenerated.java`, confirm that browser touch events now trigger the old mousePressed behavior correctly, then test the sketch in the app before making any further interaction changes.";
        }
        if (model.canApplyBrowserTouchDragSafely()) {
            return "Open `ProcessingSketchGenerated.java`, confirm that browser touch events now trigger the old mouseDragged behavior correctly, then test the sketch in the app before making any further interaction changes.";
        }
        if (model.canApplyBrowserKeyboardSafely()) {
            return "Open `ProcessingSketchGenerated.java`, confirm that browser key events replaced the local keyPressed behavior correctly, then test the sketch in the app before making any further interaction changes.";
        }
        if (model.usesMouseX() || model.usesMouseY() || model.usesPmouseX() || model.usesPmouseY()) {
            return "Open `ProcessingSketchGenerated.java`, confirm that the visual logic still makes sense locally, then decide whether mouse-based behavior should remain local or be mapped to browser touch input.";
        }
        if (model.usesKey() || model.usesKeyCode()
            || hasInputHandler(model, "keyPressed") || hasInputHandler(model, "keyReleased")) {
            return "Open `ProcessingSketchGenerated.java`, confirm that the local keyboard behavior still makes sense, then decide whether it should remain local or become browser controls.";
        }
        return "Open `ProcessingSketchGenerated.java` and confirm that the visual logic was preserved before making any browser-integration decisions.";
    }

    private boolean hasInputHandler(PdeSketchModel model, String name) {
        return model.methods().stream().anyMatch(method -> method.kind() == PdeMethodKind.INPUT_HANDLER && method.name().equals(name));
    }

    private PdeMethod findMethodByName(PdeSketchModel model, String name) {
        return model.methods().stream().filter(method -> method.name().equals(name)).findFirst().orElse(null);
    }
}
