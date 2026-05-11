package com.quertimizer.judge.application.output;

import java.util.Objects;

public class JudgeSqlStatement {

    private final String sql;
    private final ExecutionMode mode;

    public JudgeSqlStatement(String sql, ExecutionMode mode) {
        this.sql = requireText(sql);
        this.mode = Objects.requireNonNull(mode, "필수 값이 없습니다.");
    }

    public String getSql() {
        return sql;
    }

    public ExecutionMode getMode() {
        return mode;
    }

    private String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("필수 문자열이 비어 있습니다.");
        }

        return value;
    }
}
