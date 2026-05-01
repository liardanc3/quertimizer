package com.quertimizer.judge.infrastructure.config;

import com.quertimizer.judge.application.port.JudgeRuntime;
import com.quertimizer.judge.application.port.JudgeDefinitionStore;
import com.quertimizer.judge.application.port.JudgeTemplateStore;
import com.quertimizer.judge.domain.policy.SqlDefinitionPolicy;
import com.quertimizer.judge.domain.policy.SqlExecutionPolicy;
import com.quertimizer.judge.infrastructure.dialect.JudgeDialectProvider;
import com.quertimizer.judge.infrastructure.runtime.DatasetTemplateProvisioner;
import com.quertimizer.judge.infrastructure.runtime.DefaultRuntimeEnvironmentNamingStrategy;
import com.quertimizer.judge.infrastructure.runtime.JdbcJudgeRuntime;
import com.quertimizer.judge.infrastructure.runtime.LvmSnapshotDatasetTemplateProvisioner;
import com.quertimizer.judge.infrastructure.runtime.LvmSnapshotEnvironmentProvisioner;
import com.quertimizer.judge.infrastructure.runtime.LvmSnapshotRuntimeCommandFactory;
import com.quertimizer.judge.infrastructure.runtime.LvmSnapshotRuntimeNode;
import com.quertimizer.judge.infrastructure.runtime.LvmSnapshotRuntimeOptions;
import com.quertimizer.judge.infrastructure.runtime.NoOpDatasetTemplateProvisioner;
import com.quertimizer.judge.infrastructure.runtime.ProcessLvmSnapshotCommandExecutor;
import com.quertimizer.judge.infrastructure.runtime.RuntimeDatabase;
import com.quertimizer.judge.infrastructure.runtime.RuntimeDatabaseCluster;
import com.quertimizer.judge.infrastructure.runtime.RuntimeEnvironmentNamingStrategy;
import com.quertimizer.judge.infrastructure.runtime.RuntimeEnvironmentProvisioner;
import com.quertimizer.judge.infrastructure.runtime.SqlReplayEnvironmentProvisioner;
import com.quertimizer.judge.infrastructure.runtime.SqlStatementParser;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableConfigurationProperties(JudgeProperties.class)
public class JudgeConfig {

    @Bean
    public SqlStatementParser judgeSqlStatementParser() {
        return new SqlStatementParser();
    }

    @Bean
    public SqlDefinitionPolicy sqlDefinitionPolicy(SqlStatementParser statementParser) {
        return new SqlDefinitionPolicy(statementParser);
    }

    @Bean
    public SqlExecutionPolicy sqlExecutionPolicy(SqlStatementParser statementParser) {
        return new SqlExecutionPolicy(statementParser);
    }

    @Bean
    public JudgeRuntime judgeRuntime(JudgeProperties properties, JudgeDefinitionStore definitionStore,
                                     JudgeTemplateStore templateStore, SqlStatementParser statementParser,
                                     SqlDefinitionPolicy sqlDefinitionPolicy) {
        // Quertimizer 설정을 judge 런타임 구성으로 변환해 실행 경계 생성
        RuntimeDatabaseCluster databaseCluster = new RuntimeDatabaseCluster(createRuntimeDatabases(properties));
        JudgeDialectProvider dialectProvider = new JudgeDialectProvider();
        RuntimeEnvironmentNamingStrategy namingStrategy = new DefaultRuntimeEnvironmentNamingStrategy();
        LvmSnapshotRuntimeOptions lvmSnapshotRuntimeOptions = createLvmSnapshotRuntimeOptions(properties);
        LvmSnapshotRuntimeCommandFactory lvmCommandFactory = new LvmSnapshotRuntimeCommandFactory(lvmSnapshotRuntimeOptions);
        ProcessLvmSnapshotCommandExecutor lvmCommandExecutor = new ProcessLvmSnapshotCommandExecutor(
                properties.getRuntime().getLvmSnapshot().getCommandTimeoutSeconds());
        RuntimeEnvironmentProvisioner environmentProvisioner = createRuntimeEnvironmentProvisioner(
                properties, databaseCluster,
                dialectProvider, namingStrategy, templateStore,
                lvmSnapshotRuntimeOptions, lvmCommandExecutor,
                lvmCommandFactory, statementParser
        );
        DatasetTemplateProvisioner templateProvisioner = createDatasetTemplateProvisioner(
                properties, databaseCluster,
                dialectProvider, lvmSnapshotRuntimeOptions,
                lvmCommandExecutor, lvmCommandFactory, statementParser
        );

        return new JdbcJudgeRuntime(
                databaseCluster, definitionStore,
                dialectProvider, namingStrategy,
                statementParser, sqlDefinitionPolicy,
                environmentProvisioner, templateStore,
                templateProvisioner
        );
    }

