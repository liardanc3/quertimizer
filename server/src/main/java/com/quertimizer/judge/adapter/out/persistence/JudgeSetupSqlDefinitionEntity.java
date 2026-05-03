package com.quertimizer.judge.adapter.out.persistence;

import com.quertimizer.judge.domain.entity.JudgeSetupSqlId;
import com.quertimizer.judge.domain.entity.SetupSqlDefinition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "judge_setup_sql_definition")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JudgeSetupSqlDefinitionEntity {

    @Id
    @Column(name = "setup_sql_id", nullable = false, length = 80)
    private String setupSqlId;

    @Column(name = "dataset_id", nullable = false, length = 80)
    private String datasetId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_id", insertable = false, updatable = false)
    private JudgeDatasetDefinitionEntity datasetDefinition;

    @Column(name = "setup_sqls_json", nullable = false, columnDefinition = "TEXT")
    private String setupSqlsJson;

    @Column(name = "keep_base_indexes", nullable = false)
    private boolean keepBaseIndexes;

    @Column(name = "apply_setup_indexes_only", nullable = false)
    private boolean applySetupIndexesOnly;

    public static JudgeSetupSqlDefinitionEntity from(SetupSqlDefinition definition, String setupSqlsJson) {
        return new JudgeSetupSqlDefinitionEntity(
                definition.getSetupSqlId().getValue(), definition.getDatasetId().getValue(), setupSqlsJson,
                definition.getIndexPolicy().isKeepBaseIndexes(),
                definition.getIndexPolicy().isApplySetupIndexesOnly()
        );
    }

    public JudgeSetupSqlId toSetupSqlId() {
        return new JudgeSetupSqlId(setupSqlId);
    }

    private JudgeSetupSqlDefinitionEntity(String setupSqlId, String datasetId, String setupSqlsJson,
                                          boolean keepBaseIndexes, boolean applySetupIndexesOnly) {
        this.setupSqlId = setupSqlId;
        this.datasetId = datasetId;
        this.setupSqlsJson = setupSqlsJson;
        this.keepBaseIndexes = keepBaseIndexes;
        this.applySetupIndexesOnly = applySetupIndexesOnly;
    }
}
