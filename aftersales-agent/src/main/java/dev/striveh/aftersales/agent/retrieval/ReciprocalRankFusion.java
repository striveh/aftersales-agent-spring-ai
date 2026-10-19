package dev.striveh.aftersales.agent.retrieval;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Standard reciprocal rank fusion. A hit in both lists outranks a hit in only one. */
public final class ReciprocalRankFusion {

    private final int k;

    public ReciprocalRankFusion() {
        this(60);
    }

    public ReciprocalRankFusion(int k) {
        this.k = k;
    }

    public List<RankedChunk> fuse(List<PolicyChunk> vector, List<PolicyChunk> keyword) {
        Map<String, Accumulator> accumulators = new LinkedHashMap<>();
        add(accumulators, vector, true);
        add(accumulators, keyword, false);
        return accumulators.values().stream()
                .map(accumulator -> new RankedChunk(accumulator.chunk, accumulator.score, accumulator.vectorRank,
                        accumulator.keywordRank))
                .sorted(Comparator.comparingDouble(RankedChunk::score).reversed())
                .toList();
    }

    private void add(Map<String, Accumulator> accumulators, List<PolicyChunk> chunks, boolean vector) {
        for (int i = 0; i < chunks.size(); i++) {
            PolicyChunk chunk = chunks.get(i);
            int rank = i + 1;
            Accumulator accumulator = accumulators.computeIfAbsent(chunk.id(), id -> new Accumulator(chunk));
            accumulator.score += 1.0 / (k + rank);
            if (vector) {
                accumulator.vectorRank = rank;
            } else {
                accumulator.keywordRank = rank;
            }
        }
    }

    private static final class Accumulator {
        private final PolicyChunk chunk;
        private double score;
        private int vectorRank = -1;
        private int keywordRank = -1;

        private Accumulator(PolicyChunk chunk) {
            this.chunk = chunk;
        }
    }
}
