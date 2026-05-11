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
@Table(name = "problem")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "problem_id", nullable = false, unique = true, length = 12)
    private String problemId;

    @Column(name = "problem_set_id", nullable = false, length = 6)
    private String problemSetId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String ddl;

    @Enumerated(EnumType.STRING)
    @Column(name = "dbms_type", nullable = false, length = 20)
    private DbmsType dbmsType;

    @Column(columnDefinition = "TEXT")
    private String condition;

    @Column(columnDefinition = "TEXT")
    private String output;

    @Column(name = "data_example", columnDefinition = "TEXT")
    private String dataExample;

    @Column(name = "output_example", columnDefinition = "TEXT")
    private String outputExample;

    @Column(name = "schema_metadata", columnDefinition = "TEXT")
    private String schemaMetadata;

    @Column(columnDefinition = "TEXT")
    private String answerHash;

    @Column(name = "answer_sql", columnDefinition = "TEXT")
    private String answerSql;

    public static ProblemJpaEntity create(String problemId, String problemSetId,
                                          String title, String description,
                                          String ddl, DbmsType dbmsType,
                                          String condition, String output,
                                          String dataExample, String outputExample,
                                          String schemaMetadata,
                                          String answerHash, String answerSql) {
        // 문제 JPA 엔티티 생성
        return new ProblemJpaEntity(
                problemId, problemSetId, title, description, ddl, dbmsType,
                condition, output, dataExample, outputExample, schemaMetadata, answerHash, answerSql
        );
    }

    public void update(String title, String description, String ddl, DbmsType dbmsType,
                       String condition, String output, String dataExample,
                       String outputExample, String schemaMetadata, String answerHash, String answerSql) {
        // 문제 JPA 엔티티 내용 변경
        this.title = title;
        this.description = description;
        this.ddl = ddl;
        this.dbmsType = dbmsType;
        this.condition = condition;
        this.output = output;
        this.dataExample = dataExample;
        this.outputExample = outputExample;
        this.schemaMetadata = schemaMetadata;
        this.answerHash = answerHash;
        this.answerSql = answerSql;
    }

    private ProblemJpaEntity(String problemId, String problemSetId,
                             String title, String description,
                             String ddl, DbmsType dbmsType,
                             String condition, String output,
                             String dataExample, String outputExample,
                             String schemaMetadata,
                             String answerHash, String answerSql) {
        this.problemId = problemId;
        this.problemSetId = problemSetId;
        this.title = title;
        this.description = description;
        this.ddl = ddl;
        this.dbmsType = dbmsType;
        this.condition = condition;
        this.output = output;
        this.dataExample = dataExample;
        this.outputExample = outputExample;
        this.schemaMetadata = schemaMetadata;
        this.answerHash = answerHash;
        this.answerSql = answerSql;
    }
}
