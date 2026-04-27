package com.quertimizer.problem.domain.entity;

import com.quertimizer.global.constant.DbmsType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "dbms_type", nullable = false, length = 20)
    private DbmsType dbmsType;

    public static ProblemSet create(String problemSetId,
                                    String ddl,
                                    String actualDataSql,
                                    String templateVersion,
                                    DbmsType dbmsType) {
        return new ProblemSet(problemSetId, ddl, actualDataSql, templateVersion, dbmsType);
    }

    public void changeContent(String ddl,
                              String actualDataSql,
                              String templateVersion,
                              DbmsType dbmsType) {
        this.ddl = ddl;
        this.actualDataSql = actualDataSql;
        this.templateVersion = templateVersion;
        this.dbmsType = dbmsType;
    }

    public String getData() {
        // 기존 data 접근 코드를 위한 호환 getter
        return actualDataSql;
    }

    public boolean supportsDbms(DbmsType dbmsType) {
        // supports DBMS 처리
        return this.dbmsType == dbmsType;
    }

    public boolean hasSupportedDbms() {
        // Supported DBMS 여부 확인
        return dbmsType != null;
    }

    public DbmsType getDbmsType() {
        // DBMS 유형 조회
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
                       DbmsType dbmsType) {
        this.problemSetId = problemSetId;
        this.ddl = ddl;
        this.actualDataSql = actualDataSql;
        this.templateVersion = templateVersion;
        this.dbmsType = dbmsType;
    }

}
