package com.quertimizer.monitoring.domain.entity;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class JudgeConfig {

    private final String databaseId;
    private final String databaseName;
    private final DbmsType dbmsType;
    private boolean enabled;
    private int maxConcurrency;
    private LocalDateTime updatedAt;

    public static JudgeConfig create(String databaseId, String databaseName, DbmsType dbmsType,
                                     boolean enabled, int maxConcurrency) {
        // judge 런타임 설정 생성
        return new JudgeConfig(databaseId, databaseName, dbmsType, enabled, maxConcurrency, LocalDateTime.now());
    }

    public static JudgeConfig restore(String databaseId, String databaseName, DbmsType dbmsType,
                                      boolean enabled, int maxConcurrency, LocalDateTime updatedAt) {
        // 저장된 judge 런타임 설정 복원
        return new JudgeConfig(databaseId, databaseName, dbmsType, enabled, maxConcurrency, updatedAt);
    }

    public JudgeConfig update(boolean enabled, int maxConcurrency) {
        // 실행 여부와 동시 실행 수 변경
        this.enabled = enabled;
        this.maxConcurrency = maxConcurrency;
        this.updatedAt = LocalDateTime.now();
        return this;
    }

    private JudgeConfig(String databaseId, String databaseName, DbmsType dbmsType,
                        boolean enabled, int maxConcurrency, LocalDateTime updatedAt) {
        this.databaseId = databaseId;
        this.databaseName = databaseName;
        this.dbmsType = dbmsType;
        this.enabled = enabled;
        this.maxConcurrency = maxConcurrency;
        this.updatedAt = updatedAt;
    }
}
