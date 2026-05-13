package com.quertimizer.judge.adapter.out.persistence;

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
@Table(name = "database_node_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DatabaseNodeConfigJpaEntity {

    @Id
    @Column(name = "database_id", nullable = false, length = 80)
    private String databaseId;

    @Column(name = "database_name", nullable = false, length = 120)
    private String databaseName;

    @Enumerated(EnumType.STRING)
    @Column(name = "dbms_type", nullable = false, length = 30)
    private DbmsType dbmsType;

    @Column(name = "url_property_key", nullable = false, length = 120)
    private String urlPropertyKey;

    @Column(name = "username_property_key", nullable = false, length = 120)
    private String usernamePropertyKey;

    @Column(name = "password_property_key", nullable = false, length = 120)
    private String passwordPropertyKey;

    @Column(name = "container_name", nullable = false, length = 120)
    private String containerName;

    @Column(name = "host", nullable = false, length = 120)
    private String host;

    @Column(name = "port_start", nullable = false)
    private int portStart;

    @Column(name = "port_end", nullable = false)
    private int portEnd;

    @Column(name = "process_database_name", nullable = false, length = 120)
    private String processDatabaseName;

    @Column(name = "root_password_property_key", nullable = false, length = 120)
    private String rootPasswordPropertyKey;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "max_concurrency", nullable = false)
    private int maxConcurrency;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static DatabaseNodeConfigJpaEntity create(String databaseId, String databaseName, DbmsType dbmsType,
                                               String urlPropertyKey, String usernamePropertyKey, String passwordPropertyKey,
                                               String containerName, String host, int portStart,
                                               int portEnd, String processDatabaseName, String rootPasswordPropertyKey,
                                               boolean enabled, int maxConcurrency, LocalDateTime updatedAt) {
        // DB 노드 설정 JPA 엔티티 생성
        return new DatabaseNodeConfigJpaEntity(
                databaseId, databaseName, dbmsType, urlPropertyKey, usernamePropertyKey, passwordPropertyKey,
                containerName, host, portStart, portEnd,
                processDatabaseName, rootPasswordPropertyKey, enabled, maxConcurrency, updatedAt
        );
    }

    public void update(String databaseName, DbmsType dbmsType,
                       String urlPropertyKey, String usernamePropertyKey, String passwordPropertyKey,
                       String containerName, String host, int portStart, int portEnd,
                       String processDatabaseName, String rootPasswordPropertyKey,
                       boolean enabled, int maxConcurrency, LocalDateTime updatedAt) {
        // DB 노드 설정 JPA 엔티티 내용 갱신
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

    private DatabaseNodeConfigJpaEntity(String databaseId, String databaseName, DbmsType dbmsType,
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
