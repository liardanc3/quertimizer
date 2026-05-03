package com.quertimizer.judge.application.input;

import com.quertimizer.judge.domain.model.DbmsType;

import java.util.List;
import java.util.Objects;

public class CreateDataset {

    private final DbmsType dbmsType;
    private final String ddl;
    private final String dataSql;
    private final List<String> baseIndexDdls;

    public CreateDataset(DbmsType dbmsType, String ddl, String dataSql, List<String> baseIndexDdls) {
        this.dbmsType = Objects.requireNonNull(dbmsType, "필수 값이 없다.");
        this.ddl = requireText(ddl, "ddl");
        this.dataSql = requireText(dataSql, "dataSql");
        this.baseIndexDdls = List.copyOf(Objects.requireNonNull(baseIndexDdls, "필수 값이 없다."));
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
            throw new IllegalArgumentException(name + "이 비어 있다.");
        }

        return value;
    }
}
