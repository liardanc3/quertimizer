package com.quertimizer.judge.domain.entity;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class LvmSnapshotConfig {

    public static final String DEFAULT_ID = "default";

    private final String configId;
    private final String mountRoot;
    private final String volumeGroup;
    private final String thinPool;
    private final String baseTemplateVersion;
    private final int commandTimeoutSeconds;
    private final int startupTimeoutSeconds;
    private final LocalDateTime updatedAt;

    public static LvmSnapshotConfig restore(String configId, String mountRoot, String volumeGroup, String thinPool,
                                              String baseTemplateVersion, int commandTimeoutSeconds,
                                              int startupTimeoutSeconds, LocalDateTime updatedAt) {
        // 저장된 DB 실행 환경 설정 복원
        return new LvmSnapshotConfig(
                configId, mountRoot, volumeGroup, thinPool, baseTemplateVersion,
                commandTimeoutSeconds, startupTimeoutSeconds, updatedAt
        );
    }

    private LvmSnapshotConfig(String configId, String mountRoot, String volumeGroup, String thinPool,
                                String baseTemplateVersion, int commandTimeoutSeconds,
                                int startupTimeoutSeconds, LocalDateTime updatedAt) {
        this.configId = configId;
        this.mountRoot = mountRoot;
        this.volumeGroup = volumeGroup;
        this.thinPool = thinPool;
        this.baseTemplateVersion = baseTemplateVersion;
        this.commandTimeoutSeconds = commandTimeoutSeconds;
        this.startupTimeoutSeconds = startupTimeoutSeconds;
        this.updatedAt = updatedAt;
    }
}
