package com.quertimizer.problem.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.output.ProblemDataExampleOutput;
import com.quertimizer.problem.application.output.ProblemExampleTableOutput;
import com.quertimizer.problem.application.output.ProblemJudgeExecutionResult;
import com.quertimizer.problem.application.output.ProblemOutputExampleOutput;
import com.quertimizer.problem.application.output.ProblemOutputPreviewOutput;
import com.quertimizer.problem.application.output.ProblemSchemaColumnOutput;
import com.quertimizer.problem.application.output.ProblemSchemaColumnReferenceOutput;
import com.quertimizer.problem.application.output.ProblemSchemaMetadataOutput;
import com.quertimizer.problem.application.output.ProblemSchemaRelationOutput;
import com.quertimizer.problem.application.output.ProblemSchemaTableOutput;
import com.quertimizer.problem.application.port.out.ProblemJudgePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;

import static com.quertimizer.problem.domain.model.ProblemExampleConstant.MAX_VISIBLE_ROWS;
import static com.quertimizer.problem.domain.model.ProblemSqlPattern.CREATE_TABLE_PATTERN;

@Service
@RequiredArgsConstructor
public class ProblemExampleService {

    private final ProblemJudgePort problemJudgePort;
    private final ObjectMapper objectMapper;

    public String createDataExample(String environmentId, String ddl, DbmsType dbmsType) {
        // 테이블별 최대 표시 행 수 기준 데이터 예시 생성 후 직렬화
        return writeJson(createDataPreview(environmentId, ddl, dbmsType));
    }

    public ProblemDataExampleOutput createDataPreview(String environmentId, String ddl, DbmsType dbmsType) {
        // DDL 기준 예시 대상 테이블명 추출
        List<String> tableNames = extractTableNames(ddl);

        // 테이블별 최대 표시 행 수 기준 데이터 예시 생성
        return new ProblemDataExampleOutput(
                MAX_VISIBLE_ROWS,
                tableNames.stream()
                        .map(tableName -> createTableExample(environmentId, tableName, dbmsType))
                        .toList()
        );
    }

    public String createSchemaMetadata(String environmentId, String ddl, DbmsType dbmsType) {
        // DBMS metadata 조회 결과 기준 테이블 정보와 ERD 정보 생성 후 직렬화
        return writeJson(createSchemaMetadataOutput(environmentId, ddl, dbmsType));
    }

    public String createOutputExample(String environmentId, String answerSql) {
        // 정답 SQL 실행 결과 기준 출력 예시 생성 후 직렬화
        return writeJson(createOutputExampleOutput(environmentId, answerSql));
    }

    public ProblemOutputPreviewOutput createOutputPreview(String environmentId, String answerSql) {
        // 정답 SQL 실행 결과 기준 관리자 미리보기 생성
        ProblemOutputExampleOutput outputExample = createOutputExampleOutput(environmentId, answerSql);
        return new ProblemOutputPreviewOutput(
                outputExample.getColumns(), outputExample.getRows(),
                outputExample.getTotalRows(), outputExample.getVisibleRows(), outputExample.getRowLimit()
        );
    }

    private ProblemExampleTableOutput createTableExample(String environmentId, String tableName, DbmsType dbmsType) {
        // 테이블 전체 조회 SQL을 페이지 실행하여 표시 대상 행 확보
        ProblemJudgeExecutionResult result = problemJudgePort.executeInteractiveSql(
                "data-example-" + UUID.randomUUID(),
                environmentId,
                "SELECT * FROM " + quoteIdentifier(tableName, dbmsType),
                1,
                MAX_VISIBLE_ROWS
        );

        // 테이블 예시 출력 모델 반환
        return new ProblemExampleTableOutput(
                tableName, result.getColumns(), result.getRows(),
                result.getRowCount(), result.getRows().size()
        );
    }

