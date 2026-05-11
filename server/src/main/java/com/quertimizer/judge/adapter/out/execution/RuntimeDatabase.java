package com.quertimizer.judge.adapter.out.execution;

import com.quertimizer.judge.domain.model.DbmsType;

import java.util.Objects;

public class RuntimeDatabase {

    private final String id;
    private final String name;
    private final DbmsType dbmsType;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final int maxConcurrency;
    private final int weight;

    public RuntimeDatabase(String id,
                           String name,
                           DbmsType dbmsType,
                           String jdbcUrl,
                           String username,
                           String password,
                           boolean enabled,
                           int maxConcurrency,
                           int weight) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.dbmsType = Objects.requireNonNull(dbmsType, "필수 값이 없습니다.");
        this.jdbcUrl = normalizeText(jdbcUrl);
        this.username = normalizeText(username);
        this.password = Objects.requireNonNull(password, "필수 값이 없습니다.");
        this.enabled = enabled;
        this.maxConcurrency = requirePositive(maxConcurrency, "maxConcurrency");
        this.weight = requirePositive(weight, "weight");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public DbmsType getDbmsType() {
        return dbmsType;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isReady() {
        return enabled && !jdbcUrl.isBlank() && !username.isBlank();
    }

    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    public int getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return "RuntimeDatabase{"
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

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "이 비어 있습니다.");
        }

        return value;
    }

    private String normalizeText(String value) {
        return value != null ? value.trim() : "";
    }

    private int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + "은 0보다 커야 합니다.");
        }

        return value;
    }
}
