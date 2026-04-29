package com.quertimizer.problem.infrastructure.sqljudge;

import com.quertimizer.sqljudge.api.SqlJudge;
import com.quertimizer.sqljudge.db.SqlJudgeDialectProvider;
import com.quertimizer.sqljudge.definition.InMemorySqlJudgeDefinitionStore;
import com.quertimizer.sqljudge.definition.SqlJudgeDefinitionStore;
import com.quertimizer.sqljudge.policy.SqlDefinitionPolicy;
import com.quertimizer.sqljudge.runtime.DefaultRuntimeEnvironmentNamingStrategy;
import com.quertimizer.sqljudge.runtime.JdbcSqlJudge;
import com.quertimizer.sqljudge.runtime.RuntimeDatabase;
import com.quertimizer.sqljudge.runtime.RuntimeDatabaseCluster;
import com.quertimizer.sqljudge.runtime.SqlStatementParser;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 문제 인프라 계층에서 사용할 sql-judge 객체를 구성한다.
 */
@Configuration
@EnableConfigurationProperties(ProblemSqlJudgeProperties.class)
public class ProblemSqlJudgeConfig {

    /**
     * sql-judge 정의 저장소를 생성한다.
     *
     * @return sql-judge 정의 저장소
     */
    @Bean
    public SqlJudgeDefinitionStore sqlJudgeDefinitionStore() {
        // 독립 모듈 분리 전까지는 같은 실행 환경 안에서 키 기반 흐름을 검증하기 위해 인메모리 저장소를 사용한다.
        return new InMemorySqlJudgeDefinitionStore();
    }

    /**
     * sql-judge API 구현체를 생성한다.
     *
     * @param properties sql-judge 런타임 DB 설정
     * @param definitionStore sql-judge 정의 저장소
     * @return sql-judge API 구현체
     */
    @Bean
    public SqlJudge sqlJudge(ProblemSqlJudgeProperties properties, SqlJudgeDefinitionStore definitionStore) {
        // SQL 분리와 정의 검증이 같은 규칙을 쓰도록 파서를 공유한다.
        SqlStatementParser statementParser = new SqlStatementParser();

        // Quertimizer 설정을 sql-judge 런타임 구성으로 변환해 문제 도메인 밖 실행 경계를 만든다.
        return new JdbcSqlJudge(
                new RuntimeDatabaseCluster(createRuntimeDatabases(properties)),
                definitionStore,
                new SqlJudgeDialectProvider(),
                new DefaultRuntimeEnvironmentNamingStrategy(),
                statementParser,
                new SqlDefinitionPolicy(statementParser)
        );
    }

    private List<RuntimeDatabase> createRuntimeDatabases(ProblemSqlJudgeProperties properties) {
        // judge.databases 설정을 sql-judge 런타임 노드 목록으로 변환한다.
        return properties.getDatabases().stream()
                .map(this::createRuntimeDatabase)
                .toList();
    }

    private RuntimeDatabase createRuntimeDatabase(ProblemSqlJudgeProperties.DatabaseProperties properties) {
        // 애플리케이션 설정 값의 DBMS 표현을 sql-judge 독립 타입으로 변환한다.
        com.quertimizer.sqljudge.db.DbmsType dbmsType = properties.resolveEngine()
                .map(this::toSqlJudgeDbmsType)
                .orElseThrow(() -> new IllegalStateException("judge.databases engine value is invalid"));
        String id = normalize(properties.getId(), dbmsType.name().toLowerCase() + "-runtime");

        // 접속 URL과 계정이 비어 있어도 빈 생성은 허용하고, 실행 후보 여부는 RuntimeDatabase.isReady에서 판단한다.
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

    private com.quertimizer.sqljudge.db.DbmsType toSqlJudgeDbmsType(com.quertimizer.global.constant.DbmsType dbmsType) {
        // 전역 열거형과 sql-judge 열거형의 결합을 설정 경계 한 곳에만 둔다.
        return switch (dbmsType) {
            case POSTGRESQL -> com.quertimizer.sqljudge.db.DbmsType.POSTGRESQL;
            case MYSQL -> com.quertimizer.sqljudge.db.DbmsType.MYSQL;
        };
    }

    private String normalize(String value, String fallback) {
        // 빈 설정값은 런타임 객체가 처리할 수 있는 기본값으로 정리한다.
        return value != null && !value.isBlank() ? value.trim() : fallback;
    }
}
