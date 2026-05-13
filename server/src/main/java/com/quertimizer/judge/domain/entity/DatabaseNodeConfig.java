package com.quertimizer.judge.domain.entity;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DatabaseNodeConfig {

    private final String databaseId;
    private final String databaseName;
    private final DbmsType dbmsType;
    private final String urlPropertyKey;
    private final String usernamePropertyKey;
    private final String passwordPropertyKey;
    private final String containerName;
    private final String host;
    private final int portStart;
    private final int portEnd;
    private final String processDatabaseName;
    private final String rootPasswordPropertyKey;
    private boolean enabled;
    private int maxConcurrency;
    private LocalDateTime updatedAt;

    public static DatabaseNodeConfig create(String databaseId, String databaseName, DbmsType dbmsType,
                                      String urlPropertyKey, String usernamePropertyKey, String passwordPropertyKey,
                                      String containerName, String host, int portStart, int portEnd,
                                      String processDatabaseName, String rootPasswordPropertyKey,
                                      boolean enabled, int maxConcurrency) {
        // DB 실행 환경 설정 생성
        return new DatabaseNodeConfig(
                databaseId, databaseName, dbmsType, urlPropertyKey, usernamePropertyKey, passwordPropertyKey,
                containerName, host, portStart, portEnd,
                processDatabaseName, rootPasswordPropertyKey, enabled, maxConcurrency, LocalDateTime.now()
        );
    }

    public static DatabaseNodeConfig restore(String databaseId, String databaseName, DbmsType dbmsType,
                                       String urlPropertyKey, String usernamePropertyKey, String passwordPropertyKey,
                                       String containerName, String host, int portStart, int portEnd,
                                       String processDatabaseName, String rootPasswordPropertyKey,
                                       boolean enabled, int maxConcurrency, LocalDateTime updatedAt) {
        // 저장된 DB 실행 환경 설정 복원
        return new DatabaseNodeConfig(
                databaseId, databaseName, dbmsType, urlPropertyKey, usernamePropertyKey, passwordPropertyKey,
                containerName, host, portStart, portEnd,
                processDatabaseName, rootPasswordPropertyKey, enabled, maxConcurrency, updatedAt
        );
    }

    public DatabaseNodeConfig update(boolean enabled, int maxConcurrency) {
        // 실행 여부와 동시 실행 수 변경
        this.enabled = enabled;
        this.maxConcurrency = maxConcurrency;
        this.updatedAt = LocalDateTime.now();
        return this;
    }

    private DatabaseNodeConfig(String databaseId, String databaseName, DbmsType dbmsType,
                         String urlPropertyKey, String usernamePropertyKey, String passwordPropertyKey,
                         String containerName, String host, int portStart, int portEnd,
                         String processDatabaseName, String rootPasswordPropertyKey,
                         boolean enabled, int maxConcurrency, LocalDateTime updatedAt) {
        this.databaseId = databaseId;
        this.databaseName = databaseName;
        this.dbmsType = dbmsType;
        this.urlPropertyKey = urlPropertyKey;
        this.usernamePropertyKey = usernamePropertyKey;
        this.passwordPropertyKey = passwordPropertyKey;
        this.containerName = containerName;
        this.host = host;
        this.portStart = portStart;
        this.portEnd = portEnd;
        this.processDatabaseName = processDatabaseName;
        this.rootPasswordPropertyKey = rootPasswordPropertyKey;
        this.enabled = enabled;
        this.maxConcurrency = maxConcurrency;
        this.updatedAt = updatedAt;
    }
}
