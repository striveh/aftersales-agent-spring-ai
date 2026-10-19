package dev.striveh.aftersales.agent.retrieval;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * Deterministic local embedding so retrieval runs without an API key.
 * It is a hashed bag of character bigrams, not a semantic model. Swap this bean
 * for an OpenAI-compatible embedding model when you want real vectors; keep the
 * dimension in sync with the pgvector column.
 */
public final class HashingEmbeddingModel implements EmbeddingModel {

    public static final int DIMENSIONS = 384;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> inputs = request.getInstructions();
        List<Embedding> embeddings = new ArrayList<>(inputs.size());
        for (int i = 0; i < inputs.size(); i++) {
            embeddings.add(new Embedding(embed(inputs.get(i)), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIMENSIONS];
        List<String> grams = ChineseBigrams.tokens(text);
        if (grams.isEmpty()) {
            return vector;
        }
        for (String gram : grams) {
            int hash = gram.hashCode();
            int index = Math.floorMod(hash, DIMENSIONS);
            float sign = ((hash >>> 16) & 1) == 0 ? 1f : -1f;
            vector[index] += sign * (gram.length() >= 2 ? 1.4f : 0.6f);
        }
        double norm = 0;
        for (float value : vector) {
            norm += (double) value * value;
        }
        if (norm == 0) {
            return vector;
        }
        float scale = (float) (1.0 / Math.sqrt(norm));
        for (int i = 0; i < vector.length; i++) {
            vector[i] *= scale;
        }
        return vector;
    }
}
