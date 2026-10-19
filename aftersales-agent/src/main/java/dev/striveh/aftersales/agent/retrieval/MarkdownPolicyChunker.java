package dev.striveh.aftersales.agent.retrieval;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MarkdownPolicyChunker {

    static final int SPLIT_AT = 700;
    static final int WINDOW = 480;

    public List<PolicyChunk> chunkAll(List<PolicySource> sources) {
        List<PolicyChunk> chunks = new ArrayList<>();
        for (PolicySource source : sources) {
            chunks.addAll(chunk(source));
        }
        return List.copyOf(chunks);
    }

    public List<PolicyChunk> chunk(PolicySource source) {
        String title = source.docId();
        String heading = null;
        StringBuilder body = new StringBuilder();
        List<PolicyChunk> chunks = new ArrayList<>();
        int sectionIndex = 0;
        for (String line : source.markdown().split("\\R")) {
            if (line.startsWith("# ") && !line.startsWith("## ")) {
                title = line.substring(2).trim();
                continue;
            }
            if (line.startsWith("## ")) {
                sectionIndex = flush(source.docId(), title, heading, body, sectionIndex, chunks);
                heading = line.substring(3).trim();
                continue;
            }
            if (heading != null) {
                body.append(line).append('\n');
            }
        }
        flush(source.docId(), title, heading, body, sectionIndex, chunks);
        return List.copyOf(chunks);
    }

    private int flush(String docId, String title, String heading, StringBuilder body, int sectionIndex,
                      List<PolicyChunk> chunks) {
        if (heading == null) {
            return sectionIndex;
        }
        int index = sectionIndex + 1;
        String text = body.toString().trim();
        String whole = "## " + heading + "\n\n" + text;
        if (whole.length() <= SPLIT_AT) {
            chunks.add(chunk(docId, title, "s" + index, heading, whole));
        } else {
            List<String> parts = windows(text);
            for (int part = 0; part < parts.size(); part++) {
                String sectionId = parts.size() == 1 ? "s" + index : "s" + index + "-p" + (part + 1);
                chunks.add(chunk(docId, title, sectionId, heading, "## " + heading + "\n\n" + parts.get(part)));
            }
        }
        body.setLength(0);
        return index;
    }

    private List<String> windows(String text) {
        String[] paragraphs = text.split("\\n\\s*\\n");
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (current.length() > 0 && current.length() + paragraph.length() > WINDOW) {
                parts.add(current.toString().trim());
                String overlap = overlap(current.toString());
                current.setLength(0);
                current.append(overlap);
            }
            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(paragraph.trim());
        }
        if (!current.isEmpty()) {
            parts.add(current.toString().trim());
        }
        return parts.isEmpty() ? List.of(text) : parts;
    }

    private String overlap(String text) {
        String trimmed = text.trim();
        if (trimmed.length() <= 80) {
            return trimmed;
        }
        return trimmed.substring(trimmed.length() - 80);
    }

    private PolicyChunk chunk(String docId, String title, String sectionId, String heading, String content) {
        String key = docId + "#" + sectionId;
        String id = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
        return new PolicyChunk(id, docId, title, sectionId, heading, content);
    }
}
