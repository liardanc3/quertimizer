package com.quertimizer.judge.application.model;

public final class Constants {

    public static final int MAX_ENVIRONMENT_NAME_LENGTH = 63;
    public static final int POSTGRES_CTL_TIMEOUT_SECONDS = 30;
    public static final int POSTGRES_DEFAULT_STATISTICS_TARGET = 100;
    public static final int MYSQL_INNODB_STATS_PERSISTENT_SAMPLE_PAGES = 8192;
    public static final String POSTGRES_USER = "postgres";
    public static final String MYSQL_USER = "mysql";
    public static final String DATABASE_PROCESS_OWNER = "999:999";
    public static final String POSTGRES_CTL_RESOLVER =
            "pg_ctl_bin=$(command -v pg_ctl || find /usr/lib/postgresql -name pg_ctl -type f | sort | tail -n 1); ";

    private Constants() {
    }
}