    private ProblemOutputExampleOutput createOutputExampleOutput(String environmentId, String answerSql) {
        // 정답 SQL을 페이지 실행하여 표시 대상 출력 행 확보
        ProblemJudgeExecutionResult result = problemJudgePort.executeInteractiveSql(
                "output-example-" + UUID.randomUUID(),
                environmentId,
                answerSql,
                1,
                MAX_VISIBLE_ROWS
        );

        // 출력 예시 출력 모델 반환
        return new ProblemOutputExampleOutput(
                MAX_VISIBLE_ROWS, result.getColumns(), result.getRows(),
                result.getRowCount(), result.getRows().size()
        );
    }

    private ProblemSchemaMetadataOutput createSchemaMetadataOutput(String environmentId, String ddl, DbmsType dbmsType) {
        // DDL 기준 표시 대상 테이블명 추출
        List<String> tableNames = extractTableNames(ddl);
        Set<String> tableNameSet = new HashSet<>(tableNames);

        // DBMS metadata 조회 결과를 테이블과 관계 응답으로 변환
        Map<String, String> tableComments = createTableComments(environmentId, dbmsType, tableNameSet);
        List<ProblemSchemaRelationOutput> relations = createSchemaRelations(environmentId, dbmsType, tableNameSet);
        Map<String, ProblemSchemaColumnReferenceOutput> referenceByColumn = createReferenceByColumn(relations);
        Map<String, List<ProblemSchemaColumnOutput>> columnsByTable =
                createSchemaColumns(environmentId, dbmsType, tableNameSet, referenceByColumn);
        List<ProblemSchemaTableOutput> tables = tableNames.stream()
                .map(tableName -> new ProblemSchemaTableOutput(
                        tableName,
                        tableComments.getOrDefault(tableName, tableName + " 테이블"),
                        columnsByTable.getOrDefault(tableName, List.of())
                ))
                .toList();
        return new ProblemSchemaMetadataOutput(tables, relations);
    }

    private Map<String, String> createTableComments(String environmentId, DbmsType dbmsType, Set<String> tableNames) {
        // DBMS별 테이블 설명 metadata 조회
        String sql = switch (dbmsType) {
            case MYSQL -> """
                    SELECT table_name, COALESCE(table_comment, '') AS table_comment
                    FROM information_schema.tables
                    WHERE table_schema = DATABASE()
                      AND table_type = 'BASE TABLE'
                    ORDER BY table_name
                    """;
            case POSTGRESQL -> """
                    SELECT table_name,
                           COALESCE(obj_description(format('%I.%I', table_schema, table_name)::regclass, 'pg_class'), '') AS table_comment
                    FROM information_schema.tables
                    WHERE table_schema = CURRENT_SCHEMA()
                      AND table_type = 'BASE TABLE'
                    ORDER BY table_name
                    """;
        };
        ProblemJudgeExecutionResult result = problemJudgePort.executeInternalMetadataSql(
                "schema-table-" + UUID.randomUUID(), environmentId, sql, 1000
        );

        // 표시 대상 테이블 설명만 보관
        Map<String, String> comments = new HashMap<>();
        for (List<String> row : result.getRows()) {
            String tableName = valueAt(result, row, "table_name");
            if (tableNames.contains(tableName)) {
                String comment = valueAt(result, row, "table_comment");
                comments.put(tableName, comment == null || comment.isBlank() ? tableName + " 테이블" : comment);
            }
        }
        return comments;
    }

