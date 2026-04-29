package com.quertimizer.judge.infrastructure.config;

import com.quertimizer.global.constant.DbmsType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "judge")
public class JudgeDatabaseProperties {

    private DatasetProvisioning datasetProvisioning = new DatasetProvisioning();
    private List<DatabaseProperties> databases = List.of();
    private Duration leaseAcquireTimeout = Duration.ofSeconds(3);

    public String getProvisioningStrategy() {
        return datasetProvisioning != null && datasetProvisioning.getStrategy() != null && !datasetProvisioning.getStrategy().isBlank()
                ? datasetProvisioning.getStrategy().trim()
                : "sql-replay";
    }

    public List<DatabaseProperties> getDatabases() {
        return databases != null ? databases : List.of();
    }

    public Duration getLeaseAcquireTimeout() {
        return leaseAcquireTimeout != null ? leaseAcquireTimeout : Duration.ofSeconds(3);
    }

    public List<DatabaseProperties> getDatabases(DbmsType dbmsType) {
        return getDatabases().stream()
                .filter(database -> database.resolveEngine().filter(dbmsType::equals).isPresent())
                .toList();
    }

    @Getter
    @Setter
    public static class DatasetProvisioning {
        private String strategy = "sql-replay";
    }

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

        public Optional<DbmsType> resolveEngine() {
            return DbmsType.fromValue(engine != null && !engine.isBlank() ? engine : dbmsType);
        }
    }
}
