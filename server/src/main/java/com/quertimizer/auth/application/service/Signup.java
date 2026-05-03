package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.port.in.SignupUseCase;
import com.quertimizer.auth.application.input.SignupInput;
import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.auth.domain.policy.SignupPolicy;
import com.quertimizer.global.lock.Lock;
import com.quertimizer.global.lock.LockKey;
import com.quertimizer.user.domain.entity.User;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import com.quertimizer.auth.application.port.out.PasswordEncodingPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class Signup implements SignupUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncodingPort passwordEncodingPort;
    private final AuthService authService;
    private final SignupPolicy signupPolicy;

    /**
     * 이메일 회원가입을 완료한다.
     *
     * <ol>
     *   <li>이메일 중복 여부 검증
     *   <li>인증코드 검증 후 파기
     *   <li>Handle 미설정 사용자 생성
     * </ol>
     *
     * @param input 회원가입 입력
     */
    @Lock(prefix = LockKey.SIGNUP, key = "#p0.email", timeout = 500)
    @Override
    public void execute(SignupInput input) {
        signupPolicy.validateAvailableEmail(input.getEmail(), userRepository.existsByEmailIgnoreCase(input.getEmail()));

        authService.validateVerifiedSignupCode(input.getEmail(), input.getCode());
        authService.clearVerifiedSignupCode(input.getEmail(), input.getCode());

        userRepository.save(User.create(passwordEncodingPort.encode(input.getPassword()), input.getEmail()));
    }
}
