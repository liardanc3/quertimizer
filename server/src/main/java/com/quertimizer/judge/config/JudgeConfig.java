package com.quertimizer.judge.config;

import com.quertimizer.judge.application.model.Database;
import com.quertimizer.judge.application.model.DatabaseCluster;
import com.quertimizer.judge.application.model.DatabaseSelector;
import com.quertimizer.judge.application.model.Options;
import com.quertimizer.judge.application.model.RoundRobinDatabaseSelector;
import com.quertimizer.judge.application.model.DatabaseNode;
import com.quertimizer.judge.application.port.out.DatabaseNodeConfigRepositoryPort;
import com.quertimizer.judge.application.port.out.LvmSnapshotConfigRepositoryPort;
import com.quertimizer.judge.domain.entity.LvmSnapshotConfig;
import com.quertimizer.judge.domain.model.DbmsType;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static com.quertimizer.judge.domain.model.JudgeFailReason.LVM_SNAPSHOT_DEFAULT_CONFIG_NOT_FOUND;

@Configuration
public class JudgeConfig {

    @Bean
    public DatabaseSelector databaseSelector() {
        // DB 노드 선택 시작점을 라운드 로빈으로 분산
        return new RoundRobinDatabaseSelector();
    }

    @Bean
    public DatabaseCluster databaseCluster(DatabaseNodeConfigRepositoryPort databaseNodeConfigRepository,
                                           DatabaseSelector selector, Environment environment) {
        // DB 노드 설정을 DB 클러스터로 변환
        return new DatabaseCluster(createDatabases(databaseNodeConfigRepository, environment), selector);
    }

    @Bean
    public Options lvmSnapshotOptions(LvmSnapshotConfigRepositoryPort lvmSnapshotConfigRepository,
                                      DatabaseNodeConfigRepositoryPort databaseNodeConfigRepository,
                                      Environment environment) {
        // LVM 설정과 DB 노드 설정을 스냅샷 옵션으로 변환
        LvmSnapshotConfig lvmSnapshotConfig = lvmSnapshotConfigRepository.findDefault()
                .orElseThrow(() -> new IllegalStateException(LVM_SNAPSHOT_DEFAULT_CONFIG_NOT_FOUND.getMessage()));
        return new Options(
                lvmSnapshotConfig.getMountRoot(), lvmSnapshotConfig.getVolumeGroup(),
                lvmSnapshotConfig.getThinPool(), lvmSnapshotConfig.getBaseTemplateVersion(),
                lvmSnapshotConfig.getStartupTimeoutSeconds(),
                databaseNodeConfigRepository.findAll().stream()
                        .map(databaseNodeConfig -> createDatabaseNode(databaseNodeConfig, environment))
                        .toList()
        );
    }

    private DatabaseNode createDatabaseNode(com.quertimizer.judge.domain.entity.DatabaseNodeConfig databaseNodeConfig,
                                           Environment environment) {
        // LVM snapshot은 DB 컨테이너 자체가 아니라 컨테이너 안의 per-eval DB process에 접속
        return new DatabaseNode(
                databaseNodeConfig.getDatabaseId(),
                normalize(databaseNodeConfig.getContainerName(), databaseNodeConfig.getDatabaseId()),
                normalize(databaseNodeConfig.getHost(), "127.0.0.1"),
                databaseNodeConfig.getPortStart(), databaseNodeConfig.getPortEnd(),
                normalize(databaseNodeConfig.getProcessDatabaseName(), defaultProcessDatabaseName(databaseNodeConfig.getDbmsType())),
                resolveProperty(environment, databaseNodeConfig.getRootPasswordPropertyKey())
        );
    }

    private List<Database> createDatabases(DatabaseNodeConfigRepositoryPort databaseNodeConfigRepository, Environment environment) {
        // DB 노드 설정을 접속 대상 목록으로 변환
        return databaseNodeConfigRepository.findAll().stream()
                .map(databaseNodeConfig -> createDatabase(databaseNodeConfig, environment))
                .toList();
    }

    private Database createDatabase(com.quertimizer.judge.domain.entity.DatabaseNodeConfig databaseNodeConfig,
                                    Environment environment) {
        // secret 값은 DB에 저장한 property key 기준으로 환경에서 해석
        return new Database(
                databaseNodeConfig.getDatabaseId(), normalize(databaseNodeConfig.getDatabaseName(), databaseNodeConfig.getDatabaseId()),
                databaseNodeConfig.getDbmsType(),
                resolveProperty(environment, databaseNodeConfig.getUrlPropertyKey()),
                resolveProperty(environment, databaseNodeConfig.getUsernamePropertyKey()),
                resolveProperty(environment, databaseNodeConfig.getPasswordPropertyKey()),
                databaseNodeConfig.isEnabled(), databaseNodeConfig.getMaxConcurrency(), 1
        );
    }

    private String defaultProcessDatabaseName(DbmsType dbmsType) {
        // PostgreSQL은 접속 데이터베이스가 필요하고 MySQL은 접속 뒤 USE로 실행 데이터베이스 선택
        return switch (dbmsType) {
            case POSTGRESQL -> "postgres";
            case MYSQL -> "";
        };
    }

    private String resolveProperty(Environment environment, String propertyKey) {
        // DB에는 secret 자체가 아니라 Spring property key만 저장
        if (propertyKey == null || propertyKey.isBlank()) {
            return "";
        }

        String normalizedKey = propertyKey.trim();
        String resolvedValue = environment.getProperty(normalizedKey);
        if (resolvedValue == null) {
            resolvedValue = environment.getProperty("judge.secrets." + normalizedKey);
        }

        return resolvedValue != null ? resolvedValue.trim() : "";
    }

    private String normalize(String value, String fallback) {
        // 빈 설정값은 DB 연결 기본값으로 정리
        return value != null && !value.isBlank() ? value.trim() : fallback;
    }
}
