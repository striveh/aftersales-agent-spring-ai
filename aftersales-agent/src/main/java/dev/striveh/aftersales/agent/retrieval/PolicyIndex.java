package dev.striveh.aftersales.agent.retrieval;

import java.util.List;
import java.util.Optional;

public interface PolicyIndex {

    void replaceAll(List<PolicyChunk> chunks);

    List<PolicyChunk> vectorSearch(String query, int limit);

    List<PolicyChunk> keywordSearch(String query, int limit);

    Optional<PolicyChunk> findBySection(String docId, String sectionTitle);

    List<PolicyChunk> all();
}
