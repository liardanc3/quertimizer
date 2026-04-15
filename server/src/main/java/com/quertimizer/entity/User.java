package com.quertimizer.entity;

import com.quertimizer.constant.DbmsType;
import com.quertimizer.constant.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "user_id", unique = true, length = 50)
    private String userId;

    @Column(nullable = false, length = 128)
    private String password;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_dbms", length = 20)
    private DbmsType defaultDbms;

    @Column(name = "sql_public")
    private Boolean sqlPublic;

    @Column(name = "execution_percentile_public")
    private Boolean executionPercentilePublic;

    @Column(name = "solved_records_public")
    private Boolean solvedRecordsPublic;

    @Column(name = "solved_problem_count_public")
    private Boolean solvedProblemCountPublic;

    @Column(name = "solved_problem_count")
    private Integer solvedProblemCount;

    @Column(name = "solved_execution_time_sum_ms")
    private Long solvedExecutionTimeSumMs;

    @Column(name = "signup_at", nullable = false)
    private LocalDateTime signupAt;

    public static User create(String userId, String doubleHashedPassword, String email) {
        return new User(
                email,
                userId,
                doubleHashedPassword,
                "",
                UserRole.USER,
                DbmsType.POSTGRESQL,
                false,
                true,
                true,
                true,
                0,
                0L,
                LocalDateTime.now()
        );
    }

    public static User createPending(String doubleHashedPassword, String email) {
        return new User(
                email,
                null,
                doubleHashedPassword,
                "",
                UserRole.USER,
                DbmsType.POSTGRESQL,
                false,
                true,
                true,
                true,
                0,
                0L,
                LocalDateTime.now()
        );
    }

    public void configureUserId(String userId) {
        this.userId = userId;
    }

    public void changePassword(String password) {
        this.password = password;
    }

    public void changeRole(UserRole role) {
        this.role = role;
    }

    public void changeProfile(String bio,
                              DbmsType defaultDbms,
                              boolean sqlPublic,
                              boolean executionPercentilePublic,
                              boolean solvedRecordsPublic,
                              boolean solvedProblemCountPublic) {
        this.bio = bio;
        this.defaultDbms = defaultDbms;
        this.sqlPublic = sqlPublic;
        this.executionPercentilePublic = executionPercentilePublic;
        this.solvedRecordsPublic = solvedRecordsPublic;
        this.solvedProblemCountPublic = solvedProblemCountPublic;
    }

    public void changeSolvedStatistics(int solvedProblemCount, long solvedExecutionTimeSumMs) {
        this.solvedProblemCount = solvedProblemCount;
        this.solvedExecutionTimeSumMs = solvedExecutionTimeSumMs;
    }

    public String getResolvedBio() {
        return bio != null ? bio : "";
    }

    public UserRole getResolvedRole() {
        return role != null ? role : UserRole.USER;
    }

    public DbmsType getResolvedDefaultDbms() {
        return defaultDbms != null ? defaultDbms : DbmsType.POSTGRESQL;
    }

    public boolean isSqlPublicEnabled() {
        return Boolean.TRUE.equals(sqlPublic);
    }

    public boolean isExecutionPercentilePublicEnabled() {
        return Boolean.TRUE.equals(executionPercentilePublic);
    }

    public boolean isSolvedRecordsPublicEnabled() {
        return Boolean.TRUE.equals(solvedRecordsPublic);
    }

    public boolean isSolvedProblemCountPublicEnabled() {
        return Boolean.TRUE.equals(solvedProblemCountPublic);
    }

    public int getResolvedSolvedProblemCount() {
        return solvedProblemCount != null ? solvedProblemCount : 0;
    }

    public long getResolvedSolvedExecutionTimeSumMs() {
        return solvedExecutionTimeSumMs != null ? solvedExecutionTimeSumMs : 0L;
    }

    public boolean hasUserId() {
        return userId != null && !userId.isBlank();
    }

    private User(String email,
                 String userId,
                 String password,
                 String bio,
                 UserRole role,
                 DbmsType defaultDbms,
                 Boolean sqlPublic,
                 Boolean executionPercentilePublic,
                 Boolean solvedRecordsPublic,
                 Boolean solvedProblemCountPublic,
                 Integer solvedProblemCount,
                 Long solvedExecutionTimeSumMs,
                 LocalDateTime signupAt) {
        this.email = email;
        this.userId = userId;
        this.password = password;
        this.bio = bio;
        this.role = role;
        this.defaultDbms = defaultDbms;
        this.sqlPublic = sqlPublic;
        this.executionPercentilePublic = executionPercentilePublic;
        this.solvedRecordsPublic = solvedRecordsPublic;
        this.solvedProblemCountPublic = solvedProblemCountPublic;
        this.solvedProblemCount = solvedProblemCount;
        this.solvedExecutionTimeSumMs = solvedExecutionTimeSumMs;
        this.signupAt = signupAt;
    }

}
