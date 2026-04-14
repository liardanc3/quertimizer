package com.quertimizer.entity;

import com.quertimizer.constant.DbmsType;
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
public class ProblemSubmitHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submit_id", nullable = false)
    private Long submitId;

    @Column(name = "problem_id", nullable = false, length = 11)
    private String problemId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

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

    @Column(name = "row_count", nullable = false)
    private long rowCount;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    public static ProblemSubmitHistory create(String problemId,
                                              String userId,
                                              DbmsType dbmsType,
                                              String submittedSql,
                                              boolean success,
                                              String message,
                                              long executionTimeMs,
                                              long rowCount,
                                              LocalDateTime submittedAt) {
        return new ProblemSubmitHistory(
                problemId,
                userId,
                dbmsType,
                submittedSql,
                success,
                message,
                executionTimeMs,
                rowCount,
                submittedAt
        );
    }

    private ProblemSubmitHistory(String problemId,
                                 String userId,
                                 DbmsType dbmsType,
                                 String submittedSql,
                                 boolean success,
                                 String message,
                                 long executionTimeMs,
                                 long rowCount,
                                 LocalDateTime submittedAt) {
        this.problemId = problemId;
        this.userId = userId;
        this.dbmsType = dbmsType;
        this.submittedSql = submittedSql;
        this.success = success;
        this.message = message;
        this.executionTimeMs = executionTimeMs;
        this.rowCount = rowCount;
        this.submittedAt = submittedAt;
    }

}
