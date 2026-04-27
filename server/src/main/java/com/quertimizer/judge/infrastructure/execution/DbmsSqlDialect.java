package com.quertimizer.judge.infrastructure.execution;

import java.util.List;

public interface DbmsSqlDialect {

    String quoteIdentifier(String identifier);

    String createSchemaIfMissingSql(String schemaName);

    String createSchemaSql(String schemaName);

    List<String> useSchemaSqls(String schemaName);

    String dropSchemaIfExistsSql(String schemaName);
}
