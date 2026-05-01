package com.quertimizer.favorite.application.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.quertimizer.favorite.domain.model.FavoriteFailReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FavoriteTabSnapshotSupport {

    private final ObjectMapper objectMapper;

    public String serialize(JsonNode snapshot) {
        // 스냅샷 null 여부 검사
        if (snapshot == null || snapshot.isNull()) {
            return null;
        }

        // 스냅샷 JSON 문자열 변환
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(FavoriteFailReason.SNAPSHOT_SERIALIZE_FAILED.getMessage(), exception);
        }
    }

    public JsonNode deserialize(String snapshotJson) {
        // 스냅샷 JSON 문자열 존재 여부 검사
        if (snapshotJson == null || snapshotJson.isBlank()) {
            return NullNode.getInstance();
        }

        // 스냅샷 JSON 문자열 파싱 또는 빈 노드 대체
        try {
            return objectMapper.readTree(snapshotJson);
        } catch (JsonProcessingException exception) {
            return NullNode.getInstance();
        }
    }
}
