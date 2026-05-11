package com.quertimizer.monitoring.adapter.out.persistence;

import com.quertimizer.judge.domain.model.DbmsType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "judge_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JudgeConfigJpaEntity {

    @Id
    @Column(name = "database_id", nullable = false, length = 80)
    private String databaseId;

    @Column(name = "database_name", nullable = false, length = 120)
    private String databaseName;

    @Enumerated(EnumType.STRING)
    @Column(name = "dbms_type", nullable = false, length = 30)
    private DbmsType dbmsType;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "max_concurrency", nullable = false)
    private int maxConcurrency;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static JudgeConfigJpaEntity create(String databaseId, String databaseName, DbmsType dbmsType,
                                              boolean enabled, int maxConcurrency, LocalDateTime updatedAt) {
        // judge 설정 JPA 엔티티 생성
        return new JudgeConfigJpaEntity(databaseId, databaseName, dbmsType, enabled, maxConcurrency, updatedAt);
    }

    public void update(String databaseName, DbmsType dbmsType, boolean enabled,
                       int maxConcurrency, LocalDateTime updatedAt) {
        // judge 설정 JPA 엔티티 내용 갱신
        this.databaseName = databaseName;
        this.dbmsType = dbmsType;
        this.enabled = enabled;
        this.maxConcurrency = maxConcurrency;
        this.updatedAt = updatedAt;
    }

    private JudgeConfigJpaEntity(String databaseId, String databaseName, DbmsType dbmsType,
                                 boolean enabled, int maxConcurrency, LocalDateTime updatedAt) {
        this.databaseId = databaseId;
        this.databaseName = databaseName;
        this.dbmsType = dbmsType;
        this.enabled = enabled;
        this.maxConcurrency = maxConcurrency;
        this.updatedAt = updatedAt;
    }
}
