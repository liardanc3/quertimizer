package com.quertimizer.problem.adapter.out.persistence;

import com.quertimizer.judge.domain.model.DbmsType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "problem_set")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemSetJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "problem_set_id", nullable = false, unique = true, length = 6)
    private String problemSetId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String ddl;

    @Column(name = "data", nullable = false, columnDefinition = "TEXT")
    private String actualDataSql;

    @Column(name = "dataset_id", length = 80)
    private String datasetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dbms_type", nullable = false, length = 20)
    private DbmsType dbmsType;

    public static ProblemSetJpaEntity create(String problemSetId, String ddl,
                                             String actualDataSql, String datasetId,
                                             DbmsType dbmsType) {
        // 문제 테이블셋 JPA 엔티티 생성
        return new ProblemSetJpaEntity(problemSetId, ddl, actualDataSql, datasetId, dbmsType);
    }

    public void update(String ddl, String actualDataSql, String datasetId, DbmsType dbmsType) {
        // 문제 테이블셋 JPA 엔티티 내용 변경
        this.ddl = ddl;
        this.actualDataSql = actualDataSql;
        this.datasetId = datasetId;
        this.dbmsType = dbmsType;
    }

    private ProblemSetJpaEntity(String problemSetId, String ddl,
                                String actualDataSql, String datasetId,
                                DbmsType dbmsType) {
        this.problemSetId = problemSetId;
        this.ddl = ddl;
        this.actualDataSql = actualDataSql;
        this.datasetId = datasetId;
        this.dbmsType = dbmsType;
    }
}
