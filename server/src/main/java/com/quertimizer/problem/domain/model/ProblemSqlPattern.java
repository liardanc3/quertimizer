package com.quertimizer.problem.domain.model;

import java.util.regex.Pattern;

public final class ProblemSqlPattern {

    public static final Pattern CREATE_TABLE_PATTERN = Pattern.compile(
            "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(?:[`\"\\w]+\\.)?[`\"]?(\\w+)[`\"]?\\s*\\(",
            Pattern.CASE_INSENSITIVE
    );

    private ProblemSqlPattern() {
    }
}
