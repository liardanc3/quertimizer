package com.quertimizer.judge.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "judge.statistics")
public class JudgeStatisticsProperties {

    private PostgreSql postgresql = new PostgreSql();
    private MySql mysql = new MySql();

    @Getter
    @Setter
    public static class PostgreSql {
        private int defaultStatisticsTarget = 100;
    }

    @Getter
    @Setter
    public static class MySql {
        private int innodbStatsPersistentSamplePages = 8192;
    }
}
