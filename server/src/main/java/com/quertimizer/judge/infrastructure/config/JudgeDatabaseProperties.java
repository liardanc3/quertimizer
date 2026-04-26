package com.quertimizer.judge.infrastructure.config;

import com.quertimizer.global.constant.DbmsType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "judge")
public class JudgeDatabaseProperties {

    private DatasetProvisioning datasetProvisioning = new DatasetProvisioning();
    private Map<String, NamedDatabaseProperties> templateDatabases = Map.of();
    private Map<String, List<NamedDatabaseProperties>> executionDatabases = Map.of();

    public String getProvisioningStrategy() {
        return datasetProvisioning != null && datasetProvisioning.getStrategy() != null && !datasetProvisioning.getStrategy().isBlank()
                ? datasetProvisioning.getStrategy().trim()
                : "template-copy";
    }

    public NamedDatabaseProperties getTemplateDatabase(DbmsType dbmsType) {
        return templateDatabases.get(dbmsType.getValue());
    }

    public List<NamedDatabaseProperties> getExecutionDatabases(DbmsType dbmsType) {
        return executionDatabases.getOrDefault(dbmsType.getValue(), List.of());
    }

    @Getter
    @Setter
    public static class DatasetProvisioning {
        private String strategy = "template-copy";
    }

    @Getter
    @Setter
    public static class NamedDatabaseProperties {
        private String name;
        private String url;
        private String username;
        private String password;
    }
}
