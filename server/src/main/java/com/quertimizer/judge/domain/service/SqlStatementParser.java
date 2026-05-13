package com.quertimizer.judge.domain.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SqlStatementParser {

    public List<String> splitStatements(String sql) {
        // SQL 문장 분리 상태 초기화
        List<String> statements = new ArrayList<>();
        int statementStart = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        // SQL 문자 순회와 문장 경계 수집
        for (int index = 0; index < sql.length(); index++) {
            char currentChar = sql.charAt(index);
            char nextChar = index + 1 < sql.length() ? sql.charAt(index + 1) : '\0';

            // 주석 내부 문자 처리
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

            // 문자열 내부 문자 처리
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

            // 주석 또는 문자열 시작 여부 확인
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

            // 문장 구분자 기준 SQL 문장 수집
            if (currentChar == ';') {
                collectStatement(sql, statementStart, index + 1, statements);
                statementStart = index + 1;
            }
        }

        // 마지막 SQL 문장 수집 후 반환
        collectStatement(sql, statementStart, sql.length(), statements);
        return statements;
    }

    private void collectStatement(String sql, int statementStart, int statementEnd, List<String> statements) {
        // 원본 문장과 첫 내용 위치 추출
        String rawStatement = sql.substring(statementStart, statementEnd);
        int firstContentOffset = findFirstContentOffset(rawStatement);
        if (firstContentOffset < 0) {
            return;
        }

        // 끝 공백과 세미콜론 제거 후 문장 정규화
        int trailingWhitespaceLength = rawStatement.length() - rawStatement.stripTrailing().length();
        String normalizedStatement = sql.substring(statementStart + firstContentOffset, statementEnd - trailingWhitespaceLength)
                .trim()
                .replaceFirst(";\\s*$", "")
                .trim();

        // 비어 있지 않은 문장 목록 추가
        if (!normalizedStatement.isBlank()) {
            statements.add(normalizedStatement);
        }
    }

    private int findFirstContentOffset(String sql) {
        // 첫 비공백 문자 위치 조회
        for (int index = 0; index < sql.length(); index++) {
            if (!Character.isWhitespace(sql.charAt(index))) {
                return index;
            }
        }

        // 내용 없는 SQL 표시
        return -1;
    }
}
