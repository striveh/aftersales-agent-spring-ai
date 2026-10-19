package dev.striveh.aftersales.agent.retrieval;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lightweight rerank: fused rank, bigram coverage, and a title phrase bonus.
 * This is not a cross-encoder. The {@link Reranker} interface is the swap point.
 */
public final class LexicalReranker implements Reranker {

    @Override
    public List<RankedChunk> rerank(String query, List<RankedChunk> fused, int topK) {
        if (fused.isEmpty() || topK <= 0) {
            return List.of();
        }
        double max = fused.stream().mapToDouble(RankedChunk::score).max().orElse(1);
        List<String> queryGrams = ChineseBigrams.tokens(query);
        String compactQuery = ChineseBigrams.normalize(query).replace(" ", "");
        return fused.stream()
                .map(item -> item.withScore(score(item, max, queryGrams, compactQuery)))
                .sorted(Comparator.comparingDouble(RankedChunk::score).reversed())
                .limit(topK)
                .toList();
    }

    private double score(RankedChunk item, double max, List<String> queryGrams, String compactQuery) {
        double rrf = max == 0 ? 0 : item.score() / max;
        double coverage = coverage(queryGrams, ChineseBigrams.tokens(item.chunk().content()));
        double title = coverage(queryGrams, ChineseBigrams.tokens(item.chunk().sectionTitle()));
        double phrase = phrase(compactQuery, item.chunk().sectionTitle());
        return 0.35 * rrf + 0.20 * coverage + 0.25 * title + phrase;
    }

    private double coverage(List<String> queryGrams, List<String> textGrams) {
        if (queryGrams.isEmpty()) {
            return 0;
        }
        Set<String> query = new HashSet<>();
        for (String gram : queryGrams) {
            if (gram.length() >= 2) {
                query.add(gram);
            }
        }
        if (query.isEmpty()) {
            query.addAll(queryGrams);
        }
        Set<String> text = new HashSet<>(textGrams);
        long hit = query.stream().filter(text::contains).count();
        return (double) hit / query.size();
    }

    private double phrase(String compactQuery, String sectionTitle) {
        String title = ChineseBigrams.normalize(sectionTitle).replace(" ", "");
        if (compactQuery.length() < 4 || title.isEmpty()) {
            return 0;
        }
        int max = Math.min(8, compactQuery.length());
        for (int length = max; length >= 4; length--) {
            for (int i = 0; i + length <= compactQuery.length(); i++) {
                if (title.contains(compactQuery.substring(i, i + length))) {
                    return 0.45 + (length / 40.0);
                }
            }
        }
        return 0;
    }
}
