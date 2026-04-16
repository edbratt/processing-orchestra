package com.processing.server.tools.pde;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;

class GeneratedSketchCompilationTest {
    @Test
    void generatedSketchBuildsForSimpleFixtures() throws Exception {
        assertCompiles("bouncing-circle.pde");
        assertCompiles("mouse-follow.pde");
        assertCompiles("keyboard-toggle.pde");
        assertCompiles("helper-drawing.pde");
        assertCompiles("browser-touch-safe.pde", MigrationMode.BROWSER_TOUCH);
        assertCompiles("keyboard-toggle.pde", MigrationMode.BROWSER_KEYBOARD);
        assertCompiles("helper-trails.pde", MigrationMode.BROWSER_TOUCH_PRESS);
        assertCompiles("drag-paint.pde", MigrationMode.BROWSER_TOUCH_DRAG);
    }

    private void assertCompiles(String fixtureName) throws IOException, URISyntaxException {
        assertCompiles(fixtureName, MigrationMode.DEFAULT);
    }

    private void assertCompiles(String fixtureName, MigrationMode mode) throws IOException, URISyntaxException {
        Path fixture = fixturePath(fixtureName);
        PdeSketchModel model = new PdeStructureScanner().scan(fixture, Files.readString(fixture)).withMode(mode);
        String rendered = new ProcessingSketchRenderer().render(model);

        Path tempDir = Files.createTempDirectory("pde-generated-compile");
        Path packageDir = tempDir.resolve("com/processing/server");
        Path classDir = tempDir.resolve("classes");
        Files.createDirectories(packageDir);
        Files.createDirectories(classDir);
        Path javaFile = packageDir.resolve("ProcessingSketchGenerated.java");
        Files.writeString(javaFile, rendered);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "A JDK compiler is required for this test.");

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(classDir.toFile()));

            List<String> options = List.of(
                "-classpath", System.getProperty("java.class.path")
            );
            boolean success = compiler.getTask(
                null,
                fileManager,
                null,
                options,
                null,
                fileManager.getJavaFileObjects(javaFile.toFile())
            ).call();

            assertEquals(true, success, "Generated sketch should compile for fixture " + fixtureName);
        }
    }

    private Path fixturePath(String fixtureName) throws IOException, URISyntaxException {
        return Path.of(getClass().getResource("/pde-fixtures/" + fixtureName).toURI());
    }
}
