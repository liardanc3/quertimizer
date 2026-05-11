package com.quertimizer.judge.adapter.out.execution;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class LvmSnapshotRuntimeOptions {

    private final String mountRoot;
    private final String volumeGroup;
    private final String thinPool;
    private final String baseTemplateVersion;
    private final int startupTimeoutSeconds;
    private final Map<String, LvmSnapshotRuntimeNode> nodesByDatabaseId;

    public LvmSnapshotRuntimeOptions(String mountRoot, String volumeGroup,
                                     String thinPool, String baseTemplateVersion,
                                     int startupTimeoutSeconds, List<LvmSnapshotRuntimeNode> runtimeNodes) {
        this.mountRoot = normalizePath(requireText(mountRoot, "mountRoot"));
        this.volumeGroup = LvmSnapshotNameSupport.scriptName(requireText(volumeGroup, "volumeGroup"));
        this.thinPool = LvmSnapshotNameSupport.scriptName(requireText(thinPool, "thinPool"));
        this.baseTemplateVersion = LvmSnapshotNameSupport.scriptName(requireText(baseTemplateVersion, "baseTemplateVersion"));
        if (startupTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("값은 0보다 커야 합니다.");
        }

        this.startupTimeoutSeconds = startupTimeoutSeconds;
        this.nodesByDatabaseId = createNodeMap(runtimeNodes);
    }

    public String getBaseTemplateVersion() {
        return baseTemplateVersion;
    }

    public String getMountRoot() {
        return mountRoot;
    }

    public String getVolumeGroup() {
        return volumeGroup;
    }

    public String getThinPool() {
        return thinPool;
    }

    public int getStartupTimeoutSeconds() {
        return startupTimeoutSeconds;
    }

    public Optional<LvmSnapshotRuntimeNode> findNode(String databaseId) {
        if (databaseId == null || databaseId.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(nodesByDatabaseId.get(databaseId));
    }

    public LvmSnapshotRuntimeNode requireNode(String databaseId) {
        return findNode(databaseId)
                .orElseThrow(() -> new IllegalStateException("LVM 스냅샷 실행 노드 설정이 없습니다: " + databaseId));
    }

    private Map<String, LvmSnapshotRuntimeNode> createNodeMap(List<LvmSnapshotRuntimeNode> runtimeNodes) {
        Map<String, LvmSnapshotRuntimeNode> nodeMap = new LinkedHashMap<>();
        for (LvmSnapshotRuntimeNode runtimeNode : Objects.requireNonNull(runtimeNodes, "필수 값이 없습니다.")) {
            Objects.requireNonNull(runtimeNode, "필수 값이 없습니다.");
            if (nodeMap.putIfAbsent(runtimeNode.getDatabaseId(), runtimeNode) != null) {
                throw new IllegalArgumentException("LVM 스냅샷 런타임 DB ID가 중복되었습니다: " + runtimeNode.getDatabaseId());
            }
        }

        return Map.copyOf(nodeMap);
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "이 비어 있습니다.");
        }

        return value.trim();
    }

    private String normalizePath(String value) {
        return Path.of(value).normalize().toString();
    }
}
