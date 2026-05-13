package com.quertimizer.user.domain.entity;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.user.domain.model.UserRole;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
public class User {

    private String email;
    private String handle;
    private String password;
    private String bio;
    private String profileImageUrl;
    private String backgroundImageUrl;
    private UserRole role;
    private DbmsType defaultDbms;
    private Boolean sqlPublic;
    private Boolean executionPercentilePublic;
    private Boolean solvedRecordsPublic;
    private Boolean solvedProblemCountPublic;
    private Boolean communityActivityPublic;
    private Integer solvedProblemCount;
    private Long solvedExecutionTimeSumMs;
    private LocalDateTime signupAt;
    private String lastAccessIp;
    private LocalDateTime lastAccessAt;
    private Boolean blockedUser;
    private LocalDateTime blockedAt;

    public static User create(String handle, String encodedPassword, String email) {
        // 일반 회원가입 완료 사용자 기본 공개 설정과 기본 DBMS 초기화
        return new User(
                email,
                handle,
                encodedPassword,
                "",
                "",
                "",
                UserRole.USER,
                DbmsType.POSTGRESQL,
                true,
                true,
                true,
                true,
                true,
                0,
                0L,
                LocalDateTime.now(),
                null,
                null,
                false,
                null
        );
    }

    public static User create(String encodedPassword, String email) {
        // 최초 생성 시 Handle 미설정 사용자는 handle 없이 시작
        return new User(
                email,
                null,
                encodedPassword,
                "",
                "",
                "",
                UserRole.USER,
                DbmsType.POSTGRESQL,
                true,
                true,
                true,
                true,
                true,
                0,
                0L,
                LocalDateTime.now(),
                null,
                null,
                false,
                null
        );
    }

    public static User restore(String email, String handle, String password,
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
        // 저장된 사용자 상태 복원
        return new User(
                email, handle, password, bio, profileImageUrl, backgroundImageUrl,
                role, defaultDbms, sqlPublic, executionPercentilePublic,
                solvedRecordsPublic, solvedProblemCountPublic, communityActivityPublic,
                solvedProblemCount, solvedExecutionTimeSumMs, signupAt,
                lastAccessIp, lastAccessAt, blockedUser, blockedAt
        );
    }

    public void configureHandle(String handle) {
        // 최초 로그인 직후 강제되는 Handle 설정에서만 handle 반영
        this.handle = handle;
    }

    public void changePassword(String password) {
        // 비밀번호 찾기 완료 후 이중 해시된 비밀번호로 교체
        this.password = password;
    }

    public void changeRole(UserRole role) {
        // 권한 설정 페이지에서 사용자 역할 변경
        this.role = role;
    }

    public void changeProfile(String bio,
                              String profileImageUrl,
                              String backgroundImageUrl,
                              DbmsType defaultDbms,
                              boolean sqlPublic,
                              boolean executionPercentilePublic,
                              boolean solvedRecordsPublic,
                              boolean solvedProblemCountPublic,
                              boolean communityActivityPublic) {
        // 프로필 공개 설정과 기본 DBMS 일괄 갱신
        this.bio = bio;
        this.profileImageUrl = profileImageUrl;
        this.backgroundImageUrl = backgroundImageUrl;
        this.defaultDbms = defaultDbms;
        this.sqlPublic = sqlPublic;
        this.executionPercentilePublic = executionPercentilePublic;
        this.solvedRecordsPublic = solvedRecordsPublic;
        this.solvedProblemCountPublic = solvedProblemCountPublic;
        this.communityActivityPublic = communityActivityPublic;
    }

    public void changeSolvedStatistics(int solvedProblemCount, long solvedExecutionTimeSumMs) {
        // 프로필 집계 화면에서 사용하는 해결 수와 실행 시간 누적값 반영
        this.solvedProblemCount = solvedProblemCount;
        this.solvedExecutionTimeSumMs = solvedExecutionTimeSumMs;
    }

    public void updateLastAccess(String accessIp, LocalDateTime accessedAt) {
        // 같은 IP의 짧은 반복 호출에서는 마지막 접속 정보 갱신 생략
        if (accessIp == null || accessIp.isBlank()) {
            return;
        }

        if (Objects.equals(lastAccessIp, accessIp)
                && lastAccessAt != null
                && !lastAccessAt.isBefore(accessedAt.minusMinutes(1))) {
            return;
        }

        this.lastAccessIp = accessIp;
        this.lastAccessAt = accessedAt;
    }

