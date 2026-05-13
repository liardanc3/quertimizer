package com.quertimizer.auth.domain.model;

import com.quertimizer.user.domain.model.UserRole;
import com.quertimizer.judge.domain.model.DbmsType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@AllArgsConstructor
public class AuthUser {

    private String email;
    private String handle;
    private String password;
    private UserRole role;
    private DbmsType defaultDbms;
    private String lastAccessIp;
    private LocalDateTime lastAccessAt;
    private Boolean blockedUser;
    private LocalDateTime blockedAt;

    public static AuthUser create(String encodedPassword, String email) {
        // 인증 관점의 신규 사용자 기본 상태 생성
        return new AuthUser(email, null, encodedPassword, UserRole.USER, DbmsType.POSTGRESQL, null, null, false, null);
    }

    public void configureHandle(String handle) {
        // 가입 직후 handle 설정
        this.handle = handle;
    }

    public void changePassword(String password) {
        // 계정 복구 후 비밀번호 변경
        this.password = password;
    }

    public void changeRole(UserRole role) {
        // 관리자 권한 관리에서 역할 변경
        this.role = role;
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

    public UserRole getResolvedRole() {
        // 역할이 비어 있는 오래된 데이터는 일반 사용자로 간주
        return role != null ? role : UserRole.USER;
    }

    public DbmsType getResolvedDefaultDbms() {
        // 기본 DBMS가 비어 있는 오래된 데이터는 PostgreSQL로 간주
        return defaultDbms != null ? defaultDbms : DbmsType.POSTGRESQL;
    }

    public boolean hasHandle() {
        // handle 설정 완료 여부 판단
        return handle != null && !handle.isBlank();
    }

    public boolean isBlocked() {
        // 차단 여부 null-safe 판단
        return Boolean.TRUE.equals(blockedUser);
    }

}
