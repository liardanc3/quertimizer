package com.quertimizer.judge.domain.entity;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;
import lombok.Getter;

import java.util.List;
import java.util.Objects;

@Getter
public class DatasetDefinition {

    private final JudgeDatasetId datasetId;
    private final DbmsType dbmsType;
    private final String ddl;
    private final String dataSql;
    private final List<String> baseIndexDdls;

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

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return value;
    }
}
