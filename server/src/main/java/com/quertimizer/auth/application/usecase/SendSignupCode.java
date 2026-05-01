package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.SendCodeInput;
import com.quertimizer.auth.application.port.AuthMailSender;
import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.auth.domain.policy.AuthRateLimitPolicy;
import com.quertimizer.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.quertimizer.auth.domain.model.AccountRecoveryFailReason.VERIFICATION_EMAIL_SEND_FAILED;
import static com.quertimizer.auth.infrastructure.mail.MailConstant.SIGNUP_CODE_DESCRIPTION;
import static com.quertimizer.auth.infrastructure.mail.MailConstant.SIGNUP_CODE_SUBJECT;
import static com.quertimizer.auth.infrastructure.mail.MailConstant.SIGNUP_CODE_TITLE;

@Component
@RequiredArgsConstructor
public class SendSignupCode {

    private final AuthService authService;
    private final AuthMailSender authMailSender;
    private final AuthRateLimitPolicy authRateLimitPolicy;

    /**
     * 회원가입용 인증코드를 전송한다.
     *
     * <ol>
     *   <li>회원가입 인증코드 발급 제한 기록
     *   <li>인증코드 발급과 저장
     *   <li>메일 전송 실패 시 발급 코드 제거
     * </ol>
     *
     * @param input 인증코드 전송 입력
     */
    @Transactional
    public void execute(SendCodeInput input) {
        authRateLimitPolicy.recordCodeIssue("signup", input.getEmail(), input.getClientIp());
        String code = authService.issueVerificationCode(input.getEmail());

        try {
            authMailSender.sendAuthCodeMail(input.getEmail(), SIGNUP_CODE_SUBJECT, SIGNUP_CODE_TITLE, SIGNUP_CODE_DESCRIPTION, code);
        } catch (RuntimeException exception) {
            authService.clearVerificationCode(input.getEmail());
            throw new BusinessException(VERIFICATION_EMAIL_SEND_FAILED.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
