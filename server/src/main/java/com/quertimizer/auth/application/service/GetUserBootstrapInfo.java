package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.port.in.GetUserBootstrapInfoUseCase;
import com.quertimizer.auth.application.output.UserBootstrapOutput;
import com.quertimizer.auth.application.port.out.AuthUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetUserBootstrapInfo implements GetUserBootstrapInfoUseCase {

    private final AuthService authService;
    private final AuthUserPort userRepository;

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
    @Override
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
