package com.quertimizer.service;

import com.quertimizer.endpoint.api.dto.request.AccountRecoveryCodeReq;
import com.quertimizer.endpoint.api.dto.request.AccountRecoveryEmailReq;
import com.quertimizer.endpoint.api.dto.request.LoginReq;
import com.quertimizer.endpoint.api.dto.request.ResetPasswordReq;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final MailService mailService;

    private final Map<String, String> emailCodeStorage = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> codeExpiredAtStorage = new ConcurrentHashMap<>();
    private final Map<String, String> verifiedFindPasswordCodeStorage = new ConcurrentHashMap<>();

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void deleteExpiredRecoveryCode() {

        // 만료된 인증코드 삭제
        LocalDateTime now = LocalDateTime.now();
        codeExpiredAtStorage.entrySet().removeIf(entry -> entry.getValue().isBefore(now));

        // 만료된 인증코드와 연결된 이메일 정보 삭제
        emailCodeStorage.entrySet().removeIf(entry -> !codeExpiredAtStorage.containsKey(entry.getValue()));
        verifiedFindPasswordCodeStorage.entrySet().removeIf(entry -> !codeExpiredAtStorage.containsKey(entry.getValue()));
    }

    @Lock(prefix = LockKey.SIGNUP, key = "#p0.userId", timeout = 500)
    public Authentication signup(SignupReq request) {

        // userId, email 중복 검사
        if (isDuplicatedUserId(request.getUserId())) {
            throw new BusinessException(DUPLICATED_USER_ID, HttpStatus.CONFLICT);
        }
        if (isDuplicatedEmail(request.getEmail())) {
            throw new BusinessException(DUPLICATED_EMAIL, HttpStatus.CONFLICT);
        }

        // 유저 생성 후 저장
        User user = User.create(request.getUserId(), passwordEncoder.encode(request.getPassword()), request.getEmail());
        userRepository.save(user);

        // 회원가입 후 바로 로그인 처리
        return login(new LoginReq(request.getUserId(), request.getPassword(), true));
    }

    public boolean isDuplicatedUserId(String userId) {
        return userRepository.existsByUserId(userId);
    }

    public boolean isDuplicatedEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public Optional<User> findUser(String userId) {
        return userRepository.findById(userId);
    }

    public Authentication login(LoginReq request) {
        try {
            // 유저 인증
            return authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.getUserId(), request.getPassword()
                    )
            );
        } catch (AuthenticationException exception) {
            throw new BusinessException(INVALID_USER_ID_OR_PASSWORD, HttpStatus.UNAUTHORIZED);
        }
    }

    public void sendFindIdCode(AccountRecoveryEmailReq request) {

        // 이메일 존재 확인 후 인증코드 생성
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(EMAIL_NOT_FOUND, HttpStatus.NOT_FOUND));
        String email = user.getEmail().toLowerCase();
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        String previousCode = emailCodeStorage.get(email);

        // 같은 이메일 기존 인증코드 제거 후 최신 인증코드 저장
        if (previousCode != null) {
            codeExpiredAtStorage.remove(previousCode);
        }
        emailCodeStorage.put(email, code);
        codeExpiredAtStorage.put(code, LocalDateTime.now().plusMinutes(5));
        verifiedFindPasswordCodeStorage.remove(email);

        // 인증코드 메일 발송
        try {
            mailService.send(
                    user.getEmail(),
                    "[quertimizer] 아이디 찾기 인증코드",
                    """
                            quertimizer 아이디 찾기 인증코드입니다.

                            인증코드: %s
                            유효시간: 5분
                            """.formatted(code)
            );
        } catch (RuntimeException exception) {
            emailCodeStorage.remove(email);
            codeExpiredAtStorage.remove(code);
            throw new BusinessException(VERIFICATION_EMAIL_SEND_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public FindUserIdRes findUserId(AccountRecoveryCodeReq request) {

        // 이메일 검증 후 인증코드 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(EMAIL_NOT_FOUND, HttpStatus.NOT_FOUND));
        String email = request.getEmail().toLowerCase();
        String savedCode = emailCodeStorage.get(email);
        LocalDateTime expiredAt = codeExpiredAtStorage.get(request.getCode());

        // 인증코드 검증
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

        // 이메일 존재 확인 후 인증코드 생성
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(EMAIL_NOT_FOUND, HttpStatus.NOT_FOUND));
        String email = user.getEmail().toLowerCase();
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        String previousCode = emailCodeStorage.get(email);

        // 같은 이메일 기존 인증코드 제거 후 최신 인증코드 저장
        if (previousCode != null) {
            codeExpiredAtStorage.remove(previousCode);
        }
        emailCodeStorage.put(email, code);
        codeExpiredAtStorage.put(code, LocalDateTime.now().plusMinutes(5));
        verifiedFindPasswordCodeStorage.remove(email);

        // 인증코드 메일 발송
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
            throw new BusinessException(VERIFICATION_EMAIL_SEND_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void verifyFindPasswordCode(AccountRecoveryCodeReq request) {

        // 이메일 검증 후 인증코드 조회
        userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(EMAIL_NOT_FOUND, HttpStatus.NOT_FOUND));
        String email = request.getEmail().toLowerCase();
        String savedCode = emailCodeStorage.get(email);
        LocalDateTime expiredAt = codeExpiredAtStorage.get(request.getCode());

        // 인증코드 검증
        if (savedCode == null || !savedCode.equals(request.getCode()) || expiredAt == null) {
            throw new BusinessException(INVALID_VERIFICATION_CODE, HttpStatus.BAD_REQUEST);
        }
        if (expiredAt.isBefore(LocalDateTime.now())) {
            emailCodeStorage.remove(email);
            codeExpiredAtStorage.remove(request.getCode());
            throw new BusinessException(EXPIRED_VERIFICATION_CODE, HttpStatus.BAD_REQUEST);
        }

        // 비밀번호 재설정 가능 상태 저장
        verifiedFindPasswordCodeStorage.put(email, request.getCode());
    }

    public void resetPassword(ResetPasswordReq request) {

        // 이메일 검증 후 인증코드 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(EMAIL_NOT_FOUND, HttpStatus.NOT_FOUND));
        String email = request.getEmail().toLowerCase();
        String savedCode = emailCodeStorage.get(email);
        LocalDateTime expiredAt = codeExpiredAtStorage.get(request.getCode());
        String verifiedCode = verifiedFindPasswordCodeStorage.get(email);

        // 인증코드 검증 후 삭제
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

        // 비밀번호 변경
        user.changePassword(passwordEncoder.encode(request.getPassword()));
    }

}
