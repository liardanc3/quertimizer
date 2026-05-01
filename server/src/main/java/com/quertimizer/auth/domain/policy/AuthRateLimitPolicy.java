package com.quertimizer.auth.domain.policy;

import com.quertimizer.auth.application.port.AuthRateLimitRepository;
import com.quertimizer.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class AuthRateLimitPolicy {

    private static final Duration LOGIN_LOCK_WINDOW = Duration.ofMinutes(10);
    private static final int LOGIN_FAILURE_LIMIT = 10;
    private static final Duration CODE_MINUTE_WINDOW = Duration.ofMinutes(1);
    private static final Duration CODE_HOUR_WINDOW = Duration.ofHours(1);
    private static final int CODE_MINUTE_LIMIT = 20;
    private static final int CODE_HOUR_LIMIT = 100;
    private static final Duration CODE_VERIFY_WINDOW = Duration.ofMinutes(10);
    private static final int CODE_VERIFY_FAILURE_LIMIT = 5;
    private static final Duration PASSWORD_RESET_WINDOW = Duration.ofMinutes(10);
    private static final int PASSWORD_RESET_LIMIT = 3;

    private final AuthRateLimitRepository authRateLimitRepository;

    /**
     * 로그인 시도 가능 여부를 검증한다.
     *
     * @param email 로그인 대상 이메일
     * @param clientIp 로그인 요청 IP
     */
    public void validateLoginAllowed(String email, String clientIp) {
        Instant now = Instant.now();
        if (authRateLimitRepository.count(loginKey(email, clientIp), LOGIN_LOCK_WINDOW, now) >= LOGIN_FAILURE_LIMIT) {
            throw tooManyRequests("로그인 실패 횟수가 많습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    /**
     * 로그인 실패를 기록하고 한도 초과 여부를 검증한다.
     *
     * @param email 로그인 대상 이메일
     * @param clientIp 로그인 요청 IP
     */
    public void recordLoginFailure(String email, String clientIp) {
        String key = loginKey(email, clientIp);
        Instant now = Instant.now();
        authRateLimitRepository.add(key, now);
        if (authRateLimitRepository.count(key, LOGIN_LOCK_WINDOW, now) >= LOGIN_FAILURE_LIMIT) {
            throw tooManyRequests("로그인 실패 횟수가 많습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    /**
     * 로그인 실패 기록을 제거한다.
     *
     * @param email 로그인 대상 이메일
     * @param clientIp 로그인 요청 IP
     */
    public void clearLoginFailures(String email, String clientIp) {
        authRateLimitRepository.clear(loginKey(email, clientIp));
    }

    /**
     * 인증코드 발급 시도 횟수를 기록하고 한도 초과 여부를 검증한다.
     *
     * @param purpose 인증코드 발급 목적
     * @param email 인증코드 발급 대상 이메일
     * @param clientIp 인증코드 발급 요청 IP
     */
    public void recordCodeIssue(String purpose, String email, String clientIp) {
        String emailKey = codeIssueEmailKey(purpose, email);
        String ipKey = codeIssueIpKey(purpose, clientIp);
        Instant now = Instant.now();
        if (isCodeIssueLimited(emailKey, now) || isCodeIssueLimited(ipKey, now)) {
            throw tooManyRequests("인증코드 요청이 많습니다. 잠시 후 다시 시도해 주세요.");
        }

        authRateLimitRepository.add(emailKey, now);
        authRateLimitRepository.add(ipKey, now);
    }

    /**
     * 인증코드 검증 실패를 기록하고 실패 한도 도달 여부를 반환한다.
     *
     * @param purpose 인증코드 검증 목적
     * @param email 인증코드 검증 대상 이메일
     * @param clientIp 인증코드 검증 요청 IP
     * @return 실패 한도 도달 여부
     */
    public boolean recordCodeVerificationFailure(String purpose, String email, String clientIp) {
        String key = codeVerifyKey(purpose, email, clientIp);
        Instant now = Instant.now();
        authRateLimitRepository.add(key, now);
        return authRateLimitRepository.count(key, CODE_VERIFY_WINDOW, now) >= CODE_VERIFY_FAILURE_LIMIT;
    }

    /**
     * 인증코드 검증 실패 기록을 제거한다.
     *
     * @param purpose 인증코드 검증 목적
     * @param email 인증코드 검증 대상 이메일
     * @param clientIp 인증코드 검증 요청 IP
     */
    public void clearCodeVerificationFailures(String purpose, String email, String clientIp) {
        authRateLimitRepository.clear(codeVerifyKey(purpose, email, clientIp));
    }

    /**
     * 비밀번호 재설정 시도 횟수를 기록하고 한도 초과 여부를 검증한다.
     *
     * @param email 비밀번호 재설정 대상 이메일
     * @param clientIp 비밀번호 재설정 요청 IP
     */
    public void recordPasswordReset(String email, String clientIp) {
        String key = passwordResetKey(email, clientIp);
        Instant now = Instant.now();
        if (authRateLimitRepository.count(key, PASSWORD_RESET_WINDOW, now) >= PASSWORD_RESET_LIMIT) {
            throw tooManyRequests("비밀번호 재설정 요청이 많습니다. 잠시 후 다시 시도해 주세요.");
        }

        authRateLimitRepository.add(key, now);
    }

    private boolean isCodeIssueLimited(String key, Instant now) {
        return authRateLimitRepository.count(key, CODE_MINUTE_WINDOW, now) >= CODE_MINUTE_LIMIT
                || authRateLimitRepository.count(key, CODE_HOUR_WINDOW, now) >= CODE_HOUR_LIMIT;
    }

    private BusinessException tooManyRequests(String message) {
        return new BusinessException(message, HttpStatus.TOO_MANY_REQUESTS);
    }

    private String loginKey(String email, String clientIp) {
        return "login:" + normalizeEmail(email) + ":" + normalizeIp(clientIp);
    }

    private String codeIssueEmailKey(String purpose, String email) {
        return "code-issue:email:" + normalizePurpose(purpose) + ":" + normalizeEmail(email);
    }

    private String codeIssueIpKey(String purpose, String clientIp) {
        return "code-issue:ip:" + normalizePurpose(purpose) + ":" + normalizeIp(clientIp);
    }

    private String codeVerifyKey(String purpose, String email, String clientIp) {
        return "code-verify:" + normalizePurpose(purpose) + ":" + normalizeEmail(email) + ":" + normalizeIp(clientIp);
    }

    private String passwordResetKey(String email, String clientIp) {
        return "password-reset:" + normalizeEmail(email) + ":" + normalizeIp(clientIp);
    }

    private String normalizePurpose(String purpose) {
        return purpose == null || purpose.isBlank() ? "default" : purpose.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? "unknown" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeIp(String clientIp) {
        return clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim();
    }
}