    private RuntimeEnvironmentProvisioner createRuntimeEnvironmentProvisioner(
            JudgeProperties properties, RuntimeDatabaseCluster databaseCluster,
            JudgeDialectProvider dialectProvider, RuntimeEnvironmentNamingStrategy namingStrategy,
            JudgeTemplateStore templateStore, LvmSnapshotRuntimeOptions lvmSnapshotRuntimeOptions,
            ProcessLvmSnapshotCommandExecutor lvmCommandExecutor,
            LvmSnapshotRuntimeCommandFactory lvmCommandFactory,
            SqlStatementParser statementParser) {
        // 설정 값으로 영속 실행 환경 준비 방식을 선택해 LVM 전환 지점 집중
        String provisioner = properties.getRuntime().getProvisioner();
        if ("sql-replay".equalsIgnoreCase(provisioner)) {
            return new SqlReplayEnvironmentProvisioner(databaseCluster, dialectProvider, namingStrategy, statementParser);
        }
        if ("lvm-snapshot".equalsIgnoreCase(provisioner)) {
            return new LvmSnapshotEnvironmentProvisioner(
                    databaseCluster, dialectProvider, templateStore,
                    lvmSnapshotRuntimeOptions, lvmCommandExecutor, lvmCommandFactory
            );
        }

        throw new IllegalStateException("judge.runtime.provisioner value is invalid: " + provisioner);
    }

    private DatasetTemplateProvisioner createDatasetTemplateProvisioner(
            JudgeProperties properties, RuntimeDatabaseCluster databaseCluster,
            JudgeDialectProvider dialectProvider, LvmSnapshotRuntimeOptions lvmSnapshotRuntimeOptions,
            ProcessLvmSnapshotCommandExecutor lvmCommandExecutor,
            LvmSnapshotRuntimeCommandFactory lvmCommandFactory,
            SqlStatementParser statementParser) {
        // LVM 스냅샷 모드에서만 문제 생성 시 봉인 템플릿을 준비하고 재실행 모드에서는 기존 동작 유지
        String provisioner = properties.getRuntime().getProvisioner();
        if ("lvm-snapshot".equalsIgnoreCase(provisioner)) {
            return new LvmSnapshotDatasetTemplateProvisioner(
                    databaseCluster, dialectProvider,
                    lvmSnapshotRuntimeOptions, lvmCommandExecutor,
                    lvmCommandFactory, statementParser
            );
        }

        return new NoOpDatasetTemplateProvisioner();
    }

    private LvmSnapshotRuntimeOptions createLvmSnapshotRuntimeOptions(JudgeProperties properties) {
        // 각 런타임 DB 설정의 실행 컨테이너와 포트 범위를 judge LVM 옵션으로 변환
        JudgeProperties.LvmSnapshotProperties lvmSnapshot = properties.getRuntime().getLvmSnapshot();
        return new LvmSnapshotRuntimeOptions(
                lvmSnapshot.getMountRoot(), lvmSnapshot.getVolumeGroup(),
                lvmSnapshot.getThinPool(), lvmSnapshot.getBaseTemplateVersion(),
                lvmSnapshot.getStartupTimeoutSeconds(),
                properties.getDatabases().stream()
                        .map(this::createLvmSnapshotRuntimeNode)
                        .toList()
        );
    }

