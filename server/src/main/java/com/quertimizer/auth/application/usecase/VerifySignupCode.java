package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.VerifyCodeInput;
import com.quertimizer.auth.application.port.VerificationCodeRepository;
import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.auth.domain.policy.AuthRateLimitPolicy;
import com.quertimizer.auth.domain.policy.SignupPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class VerifySignupCode {

    private final AuthService authService;
    private final VerificationCodeRepository verificationCodeRepository;
    private final AuthRateLimitPolicy authRateLimitPolicy;
    private final SignupPolicy signupPolicy;

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
    public void execute(VerifyCodeInput input) {
        signupPolicy.validateAvailableEmail(input.getEmail());
        authService.validateVerificationCode(input.getEmail(), input.getCode(), input.getClientIp(), "signup");
        authRateLimitPolicy.clearCodeVerificationFailures("signup", input.getEmail(), input.getClientIp());
        verificationCodeRepository.markVerified(input.getEmail());
    }
}
