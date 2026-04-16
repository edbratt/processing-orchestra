package com.processing.server.tools.pde;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MigrationReportRendererTest {
    @Test
    void rendersSimpleReportForMouseSketch() throws Exception {
        Path path = fixturePath("mouse-follow.pde");
        PdeSketchModel model = new PdeStructureScanner().scan(path, Files.readString(path));

        String report = new MigrationReportRenderer().render(model);

        assertTrue(report.contains("# PDE Migration Report"));
        assertTrue(report.contains("## Preserved Local Interaction"));
        assertTrue(report.contains("Mouse input was preserved"));
        assertTrue(report.contains("## Unresolved TODO Decisions"));
        assertTrue(report.contains("browser touch or pointer input"));
    }

    @Test
    void rendersBrowserTouchReportForSafeSketch() throws Exception {
        Path path = fixturePath("browser-touch-safe.pde");
        PdeSketchModel model = new PdeStructureScanner()
            .scan(path, Files.readString(path))
            .withMode(MigrationMode.BROWSER_TOUCH);

        String report = new MigrationReportRenderer().render(model);

        assertTrue(report.contains("mapped to browser touch input automatically"));
        assertTrue(report.contains("confirm that replacing local mouse position with browser touch"));
        assertTrue(report.contains("browser touch replaced local mouse position correctly"));
    }

    @Test
    void rendersBrowserKeyboardReportForSafeSketch() throws Exception {
        Path path = fixturePath("keyboard-toggle.pde");
        PdeSketchModel model = new PdeStructureScanner()
            .scan(path, Files.readString(path))
            .withMode(MigrationMode.BROWSER_KEYBOARD);

        String report = new MigrationReportRenderer().render(model);

        assertTrue(report.contains("mapped to browser key events automatically"));
        assertTrue(report.contains("confirm that replacing local keyboard focus with browser key events"));
        assertTrue(report.contains("browser key events replaced the local keyPressed behavior correctly"));
    }

    @Test
    void rendersBrowserTouchPressReportForSafeSketch() throws Exception {
        Path path = fixturePath("helper-trails.pde");
        PdeSketchModel model = new PdeStructureScanner()
            .scan(path, Files.readString(path))
            .withMode(MigrationMode.BROWSER_TOUCH_PRESS);

        String report = new MigrationReportRenderer().render(model);

        assertTrue(report.contains("mapped to browser touch events automatically"));
        assertTrue(report.contains("confirm that replacing local mousePressed behavior with browser touch events"));
        assertTrue(report.contains("browser touch events now trigger the old mousePressed behavior correctly"));
    }

    @Test
    void rendersBrowserTouchDragReportForSafeSketch() throws Exception {
        Path path = fixturePath("drag-paint.pde");
        PdeSketchModel model = new PdeStructureScanner()
            .scan(path, Files.readString(path))
            .withMode(MigrationMode.BROWSER_TOUCH_DRAG);

        String report = new MigrationReportRenderer().render(model);

        assertTrue(report.contains("mapped to browser touch events automatically"));
        assertTrue(report.contains("confirm that replacing local mouseDragged behavior with browser touch events"));
        assertTrue(report.contains("browser touch events now trigger the old mouseDragged behavior correctly"));
    }

    private Path fixturePath(String fixtureName) throws IOException, URISyntaxException {
        return Path.of(getClass().getResource("/pde-fixtures/" + fixtureName).toURI());
    }
}
