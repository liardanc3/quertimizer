package com.quertimizer.judge.infrastructure.template;

import com.quertimizer.judge.application.input.RefreshTemplateDatasetInput;
import com.quertimizer.judge.application.port.JudgeTemplateDatasetPort;
import com.quertimizer.judge.domain.service.JudgeSqlStatementParser;
import com.quertimizer.judge.application.port.DbmsSqlDialect;
import com.quertimizer.judge.infrastructure.execution.DbmsSqlDialects;
import com.quertimizer.judge.infrastructure.execution.JudgeDatabaseCluster;
import com.quertimizer.judge.infrastructure.execution.JudgeDatabaseLease;
import com.quertimizer.judge.infrastructure.execution.JudgeDatabaseNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class JdbcTemplateDatasetStore implements JudgeTemplateDatasetPort {

    private final JudgeDatabaseCluster judgeDatabaseCluster;
    private final JudgeSqlStatementParser judgeSqlStatementParser;
    private final DbmsSqlDialects dbmsSqlDialects;

    @Override
    public void refreshTemplateDataset(RefreshTemplateDatasetInput input) {
        // DBMS별 judge DB node 안의 template cache schema를 canonical SQL 기준으로 재생성
        List<JudgeDatabaseNode> nodes = judgeDatabaseCluster.getReadyNodes(input.dbmsType());
        if (nodes.isEmpty()) {
            throw new IllegalStateException("%s judge DB node 설정이 0개다.".formatted(input.dbmsType().getValue()));
        }

        String schemaName = resolveTemplateSchemaName(input.problemSetId(), input.templateVersion());
        for (JudgeDatabaseNode node : nodes) {
            refreshTemplateDatasetOnNode(input, node, schemaName);
        }
    }

    public String resolveTemplateSchemaName(String problemSetId) {
        // template schema 명명 규칙을 일관되게 유지
        return resolveTemplateSchemaName(problemSetId, "latest");
    }

    public String resolveTemplateSchemaName(String problemSetId, String templateVersion) {
        // template schema 명명 규칙을 일관되게 유지
        String normalizedProblemSetId = normalizeSchemaToken(problemSetId);
        String normalizedTemplateVersion = normalizeSchemaToken(templateVersion);
        String shortVersion = normalizedTemplateVersion.length() > 16
                ? normalizedTemplateVersion.substring(0, 16)
                : normalizedTemplateVersion;

        return "qt_template_" + normalizedProblemSetId + "_" + shortVersion;
    }

    private void refreshTemplateDatasetOnNode(RefreshTemplateDatasetInput input, JudgeDatabaseNode node, String schemaName) {
        DbmsSqlDialect dialect = dbmsSqlDialects.get(input.dbmsType());
        try (JudgeDatabaseLease lease = judgeDatabaseCluster.acquireNode(node.getId());
             Connection connection = lease.openConnection()) {
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

    private void executeStatements(Connection connection, String sql) throws Exception {
        for (String statementSql : judgeSqlStatementParser.splitStatements(sql)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(statementSql);
            }
        }
    }

    private String normalizeSchemaToken(String value) {
        // schema 이름에 사용할 token을 정규화
        String normalizedValue = value != null
                ? value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_")
                : "";

        return !normalizedValue.isBlank() ? normalizedValue : "unknown";
    }
}
