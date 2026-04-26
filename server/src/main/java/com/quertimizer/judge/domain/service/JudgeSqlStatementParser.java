package com.quertimizer.judge.domain.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class JudgeSqlStatementParser {

    public List<String> splitStatements(String sql) {
        // 문자열 SQL을 인용부호와 주석을 고려해 statement 단위로 분리
        List<String> statements = new ArrayList<>();
        int statementStart = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int index = 0; index < sql.length(); index++) {
            char currentChar = sql.charAt(index);
            char nextChar = index + 1 < sql.length() ? sql.charAt(index + 1) : '\0';

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
                collectStatement(sql, statementStart, index + 1, statements);
                statementStart = index + 1;
            }
        }

        collectStatement(sql, statementStart, sql.length(), statements);
        return statements;
    }

    private void collectStatement(String sql, int statementStart, int statementEnd, List<String> statements) {
        // 비어 있지 않은 statement만 결과에 추가
        String rawStatement = sql.substring(statementStart, statementEnd);
        int firstContentOffset = findFirstContentOffset(rawStatement);
        if (firstContentOffset < 0) {
            return;
        }

        int trailingWhitespaceLength = rawStatement.length() - rawStatement.stripTrailing().length();
        String normalizedStatement = sql.substring(statementStart + firstContentOffset, statementEnd - trailingWhitespaceLength)
                .trim()
                .replaceFirst(";\\s*$", "")
                .trim();

        if (!normalizedStatement.isBlank()) {
            statements.add(normalizedStatement);
        }
    }

    private int findFirstContentOffset(String sql) {
        // 첫 번째 비공백 문자의 offset을 조회
        for (int index = 0; index < sql.length(); index++) {
            if (!Character.isWhitespace(sql.charAt(index))) {
                return index;
            }
        }

        return -1;
    }
}
