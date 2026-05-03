package com.quertimizer.community.domain.policy;

import com.quertimizer.global.exception.DomainRuleViolationException;
import com.quertimizer.global.exception.DomainRuleViolationType;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.quertimizer.community.domain.model.CommunityContentConstant.ALLOWED_MARK_ATTRS;
import static com.quertimizer.community.domain.model.CommunityContentConstant.ALLOWED_MARKS;
import static com.quertimizer.community.domain.model.CommunityContentConstant.ALLOWED_NODES;
import static com.quertimizer.community.domain.model.CommunityContentConstant.ALLOWED_NODE_ATTRS;
import static com.quertimizer.community.domain.model.CommunityContentConstant.ALLOWED_NODE_CLASSES;
import static com.quertimizer.community.domain.model.CommunityContentConstant.MAX_CONTENT_BYTES;
import static com.quertimizer.community.domain.model.CommunityContentConstant.MAX_DEPTH;
import static com.quertimizer.community.domain.model.CommunityContentConstant.MAX_NODE_COUNT;
import static com.quertimizer.community.domain.model.CommunityContentConstant.MAX_TEXT_LENGTH;

public class CommunityContentPolicy {

    public void validate(String contentJson, Object root) {
        if (contentJson == null || contentJson.isBlank()) {
            throw badContent();
        }

        if (contentJson.getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_BYTES) {
            throw new DomainRuleViolationException("본문은 최대 500000 Byte까지 입력할 수 있습니다.", DomainRuleViolationType.INVALID_REQUEST);
        }

        ContentStats stats = new ContentStats();
        validateNode(root, 1, stats);
    }

    private void validateNode(Object node, int depth, ContentStats stats) {
        if (!(node instanceof Map<?, ?> nodeMap) || depth > MAX_DEPTH) {
            throw badContent();
        }

        stats.nodeCount++;
        if (stats.nodeCount > MAX_NODE_COUNT) {
            throw badContent();
        }

        String type = textValue(nodeMap.get("type"));
        if (!ALLOWED_NODES.contains(type)) {
            throw badContent();
        }

        validateObjectFields(nodeMap, Set.of("type", "attrs", "content", "marks", "text"));
        validateNodeAttrs(type, nodeMap.get("attrs"));
        validateMarks(nodeMap.get("marks"));
        validateTextNode(type, nodeMap.get("text"), stats);
        validateChildren(nodeMap.get("content"), depth, stats);
    }

    private void validateObjectFields(Map<?, ?> node, Set<String> allowedFields) {
        for (Object key : node.keySet()) {
            String fieldName = key instanceof String value ? value : "";
            if (!allowedFields.contains(fieldName) || isDangerousAttributeName("", fieldName)) {
                throw badContent();
            }
        }
    }

    private void validateNodeAttrs(String nodeType, Object attrs) {
        if (attrs == null) {
            return;
        }

        if (!(attrs instanceof Map<?, ?> attrMap)) {
            throw badContent();
        }

        Set<String> allowedAttrs = ALLOWED_NODE_ATTRS.getOrDefault(nodeType, Set.of());
        for (Map.Entry<?, ?> field : attrMap.entrySet()) {
            String attrName = field.getKey() instanceof String value ? value : "";
            if (!allowedAttrs.contains(attrName) || isDangerousAttributeName(nodeType, attrName)) {
                throw badContent();
            }

            validateAttrValue(nodeType, attrName, field.getValue());
        }
    }

    private void validateAttrValue(String nodeType, String attrName, Object value) {
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

        if (value instanceof String text) {
            validateSafeText(text);
        }
    }

    private void validateMarks(Object marks) {
        if (marks == null) {
            return;
        }

        if (!(marks instanceof List<?> markList)) {
            throw badContent();
        }

        for (Object mark : markList) {
            if (!(mark instanceof Map<?, ?> markMap)) {
                throw badContent();
            }

            String type = textValue(markMap.get("type"));
            if (!ALLOWED_MARKS.contains(type)) {
                throw badContent();
            }

            validateObjectFields(markMap, Set.of("type", "attrs"));
            validateMarkAttrs(type, markMap.get("attrs"));
        }
    }

    private void validateMarkAttrs(String markType, Object attrs) {
        if (attrs == null) {
            return;
        }

        if (!(attrs instanceof Map<?, ?> attrMap)) {
            throw badContent();
        }

        Set<String> allowedAttrs = ALLOWED_MARK_ATTRS.getOrDefault(markType, Set.of());
        for (Map.Entry<?, ?> field : attrMap.entrySet()) {
            String attrName = field.getKey() instanceof String value ? value : "";
            if (!allowedAttrs.contains(attrName) || isDangerousAttributeName("", attrName)) {
                throw badContent();
            }

            if ("link".equals(markType) && "href".equals(attrName)) {
                validateHttpUrl(textValue(field.getValue()));
                continue;
            }

            if (field.getValue() instanceof String text) {
                validateSafeText(text);
            }
        }
    }

    private void validateTextNode(String type, Object text, ContentStats stats) {
        if (text == null) {
            return;
        }

        if (!"text".equals(type) || !(text instanceof String textValue)) {
            throw badContent();
        }

        validateSafeText(textValue);
        stats.textLength += textValue.length();
        if (stats.textLength > MAX_TEXT_LENGTH) {
            throw badContent();
        }
    }

    private void validateChildren(Object content, int depth, ContentStats stats) {
        if (content == null) {
            return;
        }

        if (!(content instanceof List<?> children)) {
            throw badContent();
        }

        for (Object child : children) {
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

    private String textValue(Object value) {
        return value instanceof String text ? text : "";
    }

    private DomainRuleViolationException badContent() {
        return new DomainRuleViolationException("본문 형식이 올바르지 않습니다.", DomainRuleViolationType.INVALID_REQUEST);
    }

    private static final class ContentStats {
        private int nodeCount;
        private int textLength;
    }
}
