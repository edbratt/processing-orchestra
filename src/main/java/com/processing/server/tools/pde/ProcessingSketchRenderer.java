package com.processing.server.tools.pde;

import java.util.ArrayList;
import java.util.List;

final class ProcessingSketchRenderer {
    String render(PdeSketchModel model) {
        StringBuilder out = new StringBuilder();
        out.append("package com.processing.server;").append(System.lineSeparator()).append(System.lineSeparator());
        out.append("import processing.core.PApplet;").append(System.lineSeparator()).append(System.lineSeparator());
        out.append("public class ProcessingSketchGenerated extends PApplet {").append(System.lineSeparator());
        out.append("    private final EventQueue eventQueue;").append(System.lineSeparator());
        out.append("    private final AudioBuffer audioBuffer;").append(System.lineSeparator());
        out.append("    private final int sketchWidth;").append(System.lineSeparator());
        out.append("    private final int sketchHeight;").append(System.lineSeparator());
        out.append("    private final DebugConfig debugConfig;").append(System.lineSeparator());
        out.append("    private final MotionConfig motionConfig;").append(System.lineSeparator()).append(System.lineSeparator());

        for (PdeField field : model.fields()) {
            out.append("    ").append(normalizeField(field.declaration())).append(System.lineSeparator());
        }
        if (model.canApplyBrowserTouchSafely() || model.canApplyBrowserTouchPressSafely() || model.canApplyBrowserTouchDragSafely()) {
            out.append("    private float touchX = 0.5f;").append(System.lineSeparator());
            out.append("    private float touchY = 0.5f;").append(System.lineSeparator());
        }
        if (!model.fields().isEmpty() || model.canApplyBrowserTouchSafely() || model.canApplyBrowserTouchPressSafely() || model.canApplyBrowserTouchDragSafely()) {
            out.append(System.lineSeparator());
        }

        writeTodoBlock(out, model);
        writeConstructor(out);
        writeSettings(out, model);
        writeSetup(out, findMethod(model, PdeMethodKind.SETUP));
        writeDraw(out, model, findMethod(model, PdeMethodKind.DRAW));
        writeLifecycleHelpers(out, model);
        writeInputHandlers(out, model);
        writeHelperMethods(out, model);
        writeRunSketch(out);
        out.append("}").append(System.lineSeparator());
        return out.toString();
    }

    private void writeTodoBlock(StringBuilder out, PdeSketchModel model) {
        List<String> todos = new ArrayList<>();
        if (model.canApplyBrowserTouchSafely()) {
            todos.add("Simple mouse-position usage was mapped to browser touch input automatically.");
        } else if (model.canApplyBrowserTouchPressSafely()) {
            todos.add("Simple mousePressed behavior was mapped to browser touch events automatically.");
        } else if (model.canApplyBrowserTouchDragSafely()) {
            todos.add("Simple mouseDragged behavior was mapped to browser touch events automatically.");
        } else if (model.canApplyBrowserKeyboardSafely()) {
            todos.add("Simple keyboard input was mapped to browser key events automatically.");
        } else if (model.usesMouseX() || model.usesMouseY() || model.usesPmouseX() || model.usesPmouseY()) {
            todos.add("Local mouse input was preserved. Decide later whether to keep it local or map it into browser touch/pointer input.");
        }
        if (!model.canApplyBrowserKeyboardSafely()
            && (model.usesKey() || model.usesKeyCode() || hasInputHandler(model, "keyPressed") || hasInputHandler(model, "keyReleased"))) {
            todos.add("Local keyboard input was preserved. Decide later whether it should stay local or become browser keys, buttons, or sliders.");
        }
        if (!todos.isEmpty()) {
            out.append("    // Migration TODOs").append(System.lineSeparator());
            for (String todo : todos) {
                out.append("    // TODO: ").append(todo).append(System.lineSeparator());
            }
            out.append(System.lineSeparator());
        }
    }

    private void writeConstructor(StringBuilder out) {
        out.append("    public ProcessingSketchGenerated(EventQueue eventQueue,").append(System.lineSeparator());
        out.append("                                     AudioBuffer audioBuffer,").append(System.lineSeparator());
        out.append("                                     int width,").append(System.lineSeparator());
        out.append("                                     int height,").append(System.lineSeparator());
        out.append("                                     DebugConfig debugConfig,").append(System.lineSeparator());
        out.append("                                     MotionConfig motionConfig) {").append(System.lineSeparator());
        out.append("        this.eventQueue = eventQueue;").append(System.lineSeparator());
        out.append("        this.audioBuffer = audioBuffer;").append(System.lineSeparator());
        out.append("        this.sketchWidth = width;").append(System.lineSeparator());
        out.append("        this.sketchHeight = height;").append(System.lineSeparator());
        out.append("        this.debugConfig = debugConfig;").append(System.lineSeparator());
        out.append("        this.motionConfig = motionConfig;").append(System.lineSeparator());
        out.append("    }").append(System.lineSeparator()).append(System.lineSeparator());
    }

