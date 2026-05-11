package com.quertimizer.judge.adapter.out.persistence;

import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.model.DbmsType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "problem_set")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JudgeProblemSetDatasetEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "dataset_id", length = 80)
    private String datasetId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String ddl;

    @Column(name = "data", nullable = false, columnDefinition = "TEXT")
    private String dataSql;

    @Enumerated(EnumType.STRING)
    @Column(name = "dbms_type", nullable = false, length = 20)
    private DbmsType dbmsType;

    public DatasetDefinition toDefinition(List<String> baseIndexDdls) {
        // 문제셋 데이터셋 원본을 judge 도메인 정의로 복원
        return new DatasetDefinition(new JudgeDatasetId(datasetId), dbmsType, ddl, dataSql, baseIndexDdls);
    }
}
