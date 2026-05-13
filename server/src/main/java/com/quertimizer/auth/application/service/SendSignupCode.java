package com.quertimizer.auth.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.auth.application.port.in.SendSignupCodeUseCase;
import com.quertimizer.auth.application.input.SendCodeInput;
import com.quertimizer.auth.application.port.out.AuthMailSenderPort;
import com.quertimizer.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.quertimizer.auth.domain.model.AccountRecoveryFailReason.VERIFICATION_EMAIL_SEND_FAILED;
import static com.quertimizer.auth.domain.model.AuthMailContentConstant.SIGNUP_CODE_DESCRIPTION;
import static com.quertimizer.auth.domain.model.AuthMailContentConstant.SIGNUP_CODE_SUBJECT;
import static com.quertimizer.auth.domain.model.AuthMailContentConstant.SIGNUP_CODE_TITLE;

@Component
@RequiredArgsConstructor
public class SendSignupCode implements SendSignupCodeUseCase {

    private final AuthService authService;
    private final AuthMailSenderPort authMailSender;
    private final AuthRateLimitService authRateLimitPolicy;

    /**
     * 회원가입용 인증코드를 전송한다.
     *
     * <ol>
     *   <li>인증코드 요청 한도 초과 검증
     *   <li>인증코드 발급과 저장
     *   <li>인증코드 전송 or 실패 시 인증코드 제거
     * </ol>
     *
     * @param input 인증코드 전송 입력
     */
    @Transactional
    @Override
    @Log("회원가입 코드 전송")
    public void execute(SendCodeInput input) {
        authRateLimitPolicy.validateTooManyRequest(input.getEmail(), input.getClientIp());

        String code = authService.issueVerificationCode(input.getEmail());

        try {
            authMailSender.sendAuthCodeMail(input.getEmail(), SIGNUP_CODE_SUBJECT, SIGNUP_CODE_TITLE, SIGNUP_CODE_DESCRIPTION, code);
        } catch (RuntimeException exception) {
            authService.clearVerificationCode(input.getEmail());
            throw new BusinessException(VERIFICATION_EMAIL_SEND_FAILED.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
