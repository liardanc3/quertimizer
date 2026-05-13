package com.quertimizer.judge.application.model;

import lombok.Data;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.quertimizer.judge.domain.model.JudgeFailReason.LVM_SNAPSHOT_NODE_CONFIG_NOT_FOUND;

@Data
public class Options {

    private final String mountRoot;
    private final String volumeGroup;
    private final String thinPool;
    private final String baseTemplateVersion;
    private final int startupTimeoutSeconds;
    private final Map<String, DatabaseNode> nodesByDatabaseId;

    public Options(String mountRoot, String volumeGroup, String thinPool, String baseTemplateVersion,
                   int startupTimeoutSeconds, List<DatabaseNode> databaseNodes) {
        this.mountRoot = normalizePath(mountRoot.trim());
        this.volumeGroup = Names.scriptName(volumeGroup.trim());
        this.thinPool = Names.scriptName(thinPool.trim());
        this.baseTemplateVersion = Names.scriptName(baseTemplateVersion.trim());
        this.startupTimeoutSeconds = startupTimeoutSeconds;
        this.nodesByDatabaseId = createNodeMap(databaseNodes);
    }

    public Optional<DatabaseNode> findNode(String databaseId) {
        // 비어 있는 DB 노드 ID는 조회 대상 없음으로 처리
        if (databaseId == null || databaseId.isBlank()) {
            return Optional.empty();
        }

        // DB 노드 ID 기준 DB 노드 조회
        return Optional.ofNullable(nodesByDatabaseId.get(databaseId));
    }

    public DatabaseNode requireNode(String databaseId) {
        // DB 노드 ID 기준 DB 노드 조회 실패 시 설정 오류 반환
        return findNode(databaseId)
                .orElseThrow(() -> new IllegalStateException(LVM_SNAPSHOT_NODE_CONFIG_NOT_FOUND.format(databaseId)));
    }

    public List<DatabaseNode> getNodes() {
        return List.copyOf(nodesByDatabaseId.values());
    }

    private Map<String, DatabaseNode> createNodeMap(List<DatabaseNode> databaseNodes) {
        // DB 노드 목록을 DB 노드 ID 기준 map으로 변환
        Map<String, DatabaseNode> nodeMap = new LinkedHashMap<>();
        for (DatabaseNode databaseNode : databaseNodes) {
            nodeMap.put(databaseNode.getDatabaseId(), databaseNode);
        }

        // 외부 변경을 막는 불변 map 반환
        return Map.copyOf(nodeMap);
    }

    private String normalizePath(String value) {
        return Path.of(value).normalize().toString();
    }
}
