package com.quertimizer.sqljudge.definition;

import com.quertimizer.sqljudge.db.DbmsType;
import com.quertimizer.sqljudge.id.JudgeDatasetId;

import java.util.List;
import java.util.Objects;

/**
 * Represents a registered SQL dataset definition owned by sql-judge.
 */
public class DatasetDefinition {

    private final JudgeDatasetId datasetId;
    private final DbmsType dbmsType;
    private final String ddl;
    private final String dataSql;
    private final List<String> baseIndexDdls;

    /**
     * Creates a registered SQL dataset definition.
     *
     * @param datasetId dataset ID
     * @param dbmsType target DBMS type
     * @param ddl schema DDL
     * @param dataSql dataset SQL
     * @param baseIndexDdls base index DDL statements
     */
    public DatasetDefinition(JudgeDatasetId datasetId,
                             DbmsType dbmsType,
                             String ddl,
                             String dataSql,
                             List<String> baseIndexDdls) {
        this.datasetId = Objects.requireNonNull(datasetId, "datasetId must not be null");
        this.dbmsType = Objects.requireNonNull(dbmsType, "dbmsType must not be null");
        this.ddl = requireText(ddl, "ddl");
        this.dataSql = requireText(dataSql, "dataSql");
        this.baseIndexDdls = List.copyOf(Objects.requireNonNull(baseIndexDdls, "baseIndexDdls must not be null"));
    }

    /**
     * Returns the dataset ID.
     *
     * @return dataset ID
     */
    public JudgeDatasetId getDatasetId() {
        return datasetId;
    }

    /**
     * Returns the target DBMS type.
     *
     * @return target DBMS type
     */
    public DbmsType getDbmsType() {
        return dbmsType;
    }

    /**
     * Returns the schema DDL.
     *
     * @return schema DDL
     */
    public String getDdl() {
        return ddl;
    }

    /**
     * Returns the dataset SQL.
     *
     * @return dataset SQL
     */
    public String getDataSql() {
        return dataSql;
    }

    /**
     * Returns the base index DDL statements.
     *
     * @return base index DDL statements
     */
    public List<String> getBaseIndexDdls() {
        return baseIndexDdls;
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return value;
    }
}
