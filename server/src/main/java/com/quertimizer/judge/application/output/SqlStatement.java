package com.quertimizer.judge.application.output;

import com.quertimizer.judge.domain.model.ExecutionMode;
import lombok.Data;

import static com.quertimizer.judge.domain.model.JudgeFailReason.REQUIRED_TEXT_BLANK;

@Data
public class SqlStatement {

    private final String sql;
    private final ExecutionMode mode;

    public SqlStatement(String sql, ExecutionMode mode) {
        this.sql = requireText(sql);
        this.mode = mode;
    }

    private String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(REQUIRED_TEXT_BLANK.getMessage());
        }

        return value;
    }
}
