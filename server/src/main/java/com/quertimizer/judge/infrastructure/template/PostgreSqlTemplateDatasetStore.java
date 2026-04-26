package com.quertimizer.judge.infrastructure.template;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.judge.application.input.RefreshTemplateDatasetInput;
import com.quertimizer.judge.application.port.JudgeTemplateDatasetPort;
import com.quertimizer.judge.domain.service.JudgeSqlStatementParser;
import com.quertimizer.judge.infrastructure.config.JudgeDatabaseProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Component
@RequiredArgsConstructor
public class PostgreSqlTemplateDatasetStore implements JudgeTemplateDatasetPort {

    private final JudgeDatabaseProperties judgeDatabaseProperties;
    private final JudgeSqlStatementParser judgeSqlStatementParser;

    @Override
    public void refreshTemplateDataset(RefreshTemplateDatasetInput input) {
        // PostgreSQL template schema를 canonical DDL + actualDataSql 기준으로 재생성
        if (input.dbmsType() == DbmsType.ORACLE) {
            throw new IllegalStateException("Oracle template dataset store는 아직 지원하지 않는다.");
        }

        JudgeDatabaseProperties.NamedDatabaseProperties properties = judgeDatabaseProperties.getTemplateDatabase(input.dbmsType());
        if (properties == null || isBlank(properties.getUrl()) || isBlank(properties.getUsername())) {
            throw new IllegalStateException("%s template DB 설정이 없다.".formatted(input.dbmsType().getValue()));
        }

        String schemaName = resolveTemplateSchemaName(input.problemSetId());
        try (Connection connection = DriverManager.getConnection(properties.getUrl(), properties.getUsername(), properties.getPassword())) {
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS " + quoteIdentifier(schemaName) + " CASCADE");
                statement.execute("CREATE SCHEMA " + quoteIdentifier(schemaName));
                statement.execute("SET LOCAL search_path TO " + quoteIdentifier(schemaName) + ", public");
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

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
