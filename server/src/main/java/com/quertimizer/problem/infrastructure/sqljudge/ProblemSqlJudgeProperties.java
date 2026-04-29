package com.quertimizer.problem.infrastructure.sqljudge;

import com.quertimizer.global.constant.DbmsType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Optional;

/**
 * SQL judge runtime database properties used by the problem infrastructure adapter.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "judge")
public class ProblemSqlJudgeProperties {

    private List<DatabaseProperties> databases = List.of();

    /**
     * Returns configured runtime databases.
     *
     * @return configured runtime databases
     */
    public List<DatabaseProperties> getDatabases() {
        return databases != null ? databases : List.of();
    }

    /**
     * SQL judge runtime database properties.
     */
    @Getter
    @Setter
    public static class DatabaseProperties {
        private String id;
        private String name;
        private String engine;
        private String dbmsType;
        private String url;
        private String username;
        private String password;
        private boolean enabled = true;
        private int maxConcurrency = 1;
        private Integer weight;

        /**
         * Resolves the configured DBMS type.
         *
         * @return configured DBMS type when it is valid
         */
        public Optional<DbmsType> resolveEngine() {
            return DbmsType.fromValue(engine != null && !engine.isBlank() ? engine : dbmsType);
        }
    }
}
