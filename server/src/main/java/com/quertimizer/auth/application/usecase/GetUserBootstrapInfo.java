package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.output.UserBootstrapOutput;
import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.global.util.CanonicalCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@CanonicalCode
@Component
@RequiredArgsConstructor
public class GetUserBootstrapInfo {

    private final AuthService authService;

    public UserBootstrapOutput execute(String email) {
        // 이메일로 유저 조회 후 부트스트랩 정보 반환
        return authService.findAuthenticatedUser(email)
                .map(user -> UserBootstrapOutput.authenticated(user.getHandle(), user.getResolvedDefaultDbms(), user.getResolvedRole(), !user.hasHandle()))
                .orElseGet(UserBootstrapOutput::unauthenticated);
    }
}
