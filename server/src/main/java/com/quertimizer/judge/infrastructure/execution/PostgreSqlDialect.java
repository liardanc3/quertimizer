package com.quertimizer.judge.infrastructure.execution;

import java.util.List;

public class PostgreSqlDialect implements DbmsSqlDialect {

    @Override
    public String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String createSchemaIfMissingSql(String schemaName) {
        return "CREATE SCHEMA IF NOT EXISTS " + quoteIdentifier(schemaName);
    }

    @Override
    public String createSchemaSql(String schemaName) {
        return "CREATE SCHEMA " + quoteIdentifier(schemaName);
    }

    @Override
    public List<String> useSchemaSqls(String schemaName) {
        return List.of("SET LOCAL search_path TO " + quoteIdentifier(schemaName) + ", public");
    }

    @Override
    public String dropSchemaIfExistsSql(String schemaName) {
        return "DROP SCHEMA IF EXISTS " + quoteIdentifier(schemaName) + " CASCADE";
    }
}
