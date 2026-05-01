package com.quertimizer.problem.domain.entity;

import com.quertimizer.global.constant.DbmsType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "problem_set")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemSet {

    @Id
    @Column(name = "problem_set_id", nullable = false, length = 6)
    private String problemSetId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String ddl;

    @Column(name = "data", nullable = false, columnDefinition = "TEXT")
    private String actualDataSql;

    @Column(name = "template_version", length = 64)
    private String templateVersion;

    @Column(name = "judge_dataset_id", length = 80)
    private String judgeDatasetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dbms_type", nullable = false, length = 20)
    private DbmsType dbmsType;

    @BatchSize(size = 50)
    @OrderBy("problemId ASC")
    @OneToMany(mappedBy = "problemSet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Problem> problems = new ArrayList<>();

    public static ProblemSet create(String problemSetId,
                                    String ddl,
                                    String actualDataSql,
                                    String templateVersion,
                                    DbmsType dbmsType) {
        return create(problemSetId, ddl, actualDataSql, templateVersion, dbmsType, "");
    }

    public static ProblemSet create(String problemSetId,
                                    String ddl,
                                    String actualDataSql,
                                    String templateVersion,
                                    DbmsType dbmsType,
                                    String judgeDatasetId) {
        return new ProblemSet(problemSetId, ddl, actualDataSql, templateVersion, dbmsType, judgeDatasetId);
    }

    public void changeContent(String ddl,
                              String actualDataSql,
                              String templateVersion,
                              DbmsType dbmsType) {
        changeContent(ddl, actualDataSql, templateVersion, dbmsType, judgeDatasetId);
    }

    public void changeContent(String ddl,
                              String actualDataSql,
                              String templateVersion,
                              DbmsType dbmsType,
                              String judgeDatasetId) {
        this.ddl = ddl;
        this.actualDataSql = actualDataSql;
        this.templateVersion = templateVersion;
        this.dbmsType = dbmsType;
        this.judgeDatasetId = judgeDatasetId;
    }

    public String getData() {
        // 기존 data 접근 코드를 위한 호환 getter
        return actualDataSql;
    }

    public boolean supportsDbms(DbmsType dbmsType) {
        // 이 테이블셋의 요청 DBMS 사용 가능 여부 판단
        return this.dbmsType == dbmsType;
    }

    public boolean hasSupportedDbms() {
        // DBMS가 지정된 최신 테이블셋 여부 판단
        return dbmsType != null;
    }

    public DbmsType getDbmsType() {
        // 이 테이블셋이 속한 DBMS 유형 반환
        return dbmsType;
    }

    public String getBaseProblemSetId() {
        // 기준 문제 테이블셋 번호 조회
        return DbmsType.extractBaseProblemSetId(problemSetId);
    }

    private ProblemSet(String problemSetId,
                       String ddl,
                       String actualDataSql,
                       String templateVersion,
                       DbmsType dbmsType,
                       String judgeDatasetId) {
        this.problemSetId = problemSetId;
        this.ddl = ddl;
        this.actualDataSql = actualDataSql;
        this.templateVersion = templateVersion;
        this.dbmsType = dbmsType;
        this.judgeDatasetId = judgeDatasetId;
    }

}
