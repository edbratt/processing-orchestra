package com.processing.server.tools.pde;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class PdeStructureScannerTest {
    private final PdeStructureScanner scanner = new PdeStructureScanner();

    @Test
    void scansSimpleBouncingSketch() throws Exception {
        PdeSketchModel model = scanFixture("bouncing-circle.pde");

        assertEquals(2, model.fields().size());
        assertEquals(2, model.methods().size());
        assertTrue(model.methods().stream().anyMatch(method -> method.name().equals("setup")));
        assertTrue(model.methods().stream().anyMatch(method -> method.name().equals("draw")));
        assertTrue(model.unsupportedReasons().isEmpty());
    }

    @Test
    void detectsMouseUsageAndHandler() throws Exception {
        PdeSketchModel model = scanFixture("mouse-follow.pde");

        assertTrue(model.usesMouseX());
        assertTrue(model.usesMouseY());
        assertTrue(model.methods().stream().anyMatch(method -> method.name().equals("mousePressed")));
    }

    @Test
    void detectsKeyboardUsageAndHandler() throws Exception {
        PdeSketchModel model = scanFixture("keyboard-toggle.pde");

        assertTrue(model.usesKey());
        assertFalse(model.usesKeyCode());
        assertTrue(model.methods().stream().anyMatch(method -> method.name().equals("keyPressed")));
    }

    @Test
    void flagsUnsupportedTopLevelClass() throws Exception {
        PdeSketchModel model = scanFixture("unsupported-top-level-class.pde");

        assertFalse(model.unsupportedReasons().isEmpty());
    }

    private PdeSketchModel scanFixture(String fixtureName) throws IOException, URISyntaxException {
        Path path = Path.of(getClass().getResource("/pde-fixtures/" + fixtureName).toURI());
        return scanner.scan(path, Files.readString(path));
    }
}
