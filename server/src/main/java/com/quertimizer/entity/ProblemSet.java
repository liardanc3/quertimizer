package com.quertimizer.entity;

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
    @Column(name = "problem_set_id", nullable = false, length = 5)
    private String problemSetId;

    @Column(name = "ddl_postgresql", nullable = false, columnDefinition = "TEXT")
    private String ddlPostgresql;

    @Column(name = "ddl_oracle", nullable = false, columnDefinition = "TEXT")
    private String ddlOracle;

    @Column(name = "data_postgresql", nullable = false, columnDefinition = "TEXT")
    private String dataPostgresql;

    @Column(name = "data_oracle", nullable = false, columnDefinition = "TEXT")
    private String dataOracle;

    public static ProblemSet create(String problemSetId,
                                    String ddlPostgresql,
                                    String ddlOracle,
                                    String dataPostgresql,
                                    String dataOracle) {
        return new ProblemSet(problemSetId, ddlPostgresql, ddlOracle, dataPostgresql, dataOracle);
    }

    public void changeContent(String ddlPostgresql,
                              String ddlOracle,
                              String dataPostgresql,
                              String dataOracle) {
        this.ddlPostgresql = ddlPostgresql;
        this.ddlOracle = ddlOracle;
        this.dataPostgresql = dataPostgresql;
        this.dataOracle = dataOracle;
    }

    private ProblemSet(String problemSetId,
                       String ddlPostgresql,
                       String ddlOracle,
                       String dataPostgresql,
                       String dataOracle) {
        this.problemSetId = problemSetId;
        this.ddlPostgresql = ddlPostgresql;
        this.ddlOracle = ddlOracle;
        this.dataPostgresql = dataPostgresql;
        this.dataOracle = dataOracle;
    }

}
