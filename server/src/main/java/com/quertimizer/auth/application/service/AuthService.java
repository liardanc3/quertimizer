package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.input.AccountRecoveryCodeInput;
import com.quertimizer.auth.application.input.AccountRecoveryEmailInput;
import com.quertimizer.auth.application.input.ResetPasswordInput;
import com.quertimizer.auth.application.input.SetupHandleInput;
import com.quertimizer.auth.application.input.SignupInput;
import com.quertimizer.auth.application.result.FoundHandleResult;
import com.quertimizer.auth.domain.policy.LoginPolicy;
import com.quertimizer.user.domain.entity.User;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.global.lock.Lock;
import com.quertimizer.global.lock.LockKey;
import com.quertimizer.user.infrastructure.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.quertimizer.auth.domain.model.AccountRecoveryFailReason.EMAIL_NOT_FOUND;
import static com.quertimizer.auth.domain.model.AccountRecoveryFailReason.EXPIRED_VERIFICATION_CODE;
import static com.quertimizer.auth.domain.model.AccountRecoveryFailReason.INVALID_VERIFICATION_CODE;
import static com.quertimizer.auth.domain.model.AccountRecoveryFailReason.PASSWORD_RESET_VERIFICATION_REQUIRED;
import static com.quertimizer.auth.domain.model.AccountRecoveryFailReason.VERIFICATION_EMAIL_SEND_FAILED;
import static com.quertimizer.auth.domain.model.AuthFailReason.HANDLE_ALREADY_CONFIGURED;
import static com.quertimizer.auth.domain.model.AuthFailReason.USER_NOT_FOUND;
import static com.quertimizer.auth.domain.model.LoginFailReason.INVALID_EMAIL_OR_PASSWORD;
import static com.quertimizer.auth.domain.model.SignupFailReason.DUPLICATED_EMAIL;
import static com.quertimizer.auth.domain.model.SignupFailReason.DUPLICATED_HANDLE;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final MailService mailService;
    private final LoginService loginService;
    private final LoginPolicy loginPolicy;

    private final Map<String, String> emailCodeStorage = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> codeExpiredAtStorage = new ConcurrentHashMap<>();
    private final Map<String, String> verifiedFindPasswordCodeStorage = new ConcurrentHashMap<>();

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void deleteExpiredRecoveryCode() {
        // 메모리 기반 인증코드는 하루에 한 번 만료 정리한다.
        // email -> code, code -> expiredAt, 비밀번호 재설정 검증 완료 상태를 함께 청소해야
        // 서로 어긋난 상태가 남지 않는다.
        LocalDateTime now = LocalDateTime.now();
        codeExpiredAtStorage.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        emailCodeStorage.entrySet().removeIf(entry -> !codeExpiredAtStorage.containsKey(entry.getValue()));
        verifiedFindPasswordCodeStorage.entrySet().removeIf(entry -> !codeExpiredAtStorage.containsKey(entry.getValue()));
    }

    @Lock(prefix = LockKey.SIGNUP, key = "#p0.email", timeout = 500)
    public Authentication signup(SignupInput input) {
        // 이메일은 인증/중복검사 전부 소문자 기준으로 다룬다.
        String normalizedEmail = normalizeEmail(input.getEmail());

        // 동일 이메일의 동시 회원가입 요청을 락과 중복검사로 함께 막는다.
        if (isDuplicatedEmail(normalizedEmail)) {
            throw new BusinessException(DUPLICATED_EMAIL.getMessage(), HttpStatus.CONFLICT);
        }

        // 가입 직후 별도 로그인 화면을 거치지 않도록 pending 사용자를 저장한 뒤
        // 같은 비밀번호 해시로 바로 로그인까지 이어간다.
        userRepository.save(User.createPending(passwordEncoder.encode(input.getPassword()), normalizedEmail));
        return authenticateSignedUpUser(normalizedEmail, input.getPassword());
    }

    public boolean isDuplicatedHandle(String handle) {
        // Handle은 trim 기준으로 정규화한 뒤 중복 확인
        return userRepository.existsByHandle(normalizeHandle(handle));
    }

    public boolean isDuplicatedEmail(String email) {
        // 이메일은 대소문자를 구분하지 않는다.
        return userRepository.existsByEmailIgnoreCase(normalizeEmail(email));
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
        // SecurityContext에는 이메일이 principal name으로 들어오므로
        // 화면/권한용 handle가 필요할 때 여기서 매핑한다.
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            return null;
        }

        return findUserByEmail(authenticatedEmail)
                .map(User::getHandle)
                .filter(handle -> handle != null && !handle.isBlank())
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
        // 최초 로그인 사용자의 Handle 설정은 한 번만 가능하므로
        // 사용자 단위 락으로 동시 요청을 막고 현재 사용자 상태를 다시 확인한다.
        User user = findUserByEmail(authenticatedEmail)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
        String normalizedHandle = normalizeHandle(input.getHandle());

        // 이미 설정된 Handle은 변경 불가
        if (user.hasHandle()) {
            throw new BusinessException(HANDLE_ALREADY_CONFIGURED.getMessage(), HttpStatus.CONFLICT);
        }
        // 다른 사용자가 먼저 선점했는지 최종 중복검사
        if (userRepository.existsByHandle(normalizedHandle)) {
            throw new BusinessException(DUPLICATED_HANDLE.getMessage(), HttpStatus.CONFLICT);
        }

        // 엔티티 내부에서 handle 및 설정 완료 상태를 갱신
        user.configureHandle(normalizedHandle);
        return user;
    }

    private Authentication authenticateSignedUpUser(String email, String password) {
        try {
            // 이메일 + 패스워드 기반 인증
            return authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            normalizeEmail(email),
                            password
                    )
            );

        } catch (AuthenticationException exception) {

            // 이메일 + 패스워드 인증 실패 시 Exception 반환
            throw new BusinessException(INVALID_EMAIL_OR_PASSWORD.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    public Authentication loginWithOAuth2(String provider, Map<String, Object> attributes, HttpServletRequest httpRequest) {
        // provider attribute에서 내부 사용자 계정을 찾거나 생성한 뒤
        // 일반 로그인과 동일한 형태의 Authentication 객체로 재구성
        User user = findOrCreateOAuth2User(provider, attributes);
        loginPolicy.validateBlockedUser(user.getEmail());
        loginService.updateLastAccess(user.getEmail(), resolveClientIp(httpRequest));

        return UsernamePasswordAuthenticationToken.authenticated(
                new org.springframework.security.core.userdetails.User(
                        user.getEmail(),
                        user.getPassword(),
                        AuthorityUtils.createAuthorityList("ROLE_" + user.getResolvedRole().name())
                ),
                user.getPassword(),
                AuthorityUtils.createAuthorityList("ROLE_" + user.getResolvedRole().name())
        );
    }

    public void sendFindHandleCode(AccountRecoveryEmailInput input) {
        // Handle 찾기는 먼저 이메일 소유 여부를 확인한 뒤
        // 해당 이메일 기준으로 6자리 인증코드를 새로 발급한다.
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(input.getEmail()))
                .orElseThrow(() -> new BusinessException(EMAIL_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
        String email = user.getEmail().toLowerCase(Locale.ROOT);
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        String previousCode = emailCodeStorage.get(email);

        // 기존 코드가 있으면 이전 만료정보까지 함께 지워
        // 가장 최근에 보낸 코드만 유효하게 유지
        if (previousCode != null) {
            codeExpiredAtStorage.remove(previousCode);
        }
        emailCodeStorage.put(email, code);
        codeExpiredAtStorage.put(code, LocalDateTime.now().plusMinutes(5));
        verifiedFindPasswordCodeStorage.remove(email);

        try {
            // 메일 발송 실패 시 메모리에 남긴 코드도 롤백해야
            // 실제로 받지 못한 코드가 유효한 상태로 남지 않는다.
            mailService.send(
                    user.getEmail(),
                    "[quertimizer] Handle 찾기 인증코드",
                    """
                            quertimizer Handle 찾기 인증코드입니다.

                            인증코드: %s
                            유효시간: 5분
                            """.formatted(code)
            );
        } catch (RuntimeException exception) {
            emailCodeStorage.remove(email);
            codeExpiredAtStorage.remove(code);
            throw new BusinessException(VERIFICATION_EMAIL_SEND_FAILED.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public FoundHandleResult findHandle(AccountRecoveryCodeInput input) {
        // 이메일과 코드가 모두 일치하고, 만료 전인지 확인한 뒤 handle를 반환
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(input.getEmail()))
                .orElseThrow(() -> new BusinessException(EMAIL_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
        String email = normalizeEmail(input.getEmail());
        String savedCode = emailCodeStorage.get(email);
        LocalDateTime expiredAt = codeExpiredAtStorage.get(input.getCode());

        // 코드 저장 여부, 이메일-코드 매핑, 만료정보 셋 중 하나라도 어긋나면 실패
        if (savedCode == null || !savedCode.equals(input.getCode()) || expiredAt == null) {
            throw new BusinessException(INVALID_VERIFICATION_CODE.getMessage(), HttpStatus.BAD_REQUEST);
        }
        // 만료된 코드는 즉시 정리하여 재사용되지 않게 한다.
        if (expiredAt.isBefore(LocalDateTime.now())) {
            emailCodeStorage.remove(email);
            codeExpiredAtStorage.remove(input.getCode());
            throw new BusinessException(EXPIRED_VERIFICATION_CODE.getMessage(), HttpStatus.BAD_REQUEST);
        }
        // Handle 찾기 코드는 1회용이므로 성공 시 바로 제거
        emailCodeStorage.remove(email);
        codeExpiredAtStorage.remove(input.getCode());

        return new FoundHandleResult(user.getHandle());
    }

    public void sendFindPasswordCode(AccountRecoveryEmailInput input) {
        // 비밀번호 찾기도 Handle 찾기와 같은 방식으로 코드를 발급하지만
        // 이후 verify/reset 두 단계에서 다시 재사용된다.
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(input.getEmail()))
                .orElseThrow(() -> new BusinessException(EMAIL_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
        String email = user.getEmail().toLowerCase(Locale.ROOT);
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        String previousCode = emailCodeStorage.get(email);

        if (previousCode != null) {
            codeExpiredAtStorage.remove(previousCode);
        }
        emailCodeStorage.put(email, code);
        codeExpiredAtStorage.put(code, LocalDateTime.now().plusMinutes(5));
        verifiedFindPasswordCodeStorage.remove(email);

        try {
            mailService.send(
                    user.getEmail(),
                    "[quertimizer] 비밀번호 찾기 인증코드",
                    """
                            quertimizer 비밀번호 찾기 인증코드입니다.

                            인증코드: %s
                            유효시간: 5분
                            """.formatted(code)
            );
        } catch (RuntimeException exception) {
            emailCodeStorage.remove(email);
            codeExpiredAtStorage.remove(code);
            throw new BusinessException(VERIFICATION_EMAIL_SEND_FAILED.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void verifyFindPasswordCode(AccountRecoveryCodeInput input) {
        // 실제 비밀번호 변경 전에 코드가 유효한지 먼저 검증하고
        // "비밀번호 재설정 가능" 상태만 별도로 기록
        userRepository.findByEmailIgnoreCase(normalizeEmail(input.getEmail()))
                .orElseThrow(() -> new BusinessException(EMAIL_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
        String email = normalizeEmail(input.getEmail());
        String savedCode = emailCodeStorage.get(email);
        LocalDateTime expiredAt = codeExpiredAtStorage.get(input.getCode());

        if (savedCode == null || !savedCode.equals(input.getCode()) || expiredAt == null) {
            throw new BusinessException(INVALID_VERIFICATION_CODE.getMessage(), HttpStatus.BAD_REQUEST);
        }
        if (expiredAt.isBefore(LocalDateTime.now())) {
            emailCodeStorage.remove(email);
            codeExpiredAtStorage.remove(input.getCode());
            throw new BusinessException(EXPIRED_VERIFICATION_CODE.getMessage(), HttpStatus.BAD_REQUEST);
        }

        // resetPassword에서는 이 verified 상태가 있어야만 최종 비밀번호 변경이 가능
        verifiedFindPasswordCodeStorage.put(email, input.getCode());
    }

    public void resetPassword(ResetPasswordInput input) {
        // reset은 이메일/코드 존재 여부, 만료 여부, verify 단계 완료 여부를 모두 다시 확인한다.
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(input.getEmail()))
                .orElseThrow(() -> new BusinessException(EMAIL_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
        String email = normalizeEmail(input.getEmail());
        String savedCode = emailCodeStorage.get(email);
        LocalDateTime expiredAt = codeExpiredAtStorage.get(input.getCode());
        String verifiedCode = verifiedFindPasswordCodeStorage.get(email);

        if (savedCode == null || !savedCode.equals(input.getCode()) || expiredAt == null) {
            throw new BusinessException(INVALID_VERIFICATION_CODE.getMessage(), HttpStatus.BAD_REQUEST);
        }
        if (expiredAt.isBefore(LocalDateTime.now())) {
            emailCodeStorage.remove(email);
            codeExpiredAtStorage.remove(input.getCode());
            verifiedFindPasswordCodeStorage.remove(email);
            throw new BusinessException(EXPIRED_VERIFICATION_CODE.getMessage(), HttpStatus.BAD_REQUEST);
        }
        if (verifiedCode == null || !verifiedCode.equals(input.getCode())) {
            throw new BusinessException(PASSWORD_RESET_VERIFICATION_REQUIRED.getMessage(), HttpStatus.BAD_REQUEST);
        }
        // 비밀번호 변경이 끝나면 코드/검증상태는 모두 제거하여 완전한 1회용 흐름으로 마감
        emailCodeStorage.remove(email);
        codeExpiredAtStorage.remove(input.getCode());
        verifiedFindPasswordCodeStorage.remove(email);

        user.changePassword(passwordEncoder.encode(input.getPassword()));
    }

    private User findOrCreateOAuth2User(String provider, Map<String, Object> attributes) {
        // provider별 계정 고유 식별자와 이메일을 추출하여
        // 내부 사용자 계정을 찾거나, 없으면 pending 사용자로 생성
        String oauth2ProviderId = resolveOAuth2ProviderId(provider, attributes);
        String resolvedEmail = resolveOAuth2Email(provider, attributes, oauth2ProviderId);

        return userRepository.findById(resolvedEmail)
                .orElseGet(() -> userRepository.save(
                        User.createPending(
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
        // 이메일 제공 provider면 실제 이메일을 쓰고,
        // 제공하지 않으면 provider 식별자 기반 가상 이메일을 내부 식별자로 사용
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

    private String resolveClientIp(HttpServletRequest httpRequest) {
        // 프록시 환경에서는 X-Forwarded-For의 첫 번째 값을 실제 접속 IP로 사용
        String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        // 프록시 정보가 없으면 직접 연결된 remote address 사용
        return httpRequest.getRemoteAddr();
    }
}