    private void writeSettings(StringBuilder out, PdeSketchModel model) {
        PdeMethod settings = findMethodByName(model, "settings");
        out.append("    @Override").append(System.lineSeparator());
        out.append("    public void settings() ").append(extractMethodBody(settings, "{" + System.lineSeparator()
            + "        size(sketchWidth, sketchHeight, JAVA2D);" + System.lineSeparator() + "    }")).append(System.lineSeparator()).append(System.lineSeparator());
    }

    private void writeSetup(StringBuilder out, PdeMethod setup) {
        out.append("    @Override").append(System.lineSeparator());
        out.append("    public void setup() ")
            .append(cleanSetupBody(extractMethodBody(setup, "{" + System.lineSeparator() + "    }")))
            .append(System.lineSeparator())
            .append(System.lineSeparator());
    }

    private void writeDraw(StringBuilder out, PdeSketchModel model, PdeMethod draw) {
        out.append("    @Override").append(System.lineSeparator());
        out.append("    public void draw() {").append(System.lineSeparator());
        out.append("        processEvents();").append(System.lineSeparator());
        out.append("        processAudio();").append(System.lineSeparator());
        out.append("        drawGeneratedSketch();").append(System.lineSeparator());
        out.append("    }").append(System.lineSeparator()).append(System.lineSeparator());

        String drawBody = extractMethodBody(draw, "{" + System.lineSeparator()
            + "        background(0);" + System.lineSeparator() + "    }");
        if (model.canApplyBrowserTouchSafely()) {
            drawBody = applyBrowserTouchMapping(drawBody);
        }
        out.append("    private void drawGeneratedSketch() ").append(drawBody).append(System.lineSeparator()).append(System.lineSeparator());
    }

    private void writeLifecycleHelpers(StringBuilder out, PdeSketchModel model) {
        out.append("    private void processEvents() {").append(System.lineSeparator());
        if (model.canApplyBrowserTouchSafely()) {
            out.append("        while (!eventQueue.isEmpty()) {").append(System.lineSeparator());
            out.append("            UserInputEvent event = eventQueue.poll();").append(System.lineSeparator());
            out.append("            if (event == null) {").append(System.lineSeparator());
            out.append("                break;").append(System.lineSeparator());
            out.append("            }").append(System.lineSeparator()).append(System.lineSeparator());
            out.append("            if (\"touch\".equals(event.eventType())) {").append(System.lineSeparator());
            out.append("                touchX = constrain(event.x(), 0f, 1f);").append(System.lineSeparator());
                out.append("                touchY = constrain(event.y(), 0f, 1f);").append(System.lineSeparator());
            out.append("            }").append(System.lineSeparator());
            out.append("        }").append(System.lineSeparator());
        } else if (model.canApplyBrowserTouchPressSafely()) {
            out.append("        while (!eventQueue.isEmpty()) {").append(System.lineSeparator());
            out.append("            UserInputEvent event = eventQueue.poll();").append(System.lineSeparator());
            out.append("            if (event == null) {").append(System.lineSeparator());
            out.append("                break;").append(System.lineSeparator());
            out.append("            }").append(System.lineSeparator()).append(System.lineSeparator());
            out.append("            if (\"touch\".equals(event.eventType())) {").append(System.lineSeparator());
            out.append("                touchX = constrain(event.x(), 0f, 1f);").append(System.lineSeparator());
            out.append("                touchY = constrain(event.y(), 0f, 1f);").append(System.lineSeparator());
            out.append("                handleBrowserTouchPress();").append(System.lineSeparator());
            out.append("            }").append(System.lineSeparator());
            out.append("        }").append(System.lineSeparator());
        } else if (model.canApplyBrowserTouchDragSafely()) {
            out.append("        while (!eventQueue.isEmpty()) {").append(System.lineSeparator());
            out.append("            UserInputEvent event = eventQueue.poll();").append(System.lineSeparator());
            out.append("            if (event == null) {").append(System.lineSeparator());
            out.append("                break;").append(System.lineSeparator());
            out.append("            }").append(System.lineSeparator()).append(System.lineSeparator());
            out.append("            if (\"touch\".equals(event.eventType())) {").append(System.lineSeparator());
            out.append("                touchX = constrain(event.x(), 0f, 1f);").append(System.lineSeparator());
            out.append("                touchY = constrain(event.y(), 0f, 1f);").append(System.lineSeparator());
            out.append("                handleBrowserTouchDrag();").append(System.lineSeparator());
            out.append("            }").append(System.lineSeparator());
            out.append("        }").append(System.lineSeparator());
        } else if (model.canApplyBrowserKeyboardSafely()) {
            out.append("        while (!eventQueue.isEmpty()) {").append(System.lineSeparator());
            out.append("            UserInputEvent event = eventQueue.poll();").append(System.lineSeparator());
            out.append("            if (event == null) {").append(System.lineSeparator());
            out.append("                break;").append(System.lineSeparator());
            out.append("            }").append(System.lineSeparator()).append(System.lineSeparator());
            out.append("            if (\"key\".equals(event.eventType()) && \"pressed\".equals(event.keyAction())) {").append(System.lineSeparator());
            out.append("                handleBrowserKeyPressed(event);").append(System.lineSeparator());
            out.append("            }").append(System.lineSeparator());
            out.append("        }").append(System.lineSeparator());
        } else {
            out.append("        // TODO: Integrate browser-driven events here if this sketch later needs controller input.").append(System.lineSeparator());
        }
        out.append("    }").append(System.lineSeparator()).append(System.lineSeparator());
        out.append("    private void processAudio() {").append(System.lineSeparator());
        out.append("        // TODO: Integrate AudioBuffer processing here if this sketch later needs browser audio.").append(System.lineSeparator());
        out.append("    }").append(System.lineSeparator()).append(System.lineSeparator());
    }

