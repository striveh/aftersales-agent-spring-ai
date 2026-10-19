package dev.striveh.aftersales.agent.retrieval;

import java.util.List;

public interface Reranker {

    List<RankedChunk> rerank(String query, List<RankedChunk> fused, int topK);
}
