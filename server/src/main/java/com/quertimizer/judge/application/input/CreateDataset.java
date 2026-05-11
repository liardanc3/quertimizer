package com.quertimizer.judge.application.input;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.domain.model.JudgeQueuePriority;

import java.util.List;
import java.util.Objects;

public class CreateDataset {

    private final DbmsType dbmsType;
    private final String ddl;
    private final String dataSql;
    private final List<String> baseIndexDdls;
    private final boolean storeSqlDefinition;
    private final JudgeQueuePriority queuePriority;

    public CreateDataset(DbmsType dbmsType, String ddl, String dataSql, List<String> baseIndexDdls) {
        this(dbmsType, ddl, dataSql, baseIndexDdls, false);
    }

    public CreateDataset(DbmsType dbmsType, String ddl, String dataSql,
                         List<String> baseIndexDdls, boolean storeSqlDefinition) {
        this(dbmsType, ddl, dataSql, baseIndexDdls, storeSqlDefinition, JudgeQueuePriority.FIRST);
    }

    public CreateDataset(DbmsType dbmsType, String ddl, String dataSql,
                         List<String> baseIndexDdls, boolean storeSqlDefinition,
                         JudgeQueuePriority queuePriority) {
        this.dbmsType = Objects.requireNonNull(dbmsType, "필수 값이 없습니다.");
        this.ddl = requireText(ddl, "ddl");
        this.dataSql = requireText(dataSql, "dataSql");
        this.baseIndexDdls = List.copyOf(Objects.requireNonNull(baseIndexDdls, "필수 값이 없습니다."));
        this.storeSqlDefinition = storeSqlDefinition;
        this.queuePriority = Objects.requireNonNull(queuePriority, "필수 값이 없습니다.");
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

    public boolean isStoreSqlDefinition() {
        return storeSqlDefinition;
    }

    public JudgeQueuePriority getQueuePriority() {
        return queuePriority;
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "이 비어 있습니다.");
        }

        return value;
    }
}
