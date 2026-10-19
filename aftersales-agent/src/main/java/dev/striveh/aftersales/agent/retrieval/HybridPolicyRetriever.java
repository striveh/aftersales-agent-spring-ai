package dev.striveh.aftersales.agent.retrieval;

import java.util.List;

public final class HybridPolicyRetriever {

    private final PolicyIndex index;
    private final ReciprocalRankFusion fusion;
    private final Reranker reranker;
    private final int candidateK;
    private final int topK;

    public HybridPolicyRetriever(PolicyIndex index, int candidateK, int topK) {
        this(index, new ReciprocalRankFusion(), new LexicalReranker(), candidateK, topK);
    }

    public HybridPolicyRetriever(PolicyIndex index, ReciprocalRankFusion fusion, Reranker reranker, int candidateK, int topK) {
        this.index = index;
        this.fusion = fusion;
        this.reranker = reranker;
        this.candidateK = candidateK;
        this.topK = topK;
    }

    public List<PolicyChunk> retrieve(String query) {
        return retrieve(query, candidateK, topK);
    }

    public List<PolicyChunk> retrieve(String query, int candidates, int limit) {
        List<PolicyChunk> vector = index.vectorSearch(query, candidates);
        List<PolicyChunk> keyword = index.keywordSearch(query, candidates);
        return reranker.rerank(query, fusion.fuse(vector, keyword), limit).stream()
                .map(RankedChunk::chunk)
                .toList();
    }

    public PolicyIndex index() {
        return index;
    }
}
