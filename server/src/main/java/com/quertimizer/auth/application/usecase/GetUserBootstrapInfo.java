package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.output.UserBootstrapOutput;
import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.user.application.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetUserBootstrapInfo {

    private final AuthService authService;
    private final UserRepository userRepository;

    /**
     * 인증 이메일 기준 사용자 부트스트랩 정보를 조회한다.
     *
     * <ol>
     *   <li>인증 이메일 존재 여부 확인
     *   <li>사용자 조회와 부트스트랩 응답 생성
     * </ol>
     *
     * @param email 부트스트랩 정보를 조회할 인증 이메일
     */
    public UserBootstrapOutput execute(String email) {
        if (email == null || email.isBlank()) {
            return UserBootstrapOutput.unauthenticated();
        }

        return userRepository.findByEmailIgnoreCase(authService.normalizeEmail(email))
                .map(user -> UserBootstrapOutput.authenticated(
                        user.getHandle(), user.getResolvedDefaultDbms(),
                        user.getResolvedRole(), !user.hasHandle()
                ))
                .orElseGet(UserBootstrapOutput::unauthenticated);
    }
}
