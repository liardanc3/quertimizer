package com.quertimizer.judge.application.service;

import java.util.regex.Pattern;

final class SqlPlanCostPatterns {

    static final Pattern POSTGRESQL_TOTAL_COST =
            Pattern.compile("cost=[0-9]+(?:\\.[0-9]+)?\\.\\.([0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE);
    static final Pattern MYSQL_QUERY_COST =
            Pattern.compile("\"query_cost\"\\s*:\\s*\"?([0-9]+(?:\\.[0-9]+)?)\"?", Pattern.CASE_INSENSITIVE);

    private SqlPlanCostPatterns() {
    }
}
