package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.output.UserBootstrapOutput;
import com.quertimizer.auth.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetUserBootstrapInfo {

    private final AuthService authService;

    /**
     * 인증 이메일 기준 사용자 부트스트랩 정보를 조회한다.
     *
     * @param email 부트스트랩 정보를 조회할 인증 이메일
     */
    public UserBootstrapOutput execute(String email) {
        return authService.findAuthenticatedUser(email)
                .map(user -> UserBootstrapOutput.authenticated(
                        user.getHandle(), user.getResolvedDefaultDbms(),
                        user.getResolvedRole(), !user.hasHandle()
                ))
                .orElseGet(UserBootstrapOutput::unauthenticated);
    }
}
