package com.quertimizer.problem.domain.entity;

import com.quertimizer.global.constant.DbmsType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(nullable = false, columnDefinition = "TEXT")
    private String data;

    @Column(name = "is_postgresql", nullable = false)
    private boolean isPostgresql;

    @Column(name = "is_oracle", nullable = false)
    private boolean isOracle;

    public static ProblemSet create(String problemSetId,
                                    String ddl,
                                    String data,
                                    boolean isPostgresql,
                                    boolean isOracle) {
        return new ProblemSet(problemSetId, ddl, data, isPostgresql, isOracle);
    }

    public void changeContent(String ddl,
                              String data,
                              boolean isPostgresql,
                              boolean isOracle) {
        this.ddl = ddl;
        this.data = data;
        this.isPostgresql = isPostgresql;
        this.isOracle = isOracle;
    }

    public boolean supportsDbms(DbmsType dbmsType) {
        // supports DBMS 처리
        if (dbmsType == DbmsType.ORACLE) {
            return isOracle;
        }

        return isPostgresql;
    }

    public boolean hasSupportedDbms() {
        // Supported DBMS 여부 확인
        return isPostgresql || isOracle;
    }

    public DbmsType getDbmsType() {
        // DBMS 유형 조회
        return isOracle ? DbmsType.ORACLE : DbmsType.POSTGRESQL;
    }

    public String getBaseProblemSetId() {
        // 기준 문제 테이블셋 번호 조회
        if (problemSetId == null || problemSetId.isBlank()) {
            return "";
        }

        return problemSetId.matches("^[PO]\\d{5}$") ? problemSetId.substring(1) : problemSetId;
    }

    private ProblemSet(String problemSetId,
                       String ddl,
                       String data,
                       boolean isPostgresql,
                       boolean isOracle) {
        this.problemSetId = problemSetId;
        this.ddl = ddl;
        this.data = data;
        this.isPostgresql = isPostgresql;
        this.isOracle = isOracle;
    }

}