    private void writeInputHandlers(StringBuilder out, PdeSketchModel model) {
        for (PdeMethod method : model.methods()) {
            if (method.kind() == PdeMethodKind.INPUT_HANDLER) {
                if (model.canApplyBrowserTouchPressSafely() && "mousePressed".equals(method.name())) {
                    out.append("    private void handleBrowserTouchPress() {").append(System.lineSeparator());
                    out.append("        float mouseX = touchX * width;").append(System.lineSeparator());
                    out.append("        float mouseY = touchY * height;").append(System.lineSeparator()).append(System.lineSeparator());
                    out.append(indentBodyContents(extractBracedBody(method.body()), 2)).append(System.lineSeparator());
                    out.append("    }").append(System.lineSeparator()).append(System.lineSeparator());
                    continue;
                }
                if (model.canApplyBrowserTouchDragSafely() && "mouseDragged".equals(method.name())) {
                    out.append("    private void handleBrowserTouchDrag() {").append(System.lineSeparator());
                    out.append("        float mouseX = touchX * width;").append(System.lineSeparator());
                    out.append("        float mouseY = touchY * height;").append(System.lineSeparator()).append(System.lineSeparator());
                    out.append(indentBodyContents(extractBracedBody(method.body()), 2)).append(System.lineSeparator());
                    out.append("    }").append(System.lineSeparator()).append(System.lineSeparator());
                    continue;
                }
                if (model.canApplyBrowserKeyboardSafely() && "keyPressed".equals(method.name())) {
                    out.append("    private void handleBrowserKeyPressed(UserInputEvent event) {").append(System.lineSeparator());
                    out.append("        char key = event.keyText().isEmpty() ? '\\0' : event.keyText().charAt(0);").append(System.lineSeparator());
                    out.append("        int keyCode = event.keyCode();").append(System.lineSeparator());
                    out.append("        final int LEFT = 37;").append(System.lineSeparator());
                    out.append("        final int UP = 38;").append(System.lineSeparator());
                    out.append("        final int RIGHT = 39;").append(System.lineSeparator());
                    out.append("        final int DOWN = 40;").append(System.lineSeparator()).append(System.lineSeparator());
                    out.append(indentBodyContents(extractBracedBody(method.body()), 2)).append(System.lineSeparator());
                    out.append("    }").append(System.lineSeparator()).append(System.lineSeparator());
                    continue;
                }
                out.append("    @Override").append(System.lineSeparator());
                out.append("    public ").append(stripAccessModifier(normalizeSignature(method.signature()))).append(" ").append(extractBracedBody(method.body())).append(System.lineSeparator()).append(System.lineSeparator());
            }
        }
    }

