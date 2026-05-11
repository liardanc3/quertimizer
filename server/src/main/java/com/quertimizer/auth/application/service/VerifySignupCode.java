package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.port.in.VerifySignupCodeUseCase;
import com.quertimizer.auth.application.input.VerifyCodeInput;
import com.quertimizer.auth.application.port.out.VerificationCodeRepositoryPort;
import com.quertimizer.auth.domain.policy.SignupPolicy;
import com.quertimizer.auth.application.port.out.AuthUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class VerifySignupCode implements VerifySignupCodeUseCase {

    private final AuthService authService;
    private final VerificationCodeRepositoryPort verificationCodeRepository;
    private final AuthRateLimitService authRateLimitPolicy;
    private final SignupPolicy signupPolicy;
    private final AuthUserPort userRepository;

    /**
     * 회원가입용 인증코드를 검증한다.
     *
     * <ol>
     *   <li>회원가입 가능 이메일 검증
     *   <li>인증코드 유효성 검증
     *   <li>인증코드 검증 실패 기록 제거와 인증 완료 처리
     * </ol>
     *
     * @param input 인증코드 검증 입력
     */
    @Transactional
    @Override
    public void execute(VerifyCodeInput input) {
        signupPolicy.validateAvailableEmail(input.getEmail(), userRepository.existsByEmailIgnoreCase(input.getEmail()));
        authService.validateVerificationCode(input.getEmail(), input.getCode(), input.getClientIp(), "signup");
        authRateLimitPolicy.clearCodeVerificationFailures("signup", input.getEmail(), input.getClientIp());
        verificationCodeRepository.markVerified(input.getEmail());
    }
}
