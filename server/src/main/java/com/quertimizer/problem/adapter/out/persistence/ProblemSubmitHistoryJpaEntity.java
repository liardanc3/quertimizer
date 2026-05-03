package com.quertimizer.problem.adapter.out.persistence;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.domain.entity.ProblemSubmitHistory;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "problem_submit_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemSubmitHistoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submit_id", nullable = false)
    private Long submitId;

    @Column(name = "problem_id", nullable = false, length = 12)
    private String problemId;

    @Column(name = "handle", nullable = false, length = 50)
    private String handle;

    @Enumerated(EnumType.STRING)
    @Column(name = "dbms_type", length = 20)
    private DbmsType dbmsType;

    @Column(name = "submitted_sql", nullable = false, columnDefinition = "TEXT")
    private String submittedSql;

    @Column(nullable = false)
    private boolean success;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "execution_time_ms", nullable = false)
    private long executionTimeMs;

    @Column(nullable = false)
    private double cost;

    @Column(name = "row_count", nullable = false)
    private long rowCount;

    @Column(name = "execution_plan_element")
    private Long executionPlanElement;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    public ProblemSubmitHistoryJpaEntity(Long submitId, String problemId, String handle, DbmsType dbmsType,
                                         String submittedSql, boolean success, String message,
                                         long executionTimeMs, double cost, long rowCount,
                                         Long executionPlanElement, LocalDateTime submittedAt) {
        this.submitId = submitId;
        this.problemId = problemId;
        this.handle = handle;
        this.dbmsType = dbmsType;
        this.submittedSql = submittedSql;
        this.success = success;
        this.message = message;
        this.executionTimeMs = executionTimeMs;
        this.cost = cost;
        this.rowCount = rowCount;
        this.executionPlanElement = executionPlanElement;
        this.submittedAt = submittedAt;
    }

    public void updateFromDomain(ProblemSubmitHistory history) {
        this.problemId = history.getProblemId();
        this.handle = history.getHandle();
        this.dbmsType = history.getDbmsType();
        this.submittedSql = history.getSubmittedSql();
        this.success = history.isSuccess();
        this.message = history.getMessage();
        this.executionTimeMs = history.getExecutionTimeMs();
        this.cost = history.getCost();
        this.rowCount = history.getRowCount();
        this.executionPlanElement = history.getExecutionPlanElement();
        this.submittedAt = history.getSubmittedAt();
    }
}
