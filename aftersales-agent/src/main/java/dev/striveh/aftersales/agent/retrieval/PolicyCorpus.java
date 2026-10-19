package dev.striveh.aftersales.agent.retrieval;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class PolicyCorpus {

    private static final List<String> FILES = List.of(
            "policies/return-policy.md",
            "policies/warranty-policy.md",
            "policies/logistics-policy.md"
    );

    public List<PolicySource> load() {
        List<PolicySource> sources = new ArrayList<>();
        for (String file : FILES) {
            try (InputStream input = PolicyCorpus.class.getClassLoader().getResourceAsStream(file)) {
                if (input == null) {
                    throw new IllegalStateException("缺少制度文件 " + file);
                }
                String markdown = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                String docId = file.substring(file.lastIndexOf('/') + 1, file.length() - 3);
                sources.add(new PolicySource(docId, markdown));
            } catch (IOException exception) {
                throw new IllegalStateException("无法读取 " + file, exception);
            }
        }
        return List.copyOf(sources);
    }
}
