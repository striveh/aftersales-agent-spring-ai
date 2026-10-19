package dev.striveh.aftersales.agent.retrieval;

public record RankedChunk(PolicyChunk chunk, double score, int vectorRank, int keywordRank) {

    public RankedChunk withScore(double updated) {
        return new RankedChunk(chunk, updated, vectorRank, keywordRank);
    }
}
