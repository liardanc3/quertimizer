package com.quertimizer.judge.application.model;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.Data;

@Data
public class Database {

    private final String id;
    private final String name;
    private final DbmsType dbmsType;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final int maxConcurrency;
    private final int weight;

    public Database(String id, String name, DbmsType dbmsType, String jdbcUrl, String username, String password,
                    boolean enabled, int maxConcurrency, int weight) {
        this.id = id.trim();
        this.name = name.trim();
        this.dbmsType = dbmsType;
        this.jdbcUrl = normalizeText(jdbcUrl);
        this.username = normalizeText(username);
        this.password = password;
        this.enabled = enabled;
        this.maxConcurrency = maxConcurrency;
        this.weight = weight;
    }

    public boolean isReady() {
        return enabled && !jdbcUrl.isBlank() && !username.isBlank();
    }

    @Override
    public String toString() {
        return "Database{"
                + "id='" + id + '\''
                + ", name='" + name + '\''
                + ", dbmsType=" + dbmsType
                + ", jdbcUrl='" + jdbcUrl + '\''
                + ", username='" + username + '\''
                + ", enabled=" + enabled
                + ", maxConcurrency=" + maxConcurrency
                + ", weight=" + weight
                + '}';
    }

    private String normalizeText(String value) {
        return value != null ? value.trim() : "";
    }
}
