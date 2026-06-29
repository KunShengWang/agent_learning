package com.agent.demo10.rag;

import java.util.ArrayList;
import java.util.List;

public interface EmbeddingModel {

    double[] embed(String text);

    default List<double[]> embedTexts(List<String> texts) {
        List<double[]> embeddings = new ArrayList<>();
        for (String text : texts) {
            embeddings.add(embed(text));
        }
        return embeddings;
    }
}
