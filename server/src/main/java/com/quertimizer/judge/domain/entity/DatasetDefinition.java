package com.quertimizer.judge.domain.entity;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.Data;

import java.util.List;

import static com.quertimizer.judge.domain.model.JudgeFailReason.REQUIRED_FIELD_BLANK;

@Data
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
        this.datasetId = datasetId;
        this.dbmsType = dbmsType;
        this.ddl = requireText(ddl, "ddl");
        this.dataSql = requireText(dataSql, "dataSql");
        this.baseIndexDdls = List.copyOf(baseIndexDdls);
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(REQUIRED_FIELD_BLANK.format(name));
        }

        return value;
    }
}