    private List<ProblemSchemaRelationOutput> createSchemaRelations(String environmentId, DbmsType dbmsType, Set<String> tableNames) {
        // DBMS별 외래키 metadata 조회
        String sql = switch (dbmsType) {
            case MYSQL -> """
                    SELECT referenced_table_name AS source_table_name,
                           referenced_column_name AS source_column_name,
                           table_name AS target_table_name,
                           column_name AS target_column_name
                    FROM information_schema.key_column_usage
                    WHERE table_schema = DATABASE()
                      AND referenced_table_name IS NOT NULL
                    ORDER BY table_name, ordinal_position
                    """;
            case POSTGRESQL -> """
                    SELECT ccu.table_name AS source_table_name,
                           ccu.column_name AS source_column_name,
                           kcu.table_name AS target_table_name,
                           kcu.column_name AS target_column_name
                    FROM information_schema.table_constraints tc
                    JOIN information_schema.key_column_usage kcu
                      ON tc.constraint_name = kcu.constraint_name
                     AND tc.table_schema = kcu.table_schema
                    JOIN information_schema.constraint_column_usage ccu
                      ON ccu.constraint_name = tc.constraint_name
                     AND ccu.table_schema = tc.table_schema
                    WHERE tc.constraint_type = 'FOREIGN KEY'
                      AND tc.table_schema = CURRENT_SCHEMA()
                    ORDER BY kcu.table_name, kcu.ordinal_position
                    """;
        };
        ProblemJudgeExecutionResult result = problemJudgePort.executeInternalMetadataSql(
                "schema-relation-" + UUID.randomUUID(), environmentId, sql, 1000
        );

        // 표시 대상 테이블끼리의 관계만 응답으로 변환
        List<ProblemSchemaRelationOutput> relations = new ArrayList<>();
        for (List<String> row : result.getRows()) {
            String sourceTableName = valueAt(result, row, "source_table_name");
            String targetTableName = valueAt(result, row, "target_table_name");
            if (!tableNames.contains(sourceTableName) || !tableNames.contains(targetTableName)) {
                continue;
            }

            relations.add(new ProblemSchemaRelationOutput(
                    sourceTableName, valueAt(result, row, "source_column_name"),
                    targetTableName, valueAt(result, row, "target_column_name")
            ));
        }
        return relations;
    }

    private Map<String, ProblemSchemaColumnReferenceOutput> createReferenceByColumn(List<ProblemSchemaRelationOutput> relations) {
        // 테이블.컬럼 기준 참조 대상 lookup 생성
        Map<String, ProblemSchemaColumnReferenceOutput> referenceByColumn = new HashMap<>();
        for (ProblemSchemaRelationOutput relation : relations) {
            referenceByColumn.put(
                    relation.getTargetTableName() + "." + relation.getTargetColumnName(),
                    new ProblemSchemaColumnReferenceOutput(relation.getSourceTableName(), relation.getSourceColumnName())
            );
        }
        return referenceByColumn;
    }

