package com.processing.server.tools.pde;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PdeStructureScanner {
    private static final Pattern TOP_LEVEL_TYPE_DECLARATION = Pattern.compile(
        "^\\s*(?:public\\s+)?(?:class|interface|enum|record)\\s+(\\w+)\\b");
    private static final Pattern METHOD_START = Pattern.compile(
        "^\\s*(?!if\\b|for\\b|while\\b|switch\\b|catch\\b)(?:public\\s+|private\\s+|protected\\s+)?(?:static\\s+)?([\\w<>\\[\\]]+)\\s+(\\w+)\\s*\\([^;]*\\)\\s*\\{?\\s*$");
    private static final Pattern SIZE_CALL = Pattern.compile(
        "\\bsize\\s*\\(\\s*([^,]+?)\\s*,\\s*([^,\\)]+?)\\s*(?:,\\s*([^\\)]+?)\\s*)?\\)\\s*;");

    PdeSketchModel scan(java.nio.file.Path sourcePath, String rawSource) {
        List<String> lines = rawSource.lines().toList();
        List<PdeField> fields = new ArrayList<>();
        List<PdeType> types = new ArrayList<>();
        List<PdeMethod> methods = new ArrayList<>();
        List<String> unsupportedReasons = new ArrayList<>();

        int depth = 0;
        int index = 0;
        StringBuilder topLevelStatement = new StringBuilder();

        while (index < lines.size()) {
            String line = lines.get(index);

            if (depth == 0) {
                Matcher matcher = METHOD_START.matcher(line);
                if (matcher.matches()) {
                    MethodCapture capture = captureMethod(lines, index, matcher.group(2));
                    methods.add(capture.method());
                    index = capture.nextIndex();
                    continue;
                }

                String trimmed = line.trim();
                Matcher typeMatcher = TOP_LEVEL_TYPE_DECLARATION.matcher(trimmed);
                if (typeMatcher.find()) {
                    TypeCapture capture = captureType(lines, index, typeMatcher.group(1));
                    types.add(capture.type());
                    index = capture.nextIndex();
                    continue;
                }
                if (!trimmed.isEmpty() && !trimmed.startsWith("//") && !trimmed.startsWith("import ")) {
                    topLevelStatement.append(line).append(System.lineSeparator());
                    if (trimmed.endsWith(";")) {
                        fields.add(new PdeField(topLevelStatement.toString().trim()));
                        topLevelStatement.setLength(0);
                    }
                } else if (topLevelStatement.length() > 0) {
                    topLevelStatement.append(line).append(System.lineSeparator());
                }
            }

            depth += countChar(line, '{');
            depth -= countChar(line, '}');
            index++;
        }

        if (depth != 0) {
            unsupportedReasons.add("Unbalanced braces detected in the PDE source.");
        }

        if (topLevelStatement.length() > 0 && !topLevelStatement.toString().trim().isEmpty()) {
            fields.add(new PdeField(topLevelStatement.toString().trim()));
        }

        PdeSizeCall sizeCall = parseSizeCall(methods);

        return new PdeSketchModel(
            sourcePath,
            rawSource,
            List.copyOf(fields),
            List.copyOf(types),
            List.copyOf(methods),
            List.copyOf(unsupportedReasons),
            sizeCall,
            MigrationMode.DEFAULT,
            containsToken(rawSource, "mouseX"),
            containsToken(rawSource, "mouseY"),
            containsToken(rawSource, "pmouseX"),
            containsToken(rawSource, "pmouseY"),
            hasInputHandler(methods, "mousePressed")
                || hasInputHandler(methods, "mouseDragged")
                || hasInputHandler(methods, "mouseReleased")
                || hasInputHandler(methods, "mouseMoved"),
            hasInputHandler(methods, "keyPressed"),
            hasInputHandler(methods, "keyReleased"),
            containsToken(rawSource, "key"),
            containsToken(rawSource, "keyCode")
        );
    }

    private TypeCapture captureType(List<String> lines, int startIndex, String typeName) {
        StringBuilder buffer = new StringBuilder();
        int depth = 0;
        boolean seenOpeningBrace = false;
        int index = startIndex;

        while (index < lines.size()) {
            String line = lines.get(index);
            buffer.append(line).append(System.lineSeparator());

            int opens = countChar(line, '{');
            int closes = countChar(line, '}');
            if (opens > 0) {
                seenOpeningBrace = true;
            }
            depth += opens;
            depth -= closes;

            index++;
            if (seenOpeningBrace && depth <= 0) {
                break;
            }
        }

        String declaration = buffer.toString().trim();
        String signature = lines.get(startIndex).trim();
        return new TypeCapture(new PdeType(signature, typeName, declaration), index);
    }

    private MethodCapture captureMethod(List<String> lines, int startIndex, String methodName) {
        StringBuilder buffer = new StringBuilder();
        int depth = 0;
        boolean seenOpeningBrace = false;
        int index = startIndex;

        while (index < lines.size()) {
            String line = lines.get(index);
            buffer.append(line).append(System.lineSeparator());

            int opens = countChar(line, '{');
            int closes = countChar(line, '}');
            if (opens > 0) {
                seenOpeningBrace = true;
            }
            depth += opens;
            depth -= closes;

            index++;
            if (seenOpeningBrace && depth <= 0) {
                break;
            }
        }

        String body = buffer.toString().trim();
        String signature = lines.get(startIndex).trim();
        return new MethodCapture(
            new PdeMethod(signature, methodName, body, classify(methodName)),
            index
        );
    }

    private PdeMethodKind classify(String methodName) {
        return switch (methodName) {
            case "setup" -> PdeMethodKind.SETUP;
            case "draw" -> PdeMethodKind.DRAW;
            case "mousePressed", "mouseDragged", "mouseReleased", "mouseMoved",
                    "keyPressed", "keyReleased" -> PdeMethodKind.INPUT_HANDLER;
            default -> PdeMethodKind.HELPER;
        };
    }

    private boolean containsToken(String source, String token) {
        return Pattern.compile("\\b" + Pattern.quote(token) + "\\b").matcher(source).find();
    }

    private int countChar(String text, char target) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }

    private boolean hasInputHandler(List<PdeMethod> methods, String name) {
        return methods.stream().anyMatch(method -> method.kind() == PdeMethodKind.INPUT_HANDLER && method.name().equals(name));
    }

    private PdeSizeCall parseSizeCall(List<PdeMethod> methods) {
        PdeMethod setup = methods.stream().filter(method -> "setup".equals(method.name())).findFirst().orElse(null);
        if (setup == null) {
            return null;
        }
        Matcher matcher = SIZE_CALL.matcher(setup.body());
        if (!matcher.find()) {
            return null;
        }
        String widthExpression = matcher.group(1).trim();
        String heightExpression = matcher.group(2).trim();
        String renderer = matcher.group(3) == null ? null : matcher.group(3).trim();
        return new PdeSizeCall(widthExpression, heightExpression, renderer);
    }

    private record MethodCapture(PdeMethod method, int nextIndex) {
    }

    private record TypeCapture(PdeType type, int nextIndex) {
    }
}
