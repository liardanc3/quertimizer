package com.quertimizer.judge.adapter.out.execution;

public final class JudgeRuntimeConstants {

    public static final int MAX_ENVIRONMENT_NAME_LENGTH = 63;
    public static final String POSTGRES_USER = "postgres";
    public static final String MYSQL_USER = "mysql";
    public static final String JUDGE_RUNTIME_OWNER = "999:999";
    public static final String POSTGRES_CTL_RESOLVER =
            "pg_ctl_bin=$(command -v pg_ctl || find /usr/lib/postgresql -name pg_ctl -type f | sort | tail -n 1); ";

    private JudgeRuntimeConstants() {
    }
}
