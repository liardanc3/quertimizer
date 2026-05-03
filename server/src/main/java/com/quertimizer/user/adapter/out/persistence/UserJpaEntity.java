package com.quertimizer.user.adapter.out.persistence;

import com.quertimizer.global.constant.UserRole;
import com.quertimizer.judge.domain.model.DbmsType;
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
public class UserJpaEntity {

    @Id
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "handle", unique = true, length = 50)
    private String handle;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "profile_image_url", length = 512)
    private String profileImageUrl;

    @Column(name = "background_image_url", length = 512)
    private String backgroundImageUrl;

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

    @Column(name = "community_activity_public")
    private Boolean communityActivityPublic;

    @Column(name = "solved_problem_count")
    private Integer solvedProblemCount;

    @Column(name = "solved_execution_time_sum_ms")
    private Long solvedExecutionTimeSumMs;

    @Column(name = "signup_at", nullable = false)
    private LocalDateTime signupAt;

    @Column(name = "last_access_ip", length = 64)
    private String lastAccessIp;

    @Column(name = "last_access_at")
    private LocalDateTime lastAccessAt;

    @Column(name = "blocked_user")
    private Boolean blockedUser;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    public static UserJpaEntity create(String email, String handle, String password,
                                       String bio, String profileImageUrl,
                                       String backgroundImageUrl, UserRole role,
                                       DbmsType defaultDbms, Boolean sqlPublic,
                                       Boolean executionPercentilePublic,
                                       Boolean solvedRecordsPublic,
                                       Boolean solvedProblemCountPublic,
                                       Boolean communityActivityPublic,
                                       Integer solvedProblemCount,
                                       Long solvedExecutionTimeSumMs,
                                       LocalDateTime signupAt, String lastAccessIp,
                                       LocalDateTime lastAccessAt,
                                       Boolean blockedUser, LocalDateTime blockedAt) {
        // 사용자 JPA 엔티티 생성
        return new UserJpaEntity(
                email, handle, password, bio, profileImageUrl, backgroundImageUrl,
                role, defaultDbms, sqlPublic, executionPercentilePublic,
                solvedRecordsPublic, solvedProblemCountPublic, communityActivityPublic,
                solvedProblemCount, solvedExecutionTimeSumMs, signupAt,
                lastAccessIp, lastAccessAt, blockedUser, blockedAt
        );
    }

    public void update(String handle, String password, String bio,
                       String profileImageUrl, String backgroundImageUrl,
                       UserRole role, DbmsType defaultDbms,
                       Boolean sqlPublic, Boolean executionPercentilePublic,
                       Boolean solvedRecordsPublic,
                       Boolean solvedProblemCountPublic,
                       Boolean communityActivityPublic,
                       Integer solvedProblemCount,
                       Long solvedExecutionTimeSumMs,
                       String lastAccessIp, LocalDateTime lastAccessAt,
                       Boolean blockedUser, LocalDateTime blockedAt) {
        // 사용자 JPA 엔티티 내용 변경
        this.handle = handle;
        this.password = password;
        this.bio = bio;
        this.profileImageUrl = profileImageUrl;
        this.backgroundImageUrl = backgroundImageUrl;
        this.role = role;
        this.defaultDbms = defaultDbms;
        this.sqlPublic = sqlPublic;
        this.executionPercentilePublic = executionPercentilePublic;
        this.solvedRecordsPublic = solvedRecordsPublic;
        this.solvedProblemCountPublic = solvedProblemCountPublic;
        this.communityActivityPublic = communityActivityPublic;
        this.solvedProblemCount = solvedProblemCount;
        this.solvedExecutionTimeSumMs = solvedExecutionTimeSumMs;
        this.lastAccessIp = lastAccessIp;
        this.lastAccessAt = lastAccessAt;
        this.blockedUser = blockedUser;
        this.blockedAt = blockedAt;
    }

    private UserJpaEntity(String email, String handle, String password,
                          String bio, String profileImageUrl,
                          String backgroundImageUrl, UserRole role,
                          DbmsType defaultDbms, Boolean sqlPublic,
                          Boolean executionPercentilePublic,
                          Boolean solvedRecordsPublic,
                          Boolean solvedProblemCountPublic,
                          Boolean communityActivityPublic,
                          Integer solvedProblemCount,
                          Long solvedExecutionTimeSumMs,
                          LocalDateTime signupAt, String lastAccessIp,
                          LocalDateTime lastAccessAt,
                          Boolean blockedUser, LocalDateTime blockedAt) {
        this.email = email;
        this.handle = handle;
        this.password = password;
        this.bio = bio;
        this.profileImageUrl = profileImageUrl;
        this.backgroundImageUrl = backgroundImageUrl;
        this.role = role;
        this.defaultDbms = defaultDbms;
        this.sqlPublic = sqlPublic;
        this.executionPercentilePublic = executionPercentilePublic;
        this.solvedRecordsPublic = solvedRecordsPublic;
        this.solvedProblemCountPublic = solvedProblemCountPublic;
        this.communityActivityPublic = communityActivityPublic;
        this.solvedProblemCount = solvedProblemCount;
        this.solvedExecutionTimeSumMs = solvedExecutionTimeSumMs;
        this.signupAt = signupAt;
        this.lastAccessIp = lastAccessIp;
        this.lastAccessAt = lastAccessAt;
        this.blockedUser = blockedUser;
        this.blockedAt = blockedAt;
    }
}