    private Map<String, List<ProblemSchemaColumnOutput>> createSchemaColumns(String environmentId,
                                                                            DbmsType dbmsType,
                                                                            Set<String> tableNames,
                                                                            Map<String, ProblemSchemaColumnReferenceOutput> referenceByColumn) {
        // DBMS별 컬럼 metadata 조회
        String sql = switch (dbmsType) {
            case MYSQL -> """
                    SELECT table_name, column_name, column_type,
                           column_key, COALESCE(column_comment, '') AS column_comment,
                           ordinal_position
                    FROM information_schema.columns
                    WHERE table_schema = DATABASE()
                    ORDER BY table_name, ordinal_position
                    """;
            case POSTGRESQL -> """
                    SELECT c.table_name, c.column_name,
                           UPPER(CASE
                               WHEN c.data_type = 'character varying' THEN 'VARCHAR(' || c.character_maximum_length || ')'
                               WHEN c.data_type = 'numeric' AND c.numeric_scale IS NOT NULL THEN 'NUMERIC(' || c.numeric_precision || ', ' || c.numeric_scale || ')'
                               WHEN c.data_type = 'integer' THEN 'INTEGER'
                               WHEN c.data_type = 'bigint' THEN 'BIGINT'
                               WHEN c.data_type = 'timestamp without time zone' THEN 'TIMESTAMP'
                               ELSE c.data_type
                           END) AS column_type,
                           CASE WHEN pk.column_name IS NULL THEN '' ELSE 'PRI' END AS column_key,
                           COALESCE(col_description(format('%I.%I', c.table_schema, c.table_name)::regclass::oid, c.ordinal_position), '') AS column_comment,
                           c.ordinal_position
                    FROM information_schema.columns c
                    LEFT JOIN (
                        SELECT kcu.table_name, kcu.column_name
                        FROM information_schema.table_constraints tc
                        JOIN information_schema.key_column_usage kcu
                          ON tc.constraint_name = kcu.constraint_name
                         AND tc.table_schema = kcu.table_schema
                        WHERE tc.constraint_type = 'PRIMARY KEY'
                          AND tc.table_schema = CURRENT_SCHEMA()
                    ) pk
                      ON pk.table_name = c.table_name
                     AND pk.column_name = c.column_name
                    WHERE c.table_schema = CURRENT_SCHEMA()
                    ORDER BY c.table_name, c.ordinal_position
                    """;
        };
        ProblemJudgeExecutionResult result = problemJudgePort.executeInternalMetadataSql(
                "schema-column-" + UUID.randomUUID(), environmentId, sql, 10000
        );

        // 표시 대상 테이블 컬럼을 테이블별로 그룹화
        Map<String, List<ProblemSchemaColumnOutput>> columnsByTable = new LinkedHashMap<>();
        for (List<String> row : result.getRows()) {
            String tableName = valueAt(result, row, "table_name");
            if (!tableNames.contains(tableName)) {
                continue;
            }

            String columnName = valueAt(result, row, "column_name");
            String columnKey = valueAt(result, row, "column_key");
            ProblemSchemaColumnReferenceOutput reference = referenceByColumn.get(tableName + "." + columnName);
            columnsByTable.computeIfAbsent(tableName, key -> new ArrayList<>())
                    .add(new ProblemSchemaColumnOutput(
                            columnName, valueAt(result, row, "column_type"),
                            resolveColumnDescription(columnName, valueAt(result, row, "column_comment")),
                            "PRI".equalsIgnoreCase(columnKey), reference != null, reference
                    ));
        }
        return columnsByTable;
    }

    private String valueAt(ProblemJudgeExecutionResult result, List<String> row, String columnName) {
        // 컬럼명 기준 실행 결과 값 조회
        for (int index = 0; index < result.getColumns().size(); index++) {
            if (result.getColumns().get(index).equalsIgnoreCase(columnName)) {
                return index < row.size() ? row.get(index) : "";
            }
        }
        return "";
    }

    private String resolveColumnDescription(String columnName, String columnComment) {
        // DB comment 우선 사용 후 컬럼명 기반 설명 대체
        if (columnComment != null && !columnComment.isBlank()) {
            return columnComment;
        }

        if (columnName.endsWith("_id")) {
            return columnName.replace("_id", "") + " ID";
        }
        if (columnName.endsWith("_at")) {
            return columnName.replace("_at", "") + " 시각";
        }
        if (columnName.endsWith("_date")) {
            return columnName.replace("_date", "") + " 날짜";
        }
        if (columnName.endsWith("_amount")) {
            return columnName.replace("_amount", "") + " 금액";
        }
        return columnName;
    }

    private List<String> extractTableNames(String ddl) {
        // CREATE TABLE 구문 기준 테이블명 순서 유지 추출
        List<String> tableNames = new ArrayList<>();
        Matcher matcher = CREATE_TABLE_PATTERN.matcher(ddl);
        while (matcher.find()) {
            String tableName = matcher.group(1);
            if (!tableNames.contains(tableName)) {
                tableNames.add(tableName);
            }
        }

        return tableNames;
    }

    private String quoteIdentifier(String identifier, DbmsType dbmsType) {
        // DBMS별 식별자 인용 방식 적용
        return switch (dbmsType) {
            case MYSQL -> "`" + identifier.replace("`", "``") + "`";
            case POSTGRESQL -> "\"" + identifier.replace("\"", "\"\"") + "\"";
        };
    }

    private String writeJson(Object value) {
        // 예시 출력 모델 JSON 직렬화
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("문제 예시 직렬화 실패", exception);
        }
    }
}
