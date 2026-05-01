package com.quertimizer.problem.domain.entity;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.user.domain.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "problem_submit_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemSubmitHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submit_id", nullable = false)
    private Long submitId;

    @Column(name = "problem_id", nullable = false, length = 12)
    private String problemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", insertable = false, updatable = false)
    private Problem problem;

    @Column(name = "handle", nullable = false, length = 50)
    private String handle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "handle", referencedColumnName = "handle", insertable = false, updatable = false)
    private User user;

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

    public static ProblemSubmitHistory create(String problemId,
                                              String handle,
                                              DbmsType dbmsType,
                                              String submittedSql,
                                              boolean success,
                                              String message,
                                              long executionTimeMs,
                                              double cost,
                                              long rowCount,
                                              long executionPlanElement,
                                              LocalDateTime submittedAt) {
        return new ProblemSubmitHistory(
                problemId,
                handle,
                dbmsType,
                submittedSql,
                success,
                message,
                executionTimeMs,
                cost,
                rowCount,
                executionPlanElement,
                submittedAt
        );
    }

    private ProblemSubmitHistory(String problemId,
                                 String handle,
                                 DbmsType dbmsType,
                                 String submittedSql,
                                 boolean success,
                                 String message,
                                 long executionTimeMs,
                                 double cost,
                                 long rowCount,
                                 long executionPlanElement,
                                 LocalDateTime submittedAt) {
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

}
