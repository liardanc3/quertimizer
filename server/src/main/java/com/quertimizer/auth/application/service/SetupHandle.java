package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.port.in.SetupHandleUseCase;
import com.quertimizer.auth.application.input.SetupHandleInput;
import com.quertimizer.auth.domain.policy.SignupPolicy;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.global.lock.Lock;
import com.quertimizer.global.lock.LockKey;
import com.quertimizer.auth.application.port.out.AuthUserPort;
import com.quertimizer.auth.domain.model.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.quertimizer.auth.domain.model.AuthFailReason.HANDLE_ALREADY_CONFIGURED;
import static com.quertimizer.auth.domain.model.AuthFailReason.USER_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class SetupHandle implements SetupHandleUseCase {

    private final AuthService authService;
    private final SignupPolicy signupPolicy;
    private final AuthUserPort userRepository;

    /**
     * 가입 직후 필요한 Handle을 설정한다.
     *
     * <ol>
     *   <li>인증 이메일 기준 사용자 조회
     *   <li>기존 Handle 설정 여부와 중복 Handle 검증
     *   <li>사용자 Handle 설정
     * </ol>
     *
     * @param input 인증 이메일과 설정할 Handle 입력
     */
    @Transactional
    @Lock(prefix = LockKey.SIGNUP, key = "#p0.authenticatedEmail", timeout = 500)
    @Override
    public void execute(SetupHandleInput input) {
        AuthUser user = authService.findUserByEmail(input.getAuthenticatedEmail())
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));

        if (user.hasHandle()) {
            throw new BusinessException(HANDLE_ALREADY_CONFIGURED.getMessage(), HttpStatus.CONFLICT);
        }

        signupPolicy.validateAvailableHandle(input.getHandle(), userRepository.existsByHandle(input.getHandle().trim()));
        user.configureHandle(input.getHandle());
        userRepository.save(user);
    }
}
