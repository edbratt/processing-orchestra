package com.processing.server.tools.pde;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ProcessingSketchRendererTest {
    @Test
    void rendersStarterSketchWithPreservedLocalInputTodo() throws Exception {
        Path path = fixturePath("mouse-follow.pde");
        PdeSketchModel model = new PdeStructureScanner().scan(path, Files.readString(path));

        String rendered = new ProcessingSketchRenderer().render(model);

        assertTrue(rendered.contains("class ProcessingSketchGenerated extends PApplet"));
        assertTrue(rendered.contains("private void drawGeneratedSketch()"));
        assertTrue(rendered.contains("TODO: Local mouse input was preserved"));
        assertTrue(rendered.contains("public void mousePressed()"));
    }

    @Test
    void rendersBrowserTouchModeForSafeMouseSketch() throws Exception {
        Path path = fixturePath("browser-touch-safe.pde");
        PdeSketchModel model = new PdeStructureScanner()
            .scan(path, Files.readString(path))
            .withMode(MigrationMode.BROWSER_TOUCH);

        String rendered = new ProcessingSketchRenderer().render(model);

        assertTrue(rendered.contains("private float touchX = 0.5f;"));
        assertTrue(rendered.contains("private float touchY = 0.5f;"));
        assertTrue(rendered.contains("if (\"touch\".equals(event.eventType()))"));
        assertTrue(rendered.contains("ellipse((touchX * width), (touchY * height), circleSize, circleSize);"));
        assertTrue(rendered.contains("TODO: Simple mouse-position usage was mapped to browser touch input automatically."));
    }

    @Test
    void rendersBrowserKeyboardModeForSafeKeyboardSketch() throws Exception {
        Path path = fixturePath("keyboard-toggle.pde");
        PdeSketchModel model = new PdeStructureScanner()
            .scan(path, Files.readString(path))
            .withMode(MigrationMode.BROWSER_KEYBOARD);

        String rendered = new ProcessingSketchRenderer().render(model);

        assertTrue(rendered.contains("if (\"key\".equals(event.eventType()) && \"pressed\".equals(event.keyAction()))"));
        assertTrue(rendered.contains("private void handleBrowserKeyPressed(UserInputEvent event)"));
        assertTrue(rendered.contains("char key = event.keyText().isEmpty() ? '\\0' : event.keyText().charAt(0);"));
        assertTrue(rendered.contains("final int LEFT = 37;"));
        assertTrue(rendered.contains("TODO: Simple keyboard input was mapped to browser key events automatically."));
    }

    @Test
    void rendersBrowserTouchPressModeForSafeMousePressSketch() throws Exception {
        Path path = fixturePath("helper-trails.pde");
        PdeSketchModel model = new PdeStructureScanner()
            .scan(path, Files.readString(path))
            .withMode(MigrationMode.BROWSER_TOUCH_PRESS);

        String rendered = new ProcessingSketchRenderer().render(model);

        assertTrue(rendered.contains("private float touchX = 0.5f;"));
        assertTrue(rendered.contains("private float touchY = 0.5f;"));
        assertTrue(rendered.contains("handleBrowserTouchPress();"));
        assertTrue(rendered.contains("private void handleBrowserTouchPress()"));
        assertTrue(rendered.contains("float mouseX = touchX * width;"));
        assertTrue(rendered.contains("float mouseY = touchY * height;"));
        assertTrue(rendered.contains("TODO: Simple mousePressed behavior was mapped to browser touch events automatically."));
    }

    @Test
    void rendersBrowserTouchDragModeForSafeMouseDragSketch() throws Exception {
        Path path = fixturePath("drag-paint.pde");
        PdeSketchModel model = new PdeStructureScanner()
            .scan(path, Files.readString(path))
            .withMode(MigrationMode.BROWSER_TOUCH_DRAG);

        String rendered = new ProcessingSketchRenderer().render(model);

        assertTrue(rendered.contains("private float touchX = 0.5f;"));
        assertTrue(rendered.contains("private float touchY = 0.5f;"));
        assertTrue(rendered.contains("handleBrowserTouchDrag();"));
        assertTrue(rendered.contains("private void handleBrowserTouchDrag()"));
        assertTrue(rendered.contains("float mouseX = touchX * width;"));
        assertTrue(rendered.contains("float mouseY = touchY * height;"));
        assertTrue(rendered.contains("TODO: Simple mouseDragged behavior was mapped to browser touch events automatically."));
    }

    private Path fixturePath(String fixtureName) throws IOException, URISyntaxException {
        return Path.of(getClass().getResource("/pde-fixtures/" + fixtureName).toURI());
    }
}
