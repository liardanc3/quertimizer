package com.quertimizer.sqljudge.runtime;

import com.quertimizer.sqljudge.db.DbmsType;

import java.util.Objects;

/**
 * Represents a configured runtime database node.
 */
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

    /**
     * Creates a runtime database node.
     *
     * @param id runtime database ID
     * @param name runtime database display name
     * @param dbmsType runtime DBMS type
     * @param jdbcUrl JDBC URL
     * @param username JDBC username
     * @param password JDBC password
     * @param enabled whether this runtime database can receive executions
     * @param maxConcurrency maximum concurrent leases
     * @param weight selection weight
     */
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
        this.dbmsType = Objects.requireNonNull(dbmsType, "dbmsType must not be null");
        this.jdbcUrl = normalizeText(jdbcUrl);
        this.username = normalizeText(username);
        this.password = Objects.requireNonNull(password, "password must not be null");
        this.enabled = enabled;
        this.maxConcurrency = requirePositive(maxConcurrency, "maxConcurrency");
        this.weight = requirePositive(weight, "weight");
    }

    /**
     * Returns the runtime database ID.
     *
     * @return runtime database ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the runtime database display name.
     *
     * @return runtime database display name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the runtime DBMS type.
     *
     * @return runtime DBMS type
     */
    public DbmsType getDbmsType() {
        return dbmsType;
    }

    /**
     * Returns the JDBC URL.
     *
     * @return JDBC URL
     */
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    /**
     * Returns the JDBC username.
     *
     * @return JDBC username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the JDBC password.
     *
     * @return JDBC password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Returns whether this runtime database can receive executions.
     *
     * @return true when this runtime database is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns whether this runtime database has enough connection information to receive executions.
     *
     * @return true when this runtime database is enabled and connectable
     */
    public boolean isReady() {
        return enabled && !jdbcUrl.isBlank() && !username.isBlank();
    }

    /**
     * Returns the maximum concurrent leases.
     *
     * @return maximum concurrent leases
     */
    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    /**
     * Returns the selection weight.
     *
     * @return selection weight
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Returns a string representation that does not expose the password.
     *
     * @return string representation without the password
     */
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
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return value;
    }

    private String normalizeText(String value) {
        return value != null ? value.trim() : "";
    }

    private int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }

        return value;
    }
}