    private void writeHelperMethods(StringBuilder out, PdeSketchModel model) {
        for (PdeMethod method : model.methods()) {
            if (method.kind() == PdeMethodKind.HELPER && !"settings".equals(method.name())) {
                out.append("    ").append(normalizeSignature(method.signature())).append(" ").append(extractBracedBody(method.body())).append(System.lineSeparator()).append(System.lineSeparator());
            }
        }
    }

    private void writeRunSketch(StringBuilder out) {
        out.append("    public void runSketch() {").append(System.lineSeparator());
        out.append("        String[] args = {this.getClass().getName()};").append(System.lineSeparator());
        out.append("        PApplet.runSketch(args, this);").append(System.lineSeparator());
        out.append("    }").append(System.lineSeparator());
    }

    private PdeMethod findMethod(PdeSketchModel model, PdeMethodKind kind) {
        return model.methods().stream().filter(method -> method.kind() == kind).findFirst().orElse(null);
    }

    private PdeMethod findMethodByName(PdeSketchModel model, String name) {
        return model.methods().stream().filter(method -> method.name().equals(name)).findFirst().orElse(null);
    }

    private boolean hasInputHandler(PdeSketchModel model, String name) {
        return model.methods().stream().anyMatch(method -> method.kind() == PdeMethodKind.INPUT_HANDLER && method.name().equals(name));
    }

    private String normalizeField(String declaration) {
        String trimmed = normalizeFloatFieldInitializer(declaration.trim());
        if (trimmed.startsWith("public ") || trimmed.startsWith("private ") || trimmed.startsWith("protected ")) {
            return trimmed;
        }
        return "private " + trimmed;
    }

    private String normalizeFloatFieldInitializer(String declaration) {
        return declaration.replaceAll(
            "(?m)^((?:public|private|protected)\\s+)?float\\s+([A-Za-z_]\\w*\\s*=\\s*)(-?\\d+\\.\\d+)(\\s*;)$",
            "$1float $2$3f$4"
        );
    }

    private String normalizeSignature(String signature) {
        String trimmed = signature.trim();
        if (trimmed.endsWith("{")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        if (trimmed.startsWith("public ") || trimmed.startsWith("private ") || trimmed.startsWith("protected ")) {
            return trimmed;
        }
        return "private " + trimmed;
    }

    private String stripAccessModifier(String signature) {
        if (signature.startsWith("public ")) {
            return signature.substring("public ".length());
        }
        if (signature.startsWith("private ")) {
            return signature.substring("private ".length());
        }
        if (signature.startsWith("protected ")) {
            return signature.substring("protected ".length());
        }
        return signature;
    }

    private String extractMethodBody(PdeMethod method, String fallback) {
        return method == null ? fallback : extractBracedBody(method.body());
    }

    private String cleanSetupBody(String body) {
        String[] lines = body.split("\\R", -1);
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("size(") || trimmed.startsWith("fullScreen(") || "fullScreen();".equals(trimmed)) {
                continue;
            }
            out.append(line).append(System.lineSeparator());
        }
        String cleaned = out.toString().trim();
        if (cleaned.isEmpty()) {
            return "{" + System.lineSeparator() + "    }";
        }
        return cleaned;
    }

    private String extractBracedBody(String fullMethodText) {
        int firstBrace = fullMethodText.indexOf('{');
        int lastBrace = fullMethodText.lastIndexOf('}');
        if (firstBrace < 0 || lastBrace < firstBrace) {
            return "{" + System.lineSeparator() + "    }";
        }
        return fullMethodText.substring(firstBrace, lastBrace + 1);
    }

    private String applyBrowserTouchMapping(String body) {
        return body
            .replaceAll("\\bmouseX\\b", "(touchX * width)")
            .replaceAll("\\bmouseY\\b", "(touchY * height)");
    }

    private String indentBodyContents(String bracedBody, int indentLevel) {
        String[] lines = bracedBody.split("\\R", -1);
        String indent = "    ".repeat(indentLevel);
        StringBuilder out = new StringBuilder();
        for (int i = 1; i < lines.length - 1; i++) {
            String line = lines[i];
            if (line.isBlank()) {
                out.append(System.lineSeparator());
            } else {
                out.append(indent).append(line.stripLeading()).append(System.lineSeparator());
            }
        }
        return out.toString().stripTrailing();
    }
}
