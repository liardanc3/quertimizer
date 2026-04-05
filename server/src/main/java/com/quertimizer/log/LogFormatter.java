package com.quertimizer.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class LogFormatter {

    private static final int ACTOR_WIDTH = 15;
    private static final int REQUEST_FIELD_MAX_LENGTH = 200;
    private static final int RESPONSE_FIELD_MAX_LENGTH = 100;
    private static final String REQUEST_TRUNCATED_SUFFIX = "...";
    private static final String RESPONSE_TRUNCATED_SUFFIX = "...truncated";
    private static final String ARRAY_OMITTED_MESSAGE = "... %d more truncated";
    private static final List<String> SENSITIVE_KEYS = List.of(
            "password",
            "token",
            "authorization",
            "cookie",
            "secret"
    );

    private final ObjectMapper objectMapper;

    public String formatHttpLine(String actor, String label, String message) {
        return formatLine(actor, label, message);
    }

    public String formatWebSocketLine(String actor, String label, String message) {
        return formatLine(actor, label, message);
    }

    public List<String> formatRequestBodyLines(String prefix, String body) {
        return formatPayload(prefix, body, REQUEST_FIELD_MAX_LENGTH, REQUEST_TRUNCATED_SUFFIX);
    }

    public List<String> formatResponseBodyLines(String prefix, String body) {
        return formatPayload(prefix, body, RESPONSE_FIELD_MAX_LENGTH, RESPONSE_TRUNCATED_SUFFIX);
    }

    public List<String> formatQueryStringLines(String prefix, String queryString) {
        if (queryString == null || queryString.isBlank()) {
            return List.of();
        }

        return Arrays.stream(queryString.split("&"))
                .filter(queryParameter -> !queryParameter.isBlank())
                .map(queryParameter -> prefix + sanitizeQueryParameter(queryParameter))
                .toList();
    }

    public String prefix(String actor) {
        return "[" + String.format("%" + ACTOR_WIDTH + "s", normalizeActor(actor)) + "] ";
    }

    private List<String> formatPayload(String prefix, String body, int maxFieldLength, String truncatedSuffix) {
        if (body == null || body.isBlank()) {
            return List.of();
        }

        // JSON 민감정보 마스킹, 길이 제한 적용
        String formattedBody = tryFormatJson(body, maxFieldLength, truncatedSuffix);
        if (formattedBody == null) {

            // 일반 텍스트 길이 제한 적용
            formattedBody = truncateText(null, body, maxFieldLength, truncatedSuffix);
        }

        return prefixEachLine(prefix, formattedBody);
    }

    private String formatLine(String actor, String label, String message) {
        if (message == null || message.isBlank()) {
            return prefix(actor) + label;
        }

        return prefix(actor) + label + " : " + message;
    }

    private String tryFormatJson(String body, int maxFieldLength, String truncatedSuffix) {
        try {
            JsonNode jsonNode = objectMapper.readTree(body);
            JsonNode sanitizedJsonNode = sanitizeJsonNode(jsonNode, null, maxFieldLength, truncatedSuffix);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sanitizedJsonNode);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private JsonNode sanitizeJsonNode(JsonNode jsonNode, String fieldName, int maxFieldLength, String truncatedSuffix) {
        if (jsonNode.isObject()) {

            // 객체 필드별 마스킹, 길이 제한 적용
            ObjectNode objectNode = objectMapper.createObjectNode();
            jsonNode.fields().forEachRemaining(entry ->
                    objectNode.set(
                            entry.getKey(),
                            sanitizeJsonNode(entry.getValue(), entry.getKey(), maxFieldLength, truncatedSuffix)
                    )
            );
            return objectNode;
        }

        if (jsonNode.isArray()) {

            // 리스트는 첫 1개만 출력, 나머지 생략
            ArrayNode arrayNode = objectMapper.createArrayNode();
            if (jsonNode.isEmpty()) {
                return arrayNode;
            }

            arrayNode.add(sanitizeJsonNode(jsonNode.get(0), fieldName, maxFieldLength, truncatedSuffix));
            if (jsonNode.size() > 1) {
                arrayNode.add(TextNode.valueOf(ARRAY_OMITTED_MESSAGE.formatted(jsonNode.size() - 1)));
            }

            return arrayNode;
        }

        if (jsonNode.isTextual()) {
            return TextNode.valueOf(truncateText(fieldName, jsonNode.textValue(), maxFieldLength, truncatedSuffix));
        }

        return jsonNode;
    }

    private String truncateText(String fieldName, String value, int maxFieldLength, String truncatedSuffix) {
        if (value == null) {
            return null;
        }

        // 민감 필드 값 마스킹
        if (fieldName != null && isSensitiveKey(fieldName)) {
            return "***";
        }

        if (value.length() <= maxFieldLength) {
            return value;
        }

        return value.substring(0, maxFieldLength) + truncatedSuffix;
    }

    private boolean isSensitiveKey(String fieldName) {
        String normalizedFieldName = fieldName.toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYS.stream().anyMatch(normalizedFieldName::contains);
    }

    private String sanitizeQueryParameter(String queryParameter) {
        int separatorIndex = queryParameter.indexOf('=');
        if (separatorIndex < 0) {
            return truncateText(null, queryParameter, REQUEST_FIELD_MAX_LENGTH, REQUEST_TRUNCATED_SUFFIX);
        }

        String parameterName = queryParameter.substring(0, separatorIndex);
        String parameterValue = queryParameter.substring(separatorIndex + 1);
        return parameterName + "=" + truncateText(parameterName, parameterValue, REQUEST_FIELD_MAX_LENGTH, REQUEST_TRUNCATED_SUFFIX);
    }

    private String normalizeActor(String actor) {
        if (actor == null || actor.isBlank()) {
            return "unknown";
        }

        String normalizedActor = actor.split("%", 2)[0];

        // localhost loopback 주소 통일
        if ("::1".equals(normalizedActor) || "0:0:0:0:0:0:0:1".equals(normalizedActor)) {
            normalizedActor = "127.0.0.1";
        }

        if (normalizedActor.length() <= ACTOR_WIDTH) {
            return normalizedActor;
        }

        return normalizedActor.substring(0, ACTOR_WIDTH);
    }

    private List<String> prefixEachLine(String prefix, String value) {
        return Arrays.stream(value.split("\\R", -1))
                .map(line -> prefix + line)
                .toList();
    }

}
