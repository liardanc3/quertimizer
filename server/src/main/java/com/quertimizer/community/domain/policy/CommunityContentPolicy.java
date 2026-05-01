package com.quertimizer.community.domain.policy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CommunityContentPolicy {

    private static final int MAX_CONTENT_BYTES = 500_000;
    private static final int MAX_DEPTH = 30;
    private static final int MAX_NODE_COUNT = 2_000;
    private static final int MAX_TEXT_LENGTH = 100_000;
    private static final Set<String> ALLOWED_NODES = Set.of(
            "doc", "paragraph", "text", "heading", "bulletList", "orderedList", "listItem",
            "codeBlock", "blockquote", "hardBreak", "horizontalRule", "image",
            "details", "detailsSummary", "detailsContent", "taskList", "taskItem"
    );
    private static final Set<String> ALLOWED_MARKS = Set.of(
            "bold", "italic", "strike", "underline", "highlight", "code", "link"
    );
    private static final Map<String, Set<String>> ALLOWED_NODE_ATTRS = Map.ofEntries(
            Map.entry("heading", Set.of("level")),
            Map.entry("orderedList", Set.of("start")),
            Map.entry("codeBlock", Set.of("language", "class")),
            Map.entry("image", Set.of("src", "alt", "title", "imageId", "class")),
            Map.entry("details", Set.of("open")),
            Map.entry("taskList", Set.of("class")),
            Map.entry("taskItem", Set.of("checked", "class"))
    );
    private static final Map<String, Set<String>> ALLOWED_NODE_CLASSES = Map.of(
            "codeBlock", Set.of("community-code-block"),
            "image", Set.of("community-content-image"),
            "taskList", Set.of("community-task-list"),
            "taskItem", Set.of("community-task-item")
    );
    private static final Map<String, Set<String>> ALLOWED_MARK_ATTRS = Map.of(
            "link", Set.of("href", "target", "rel"),
            "highlight", Set.of("color")
    );

    private final ObjectMapper objectMapper;

    /**
     * 커뮤니티 본문 JSON을 허용된 문서 구조로 검증한다.
     *
     * <ol>
     *   <li>본문 존재 여부와 크기 검증
     *   <li>JSON 파싱
     *   <li>노드 구조와 속성 검증
     * </ol>
     *
     * @param contentJson 검증할 커뮤니티 본문 JSON
     */
    public void validate(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            throw badContent();
        }

        if (contentJson.getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_BYTES) {
            throw new BusinessException("본문은 최대 500000 Byte까지 입력할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }

        JsonNode root = parse(contentJson);
        ContentStats stats = new ContentStats();
        validateNode(root, 1, stats);
    }

    private JsonNode parse(String contentJson) {
        try {
            return objectMapper.readTree(contentJson);
        } catch (JsonProcessingException exception) {
            throw badContent();
        }
    }

    private void validateNode(JsonNode node, int depth, ContentStats stats) {
        if (!node.isObject() || depth > MAX_DEPTH) {
            throw badContent();
        }

        stats.nodeCount++;
        if (stats.nodeCount > MAX_NODE_COUNT) {
            throw badContent();
        }

        String type = textValue(node.get("type"));
        if (!ALLOWED_NODES.contains(type)) {
            throw badContent();
        }

        validateObjectFields(node, Set.of("type", "attrs", "content", "marks", "text"));
        validateNodeAttrs(type, node.get("attrs"));
        validateMarks(node.get("marks"));
        validateTextNode(type, node.get("text"), stats);
        validateChildren(node.get("content"), depth, stats);
    }

    private void validateObjectFields(JsonNode node, Set<String> allowedFields) {
        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!allowedFields.contains(fieldName) || isDangerousAttributeName("", fieldName)) {
                throw badContent();
            }
        }
    }

    private void validateNodeAttrs(String nodeType, JsonNode attrs) {
        if (attrs == null || attrs.isNull()) {
            return;
        }

        if (!attrs.isObject()) {
            throw badContent();
        }

        Set<String> allowedAttrs = ALLOWED_NODE_ATTRS.getOrDefault(nodeType, Set.of());
        Iterator<Map.Entry<String, JsonNode>> fields = attrs.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String attrName = field.getKey();
            if (!allowedAttrs.contains(attrName) || isDangerousAttributeName(nodeType, attrName)) {
                throw badContent();
            }

            validateAttrValue(nodeType, attrName, field.getValue());
        }
    }

    private void validateAttrValue(String nodeType, String attrName, JsonNode value) {
        if ("image".equals(nodeType) && "src".equals(attrName)) {
            validateImageSrc(textValue(value));
            return;
        }

        if ("image".equals(nodeType) && "imageId".equals(attrName)) {
            validateImageId(textValue(value));
            return;
        }

        if ("class".equals(attrName)) {
            validateAllowedClass(nodeType, textValue(value));
            return;
        }

        if (value.isTextual()) {
            validateSafeText(value.textValue());
        }
    }

    private void validateMarks(JsonNode marks) {
        if (marks == null || marks.isNull()) {
            return;
        }

        if (!marks.isArray()) {
            throw badContent();
        }

        for (JsonNode mark : marks) {
            if (!mark.isObject()) {
                throw badContent();
            }

            String type = textValue(mark.get("type"));
            if (!ALLOWED_MARKS.contains(type)) {
                throw badContent();
            }

            validateObjectFields(mark, Set.of("type", "attrs"));
            validateMarkAttrs(type, mark.get("attrs"));
        }
    }

    private void validateMarkAttrs(String markType, JsonNode attrs) {
        if (attrs == null || attrs.isNull()) {
            return;
        }

        if (!attrs.isObject()) {
            throw badContent();
        }

        Set<String> allowedAttrs = ALLOWED_MARK_ATTRS.getOrDefault(markType, Set.of());
        Iterator<Map.Entry<String, JsonNode>> fields = attrs.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (!allowedAttrs.contains(field.getKey()) || isDangerousAttributeName("", field.getKey())) {
                throw badContent();
            }

            if ("link".equals(markType) && "href".equals(field.getKey())) {
                validateHttpUrl(textValue(field.getValue()));
                continue;
            }

            if (field.getValue().isTextual()) {
                validateSafeText(field.getValue().textValue());
            }
        }
    }

    private void validateTextNode(String type, JsonNode text, ContentStats stats) {
        if (text == null || text.isNull()) {
            return;
        }

        if (!"text".equals(type) || !text.isTextual()) {
            throw badContent();
        }

        validateSafeText(text.textValue());
        stats.textLength += text.textValue().length();
        if (stats.textLength > MAX_TEXT_LENGTH) {
            throw badContent();
        }
    }

    private void validateChildren(JsonNode content, int depth, ContentStats stats) {
        if (content == null || content.isNull()) {
            return;
        }

        if (!content.isArray()) {
            throw badContent();
        }

        for (JsonNode child : content) {
            validateNode(child, depth + 1, stats);
        }
    }

    private void validateImageSrc(String src) {
        validateSafeText(src);
        String normalizedSrc = src != null ? src.trim() : "";
        if (!normalizedSrc.matches("^/community/images/[a-fA-F0-9]{32}\\.(jpg|jpeg|png|gif|webp)$")) {
            throw badContent();
        }
    }

    private void validateImageId(String imageId) {
        if (imageId == null || !imageId.matches("[a-fA-F0-9]{32}\\.(jpg|jpeg|png|gif|webp)")) {
            throw badContent();
        }
    }

    private void validateHttpUrl(String url) {
        validateSafeText(url);
        String normalizedUrl = url != null ? url.trim().toLowerCase(Locale.ROOT) : "";
        if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://")) {
            throw badContent();
        }
    }

    private void validateSafeText(String value) {
        String normalizedValue = value != null ? value.trim().toLowerCase(Locale.ROOT) : "";
        if (normalizedValue.startsWith("javascript:") || normalizedValue.startsWith("data:")) {
            throw badContent();
        }
    }

    private void validateAllowedClass(String nodeType, String className) {
        if (!ALLOWED_NODE_CLASSES.getOrDefault(nodeType, Set.of()).contains(className)) {
            throw badContent();
        }
    }

    private boolean isDangerousAttributeName(String nodeType, String attrName) {
        String normalizedAttrName = attrName.toLowerCase(Locale.ROOT);
        if ("class".equals(normalizedAttrName) && ALLOWED_NODE_CLASSES.containsKey(nodeType)) {
            return false;
        }

        return normalizedAttrName.startsWith("on")
                || "style".equals(normalizedAttrName)
                || "class".equals(normalizedAttrName);
    }

    private String textValue(JsonNode value) {
        return value != null && value.isTextual() ? value.textValue() : "";
    }

    private BusinessException badContent() {
        return new BusinessException("본문 형식이 올바르지 않습니다.", HttpStatus.BAD_REQUEST);
    }

    private static final class ContentStats {
        private int nodeCount;
        private int textLength;
    }
}
