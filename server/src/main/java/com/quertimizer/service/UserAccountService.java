package com.quertimizer.service;

import com.quertimizer.endpoint.api.dto.request.AccountRecoveryCodeReq;
import com.quertimizer.endpoint.api.dto.request.AccountRecoveryEmailReq;
import com.quertimizer.endpoint.api.dto.request.LoginReq;
import com.quertimizer.endpoint.api.dto.request.ResetPasswordReq;
import com.quertimizer.endpoint.api.dto.request.SetupUserIdReq;
import com.quertimizer.endpoint.api.dto.request.SignupReq;
import com.quertimizer.endpoint.api.dto.response.FindUserIdRes;
import com.quertimizer.entity.User;
import com.quertimizer.exception.BusinessException;
import com.quertimizer.lock.Lock;
import com.quertimizer.lock.LockKey;
import com.quertimizer.repository.UserRepository;
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

import static com.quertimizer.constant.AccountRecoveryFailReason.EMAIL_NOT_FOUND;
import static com.quertimizer.constant.AccountRecoveryFailReason.EXPIRED_VERIFICATION_CODE;
import static com.quertimizer.constant.AccountRecoveryFailReason.INVALID_VERIFICATION_CODE;
import static com.quertimizer.constant.AccountRecoveryFailReason.PASSWORD_RESET_VERIFICATION_REQUIRED;
import static com.quertimizer.constant.AccountRecoveryFailReason.VERIFICATION_EMAIL_SEND_FAILED;
import static com.quertimizer.constant.LoginFailReason.INVALID_USER_ID_OR_PASSWORD;
import static com.quertimizer.constant.SignupFailReason.DUPLICATED_EMAIL;
import static com.quertimizer.constant.SignupFailReason.DUPLICATED_USER_ID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserAccountService {

    private static final String USER_NOT_FOUND_MESSAGE = "\uC874\uC7AC\uD558\uC9C0 \uC54A\uB294 \uC0AC\uC6A9\uC790\uC785\uB2C8\uB2E4.";
    private static final String USER_ID_ALREADY_CONFIGURED_MESSAGE = "\uC774\uBBF8 ID \uC124\uC815\uC774 \uC644\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final MailService mailService;
    private final AccountRestrictionService accountRestrictionService;

    private final Map<String, String> emailCodeStorage = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> codeExpiredAtStorage = new ConcurrentHashMap<>();
    private final Map<String, String> verifiedFindPasswordCodeStorage = new ConcurrentHashMap<>();

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void deleteExpiredRecoveryCode() {
        LocalDateTime now = LocalDateTime.now();
        codeExpiredAtStorage.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        emailCodeStorage.entrySet().removeIf(entry -> !codeExpiredAtStorage.containsKey(entry.getValue()));
        verifiedFindPasswordCodeStorage.entrySet().removeIf(entry -> !codeExpiredAtStorage.containsKey(entry.getValue()));
    }

    @Lock(prefix = LockKey.SIGNUP, key = "#p0.email", timeout = 500)
    public Authentication signup(SignupReq request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (isDuplicatedEmail(normalizedEmail)) {
            throw new BusinessException(DUPLICATED_EMAIL, HttpStatus.CONFLICT);
        }

        userRepository.save(User.createPending(passwordEncoder.encode(request.getPassword()), normalizedEmail));
        return login(new LoginReq(normalizedEmail, request.getPassword(), true));
    }

    public boolean isDuplicatedUserId(String userId) {
        return userRepository.existsByUserId(normalizeUserId(userId));
    }

    public boolean isDuplicatedEmail(String email) {
        return userRepository.existsByEmailIgnoreCase(normalizeEmail(email));
    }

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email));
    }

    public Optional<User> findUserByUserId(String userId) {
        return userRepository.findByUserId(normalizeUserId(userId));
    }

    public String resolveCurrentUserId(String authenticatedEmail) {
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            return null;
        }

        return findUserByEmail(authenticatedEmail)
                .map(User::getUserId)
                .filter(userId -> userId != null && !userId.isBlank())
                .orElse(null);
    }

    public Optional<User> findAuthenticatedUser(String authenticatedEmail) {
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            return Optional.empty();
        }

        return findUserByEmail(authenticatedEmail);
    }

    @Lock(prefix = LockKey.SIGNUP, key = "#p0", timeout = 500)
    public User configureUserId(String authenticatedEmail, SetupUserIdReq request) {
        User user = findUserByEmail(authenticatedEmail)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
        String normalizedUserId = normalizeUserId(request.getUserId());

        if (user.hasUserId()) {
            throw new BusinessException(USER_ID_ALREADY_CONFIGURED_MESSAGE, HttpStatus.CONFLICT);
        }
        if (userRepository.existsByUserId(normalizedUserId)) {
            throw new BusinessException(DUPLICATED_USER_ID, HttpStatus.CONFLICT);
        }

        user.configureUserId(normalizedUserId);
        return user;
    }

    public Authentication login(LoginReq request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            normalizeEmail(request.getEmail()),
                            request.getPassword()
                    )
            );

            validateBlockedUser(authentication.getName());
            return authentication;
        } catch (AuthenticationException exception) {
            throw new BusinessException(INVALID_USER_ID_OR_PASSWORD, HttpStatus.UNAUTHORIZED);
        }
    }

    public Authentication loginWithOAuth2(String provider, Map<String, Object> attributes) {
        User user = findOrCreateOAuth2User(provider, attributes);
        validateBlockedUser(user.getEmail());

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

    public void recordAccess(String authenticatedEmail, String accessIp) {
        if (authenticatedEmail == null || authenticatedEmail.isBlank() || accessIp == null || accessIp.isBlank()) {
            return;
        }

        findUserByEmail(authenticatedEmail).ifPresent(user -> user.recordAccess(accessIp.trim(), LocalDateTime.now()));
    }

    public void sendFindIdCode(AccountRecoveryEmailReq request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new BusinessException(EMAIL_NOT_FOUND, HttpStatus.NOT_FOUND));
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
                    "[quertimizer] \uC544\uC774\uB514 \uCC3E\uAE30 \uC778\uC99D\uCF54\uB4DC",
                    """
                            quertimizer \uC544\uC774\uB514 \uCC3E\uAE30 \uC778\uC99D\uCF54\uB4DC\uC785\uB2C8\uB2E4.

                            \uC778\uC99D\uCF54\uB4DC: %s
                            \uC720\uD6A8\uC2DC\uAC04: 5\uBD84
                            """.formatted(code)
            );
        } catch (RuntimeException exception) {
            emailCodeStorage.remove(email);
            codeExpiredAtStorage.remove(code);
            throw new BusinessException(VERIFICATION_EMAIL_SEND_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public FindUserIdRes findUserId(AccountRecoveryCodeReq request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new BusinessException(EMAIL_NOT_FOUND, HttpStatus.NOT_FOUND));
        String email = normalizeEmail(request.getEmail());
        String savedCode = emailCodeStorage.get(email);
        LocalDateTime expiredAt = codeExpiredAtStorage.get(request.getCode());

        if (savedCode == null || !savedCode.equals(request.getCode()) || expiredAt == null) {
            throw new BusinessException(INVALID_VERIFICATION_CODE, HttpStatus.BAD_REQUEST);
        }
        if (expiredAt.isBefore(LocalDateTime.now())) {
            emailCodeStorage.remove(email);
            codeExpiredAtStorage.remove(request.getCode());
            throw new BusinessException(EXPIRED_VERIFICATION_CODE, HttpStatus.BAD_REQUEST);
        }
        emailCodeStorage.remove(email);
        codeExpiredAtStorage.remove(request.getCode());

        return new FindUserIdRes(user.getUserId());
    }

    public void sendFindPasswordCode(AccountRecoveryEmailReq request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new BusinessException(EMAIL_NOT_FOUND, HttpStatus.NOT_FOUND));
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
                    "[quertimizer] \uBE44\uBC00\uBC88\uD638 \uCC3E\uAE30 \uC778\uC99D\uCF54\uB4DC",
                    """
                            quertimizer \uBE44\uBC00\uBC88\uD638 \uCC3E\uAE30 \uC778\uC99D\uCF54\uB4DC\uC785\uB2C8\uB2E4.

                            \uC778\uC99D\uCF54\uB4DC: %s
                            \uC720\uD6A8\uC2DC\uAC04: 5\uBD84
                            """.formatted(code)
            );
        } catch (RuntimeException exception) {
            emailCodeStorage.remove(email);
            codeExpiredAtStorage.remove(code);
            throw new BusinessException(VERIFICATION_EMAIL_SEND_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void verifyFindPasswordCode(AccountRecoveryCodeReq request) {
        userRepository.findByEmailIgnoreCase(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new BusinessException(EMAIL_NOT_FOUND, HttpStatus.NOT_FOUND));
        String email = normalizeEmail(request.getEmail());
        String savedCode = emailCodeStorage.get(email);
        LocalDateTime expiredAt = codeExpiredAtStorage.get(request.getCode());

        if (savedCode == null || !savedCode.equals(request.getCode()) || expiredAt == null) {
            throw new BusinessException(INVALID_VERIFICATION_CODE, HttpStatus.BAD_REQUEST);
        }
        if (expiredAt.isBefore(LocalDateTime.now())) {
            emailCodeStorage.remove(email);
            codeExpiredAtStorage.remove(request.getCode());
            throw new BusinessException(EXPIRED_VERIFICATION_CODE, HttpStatus.BAD_REQUEST);
        }

        verifiedFindPasswordCodeStorage.put(email, request.getCode());
    }

    public void resetPassword(ResetPasswordReq request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new BusinessException(EMAIL_NOT_FOUND, HttpStatus.NOT_FOUND));
        String email = normalizeEmail(request.getEmail());
        String savedCode = emailCodeStorage.get(email);
        LocalDateTime expiredAt = codeExpiredAtStorage.get(request.getCode());
        String verifiedCode = verifiedFindPasswordCodeStorage.get(email);

        if (savedCode == null || !savedCode.equals(request.getCode()) || expiredAt == null) {
            throw new BusinessException(INVALID_VERIFICATION_CODE, HttpStatus.BAD_REQUEST);
        }
        if (expiredAt.isBefore(LocalDateTime.now())) {
            emailCodeStorage.remove(email);
            codeExpiredAtStorage.remove(request.getCode());
            verifiedFindPasswordCodeStorage.remove(email);
            throw new BusinessException(EXPIRED_VERIFICATION_CODE, HttpStatus.BAD_REQUEST);
        }
        if (verifiedCode == null || !verifiedCode.equals(request.getCode())) {
            throw new BusinessException(PASSWORD_RESET_VERIFICATION_REQUIRED, HttpStatus.BAD_REQUEST);
        }
        emailCodeStorage.remove(email);
        codeExpiredAtStorage.remove(request.getCode());
        verifiedFindPasswordCodeStorage.remove(email);

        user.changePassword(passwordEncoder.encode(request.getPassword()));
    }

    private User findOrCreateOAuth2User(String provider, Map<String, Object> attributes) {
        String oauth2UserId = resolveOAuth2UserId(provider, attributes);
        String resolvedEmail = resolveOAuth2Email(provider, attributes, oauth2UserId);

        return userRepository.findById(resolvedEmail)
                .orElseGet(() -> userRepository.save(
                        User.createPending(
                                passwordEncoder.encode(UUID.randomUUID().toString()),
                                resolvedEmail
                        )
                ));
    }

    private String resolveOAuth2UserId(String provider, Map<String, Object> attributes) {
        Object providerUserId = switch (provider) {
            case "github" -> attributes.get("id");
            case "google" -> attributes.get("sub");
            case "kakao" -> attributes.get("id");
            default -> null;
        };
        if (providerUserId == null) {
            throw new BusinessException(INVALID_USER_ID_OR_PASSWORD, HttpStatus.UNAUTHORIZED);
        }

        String resolvedUserId = providerUserId.toString().trim();
        if (resolvedUserId.isEmpty()) {
            throw new BusinessException(INVALID_USER_ID_OR_PASSWORD, HttpStatus.UNAUTHORIZED);
        }

        return resolvedUserId;
    }

    private String resolveOAuth2Email(String provider, Map<String, Object> attributes, String oauth2UserId) {
        String rawEmail = Optional.ofNullable(extractOAuth2Email(provider, attributes))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElse(null);

        if (rawEmail != null) {
            return rawEmail;
        }

        return "%s_%s@users.quertimizer.local".formatted(provider, oauth2UserId);
    }

    @SuppressWarnings("unchecked")
    private String extractOAuth2Email(String provider, Map<String, Object> attributes) {
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

    private void validateBlockedUser(String authenticatedEmail) {
        String currentUserId = resolveCurrentUserId(authenticatedEmail);
        if (currentUserId == null || currentUserId.isBlank()) {
            return;
        }

        if (accountRestrictionService.isBlockedUser(currentUserId)) {
            throw new BusinessException("차단된 계정입니다.", HttpStatus.FORBIDDEN);
        }
    }

    private String normalizeEmail(String email) {
        return Optional.ofNullable(email)
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElse("");
    }

    private String normalizeUserId(String userId) {
        return Optional.ofNullable(userId)
                .map(String::trim)
                .orElse("");
    }
}
