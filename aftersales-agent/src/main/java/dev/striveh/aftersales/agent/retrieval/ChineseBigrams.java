package dev.striveh.aftersales.agent.retrieval;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Character bigrams stand in for a Chinese analyzer. The official pgvector image
 * does not ship zhparser or pg_jieba, and PostgreSQL's simple tokenizer cannot
 * split unspaced Chinese. The same tokens feed hashing embeddings, in-memory
 * keyword rank, and the Postgres {@code simple} tsvector.
 */
public final class ChineseBigrams {

    private ChineseBigrams() {
    }

    public static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = Character.toLowerCase(text.charAt(i));
            if (Character.isWhitespace(c) || isPunctuation(c)) {
                out.append(' ');
            } else {
                out.append(c);
            }
        }
        return out.toString().replaceAll(" +", " ").trim();
    }

    public static List<String> tokens(String text) {
        String normalized = normalize(text);
        List<String> tokens = new ArrayList<>();
        List<Character> cjk = new ArrayList<>();
        StringBuilder latin = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c == ' ') {
                flushCjk(cjk, tokens);
                flushLatin(latin, tokens);
            } else if (isCjk(c)) {
                flushLatin(latin, tokens);
                cjk.add(c);
            } else if (isLatinOrDigit(c)) {
                flushCjk(cjk, tokens);
                latin.append(c);
            } else {
                flushCjk(cjk, tokens);
                flushLatin(latin, tokens);
            }
        }
        flushCjk(cjk, tokens);
        flushLatin(latin, tokens);
        return tokens;
    }

    public static String orQuery(String text) {
        Set<String> grams = new LinkedHashSet<>();
        for (String token : tokens(text)) {
            if (token.length() >= 2 && safeTs(token) != null) {
                grams.add(token);
            }
        }
        if (grams.isEmpty()) {
            for (String token : tokens(text)) {
                if (safeTs(token) != null) {
                    grams.add(token);
                }
            }
        }
        return grams.stream().limit(24).reduce((left, right) -> left + " | " + right).orElse("");
    }

    public static String safeTs(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (!isCjk(c) && !isLatinOrDigit(c)) {
                return null;
            }
        }
        return token.toLowerCase(Locale.ROOT);
    }

    private static void flushCjk(List<Character> chars, List<String> tokens) {
        for (char c : chars) {
            tokens.add(String.valueOf(c));
        }
        for (int i = 0; i + 1 < chars.size(); i++) {
            tokens.add(String.valueOf(chars.get(i)) + chars.get(i + 1));
        }
        chars.clear();
    }

    private static void flushLatin(StringBuilder latin, List<String> tokens) {
        if (!latin.isEmpty()) {
            tokens.add(latin.toString());
            if (latin.length() > 1) {
                for (int i = 0; i + 1 < latin.length(); i++) {
                    tokens.add(latin.substring(i, i + 2));
                }
            }
            latin.setLength(0);
        }
    }

    private static boolean isCjk(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }

    private static boolean isLatinOrDigit(char c) {
        return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
    }

    private static boolean isPunctuation(char c) {
        return !Character.isLetterOrDigit(c);
    }
}