    private LvmSnapshotRuntimeNode createLvmSnapshotRuntimeNode(JudgeProperties.DatabaseProperties properties) {
        // LVM provisioner는 DB 컨테이너 자체가 아니라 컨테이너 안의 per-eval DB process에 접속
        com.quertimizer.judge.domain.model.DbmsType dbmsType = properties.resolveEngine()
                .map(this::toJudgeDbmsType)
                .orElseThrow(() -> new IllegalStateException("judge.databases engine value is invalid"));
        String id = resolveRuntimeDatabaseId(properties, dbmsType);
        int portStart = properties.getRuntimePortStart() != null ? properties.getRuntimePortStart() : defaultRuntimePortStart(dbmsType);
        int portEnd = properties.getRuntimePortEnd() != null ? properties.getRuntimePortEnd() : defaultRuntimePortEnd(dbmsType);
        String runtimeDatabaseName = normalize(properties.getRuntimeDatabaseName(), defaultRuntimeDatabaseName(dbmsType));

        return new LvmSnapshotRuntimeNode(
                id, normalize(properties.getRunnerContainer(), id),
                normalize(properties.getRuntimeHost(), "127.0.0.1"),
                portStart, portEnd,
                runtimeDatabaseName, properties.getRootPassword());
    }

    private List<RuntimeDatabase> createRuntimeDatabases(JudgeProperties properties) {
        // judge.databases 설정을 judge 런타임 노드 목록으로 변환
        return properties.getDatabases().stream()
                .map(this::createRuntimeDatabase)
                .toList();
    }

    private RuntimeDatabase createRuntimeDatabase(JudgeProperties.DatabaseProperties properties) {
        // 애플리케이션 설정 값의 DBMS 표현을 judge 독립 타입으로 변환
        com.quertimizer.judge.domain.model.DbmsType dbmsType = properties.resolveEngine()
                .map(this::toJudgeDbmsType)
                .orElseThrow(() -> new IllegalStateException("judge.databases engine value is invalid"));
        String id = resolveRuntimeDatabaseId(properties, dbmsType);

        // 접속 URL과 계정이 비어 있어도 빈 생성은 허용하고 실행 후보 여부는 RuntimeDatabase.isReady에서 판단
        return new RuntimeDatabase(
                id,
                normalize(properties.getName(), id),
                dbmsType,
                normalize(properties.getUrl(), ""),
                normalize(properties.getUsername(), ""),
                properties.getPassword() != null ? properties.getPassword() : "",
                properties.isEnabled(),
                properties.getMaxConcurrency(),
                properties.getWeight() != null ? properties.getWeight() : 1
        );
    }

    private String resolveRuntimeDatabaseId(JudgeProperties.DatabaseProperties properties,
                                            com.quertimizer.judge.domain.model.DbmsType dbmsType) {
        // ID 기본값을 한 곳에서 맞춰 LVM 실행 설정과 RuntimeDatabase 설정 키 일치 보장
        return normalize(properties.getId(), dbmsType.name().toLowerCase() + "-runtime");
    }

    private int defaultRuntimePortStart(com.quertimizer.judge.domain.model.DbmsType dbmsType) {
        // 운영 docker-compose의 첫 번째 실행 포트 범위와 맞춘 기본값
        return switch (dbmsType) {
            case POSTGRESQL -> 56000;
            case MYSQL -> 57000;
        };
    }

    private int defaultRuntimePortEnd(com.quertimizer.judge.domain.model.DbmsType dbmsType) {
        // 운영 docker-compose의 첫 번째 실행 포트 범위와 맞춘 기본값
        return switch (dbmsType) {
            case POSTGRESQL -> 56049;
            case MYSQL -> 57049;
        };
    }

    private String defaultRuntimeDatabaseName(com.quertimizer.judge.domain.model.DbmsType dbmsType) {
        // PostgreSQL은 접속 데이터베이스가 필요하고 MySQL은 접속 뒤 USE로 실행 데이터베이스 선택
        return switch (dbmsType) {
            case POSTGRESQL -> "postgres";
            case MYSQL -> "";
        };
    }

    private com.quertimizer.judge.domain.model.DbmsType toJudgeDbmsType(com.quertimizer.global.constant.DbmsType dbmsType) {
        // 전역 열거형과 judge 열거형의 결합을 설정 경계 한 곳에만 배치
        return switch (dbmsType) {
            case POSTGRESQL -> com.quertimizer.judge.domain.model.DbmsType.POSTGRESQL;
            case MYSQL -> com.quertimizer.judge.domain.model.DbmsType.MYSQL;
        };
    }

    private String normalize(String value, String fallback) {
        // 빈 설정값은 런타임 객체가 처리할 수 있는 기본값으로 정리
        return value != null && !value.isBlank() ? value.trim() : fallback;
    }
}
