package dev.striveh.aftersales.agent.retrieval;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Vector channel goes through Spring AI's {@link VectorStore} (pgvector).
 * Keyword channel is Postgres full text over the same bigram tokens, ranked with
 * {@code ts_rank_cd}. The two lists are fused outside this class.
 */
public final class PgPolicyIndex implements PolicyIndex {

    private static final Logger log = LoggerFactory.getLogger(PgPolicyIndex.class);

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbc;
    private final List<PolicyChunk> cache = new ArrayList<>();
    private boolean schemaReady;

    public PgPolicyIndex(VectorStore vectorStore, JdbcTemplate jdbc) {
        this.vectorStore = vectorStore;
        this.jdbc = jdbc;
    }

    @Override
    public void replaceAll(List<PolicyChunk> chunks) {
        ensureSchema();
        jdbc.update("TRUNCATE TABLE policy_fts");
        try {
            jdbc.update("DELETE FROM vector_store");
        } catch (DataAccessException exception) {
            log.warn("vector_store 还不能清空，继续写入。{}", exception.getMessage());
        }
        if (!chunks.isEmpty()) {
            vectorStore.add(chunks.stream().map(this::toDocument).toList());
            jdbc.batchUpdate("""
                    INSERT INTO policy_fts (id, doc_id, doc_title, section_id, section_title, content, content_tokens)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, chunks, chunks.size(), (statement, chunk) -> {
                statement.setString(1, chunk.id());
                statement.setString(2, chunk.docId());
                statement.setString(3, chunk.docTitle());
                statement.setString(4, chunk.sectionId());
                statement.setString(5, chunk.sectionTitle());
                statement.setString(6, chunk.content());
                statement.setString(7, String.join(" ", ChineseBigrams.tokens(chunk.sectionTitle() + "\n" + chunk.content())));
            });
        }
        cache.clear();
        cache.addAll(chunks);
    }

    @Override
    public List<PolicyChunk> vectorSearch(String query, int limit) {
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(limit)
                .similarityThreshold(0.0)
                .build());
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        List<PolicyChunk> chunks = new ArrayList<>();
        for (Document document : documents) {
            fromDocument(document).ifPresent(chunks::add);
        }
        return chunks;
    }

    @Override
    public List<PolicyChunk> keywordSearch(String query, int limit) {
        String tsQuery = ChineseBigrams.orQuery(query);
        if (tsQuery.isBlank()) {
            return List.of();
        }
        try {
            return jdbc.query("""
                    WITH q AS (SELECT to_tsquery('simple', ?) AS query)
                    SELECT id, doc_id, doc_title, section_id, section_title, content
                    FROM policy_fts, q
                    WHERE content_tsv @@ q.query
                    ORDER BY ts_rank_cd(content_tsv, q.query) DESC
                    LIMIT ?
                    """, (rs, row) -> new PolicyChunk(
                    rs.getString("id"),
                    rs.getString("doc_id"),
                    rs.getString("doc_title"),
                    rs.getString("section_id"),
                    rs.getString("section_title"),
                    rs.getString("content")), tsQuery, limit);
        } catch (DataAccessException exception) {
            log.warn("全文检索失败，这一路返回空列表。query={} error={}", tsQuery, exception.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<PolicyChunk> findBySection(String docId, String sectionTitle) {
        return cache.stream()
                .filter(chunk -> chunk.docId().equals(docId) && chunk.sectionTitle().contains(sectionTitle))
                .findFirst();
    }

    @Override
    public List<PolicyChunk> all() {
        return List.copyOf(cache);
    }

    private Document toDocument(PolicyChunk chunk) {
        return new Document(chunk.id(), chunk.content(), Map.of(
                "docId", chunk.docId(),
                "docTitle", chunk.docTitle(),
                "sectionId", chunk.sectionId(),
                "sectionTitle", chunk.sectionTitle()));
    }

    private Optional<PolicyChunk> fromDocument(Document document) {
        return cache.stream().filter(chunk -> chunk.id().equals(document.getId())).findFirst()
                .or(() -> Optional.of(new PolicyChunk(
                        document.getId(),
                        stringMeta(document, "docId"),
                        stringMeta(document, "docTitle"),
                        stringMeta(document, "sectionId"),
                        stringMeta(document, "sectionTitle"),
                        document.getText())));
    }

    private String stringMeta(Document document, String key) {
        Object value = document.getMetadata().get(key);
        return value == null ? "" : value.toString();
    }

    private void ensureSchema() {
        if (schemaReady) {
            return;
        }
        try (InputStream input = PgPolicyIndex.class.getResourceAsStream("/schema.sql")) {
            if (input == null) {
                throw new IllegalStateException("缺少 schema.sql");
            }
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            for (String statement : sql.split(";")) {
                if (!statement.isBlank()) {
                    jdbc.execute(statement);
                }
            }
            schemaReady = true;
        } catch (IOException exception) {
            throw new IllegalStateException("无法初始化 policy_fts", exception);
        }
    }
}
