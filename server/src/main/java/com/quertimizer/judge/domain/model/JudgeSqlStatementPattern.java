package com.quertimizer.judge.domain.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum JudgeSqlStatementPattern {

    CREATE_INDEX("^CREATE\\s+(UNIQUE\\s+)?INDEX\\b", Pattern.CASE_INSENSITIVE),
    DROP_INDEX("^DROP\\s+INDEX\\b", Pattern.CASE_INSENSITIVE),
    ALTER_INDEX("^ALTER\\s+INDEX\\b", Pattern.CASE_INSENSITIVE),
    EXPLAIN_ANALYZE("^EXPLAIN\\s+(\\([^)]*ANALYZE[^)]*\\)|ANALYZE\\b)", Pattern.CASE_INSENSITIVE),
    OTHER_WORKSPACE("\\b[a-z0-9_]+_problem_\\d{5}_\\d{5}\\b", Pattern.CASE_INSENSITIVE),
    BASE_WORKSPACE("\\bproblem_set_\\d{5}\\b", Pattern.CASE_INSENSITIVE),
    TEMPLATE("\\bproblem_[a-z0-9_]+_template\\b", Pattern.CASE_INSENSITIVE),
    SESSION_SCHEMA("\\bsession_[a-z0-9_]+\\b", Pattern.CASE_INSENSITIVE),
    MULTI_STATEMENT(";(?=.+\\S)", 0),
    WRITE_CTE("\\bWITH\\b[\\s\\S]*\\b(INSERT|UPDATE|DELETE|MERGE)\\b", Pattern.CASE_INSENSITIVE);

    private final Pattern pattern;

    JudgeSqlStatementPattern(String regex, int flags) {
        this.pattern = Pattern.compile(regex, flags);
    }

    public boolean findIn(String sql) {
        // SQL 문자열에서 패턴 존재 여부 확인
        return pattern.matcher(sql).find();
    }

    public Matcher matcher(String sql) {
        // SQL 문자열 Matcher 생성
        return pattern.matcher(sql);
    }
}
