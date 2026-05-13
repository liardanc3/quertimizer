package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.port.out.VerificationCodeRepositoryPort;
import com.quertimizer.auth.domain.model.AuthUser;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.auth.application.port.out.AuthUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import com.quertimizer.auth.application.port.out.PasswordEncodingPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.quertimizer.auth.domain.model.AccountRecoveryFailReason.EXPIRED_VERIFICATION_CODE;
import static com.quertimizer.auth.domain.model.AccountRecoveryFailReason.INVALID_VERIFICATION_CODE;
import static com.quertimizer.auth.domain.model.AuthFailReason.VERIFICATION_FAILURE_LIMIT_EXCEEDED;
import static com.quertimizer.auth.domain.model.LoginFailReason.INVALID_EMAIL_OR_PASSWORD;
import static com.quertimizer.auth.domain.model.SignupFailReason.SIGNUP_VERIFICATION_REQUIRED;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final AuthUserPort userRepository;
    private final PasswordEncodingPort passwordEncodingPort;
    private final VerificationCodeRepositoryPort verificationCodeRepository;
    private final AuthRateLimitService authRateLimitPolicy;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void deleteExpiredRecoveryCode() {
        verificationCodeRepository.deleteExpired(LocalDateTime.now());
    }

    public Optional<AuthUser> findUserByEmail(String email) {
        // 인증 전반에서 이메일을 사용자 식별자처럼 사용하므로 항상 normalize 후 조회
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email));
    }

    public Optional<AuthUser> findUserByHandle(String handle) {
        // 프로필, Handle 설정, 검색 등 handle 기반 진입점용 조회
        return userRepository.findByHandle(normalizeHandle(handle));
    }

    public String resolveCurrentHandle(String authenticatedEmail) {
        // 인증 이메일을 현재 Handle로 매핑
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            return null;
        }

        return findUserByEmail(authenticatedEmail)
                .map(AuthUser::getHandle)
                .filter(handle -> !handle.isBlank())
                .orElse(null);
    }

    public Optional<AuthUser> findAuthenticatedUser(String authenticatedEmail) {
        // 세션 복원 응답, 권한 확인, 프로필 후속 처리에서 공통으로 쓰는 현재 사용자 조회
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            return Optional.empty();
        }

        return findUserByEmail(authenticatedEmail);
    }

    public void validateVerifiedSignupCode(String email, String code) {
        // verify-code 단계까지 끝난 가입 이메일인지 최종 확인
        validateVerifiedEmail(email, SIGNUP_VERIFICATION_REQUIRED.getMessage());
    }

    public void clearVerifiedSignupCode(String email, String code) {
        // 회원가입 완료 또는 메일 발송 실패 시 가입 인증코드와 인증상태를 함께 정리
        clearVerificationCode(email);
    }

    public void validateVerificationCode(String email, String code, String clientIp, String purpose) {
        // 인증코드 유효성 검증
        String savedCode = verificationCodeRepository.findCode(email).orElse(null);
        LocalDateTime expiredAt = verificationCodeRepository.findExpiredAt(email).orElse(null);

        if (savedCode == null || expiredAt == null || !savedCode.equals(code)) {
            if (authRateLimitPolicy.recordCodeVerificationFailure(purpose, email, clientIp)) {
                clearVerificationCode(email);
                throw new BusinessException(VERIFICATION_FAILURE_LIMIT_EXCEEDED.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
            }
            throw new BusinessException(INVALID_VERIFICATION_CODE.getMessage(), HttpStatus.BAD_REQUEST);
        }
        if (expiredAt.isBefore(LocalDateTime.now())) {
            clearVerificationCode(email);
            throw new BusinessException(EXPIRED_VERIFICATION_CODE.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    public void validateVerifiedEmail(String email, String message) {
        // 인증 완료 이메일 상태 검증
        LocalDateTime expiredAt = verificationCodeRepository.findExpiredAt(email).orElse(null);

        if (expiredAt != null && expiredAt.isBefore(LocalDateTime.now())) {
            clearVerificationCode(email);
        }
        if (!verificationCodeRepository.isVerified(email) || expiredAt == null) {
            throw new BusinessException(message, HttpStatus.BAD_REQUEST);
        }
    }

    public String issueVerificationCode(String email) {
        // 인증코드 발급 후 저장
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);

        verificationCodeRepository.saveCode(email, code, LocalDateTime.now().plusMinutes(5));
        return code;
    }

    public void clearVerificationCode(String email) {
        // 인증코드 상태 정리
        verificationCodeRepository.clear(email);
    }

    public AuthUser findOrCreateOAuth2User(String provider, Map<String, Object> attributes) {
        // OAuth2 사용자 계정을 조회 또는 생성
        String oauth2ProviderId = resolveOAuth2ProviderId(provider, attributes);
        String resolvedEmail = resolveOAuth2Email(provider, attributes, oauth2ProviderId);

        return userRepository.findById(resolvedEmail)
                .orElseGet(() -> userRepository.save(
                        AuthUser.create(
                                passwordEncodingPort.encode(UUID.randomUUID().toString()),
                                resolvedEmail
                        )
                ));
    }

    private String resolveOAuth2ProviderId(String provider, Map<String, Object> attributes) {
        // provider별로 사용자 식별자로 쓰는 attribute key가 다르므로 분기 처리
        Object providerAccountId = switch (provider) {
            case "github" -> attributes.get("id");
            case "google" -> attributes.get("sub");
            case "kakao" -> attributes.get("id");
            default -> null;
        };
        if (providerAccountId == null) {
            throw new BusinessException(INVALID_EMAIL_OR_PASSWORD.getMessage(), HttpStatus.UNAUTHORIZED);
        }

        String resolvedProviderId = providerAccountId.toString().trim();
        if (resolvedProviderId.isEmpty()) {
            throw new BusinessException(INVALID_EMAIL_OR_PASSWORD.getMessage(), HttpStatus.UNAUTHORIZED);
        }

        return resolvedProviderId;
    }

    private String resolveOAuth2Email(String provider, Map<String, Object> attributes, String oauth2ProviderId) {
        // OAuth2 이메일 식별자 결정
        String rawEmail = Optional.ofNullable(extractOAuth2Email(provider, attributes))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElse(null);

        if (rawEmail != null) {
            return rawEmail;
        }

        return "%s_%s@users.quertimizer.local".formatted(provider, oauth2ProviderId);
    }

    @SuppressWarnings("unchecked")
    private String extractOAuth2Email(String provider, Map<String, Object> attributes) {
        // Kakao는 email이 최상위가 아니라 kakao_account 내부에 들어오므로 별도 처리
        if ("kakao".equals(provider)) {
            Object kakaoAccount = attributes.get("kakao_account");
            if (kakaoAccount instanceof Map<?, ?> kakaoAccountMap) {
                Object email = ((Map<String, Object>) kakaoAccountMap).get("email");
                return email != null ? email.toString() : null;
            }

            return null;
        }

        Object email = attributes.get("email");
        return email != null ? email.toString() : null;
    }

    public String normalizeEmail(String email) {
        // 이메일 공백 제거 + 소문자 변환
        return Optional.ofNullable(email)
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElse("");
    }

    private String normalizeHandle(String handle) {
        // Handle 공백 제거
        return Optional.ofNullable(handle)
                .map(String::trim)
                .orElse("");
    }

}
