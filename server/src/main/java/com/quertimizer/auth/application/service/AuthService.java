package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.input.SendCodeInput;
import com.quertimizer.auth.application.input.ResetPasswordInput;
import com.quertimizer.auth.application.input.SetupHandleInput;
import com.quertimizer.auth.application.input.VerifyCodeInput;
import com.quertimizer.auth.application.port.AuthMailSender;
import com.quertimizer.auth.application.port.VerificationCodeRepository;
import com.quertimizer.auth.domain.policy.LoginPolicy;
import com.quertimizer.auth.domain.policy.SignupPolicy;
import com.quertimizer.user.domain.entity.User;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.global.lock.Lock;
import com.quertimizer.global.lock.LockKey;
import com.quertimizer.user.application.port.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.quertimizer.auth.domain.model.AccountRecoveryFailReason.EMAIL_NOT_FOUND;
import static com.quertimizer.auth.domain.model.AccountRecoveryFailReason.EXPIRED_VERIFICATION_CODE;
import static com.quertimizer.auth.domain.model.AccountRecoveryFailReason.INVALID_VERIFICATION_CODE;
import static com.quertimizer.auth.domain.model.AccountRecoveryFailReason.PASSWORD_RESET_VERIFICATION_REQUIRED;
import static com.quertimizer.auth.domain.model.AccountRecoveryFailReason.VERIFICATION_EMAIL_SEND_FAILED;
import static com.quertimizer.auth.domain.model.AuthFailReason.HANDLE_ALREADY_CONFIGURED;
import static com.quertimizer.auth.domain.model.AuthFailReason.USER_NOT_FOUND;
import static com.quertimizer.auth.domain.model.LoginFailReason.INVALID_EMAIL_OR_PASSWORD;
import static com.quertimizer.auth.domain.model.SignupFailReason.DUPLICATED_HANDLE;
import static com.quertimizer.auth.domain.model.SignupFailReason.SIGNUP_VERIFICATION_REQUIRED;
import static com.quertimizer.auth.infrastructure.mail.MailConstant.FIND_PASSWORD_CODE_DESCRIPTION;
import static com.quertimizer.auth.infrastructure.mail.MailConstant.FIND_PASSWORD_CODE_SUBJECT;
import static com.quertimizer.auth.infrastructure.mail.MailConstant.FIND_PASSWORD_CODE_TITLE;
import static com.quertimizer.auth.infrastructure.mail.MailConstant.SIGNUP_CODE_DESCRIPTION;
import static com.quertimizer.auth.infrastructure.mail.MailConstant.SIGNUP_CODE_SUBJECT;
import static com.quertimizer.auth.infrastructure.mail.MailConstant.SIGNUP_CODE_TITLE;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthMailSender authMailSender;
    private final VerificationCodeRepository verificationCodeRepository;
    private final LoginService loginService;
    private final LoginPolicy loginPolicy;
    private final SignupPolicy signupPolicy;

    /**
     * 만료된 인증코드를 스케줄러로 정리한다.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void deleteExpiredRecoveryCode() {
        verificationCodeRepository.deleteExpired(LocalDateTime.now());
    }

    /**
     * 회원가입용 인증코드를 발급하고 메일로 전송한다.
     *
     * <ol>
     *   <li>인증코드 발급
     *   <li>메일 전송
     * </ol>
     *
     * @param targetEmail 인증코드를 받을 이메일
     */
    public void sendSignupCode(String targetEmail) {
        String code = issueVerificationCode(targetEmail);

        try {
            authMailSender.sendAuthCodeMail(targetEmail, SIGNUP_CODE_SUBJECT, SIGNUP_CODE_TITLE, SIGNUP_CODE_DESCRIPTION, code);
        } catch (RuntimeException exception) {
            clearVerificationCode(targetEmail);
            throw new BusinessException(VERIFICATION_EMAIL_SEND_FAILED.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Optional<User> findUserByEmail(String email) {
        // 인증 전반에서 이메일을 사용자 식별자처럼 사용하므로 항상 normalize 후 조회
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email));
    }

    public Optional<User> findUserByHandle(String handle) {
        // 프로필, Handle 설정, 검색 등 handle 기반 진입점용 조회
        return userRepository.findByHandle(normalizeHandle(handle));
    }

    public String resolveCurrentHandle(String authenticatedEmail) {
        // 인증 이메일을 현재 Handle로 매핑
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            return null;
        }

        return findUserByEmail(authenticatedEmail)
                .map(User::getHandle)
                .filter(handle -> !handle.isBlank())
                .orElse(null);
    }

    public Optional<User> findAuthenticatedUser(String authenticatedEmail) {
        // 세션 복원 응답, 권한 확인, 프로필 후속 처리에서 공통으로 쓰는 현재 사용자 조회
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            return Optional.empty();
        }

        return findUserByEmail(authenticatedEmail);
    }

    @Lock(prefix = LockKey.SIGNUP, key = "#p0", timeout = 500)
    public User configureHandle(String authenticatedEmail, SetupHandleInput input) {
        // 최초 로그인 사용자의 Handle 설정
        User user = findUserByEmail(authenticatedEmail)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
        String handle = input.getHandle();

        // 이미 설정된 Handle은 변경 불가
        if (user.hasHandle()) {
            throw new BusinessException(HANDLE_ALREADY_CONFIGURED.getMessage(), HttpStatus.CONFLICT);
        }
        // 다른 사용자가 먼저 선점했는지 최종 중복검사
        if (userRepository.existsByHandle(handle)) {
            throw new BusinessException(DUPLICATED_HANDLE.getMessage(), HttpStatus.CONFLICT);
        }

        // 엔티티 내부에서 handle 및 설정 완료 상태를 갱신
        user.configureHandle(handle);
        return user;
    }

    public Authentication loginWithOAuth2(String provider, Map<String, Object> attributes, String accessIp) {
        // OAuth2 인증결과를 내부 인증객체로 변환
        User user = findOrCreateOAuth2User(provider, attributes);
        loginPolicy.validateBlockedUser(user.getEmail());
        loginService.updateLastAccess(user.getEmail(), accessIp);

        return loginService.getAuthentication(user);
    }

    public void verifySignupCode(VerifyCodeInput input) {
        // 회원가입 가능 이메일, 인증코드 유효성 확인 후 verify 완료 상태 기록
        signupPolicy.validateAvailableEmail(input.getEmail());
        String email = input.getEmail();
        validateVerificationCode(email, input.getCode());
        verificationCodeRepository.markVerified(email);
    }

    /**
     * 비밀번호 찾기 인증코드를 발급하고 메일로 전송한다.
     *
     * <ol>
     *   <li>가입 이메일 조회와 인증코드 발급
     *   <li>메일 전송
     * </ol>
     *
     * @param input 인증코드를 받을 이메일 입력
     */
    public void sendFindPasswordCode(SendCodeInput input) {
        User user = userRepository.findByEmailIgnoreCase(input.getEmail())
                .orElseThrow(() -> new BusinessException(EMAIL_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
        String email = user.getEmail().toLowerCase(Locale.ROOT);
        String code = issueVerificationCode(email);

        try {
            authMailSender.sendAuthCodeMail(user.getEmail(),
                    FIND_PASSWORD_CODE_SUBJECT, FIND_PASSWORD_CODE_TITLE, FIND_PASSWORD_CODE_DESCRIPTION, code);
        } catch (RuntimeException exception) {
            clearVerificationCode(email);
            throw new BusinessException(VERIFICATION_EMAIL_SEND_FAILED.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void verifyFindPasswordCode(VerifyCodeInput input) {
        // 비밀번호 재설정 가능 상태로 전환
        userRepository.findByEmailIgnoreCase(input.getEmail())
                .orElseThrow(() -> new BusinessException(EMAIL_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
        String email = input.getEmail();
        validateVerificationCode(email, input.getCode());
        verificationCodeRepository.markVerified(email);
    }

    public void resetPassword(ResetPasswordInput input) {
        // 인증코드 검사여부 검증 후 인증코드 파기
        validateVerifiedEmail(input.getEmail(), PASSWORD_RESET_VERIFICATION_REQUIRED.getMessage());
        clearVerificationCode(input.getEmail());

        // 비밀번호 변경
        userRepository.findByEmail(input.getEmail())
                      .ifPresentOrElse(
                              user -> user.changePassword(passwordEncoder.encode(input.getPassword())),
                              () -> { throw new BusinessException(EMAIL_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND);}
                      );
    }

    public void validateVerifiedSignupCode(String email, String code) {
        // verify-code 단계까지 끝난 가입 이메일인지 최종 확인
        validateVerifiedEmail(email, SIGNUP_VERIFICATION_REQUIRED.getMessage());
    }

    public void clearVerifiedSignupCode(String email, String code) {
        // 회원가입 완료 또는 메일 발송 실패 시 가입 인증코드와 인증상태를 함께 정리
        clearVerificationCode(email);
    }

    private void validateVerificationCode(String email, String code) {
        // 인증코드 유효성 검증
        String savedCode = verificationCodeRepository.findCode(email).orElse(null);
        LocalDateTime expiredAt = verificationCodeRepository.findExpiredAt(email).orElse(null);

        if (savedCode == null || expiredAt == null || !savedCode.equals(code)) {
            throw new BusinessException(INVALID_VERIFICATION_CODE.getMessage(), HttpStatus.BAD_REQUEST);
        }
        if (expiredAt.isBefore(LocalDateTime.now())) {
            clearVerificationCode(email);
            throw new BusinessException(EXPIRED_VERIFICATION_CODE.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private void validateVerifiedEmail(String email, String message) {
        // 인증 완료 이메일 상태 검증
        LocalDateTime expiredAt = verificationCodeRepository.findExpiredAt(email).orElse(null);

        if (expiredAt != null && expiredAt.isBefore(LocalDateTime.now())) {
            clearVerificationCode(email);
        }
        if (!verificationCodeRepository.isVerified(email) || expiredAt == null) {
            throw new BusinessException(message, HttpStatus.BAD_REQUEST);
        }
    }

    private String issueVerificationCode(String email) {
        // 인증코드 발급 후 저장
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);

        verificationCodeRepository.saveCode(email, code, LocalDateTime.now().plusMinutes(5));
        return code;
    }

    private void clearVerificationCode(String email) {
        // 인증코드 상태 정리
        verificationCodeRepository.clear(email);
    }

    private User findOrCreateOAuth2User(String provider, Map<String, Object> attributes) {
        // OAuth2 사용자 계정을 조회 또는 생성
        String oauth2ProviderId = resolveOAuth2ProviderId(provider, attributes);
        String resolvedEmail = resolveOAuth2Email(provider, attributes, oauth2ProviderId);

        return userRepository.findById(resolvedEmail)
                .orElseGet(() -> userRepository.save(
                        User.create(
                                passwordEncoder.encode(UUID.randomUUID().toString()),
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
