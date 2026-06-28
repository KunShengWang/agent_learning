package com.agent.demo6.framework;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
            throw new IllegalStateException("字段不是字符串：" + fieldName);
        }

        return readString(json, valueStart);
    }

    public static Boolean booleanField(String json, String fieldName) {
        int fieldIndex = json.indexOf("\"" + fieldName + "\"");
        if (fieldIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(':', fieldIndex);
        int valueStart = skipWhitespace(json, colonIndex + 1);
        if (startsWith(json, valueStart, "true")) {
            return true;
        }
        if (startsWith(json, valueStart, "false")) {
            return false;
        }
        if (startsWith(json, valueStart, "null")) {
            return null;
        }
        throw new IllegalStateException("字段不是 boolean：" + fieldName);
    }

    public static String objectField(String json, String fieldName) {
        int fieldIndex = json.indexOf("\"" + fieldName + "\"");
        if (fieldIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(':', fieldIndex);
        int valueStart = skipWhitespace(json, colonIndex + 1);
        if (startsWith(json, valueStart, "null")) {
            return null;
        }
        if (valueStart >= json.length() || json.charAt(valueStart) != '{') {
            throw new IllegalStateException("字段不是对象：" + fieldName);
        }

        return readBalanced(json, valueStart, '{', '}');
    }

    public static String arrayField(String json, String fieldName) {
        int fieldIndex = json.indexOf("\"" + fieldName + "\"");
        if (fieldIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(':', fieldIndex);
        int valueStart = skipWhitespace(json, colonIndex + 1);
        if (startsWith(json, valueStart, "null")) {
            return null;
        }
        if (valueStart >= json.length() || json.charAt(valueStart) != '[') {
            throw new IllegalStateException("字段不是数组：" + fieldName);
        }

        return readBalanced(json, valueStart, '[', ']');
    }

    public static List<String> splitTopLevelObjects(String arrayJson) {
        List<String> objects = new ArrayList<>();
        int index = 1;
        while (index < arrayJson.length() - 1) {
            index = skipWhitespaceAndCommas(arrayJson, index);
            if (index >= arrayJson.length() - 1) {
                break;
            }
            if (arrayJson.charAt(index) != '{') {
                throw new IllegalStateException("数组元素不是对象：" + arrayJson);
            }

            int end = findMatching(arrayJson, index, '{', '}');
            objects.add(arrayJson.substring(index, end + 1));
            index = end + 1;
        }
        return objects;
    }

    public static String firstChoiceMessageObject(String json) {
        int choicesIndex = json.indexOf("\"choices\"");
        if (choicesIndex < 0) {
            throw new IllegalStateException("响应中没有 choices 字段：" + json);
        }

        int choicesArrayStart = json.indexOf('[', choicesIndex);
        int firstChoiceStart = json.indexOf('{', choicesArrayStart);
        if (choicesArrayStart < 0 || firstChoiceStart < 0) {
            throw new IllegalStateException("choices 字段格式不正确：" + json);
        }

        String firstChoiceObject = readBalanced(json, firstChoiceStart, '{', '}');
        return objectField(firstChoiceObject, "message");
    }

    public static Map<String, String> flatStringMap(String objectJson) {
        Map<String, String> values = new LinkedHashMap<>();
        if (objectJson == null || objectJson.length() < 2) {
            return values;
        }

        int index = 1;
        while (index < objectJson.length() - 1) {
            index = skipWhitespaceAndCommas(objectJson, index);
            if (index >= objectJson.length() - 1) {
                break;
            }
            if (objectJson.charAt(index) != '"') {
                break;
            }

            String key = readString(objectJson, index);
            int keyEnd = findStringEnd(objectJson, index);
            int colonIndex = objectJson.indexOf(':', keyEnd + 1);
            int valueStart = skipWhitespace(objectJson, colonIndex + 1);

            if (valueStart < objectJson.length() && objectJson.charAt(valueStart) == '"') {
                values.put(key, readString(objectJson, valueStart));
                index = findStringEnd(objectJson, valueStart) + 1;
            } else {
                int valueEnd = valueStart;
                while (valueEnd < objectJson.length()
                        && objectJson.charAt(valueEnd) != ','
                        && objectJson.charAt(valueEnd) != '}') {
                    valueEnd++;
                }
                values.put(key, objectJson.substring(valueStart, valueEnd).trim());
                index = valueEnd;
            }
        }

        return values;
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

        throw new IllegalStateException("JSON 字符串没有正常结束：" + json);
    }

    public static String readBalanced(String text, int start, char open, char close) {
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

        throw new IllegalStateException("JSON 结构没有正常闭合：" + text.substring(start));
    }

    private static int findStringEnd(String json, int openingQuoteIndex) {
        boolean escaping = false;
        for (int i = openingQuoteIndex + 1; i < json.length(); i++) {
            char current = json.charAt(i);
            if (escaping) {
                escaping = false;
                continue;
            }
            if (current == '\\') {
                escaping = true;
                continue;
            }
            if (current == '"') {
                return i;
            }
        }
        throw new IllegalStateException("JSON 字符串没有正常结束：" + json);
    }

    private static int skipWhitespace(String text, int start) {
        int index = start;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private static int skipWhitespaceAndCommas(String text, int start) {
        int index = start;
        while (index < text.length()) {
            char current = text.charAt(index);
            if (!Character.isWhitespace(current) && current != ',') {
                return index;
            }
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
