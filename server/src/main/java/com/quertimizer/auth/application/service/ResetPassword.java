package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.port.in.ResetPasswordUseCase;
import com.quertimizer.auth.application.input.ResetPasswordInput;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.auth.application.port.out.AuthUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import com.quertimizer.auth.application.port.out.PasswordEncodingPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.quertimizer.auth.domain.model.AccountRecoveryFailReason.EMAIL_NOT_FOUND;
import static com.quertimizer.auth.domain.model.AccountRecoveryFailReason.PASSWORD_RESET_VERIFICATION_REQUIRED;

@Component
@RequiredArgsConstructor
public class ResetPassword implements ResetPasswordUseCase {

    private final AuthService authService;
    private final AuthUserPort userRepository;
    private final PasswordEncodingPort passwordEncodingPort;
    private final AuthRateLimitService authRateLimitPolicy;

    /**
     * 인증이 끝난 이메일의 비밀번호를 재설정한다.
     *
     * <ol>
     *   <li>비밀번호 재설정 rate limit 기록
     *   <li>비밀번호 재설정 인증 완료 여부 검증과 인증코드 제거
     *   <li>사용자 비밀번호 변경
     * </ol>
     *
     * @param input 비밀번호 재설정 입력
     */
    @Transactional
    @Override
    public void execute(ResetPasswordInput input) {
        authRateLimitPolicy.recordPasswordReset(input.getEmail(), input.getClientIp());
        authService.validateVerifiedEmail(input.getEmail(), PASSWORD_RESET_VERIFICATION_REQUIRED.getMessage());
        authService.clearVerificationCode(input.getEmail());

        userRepository.findByEmail(input.getEmail())
                .ifPresentOrElse(
                        user -> {
                            user.changePassword(passwordEncodingPort.encode(input.getPassword()));
                            userRepository.save(user);
                        },
                        () -> { throw new BusinessException(EMAIL_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND); }
                );
    }
}
