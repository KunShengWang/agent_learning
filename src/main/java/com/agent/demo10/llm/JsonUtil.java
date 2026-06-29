package com.agent.demo10.llm;

public final class JsonUtil {

    private JsonUtil() {
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            switch (current) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(current);
            }
        }
        return builder.toString();
    }

    public static String stringField(String json, String fieldName) {
        int fieldIndex = json.indexOf("\"" + fieldName + "\"");
        if (fieldIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(':', fieldIndex);
        int valueStart = skipWhitespace(json, colonIndex + 1);
        if (startsWith(json, valueStart, "null")) {
            return null;
        }
        if (valueStart >= json.length() || json.charAt(valueStart) != '"') {
            return null;
        }
        return readString(json, valueStart);
    }

    public static String firstChoiceMessageObject(String json) {
        int choicesIndex = json.indexOf("\"choices\"");
        if (choicesIndex < 0) {
            throw new IllegalStateException("Response has no choices field: " + json);
        }

        int choicesArrayStart = json.indexOf('[', choicesIndex);
        int firstChoiceStart = json.indexOf('{', choicesArrayStart);
        if (choicesArrayStart < 0 || firstChoiceStart < 0) {
            throw new IllegalStateException("Invalid choices field: " + json);
        }

        String firstChoiceObject = readBalanced(json, firstChoiceStart, '{', '}');
        return objectField(firstChoiceObject, "message");
    }

    public static String objectField(String json, String fieldName) {
        int fieldIndex = json.indexOf("\"" + fieldName + "\"");
        if (fieldIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(':', fieldIndex);
        int valueStart = skipWhitespace(json, colonIndex + 1);
        if (valueStart >= json.length() || json.charAt(valueStart) != '{') {
            return null;
        }
        return readBalanced(json, valueStart, '{', '}');
    }

    public static String readString(String json, int openingQuoteIndex) {
        StringBuilder builder = new StringBuilder();
        boolean escaping = false;

        for (int i = openingQuoteIndex + 1; i < json.length(); i++) {
            char current = json.charAt(i);

            if (escaping) {
                if (current == 'u' && i + 4 < json.length()) {
                    String hex = json.substring(i + 1, i + 5);
                    builder.append((char) Integer.parseInt(hex, 16));
                    i += 4;
                } else {
                    builder.append(switch (current) {
                        case '"' -> '"';
                        case '\\' -> '\\';
                        case '/' -> '/';
                        case 'b' -> '\b';
                        case 'f' -> '\f';
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 't' -> '\t';
                        default -> current;
                    });
                }
                escaping = false;
                continue;
            }

            if (current == '\\') {
                escaping = true;
                continue;
            }
            if (current == '"') {
                return builder.toString();
            }
            builder.append(current);
        }
        throw new IllegalStateException("JSON string is not closed: " + json);
    }

    private static String readBalanced(String text, int start, char open, char close) {
        int end = findMatching(text, start, open, close);
        return text.substring(start, end + 1);
    }

    private static int findMatching(String text, int start, char open, char close) {
        int depth = 0;
        boolean inString = false;
        boolean escaping = false;

        for (int i = start; i < text.length(); i++) {
            char current = text.charAt(i);

            if (escaping) {
                escaping = false;
                continue;
            }
            if (current == '\\') {
                escaping = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }

            if (current == open) {
                depth++;
            } else if (current == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        throw new IllegalStateException("JSON structure is not closed: " + text.substring(start));
    }

    private static int skipWhitespace(String text, int start) {
        int index = start;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private static boolean startsWith(String text, int start, String expected) {
        return start >= 0
                && start + expected.length() <= text.length()
                && text.regionMatches(start, expected, 0, expected.length());
    }
}
