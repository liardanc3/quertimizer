package com.quertimizer.community.domain.model;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class CommunityContentConstant {

    public static final int MAX_CONTENT_BYTES = 500_000;
    public static final int MAX_DEPTH = 30;
    public static final int MAX_NODE_COUNT = 2_000;
    public static final int MAX_TEXT_LENGTH = 100_000;
    public static final Set<String> ALLOWED_NODES = Set.of(
            "doc", "paragraph", "text", "heading", "bulletList", "orderedList", "listItem",
            "codeBlock", "blockquote", "hardBreak", "horizontalRule", "image",
            "details", "detailsSummary", "detailsContent", "taskList", "taskItem"
    );
    public static final Set<String> ALLOWED_MARKS = Set.of(
            "bold", "italic", "strike", "underline", "highlight", "code", "link"
    );
    public static final Map<String, Set<String>> ALLOWED_NODE_ATTRS = Map.ofEntries(
            Map.entry("heading", Set.of("level")),
            Map.entry("orderedList", Set.of("start")),
            Map.entry("codeBlock", Set.of("language", "class")),
            Map.entry("image", Set.of("src", "alt", "title", "width", "height", "imageId", "class")),
            Map.entry("details", Set.of("open")),
            Map.entry("taskList", Set.of("class")),
            Map.entry("taskItem", Set.of("checked", "class"))
    );
    public static final Map<String, Set<String>> ALLOWED_NODE_CLASSES = Map.of(
            "codeBlock", Set.of("community-code-block"),
            "image", Set.of("community-content-image"),
            "taskList", Set.of("community-task-list"),
            "taskItem", Set.of("community-task-item")
    );
    public static final Map<String, Set<String>> ALLOWED_MARK_ATTRS = Map.of(
            "link", Set.of("href", "target", "rel"),
            "highlight", Set.of("color")
    );
    public static final Pattern COMMUNITY_IMAGE_PATH_PATTERN =
            Pattern.compile("^/community/images/[a-fA-F0-9]{32}\\.(jpg|jpeg|png|gif|webp)$");
    public static final Pattern COMMUNITY_IMAGE_ABSOLUTE_URL_PATTERN =
            Pattern.compile("^https?://[^\\s/]+/community/images/[a-fA-F0-9]{32}\\.(jpg|jpeg|png|gif|webp)$", Pattern.CASE_INSENSITIVE);
    public static final Pattern COMMUNITY_IMAGE_ID_PATTERN =
            Pattern.compile("^[a-fA-F0-9]{32}\\.(jpg|jpeg|png|gif|webp)$");

    private CommunityContentConstant() {
    }
}
