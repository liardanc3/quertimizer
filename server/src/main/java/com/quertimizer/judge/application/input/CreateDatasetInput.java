package com.quertimizer.judge.application.input;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.domain.model.QueuePriority;
import lombok.Data;

import java.util.List;

import static com.quertimizer.judge.domain.model.JudgeFailReason.REQUIRED_FIELD_BLANK;

@Data
public class CreateDatasetInput {

    private final DbmsType dbmsType;
    private final String ddl;
    private final String dataSql;
    private final List<String> baseIndexDdls;
    private final boolean storeSqlDefinition;
    private final QueuePriority queuePriority;

    public CreateDatasetInput(DbmsType dbmsType, String ddl, String dataSql, List<String> baseIndexDdls) {
        this(dbmsType, ddl, dataSql, baseIndexDdls, false);
    }

    public CreateDatasetInput(DbmsType dbmsType, String ddl, String dataSql,
                              List<String> baseIndexDdls, boolean storeSqlDefinition) {
        this(dbmsType, ddl, dataSql, baseIndexDdls, storeSqlDefinition, QueuePriority.FIRST);
    }

    public CreateDatasetInput(DbmsType dbmsType, String ddl, String dataSql,
                              List<String> baseIndexDdls, boolean storeSqlDefinition,
                              QueuePriority queuePriority) {
        this.dbmsType = dbmsType;
        this.ddl = requireText(ddl, "ddl");
        this.dataSql = requireText(dataSql, "dataSql");
        this.baseIndexDdls = List.copyOf(baseIndexDdls);
        this.storeSqlDefinition = storeSqlDefinition;
        this.queuePriority = queuePriority;
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(REQUIRED_FIELD_BLANK.format(name));
        }

        return value;
    }
}
