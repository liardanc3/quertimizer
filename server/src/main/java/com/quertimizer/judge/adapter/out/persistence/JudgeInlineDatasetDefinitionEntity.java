package com.quertimizer.judge.adapter.out.persistence;

import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.model.DbmsType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "judge_inline_dataset_definition")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JudgeInlineDatasetDefinitionEntity {

    @Id
    @Column(name = "dataset_id", nullable = false, length = 80)
    private String datasetId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_id", insertable = false, updatable = false)
    private JudgeDatasetDefinitionEntity datasetDefinition;

    @Column(name = "dbms_type", nullable = false, length = 20)
    private String dbmsType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String ddl;

    @Column(name = "data_sql", nullable = false, columnDefinition = "TEXT")
    private String dataSql;

    public static JudgeInlineDatasetDefinitionEntity from(DatasetDefinition definition) {
        // 임시 데이터셋 SQL 정의 엔티티 생성
        return new JudgeInlineDatasetDefinitionEntity(
                definition.getDatasetId().getValue(), definition.getDbmsType().name(),
                definition.getDdl(), definition.getDataSql()
        );
    }

    public DatasetDefinition toDefinition(List<String> baseIndexDdls) {
        // 임시 데이터셋 SQL 정의를 judge 도메인 정의로 복원
        return new DatasetDefinition(new JudgeDatasetId(datasetId), DbmsType.valueOf(dbmsType), ddl, dataSql, baseIndexDdls);
    }

    private JudgeInlineDatasetDefinitionEntity(String datasetId, String dbmsType, String ddl, String dataSql) {
        this.datasetId = datasetId;
        this.dbmsType = dbmsType;
        this.ddl = ddl;
        this.dataSql = dataSql;
    }
}
