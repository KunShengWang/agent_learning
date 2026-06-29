package com.agent.demo10.rag;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LocalHashEmbeddingModel implements EmbeddingModel {

    private final int dimension;

    public LocalHashEmbeddingModel(int dimension) {
        this.dimension = dimension;
    }

    @Override
    public double[] embed(String text) {
        double[] vector = new double[dimension];
        for (String token : tokenize(text)) {
            int hash = stableHash(token);
            int index = Math.floorMod(hash, dimension);
            double sign = (hash & 1) == 0 ? 1.0 : -1.0;
            vector[index] += sign;
        }
        normalize(vector);
        return vector;
    }

    private static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }

        StringBuilder asciiWord = new StringBuilder();
        StringBuilder compactChars = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (Character.isLetterOrDigit(current) && current < 128) {
                asciiWord.append(Character.toLowerCase(current));
                compactChars.append(Character.toLowerCase(current));
                continue;
            }

            if (!asciiWord.isEmpty()) {
                tokens.add(asciiWord.toString());
                asciiWord.setLength(0);
            }

            if (!Character.isWhitespace(current)) {
                compactChars.append(current);
                tokens.add(String.valueOf(current));
            }
        }

        if (!asciiWord.isEmpty()) {
            tokens.add(asciiWord.toString());
        }

        String compact = compactChars.toString();
        for (int i = 0; i + 1 < compact.length(); i++) {
            tokens.add(compact.substring(i, i + 2));
        }

        return tokens;
    }

    private static int stableHash(String token) {
        byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
        int hash = 0x811c9dc5;
        for (byte current : bytes) {
            hash ^= current & 0xff;
            hash *= 0x01000193;
        }
        return hash;
    }

    private static void normalize(double[] vector) {
        double sum = 0.0;
        for (double value : vector) {
            sum += value * value;
        }
        if (sum == 0.0) {
            return;
        }

        double norm = Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / norm;
        }
    }
}
