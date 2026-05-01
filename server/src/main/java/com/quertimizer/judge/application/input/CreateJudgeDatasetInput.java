package com.quertimizer.judge.application.input;

import com.quertimizer.judge.domain.model.DbmsType;

import java.util.List;
import java.util.Objects;

public class CreateJudgeDatasetInput {

    private final DbmsType dbmsType;
    private final String ddl;
    private final String dataSql;
    private final List<String> baseIndexDdls;

    public CreateJudgeDatasetInput(DbmsType dbmsType, String ddl, String dataSql, List<String> baseIndexDdls) {
        this.dbmsType = Objects.requireNonNull(dbmsType, "dbmsType must not be null");
        this.ddl = requireText(ddl, "ddl");
        this.dataSql = requireText(dataSql, "dataSql");
        this.baseIndexDdls = List.copyOf(Objects.requireNonNull(baseIndexDdls, "baseIndexDdls must not be null"));
    }

    public DbmsType getDbmsType() {
        return dbmsType;
    }

    public String getDdl() {
        return ddl;
    }

    public String getDataSql() {
        return dataSql;
    }

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
