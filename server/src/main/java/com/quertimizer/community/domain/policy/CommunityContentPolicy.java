package com.quertimizer.community.domain.policy;

import com.quertimizer.global.exception.DomainRuleViolationException;
import com.quertimizer.global.exception.DomainRuleViolationType;
import org.springframework.stereotype.Component;

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
import static com.quertimizer.community.domain.model.CommunityContentConstant.COMMUNITY_IMAGE_ABSOLUTE_URL_PATTERN;
import static com.quertimizer.community.domain.model.CommunityContentConstant.COMMUNITY_IMAGE_ID_PATTERN;
import static com.quertimizer.community.domain.model.CommunityContentConstant.COMMUNITY_IMAGE_PATH_PATTERN;
import static com.quertimizer.community.domain.model.CommunityContentConstant.MAX_CONTENT_BYTES;
import static com.quertimizer.community.domain.model.CommunityContentConstant.MAX_DEPTH;
import static com.quertimizer.community.domain.model.CommunityContentConstant.MAX_NODE_COUNT;
import static com.quertimizer.community.domain.model.CommunityContentConstant.MAX_TEXT_LENGTH;
import static com.quertimizer.community.domain.model.CommunityFailReason.CONTENT_INVALID;
import static com.quertimizer.community.domain.model.CommunityFailReason.CONTENT_SIZE_EXCEEDED;

@Component
public class CommunityContentPolicy {

    public void validate(String contentJson, Object root) {
        if (contentJson == null || contentJson.isBlank()) {
            throw badContent();
        }

        if (contentJson.getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_BYTES) {
            throw new DomainRuleViolationException(CONTENT_SIZE_EXCEEDED.getMessage(), DomainRuleViolationType.INVALID_REQUEST);
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

        if ("image".equals(nodeType) && ("width".equals(attrName) || "height".equals(attrName))) {
            validateImageDimension(value);
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
        if (!COMMUNITY_IMAGE_PATH_PATTERN.matcher(normalizedSrc).matches()
                && !COMMUNITY_IMAGE_ABSOLUTE_URL_PATTERN.matcher(normalizedSrc).matches()) {
            throw badContent();
        }
    }

    private void validateImageId(String imageId) {
        // 이미지 번호 없는 optional attr 검증 생략
        String normalizedImageId = imageId != null ? imageId.trim() : "";
        if (normalizedImageId.isEmpty()) {
            return;
        }

        // 이미지 번호 형식 검증
        if (!COMMUNITY_IMAGE_ID_PATTERN.matcher(normalizedImageId).matches()) {
            throw badContent();
        }
    }

    private void validateImageDimension(Object value) {
        // 이미지 크기 없는 optional attr 검증 생략
        if (value == null) {
            return;
        }

        // 이미지 크기 숫자 값 검증
        if (value instanceof Number number) {
            double dimension = number.doubleValue();
            if (dimension > 0 && dimension <= 10000) {
                return;
            }
            throw badContent();
        }

        // 이미지 크기 문자열 값 검증
        if (!(value instanceof String text)) {
            throw badContent();
        }

        String normalizedValue = text.trim();
        if (normalizedValue.isEmpty()) {
            return;
        }

        try {
            double dimension = Double.parseDouble(normalizedValue);
            if (dimension > 0 && dimension <= 10000) {
                return;
            }
        } catch (NumberFormatException exception) {
            throw badContent();
        }

        throw badContent();
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
        // class 없는 optional attr 검증 생략
        String normalizedClassName = className != null ? className.trim() : "";
        if (normalizedClassName.isEmpty()) {
            return;
        }

        // 노드별 허용 class 검증
        if (!ALLOWED_NODE_CLASSES.getOrDefault(nodeType, Set.of()).contains(normalizedClassName)) {
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
        return new DomainRuleViolationException(CONTENT_INVALID.getMessage(), DomainRuleViolationType.INVALID_REQUEST);
    }

    private static final class ContentStats {
        private int nodeCount;
        private int textLength;
    }
}
