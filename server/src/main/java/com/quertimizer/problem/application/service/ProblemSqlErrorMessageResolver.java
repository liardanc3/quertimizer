package com.quertimizer.problem.application.service;

import com.quertimizer.judge.application.exception.UserSqlExecutionException;

final class ProblemSqlErrorMessageResolver {

    private ProblemSqlErrorMessageResolver() {
    }

    static String resolveUserSqlMessage(Throwable exception) {
        // 예외 체인에서 사용자 SQL 실행 오류 우선 조회
        UserSqlExecutionException sqlException = findUserSqlExecutionException(exception);
        if (sqlException == null || !hasText(sqlException.getReason())) {
            return null;
        }

        return sqlException.getReason();
    }

    static String resolve(Throwable exception, String fallbackMessage) {
        // 사용자 SQL 실행 오류는 내부 wrapper보다 우선 노출
        String userSqlMessage = resolveUserSqlMessage(exception);
        if (userSqlMessage != null) {
            return userSqlMessage;
        }

        // 기존 제출 오류 메시지 선택 방식 유지
        Throwable current = exception;
        while (current != null) {
            if (hasText(current.getMessage())) {
                return current.getMessage();
            }

            current = current.getCause();
        }

        return fallbackMessage;
    }

    private static UserSqlExecutionException findUserSqlExecutionException(Throwable exception) {
        // cause chain에서 JDBC 사용자 SQL 오류 표식 조회
        Throwable current = exception;
        while (current != null) {
            if (current instanceof UserSqlExecutionException sqlException) {
                return sqlException;
            }

            current = current.getCause();
        }

        return null;
    }

    private static boolean hasText(String value) {
        // 공백 메시지 제외
        return value != null && !value.isBlank();
    }
}
