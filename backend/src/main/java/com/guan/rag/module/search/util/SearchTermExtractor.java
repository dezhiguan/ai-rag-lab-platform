package com.guan.rag.module.search.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 Chunk 内容或查询文本中抽取专有词（错误码、API 路径、API 短名），供 ES terms 字段与 BM25 加权查询使用。
 */
public final class SearchTermExtractor {

    private static final Pattern ERROR_CODE = Pattern.compile("\\b[A-Z]{2,}_[0-9]{3,}\\b");
    private static final Pattern API_PATH = Pattern.compile("/api/[A-Za-z0-9/_-]+");
    /** 查询中的 API 短名（如 send-code），索引侧主要从路径末段得到 */
    private static final Pattern API_SLUG = Pattern.compile("\\b[a-z][a-z0-9]*(?:-[a-z0-9]+)+\\b");

    private SearchTermExtractor() {
    }

    public static List<String> extractFromContent(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        Set<String> terms = new LinkedHashSet<>();
        collectMatches(ERROR_CODE, text, terms);
        Set<String> paths = new LinkedHashSet<>();
        collectMatches(API_PATH, text, paths);
        terms.addAll(paths);
        for (String path : paths) {
            String shortName = lastPathSegment(path);
            if (!shortName.isEmpty()) {
                terms.add(shortName);
            }
        }
        return List.copyOf(terms);
    }

    public static List<String> extractFromQuery(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        Set<String> terms = new LinkedHashSet<>(extractFromContent(query));
        collectMatches(API_SLUG, query, terms);
        return List.copyOf(terms);
    }

    public static List<String> matchedTerms(List<String> extractedTerms, String content) {
        if (extractedTerms == null || extractedTerms.isEmpty() || content == null) {
            return List.of();
        }
        List<String> matched = new ArrayList<>();
        for (String term : extractedTerms) {
            if (content.contains(term)) {
                matched.add(term);
            }
        }
        return matched;
    }

    private static void collectMatches(Pattern pattern, String text, Set<String> out) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            out.add(matcher.group());
        }
    }

    private static String lastPathSegment(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash >= path.length() - 1) {
            return "";
        }
        return path.substring(lastSlash + 1);
    }
}
