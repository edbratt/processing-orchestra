package com.processing.server.tools.pde;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PdeToProcessingSketchMain {
    private PdeToProcessingSketchMain() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: PdeToProcessingSketchMain <input.pde> [--mode browser-touch|browser-keyboard|browser-touch-press|browser-touch-drag] [--output-dir <dir>]");
            System.exit(1);
        }

        Path sourcePath = Path.of(args[0]);
        if (!Files.exists(sourcePath) || !Files.isRegularFile(sourcePath)) {
            System.err.println("Input file not found: " + sourcePath);
            System.exit(1);
        }
        if (!sourcePath.toString().endsWith(".pde")) {
            System.err.println("Expected a .pde file: " + sourcePath);
            System.exit(1);
        }

        Path outputDir = Path.of(".");
        MigrationMode mode = MigrationMode.DEFAULT;
        int argIndex = 1;
        while (argIndex < args.length) {
            switch (args[argIndex]) {
                case "--output-dir" -> {
                    if (argIndex + 1 >= args.length) {
                        System.err.println("Missing value for --output-dir");
                        System.exit(1);
                    }
                    outputDir = Path.of(args[argIndex + 1]);
                    argIndex += 2;
                }
                case "--mode" -> {
                    if (argIndex + 1 >= args.length) {
                        System.err.println("Missing value for --mode");
                        System.exit(1);
                    }
                    try {
                        mode = MigrationMode.fromCli(args[argIndex + 1]);
                    } catch (IllegalArgumentException e) {
                        System.err.println(e.getMessage());
                        System.exit(1);
                    }
                    argIndex += 2;
                }
                default -> {
                    System.err.println("Unsupported option: " + args[argIndex]);
                    System.exit(1);
                }
            }
        }

        String rawSource = Files.readString(sourcePath);
        PdeSketchModel model = new PdeStructureScanner().scan(sourcePath, rawSource).withMode(mode);
        if (!model.unsupportedReasons().isEmpty()) {
            System.err.println("Unsupported PDE input for v1:");
            for (String reason : model.unsupportedReasons()) {
                System.err.println("- " + reason);
            }
            System.exit(1);
        }

        System.out.println("PDE Scanner Summary");
        System.out.println("Source: " + model.sourcePath());
        System.out.println("Mode: " + model.mode());
        System.out.println("Fields found: " + model.fields().size());
        System.out.println("Methods found: " + model.methods().size());
        System.out.println();

        if (!model.fields().isEmpty()) {
            System.out.println("Top-level fields:");
            for (PdeField field : model.fields()) {
                System.out.println("- " + firstLine(field.declaration()));
            }
            System.out.println();
        }

        if (!model.methods().isEmpty()) {
            System.out.println("Methods:");
            for (PdeMethod method : model.methods()) {
                System.out.println("- " + method.name() + " [" + method.kind() + "]");
            }
            System.out.println();
        }

        System.out.println("Detected local input usage:");
        System.out.println("- mouseX: " + yesNo(model.usesMouseX()));
        System.out.println("- mouseY: " + yesNo(model.usesMouseY()));
        System.out.println("- pmouseX: " + yesNo(model.usesPmouseX()));
        System.out.println("- pmouseY: " + yesNo(model.usesPmouseY()));
        System.out.println("- mouse handlers: " + yesNo(model.hasMouseHandlers()));
        System.out.println("- keyPressed handler: " + yesNo(model.hasKeyPressedHandler()));
        System.out.println("- keyReleased handler: " + yesNo(model.hasKeyReleasedHandler()));
        System.out.println("- key: " + yesNo(model.usesKey()));
        System.out.println("- keyCode: " + yesNo(model.usesKeyCode()));
        if (model.mode() == MigrationMode.BROWSER_TOUCH) {
            System.out.println("- browser-touch applied safely: " + yesNo(model.canApplyBrowserTouchSafely()));
        } else if (model.mode() == MigrationMode.BROWSER_TOUCH_PRESS) {
            System.out.println("- browser-touch-press applied safely: " + yesNo(model.canApplyBrowserTouchPressSafely()));
        } else if (model.mode() == MigrationMode.BROWSER_TOUCH_DRAG) {
            System.out.println("- browser-touch-drag applied safely: " + yesNo(model.canApplyBrowserTouchDragSafely()));
        } else if (model.mode() == MigrationMode.BROWSER_KEYBOARD) {
            System.out.println("- browser-keyboard applied safely: " + yesNo(model.canApplyBrowserKeyboardSafely()));
        }

        Files.createDirectories(outputDir);
        Path generatedFile = outputDir.resolve("ProcessingSketchGenerated.java");
        Path reportFile = outputDir.resolve("pde-migration-report.md");
        String rendered = new ProcessingSketchRenderer().render(model);
        String report = new MigrationReportRenderer().render(model);
        Files.writeString(generatedFile, rendered);
        Files.writeString(reportFile, report);
        System.out.println();
        System.out.println("Generated starter class: " + generatedFile.toAbsolutePath());
        System.out.println("Generated migration report: " + reportFile.toAbsolutePath());
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    private static String firstLine(String text) {
        int newline = text.indexOf(System.lineSeparator());
        return newline >= 0 ? text.substring(0, newline).trim() : text.trim();
    }
}
