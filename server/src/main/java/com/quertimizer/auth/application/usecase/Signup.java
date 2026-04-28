package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.SignupInput;
import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.auth.domain.policy.SignupPolicy;
import com.quertimizer.global.lock.Lock;
import com.quertimizer.global.lock.LockKey;
import com.quertimizer.user.domain.entity.User;
import com.quertimizer.user.application.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class Signup {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
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
    public void execute(SignupInput input) {
        signupPolicy.validateAvailableEmail(input.getEmail());

        authService.validateVerifiedSignupCode(input.getEmail(), input.getCode());
        authService.clearVerifiedSignupCode(input.getEmail(), input.getCode());

        userRepository.save(User.create(passwordEncoder.encode(input.getPassword()), input.getEmail()));
    }
}
