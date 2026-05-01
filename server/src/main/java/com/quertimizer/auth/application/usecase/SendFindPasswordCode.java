package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.SendCodeInput;
import com.quertimizer.auth.application.port.AuthMailSender;
import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.auth.domain.policy.AuthRateLimitPolicy;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.user.application.port.UserRepository;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

import static com.quertimizer.auth.domain.model.AccountRecoveryFailReason.VERIFICATION_EMAIL_SEND_FAILED;
import static com.quertimizer.auth.infrastructure.mail.MailConstant.FIND_PASSWORD_CODE_DESCRIPTION;
import static com.quertimizer.auth.infrastructure.mail.MailConstant.FIND_PASSWORD_CODE_SUBJECT;
import static com.quertimizer.auth.infrastructure.mail.MailConstant.FIND_PASSWORD_CODE_TITLE;

@Component
@RequiredArgsConstructor
public class SendFindPasswordCode {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final AuthMailSender authMailSender;
    private final AuthRateLimitPolicy authRateLimitPolicy;

    /**
     * 비밀번호 찾기 인증코드를 전송한다.
     *
     * <ol>
     *   <li>비밀번호 찾기 인증코드 발급 제한 기록
     *   <li>가입 이메일 조회와 인증코드 발급
     *   <li>메일 전송 실패 시 발급 코드 제거
     * </ol>
     *
     * @param input 인증코드 전송 입력
     */
    @Transactional
    public void execute(SendCodeInput input) {
        authRateLimitPolicy.recordCodeIssue("find-password", input.getEmail(), input.getClientIp());
        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(input.getEmail());
        if (userOptional.isEmpty()) {
            return;
        }

        User user = userOptional.get();
        String email = user.getEmail().toLowerCase(Locale.ROOT);
        String code = authService.issueVerificationCode(email);
        try {
            authMailSender.sendAuthCodeMail(user.getEmail(),
                    FIND_PASSWORD_CODE_SUBJECT, FIND_PASSWORD_CODE_TITLE, FIND_PASSWORD_CODE_DESCRIPTION, code);
        } catch (RuntimeException exception) {
            authService.clearVerificationCode(email);
            throw new BusinessException(VERIFICATION_EMAIL_SEND_FAILED.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
