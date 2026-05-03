package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.port.out.AuthRateLimitRepositoryPort;
import com.quertimizer.auth.domain.policy.AuthRateLimitPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;

import static com.quertimizer.auth.domain.model.AuthRateLimitConstant.CODE_HOUR_WINDOW;
import static com.quertimizer.auth.domain.model.AuthRateLimitConstant.CODE_MINUTE_WINDOW;
import static com.quertimizer.auth.domain.model.AuthRateLimitConstant.CODE_VERIFY_WINDOW;
import static com.quertimizer.auth.domain.model.AuthRateLimitConstant.LOGIN_LOCK_WINDOW;
import static com.quertimizer.auth.domain.model.AuthRateLimitConstant.PASSWORD_RESET_WINDOW;

@Service
@RequiredArgsConstructor
public class AuthRateLimitService {

    private final AuthRateLimitRepositoryPort authRateLimitRepository;
    private final AuthRateLimitPolicy authRateLimitPolicy;

    public void validateLoginAllowed(String email, String clientIp) {
        // 로그인 실패 기록 기준 현재 한도 초과 여부 검증
        Instant now = Instant.now();
        authRateLimitPolicy.validateLoginAllowed(authRateLimitRepository.count(loginKey(email, clientIp), LOGIN_LOCK_WINDOW, now));
    }

    public void recordLoginFailure(String email, String clientIp) {
        // 로그인 실패 시도 기록 후 한도 초과 여부 검증
        String key = loginKey(email, clientIp);
        Instant now = Instant.now();
        authRateLimitRepository.add(key, now);
        authRateLimitPolicy.validateLoginAllowed(authRateLimitRepository.count(key, LOGIN_LOCK_WINDOW, now));
    }

    public void clearLoginFailures(String email, String clientIp) {
        // 로그인 실패 기록 제거
        authRateLimitRepository.clear(loginKey(email, clientIp));
    }

    public void validateTooManyRequest(String email, String clientIp) {
        // 이메일과 IP 기준 인증코드 요청 제한 검증
        String normalizedEmail = normalizeEmail(email);
        String normalizedIp = normalizeIp(clientIp);
        Instant now = Instant.now();
        validateCodeIssueAllowed(normalizedEmail, now);
        validateCodeIssueAllowed(normalizedIp, now);

        // 인증코드 요청 기록
        authRateLimitRepository.add(normalizedEmail, now);
        authRateLimitRepository.add(normalizedIp, now);
    }

    public boolean recordCodeVerificationFailure(String purpose, String email, String clientIp) {
        // 인증코드 검증 실패 기록 후 한도 도달 여부 반환
        String key = codeVerifyKey(purpose, email, clientIp);
        Instant now = Instant.now();
        authRateLimitRepository.add(key, now);
        return authRateLimitPolicy.isCodeVerificationFailureLimited(authRateLimitRepository.count(key, CODE_VERIFY_WINDOW, now));
    }

    public void clearCodeVerificationFailures(String purpose, String email, String clientIp) {
        // 인증코드 검증 실패 기록 제거
        authRateLimitRepository.clear(codeVerifyKey(purpose, email, clientIp));
    }

    public void recordPasswordReset(String email, String clientIp) {
        // 비밀번호 재설정 요청 제한 검증 후 기록
        String key = passwordResetKey(email, clientIp);
        Instant now = Instant.now();
        authRateLimitPolicy.validatePasswordResetAllowed(authRateLimitRepository.count(key, PASSWORD_RESET_WINDOW, now));
        authRateLimitRepository.add(key, now);
    }

    private void validateCodeIssueAllowed(String key, Instant now) {
        // 인증코드 요청 키별 분당, 시간당 횟수 검증
        authRateLimitPolicy.validateCodeIssueAllowed(
                authRateLimitRepository.count(key, CODE_MINUTE_WINDOW, now),
                authRateLimitRepository.count(key, CODE_HOUR_WINDOW, now)
        );
    }

    private String loginKey(String email, String clientIp) {
        // 로그인 실패 제한 키 생성
        return "login:" + normalizeEmail(email) + ":" + normalizeIp(clientIp);
    }

    private String codeVerifyKey(String purpose, String email, String clientIp) {
        // 인증코드 검증 실패 제한 키 생성
        return "code-verify:" + normalizePurpose(purpose) + ":" + normalizeEmail(email) + ":" + normalizeIp(clientIp);
    }

    private String passwordResetKey(String email, String clientIp) {
        // 비밀번호 재설정 제한 키 생성
        return "password-reset:" + normalizeEmail(email) + ":" + normalizeIp(clientIp);
    }

    private String normalizePurpose(String purpose) {
        // 제한 목적 기본값 정규화
        return purpose == null || purpose.isBlank() ? "default" : purpose.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        // 제한 키용 이메일 정규화
        return email == null || email.isBlank() ? "unknown" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeIp(String clientIp) {
        // 제한 키용 IP 정규화
        return clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim();
    }
}
