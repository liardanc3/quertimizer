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
 * Configures sql-judge objects for the problem infrastructure adapter.
 */
@Configuration
@EnableConfigurationProperties(ProblemSqlJudgeProperties.class)
public class ProblemSqlJudgeConfig {

    /**
     * Creates the sql-judge definition store.
     *
     * @return sql-judge definition store
     */
    @Bean
    public SqlJudgeDefinitionStore sqlJudgeDefinitionStore() {
        return new InMemorySqlJudgeDefinitionStore();
    }

    /**
     * Creates the sql-judge API implementation.
     *
     * @param properties SQL judge runtime database properties
     * @param definitionStore sql-judge definition store
     * @return sql-judge API implementation
     */
    @Bean
    public SqlJudge sqlJudge(ProblemSqlJudgeProperties properties, SqlJudgeDefinitionStore definitionStore) {
        SqlStatementParser statementParser = new SqlStatementParser();

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
        return properties.getDatabases().stream()
                .map(this::createRuntimeDatabase)
                .toList();
    }

    private RuntimeDatabase createRuntimeDatabase(ProblemSqlJudgeProperties.DatabaseProperties properties) {
        com.quertimizer.sqljudge.db.DbmsType dbmsType = properties.resolveEngine()
                .map(this::toSqlJudgeDbmsType)
                .orElseThrow(() -> new IllegalStateException("judge.databases engine value is invalid"));
        String id = normalize(properties.getId(), dbmsType.name().toLowerCase() + "-runtime");

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
        return switch (dbmsType) {
            case POSTGRESQL -> com.quertimizer.sqljudge.db.DbmsType.POSTGRESQL;
            case MYSQL -> com.quertimizer.sqljudge.db.DbmsType.MYSQL;
        };
    }

    private String normalize(String value, String fallback) {
        return value != null && !value.isBlank() ? value.trim() : fallback;
    }
}
