package dev.striveh.aftersales.agent.retrieval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class InMemoryPolicyIndex implements PolicyIndex {

    private final HashingEmbeddingModel embeddings;
    private final List<Indexed> chunks = new ArrayList<>();

    public InMemoryPolicyIndex(HashingEmbeddingModel embeddings) {
        this.embeddings = embeddings;
    }

    @Override
    public void replaceAll(List<PolicyChunk> incoming) {
        chunks.clear();
        for (PolicyChunk chunk : incoming) {
            chunks.add(new Indexed(chunk, embeddings.embed(chunk.sectionTitle() + "\n" + chunk.content())));
        }
    }

    @Override
    public List<PolicyChunk> vectorSearch(String query, int limit) {
        float[] probe = embeddings.embed(query);
        return chunks.stream()
                .map(indexed -> new Scored(indexed.chunk, cosine(probe, indexed.vector)))
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(limit)
                .map(Scored::chunk)
                .toList();
    }

    @Override
    public List<PolicyChunk> keywordSearch(String query, int limit) {
        Set<String> queryTokens = new HashSet<>();
        for (String token : ChineseBigrams.tokens(query)) {
            if (token.length() >= 2) {
                queryTokens.add(token);
            }
        }
        if (queryTokens.isEmpty()) {
            return List.of();
        }
        return chunks.stream()
                .map(indexed -> new Scored(indexed.chunk, overlap(queryTokens, indexed.chunk)))
                .filter(scored -> scored.score > 0)
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(limit)
                .map(Scored::chunk)
                .toList();
    }

    @Override
    public Optional<PolicyChunk> findBySection(String docId, String sectionTitle) {
        return chunks.stream()
                .map(indexed -> indexed.chunk)
                .filter(chunk -> chunk.docId().equals(docId) && chunk.sectionTitle().contains(sectionTitle))
                .findFirst();
    }

    @Override
    public List<PolicyChunk> all() {
        return chunks.stream().map(indexed -> indexed.chunk).toList();
    }

    private double overlap(Set<String> queryTokens, PolicyChunk chunk) {
        Set<String> text = new HashSet<>(ChineseBigrams.tokens(chunk.sectionTitle() + "\n" + chunk.content()));
        long hit = queryTokens.stream().filter(text::contains).count();
        return (double) hit / queryTokens.size();
    }

    static double cosine(float[] left, float[] right) {
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < left.length; i++) {
            dot += (double) left[i] * right[i];
            leftNorm += (double) left[i] * left[i];
            rightNorm += (double) right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private record Indexed(PolicyChunk chunk, float[] vector) {
    }

    private record Scored(PolicyChunk chunk, double score) {
    }
}
