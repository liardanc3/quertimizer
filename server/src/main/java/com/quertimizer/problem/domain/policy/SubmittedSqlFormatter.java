package com.quertimizer.problem.domain.policy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SubmittedSqlFormatter {

    private SubmittedSqlFormatter() {
    }

    public static String merge(List<String> indexSqls, String submittedSql) {
        // index DDL과 제출 SQL을 문장 단위로 병합
        Map<String, String> statements = new LinkedHashMap<>();
        if (indexSqls != null) {
            indexSqls.forEach(indexSql -> appendStatements(statements, indexSql));
        }
        appendStatements(statements, submittedSql);

        // 제출 목록 표시용 SQL 반환
        return String.join("\n", statements.values());
    }

    public static String format(String sql) {
        // 기존 저장값을 문장 단위 표시 형식으로 정리
        Map<String, String> statements = new LinkedHashMap<>();
        appendStatements(statements, sql);
        return String.join("\n", statements.values());
    }

    private static void appendStatements(Map<String, String> statements, String sql) {
        // 비어 있지 않은 SQL 문장을 중복 없이 추가
        splitStatements(sql).stream()
                .map(SubmittedSqlFormatter::normalizeStatement)
                .filter(statement -> !statement.isBlank())
                .forEach(statement -> statements.putIfAbsent(normalizeComparableSql(statement), statement + ";"));
    }

    private static List<String> splitStatements(String sql) {
        // SQL 문장 분리 상태 초기화
        String normalizedSql = sql != null ? sql.replace("\r\n", "\n").replace('\r', '\n') : "";
        List<String> statements = new ArrayList<>();
        int statementStart = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        // SQL 문자 순회와 문장 경계 수집
        for (int index = 0; index < normalizedSql.length(); index++) {
            char currentChar = normalizedSql.charAt(index);
            char nextChar = index + 1 < normalizedSql.length() ? normalizedSql.charAt(index + 1) : '\0';

            if (inLineComment) {
                if (currentChar == '\n') {
                    inLineComment = false;
                }
                continue;
            }

            if (inBlockComment) {
                if (currentChar == '*' && nextChar == '/') {
                    inBlockComment = false;
                    index++;
                }
                continue;
            }

            if (inSingleQuote) {
                if (currentChar == '\'' && nextChar == '\'') {
                    index++;
                    continue;
                }
                if (currentChar == '\'') {
                    inSingleQuote = false;
                }
                continue;
            }

            if (inDoubleQuote) {
                if (currentChar == '"' && nextChar == '"') {
                    index++;
                    continue;
                }
                if (currentChar == '"') {
                    inDoubleQuote = false;
                }
                continue;
            }

            if (currentChar == '-' && nextChar == '-') {
                inLineComment = true;
                index++;
                continue;
            }

            if (currentChar == '/' && nextChar == '*') {
                inBlockComment = true;
                index++;
                continue;
            }

            if (currentChar == '\'') {
                inSingleQuote = true;
                continue;
            }

            if (currentChar == '"') {
                inDoubleQuote = true;
                continue;
            }

            if (currentChar == ';') {
                collectStatement(normalizedSql, statementStart, index, statements);
                statementStart = index + 1;
            }
        }

        // 마지막 SQL 문장 수집 후 반환
        collectStatement(normalizedSql, statementStart, normalizedSql.length(), statements);
        return statements;
    }

    private static void collectStatement(String sql, int statementStart, int statementEnd, List<String> statements) {
        // 바깥 공백과 중복 세미콜론 제거 후 문장 추가
        String statement = normalizeStatement(sql.substring(statementStart, statementEnd));
        if (!statement.isBlank()) {
            statements.add(statement);
        }
    }

    private static String normalizeStatement(String sql) {
        // 표시용 문장 정리
        return sql != null ? sql.trim().replaceFirst(";+\\s*$", "").trim() : "";
    }

    private static String normalizeComparableSql(String sql) {
        // 중복 제거 비교 키 정리
        return normalizeStatement(sql).replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
