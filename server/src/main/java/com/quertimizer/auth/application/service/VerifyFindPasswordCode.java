package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.port.in.VerifyFindPasswordCodeUseCase;
import com.quertimizer.auth.application.input.VerifyCodeInput;
import com.quertimizer.auth.application.port.out.VerificationCodeRepositoryPort;
import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.auth.application.service.AuthRateLimitService;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.quertimizer.auth.domain.model.AccountRecoveryFailReason.EMAIL_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class VerifyFindPasswordCode implements VerifyFindPasswordCodeUseCase {

    private final AuthService authService;
    private final UserRepositoryPort userRepository;
    private final VerificationCodeRepositoryPort verificationCodeRepository;
    private final AuthRateLimitService authRateLimitPolicy;

    /**
     * 비밀번호 찾기 인증코드를 검증한다.
     *
     * <ol>
     *   <li>가입 이메일 존재 검증
     *   <li>인증코드 유효성 검증
     *   <li>인증코드 검증 실패 기록 제거와 인증 완료 처리
     * </ol>
     *
     * @param input 인증코드 검증 입력
     */
    @Transactional
    @Override
    public void execute(VerifyCodeInput input) {
        userRepository.findByEmailIgnoreCase(input.getEmail())
                .orElseThrow(() -> new BusinessException(EMAIL_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
        authService.validateVerificationCode(input.getEmail(), input.getCode(), input.getClientIp(), "find-password");
        authRateLimitPolicy.clearCodeVerificationFailures("find-password", input.getEmail(), input.getClientIp());
        verificationCodeRepository.markVerified(input.getEmail());
    }
}