    public void block() {
        // 사용자 차단 시 차단 여부와 시점 기록
        this.blockedUser = true;
        this.blockedAt = LocalDateTime.now();
    }

    public void unblock() {
        // 차단 해제 시 차단 상태와 시점 초기화
        this.blockedUser = false;
        this.blockedAt = null;
    }

    public String getResolvedBio() {
        // 프로필 응답에서는 null 대신 빈 문자열 사용
        return bio != null ? bio : "";
    }

    public String getResolvedProfileImageUrl() {
        // 프로필 이미지 응답에서는 null 대신 빈 문자열 사용
        return profileImageUrl != null ? profileImageUrl : "";
    }

    public String getResolvedBackgroundImageUrl() {
        // 프로필 배경 이미지 응답에서는 null 대신 빈 문자열 사용
        return backgroundImageUrl != null ? backgroundImageUrl : "";
    }

    public UserRole getResolvedRole() {
        // 역할이 비어 있는 오래된 데이터는 일반 사용자로 간주
        return role != null ? role : UserRole.USER;
    }

    public DbmsType getResolvedDefaultDbms() {
        // 기본 DBMS가 비어 있는 오래된 데이터는 PostgreSQL로 간주
        return defaultDbms != null ? defaultDbms : DbmsType.POSTGRESQL;
    }

    public boolean isSqlPublicEnabled() {
        // SQL 공개 설정은 null이면 공개 상태로 해석
        return sqlPublic == null || Boolean.TRUE.equals(sqlPublic);
    }

    public boolean isExecutionPercentilePublicEnabled() {
        // 실행 백분위 공개 설정은 null이면 공개 상태로 해석
        return executionPercentilePublic == null || Boolean.TRUE.equals(executionPercentilePublic);
    }

    public boolean isSolvedRecordsPublicEnabled() {
        // 풀이 기록 목록 공개 설정은 null이면 공개 상태로 해석
        return solvedRecordsPublic == null || Boolean.TRUE.equals(solvedRecordsPublic);
    }

    public boolean isSolvedProblemCountPublicEnabled() {
        // 해결한 문제 수 공개 설정은 null이면 공개 상태로 해석
        return solvedProblemCountPublic == null || Boolean.TRUE.equals(solvedProblemCountPublic);
    }

    public boolean isCommunityActivityPublicEnabled() {
        // 커뮤니티 활동 공개 설정은 null이면 공개 상태로 해석
        return communityActivityPublic == null || Boolean.TRUE.equals(communityActivityPublic);
    }

    public int getResolvedSolvedProblemCount() {
        // 통계 응답에서 null-safe 정수 값 보장
        return solvedProblemCount != null ? solvedProblemCount : 0;
    }

    public long getResolvedSolvedExecutionTimeSumMs() {
        // 통계 응답에서 null-safe 실행 시간 누적값 보장
        return solvedExecutionTimeSumMs != null ? solvedExecutionTimeSumMs : 0L;
    }

    public boolean hasHandle() {
        // 가입 직후 사용할 handle 설정 완료 여부 판단
        return handle != null && !handle.isBlank();
    }

    public boolean isBlocked() {
        // 차단 여부는 nullable boolean 대신 null-safe 도메인 메서드로 판단
        return Boolean.TRUE.equals(blockedUser);
    }

    private User(String email,
                 String handle,
                 String password,
                 String bio,
                 String profileImageUrl,
                 String backgroundImageUrl,
                 UserRole role,
                 DbmsType defaultDbms,
                 Boolean sqlPublic,
                 Boolean executionPercentilePublic,
                 Boolean solvedRecordsPublic,
                 Boolean solvedProblemCountPublic,
                 Boolean communityActivityPublic,
                 Integer solvedProblemCount,
                 Long solvedExecutionTimeSumMs,
                 LocalDateTime signupAt,
                 String lastAccessIp,
                 LocalDateTime lastAccessAt,
                 Boolean blockedUser,
                 LocalDateTime blockedAt) {
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
