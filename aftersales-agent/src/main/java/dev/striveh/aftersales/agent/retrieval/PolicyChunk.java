package dev.striveh.aftersales.agent.retrieval;

public record PolicyChunk(
        String id,
        String docId,
        String docTitle,
        String sectionId,
        String sectionTitle,
        String content
) {
}
