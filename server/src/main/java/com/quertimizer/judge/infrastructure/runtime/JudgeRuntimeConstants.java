package com.quertimizer.judge.infrastructure.runtime;

final class JudgeRuntimeConstants {

    static final int MAX_ENVIRONMENT_NAME_LENGTH = 63;
    static final String POSTGRES_USER = "postgres";
    static final String MYSQL_USER = "mysql";
    static final String JUDGE_RUNTIME_OWNER = "999:999";
    static final String POSTGRES_CTL_RESOLVER =
            "pg_ctl_bin=$(command -v pg_ctl || find /usr/lib/postgresql -name pg_ctl -type f | sort | tail -n 1); ";

    private JudgeRuntimeConstants() {
    }
}
