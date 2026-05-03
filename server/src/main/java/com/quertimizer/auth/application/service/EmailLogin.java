package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.port.in.EmailLoginUseCase;
import com.quertimizer.auth.application.input.EmailLoginInput;
import com.quertimizer.auth.application.output.AuthenticatedUserOutput;
import com.quertimizer.auth.domain.policy.LoginPolicy;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import static com.quertimizer.auth.domain.model.AuthFailReason.USER_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class EmailLogin implements EmailLoginUseCase {

    private final LoginService loginService;
    private final LoginPolicy loginPolicy;
    private final AuthRateLimitService authRateLimitPolicy;
    private final UserRepositoryPort userRepository;

    /**
     * 이메일 로그인 인증 결과를 생성하고 계정 상태와 접속 정보를 반영한다.
     *
     * <ol>
     *   <li>이메일 인증 결과 생성
     *   <li>차단 계정 검증
     *   <li>마지막 접속 정보 갱신 후 인증 결과 반환
     * </ol>
     *
     * @param input 이메일 로그인 입력
     */
    @Override
    public AuthenticatedUserOutput execute(EmailLoginInput input) {
        authRateLimitPolicy.validateLoginAllowed(input.getEmail(), input.getAccessIp());

        String authenticatedEmail;
        try {
            authenticatedEmail = loginService.authenticateByEmailPassword(input.getEmail(), input.getPassword());
            authRateLimitPolicy.clearLoginFailures(input.getEmail(), input.getAccessIp());
        } catch (BusinessException exception) {
            authRateLimitPolicy.recordLoginFailure(input.getEmail(), input.getAccessIp());
            throw exception;
        }

        User user = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND.getMessage(), HttpStatus.UNAUTHORIZED));

        loginPolicy.validateBlockedUser(user);
        loginService.updateLastAccess(user.getEmail(), input.getAccessIp());
        return AuthenticatedUserOutput.from(user);
    }
}
