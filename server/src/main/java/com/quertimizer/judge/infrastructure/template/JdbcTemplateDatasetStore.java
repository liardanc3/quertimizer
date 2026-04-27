package com.quertimizer.judge.infrastructure.template;

import com.quertimizer.judge.application.input.RefreshTemplateDatasetInput;
import com.quertimizer.judge.application.port.JudgeTemplateDatasetPort;
import com.quertimizer.judge.domain.service.JudgeSqlStatementParser;
import com.quertimizer.judge.infrastructure.config.JudgeDatabaseProperties;
import com.quertimizer.judge.infrastructure.execution.DbmsSqlDialect;
import com.quertimizer.judge.infrastructure.execution.DbmsSqlDialects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Component
@RequiredArgsConstructor
public class JdbcTemplateDatasetStore implements JudgeTemplateDatasetPort {

    private final JudgeDatabaseProperties judgeDatabaseProperties;
    private final JudgeSqlStatementParser judgeSqlStatementParser;
    private final DbmsSqlDialects dbmsSqlDialects;

    @Override
    public void refreshTemplateDataset(RefreshTemplateDatasetInput input) {
        // DBMS별 template schema를 canonical DDL + actualDataSql 기준으로 재생성
        JudgeDatabaseProperties.NamedDatabaseProperties properties = judgeDatabaseProperties.getTemplateDatabase(input.dbmsType());
        if (properties == null || isBlank(properties.getUrl()) || isBlank(properties.getUsername())) {
            throw new IllegalStateException("%s template DB 설정이 없다.".formatted(input.dbmsType().getValue()));
        }

        String schemaName = resolveTemplateSchemaName(input.problemSetId());
        DbmsSqlDialect dialect = dbmsSqlDialects.get(input.dbmsType());
        try (Connection connection = DriverManager.getConnection(properties.getUrl(), properties.getUsername(), properties.getPassword())) {
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                statement.execute(dialect.dropSchemaIfExistsSql(schemaName));
                statement.execute(dialect.createSchemaSql(schemaName));
                for (String useSchemaSql : dialect.useSchemaSqls(schemaName)) {
                    statement.execute(useSchemaSql);
                }
            }

            executeStatements(connection, input.ddl());
            executeStatements(connection, input.actualDataSql());
            connection.commit();
        } catch (Exception exception) {
            throw new IllegalStateException("template dataset 갱신에 실패했다.", exception);
        }
    }

    public String resolveTemplateSchemaName(String problemSetId) {
        // template schema 명명 규칙을 일관되게 유지
        return "judge_template_" + problemSetId.toLowerCase();
    }

    private void executeStatements(Connection connection, String sql) throws Exception {
        for (String statementSql : judgeSqlStatementParser.splitStatements(sql)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(statementSql);
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
