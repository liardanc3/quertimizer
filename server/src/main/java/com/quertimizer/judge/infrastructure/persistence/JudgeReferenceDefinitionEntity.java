package com.quertimizer.judge.infrastructure.persistence;

import com.quertimizer.judge.domain.entity.ReferenceDefinition;
import com.quertimizer.judge.domain.entity.ids.JudgeReferenceId;
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
@Table(name = "sql_judge_reference_definition")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JudgeReferenceDefinitionEntity {

    @Id
    @Column(name = "reference_id", nullable = false, length = 80)
    private String referenceId;

    @Column(name = "dataset_id", nullable = false, length = 80)
    private String datasetId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_id", insertable = false, updatable = false)
    private JudgeDatasetDefinitionEntity datasetDefinition;

    @Column(name = "reference_sql", nullable = false, columnDefinition = "TEXT")
    private String referenceSql;

    @Column(name = "result_hash", nullable = false, length = 256)
    private String resultHash;

    public static JudgeReferenceDefinitionEntity from(ReferenceDefinition definition) {
        return new JudgeReferenceDefinitionEntity(
                definition.getReferenceId().getValue(),
                definition.getDatasetId().getValue(),
                definition.getReferenceSql(),
                definition.getResultHash()
        );
    }

    public JudgeReferenceId toReferenceId() {
        return new JudgeReferenceId(referenceId);
    }

    private JudgeReferenceDefinitionEntity(String referenceId, String datasetId,
                                              String referenceSql, String resultHash) {
        this.referenceId = referenceId;
        this.datasetId = datasetId;
        this.referenceSql = referenceSql;
        this.resultHash = resultHash;
    }
}
