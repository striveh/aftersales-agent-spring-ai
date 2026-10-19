package dev.striveh.aftersales.agent.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class RetrievalTest {

    @Test
    void bigramsOverlapOnChinese() {
        List<String> tokens = ChineseBigrams.tokens("七天无理由退货");
        assertThat(tokens).contains("七天", "无理", "理由");
        assertThat(ChineseBigrams.orQuery("耳机退货")).contains("|");
    }

    @Test
    void embeddingsAreStableAndNormalized() {
        HashingEmbeddingModel model = new HashingEmbeddingModel();
        float[] first = model.embed("七天无理由");
        float[] second = model.embed("七天无理由");
        assertThat(first).containsExactly(second);
        assertThat(model.dimensions()).isEqualTo(384);
        double norm = 0;
        for (float value : first) {
            norm += value * value;
        }
        assertThat(norm).isBetween(0.99, 1.01);
    }

    @Test
    void fusionPrefersADocumentFoundByBothChannels() {
        PolicyChunk both = chunk("both");
        PolicyChunk vectorOnly = chunk("vector");
        ReciprocalRankFusion fusion = new ReciprocalRankFusion(60);
        List<RankedChunk> ranked = fusion.fuse(List.of(vectorOnly, both), List.of(both));
        assertThat(ranked.get(0).chunk().id()).isEqualTo("both");
        assertThat(ranked.get(0).vectorRank()).isEqualTo(2);
        assertThat(ranked.get(0).keywordRank()).isEqualTo(1);
    }

    @Test
    void corpusCitesEverySectionTheRulesUse() {
        List<PolicyChunk> chunks = new MarkdownPolicyChunker().chunkAll(new PolicyCorpus().load());
        assertThat(chunks).extracting(PolicyChunk::sectionTitle).contains(
                PolicySections.NO_REASON,
                PolicySections.EXCLUDED,
                PolicySections.FOOD,
                PolicySections.EXCHANGE,
                PolicySections.PARTS,
                PolicySections.FREIGHT,
                PolicySections.CONFIRM,
                PolicySections.WARRANTY,
                PolicySections.HUMAN,
                PolicySections.INTERCEPT,
                PolicySections.LOST,
                PolicySections.DAMAGE);
    }

    @Test
    void unpackedEarphoneQueryHitsTheNoReasonClause() {
        InMemoryPolicyIndex index = new InMemoryPolicyIndex(new HashingEmbeddingModel());
        index.replaceAll(new MarkdownPolicyChunker().chunkAll(new PolicyCorpus().load()));
        HybridPolicyRetriever retriever = new HybridPolicyRetriever(index, 8, 3);
        List<PolicyChunk> top = retriever.retrieve("拆封的耳机还能七天无理由退货吗");
        assertThat(top).isNotEmpty();
        assertThat(top.get(0).sectionTitle()).contains("七天无理由");
    }

    @Test
    void longSectionIsSplitWithOverlap() {
        String paragraph = "配件必须齐全。".repeat(80);
        String markdown = "# 测试制度\n\n## 很长的一节\n\n" + paragraph + "\n\n" + paragraph;
        List<PolicyChunk> chunks = new MarkdownPolicyChunker().chunk(new PolicySource("demo", markdown));
        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(chunks).allMatch(chunk -> chunk.sectionTitle().equals("很长的一节"));
        assertThat(chunks.get(1).content()).contains(chunks.get(0).content().substring(chunks.get(0).content().length() - 20));
    }

    private static PolicyChunk chunk(String id) {
        return new PolicyChunk(id, "doc", "title", id, id, id);
    }
}
