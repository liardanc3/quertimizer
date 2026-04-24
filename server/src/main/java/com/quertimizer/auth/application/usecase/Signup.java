package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.SignupInput;
import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.auth.domain.policy.SignupPolicy;
import com.quertimizer.global.lock.Lock;
import com.quertimizer.global.lock.LockKey;
import com.quertimizer.global.util.CanonicalCode;
import com.quertimizer.user.domain.entity.User;
import com.quertimizer.user.application.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@CanonicalCode
@Component
@RequiredArgsConstructor
@Transactional
public class Signup {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final SignupPolicy signupPolicy;

    @Lock(prefix = LockKey.SIGNUP, key = "#p0.email", timeout = 500)
    public void execute(SignupInput input) {
        // 이메일 중복 여부 검증
        signupPolicy.validateAvailableEmail(input.getEmail());

        // 인증코드 검사여부 검증 후 파기
        authService.validateVerifiedSignupCode(input.getEmail(), input.getCode());
        authService.clearVerifiedSignupCode(input.getEmail(), input.getCode());

        // Handle 미설정 상태의 유저 생성 후 저장
        userRepository.save(User.create(passwordEncoder.encode(input.getPassword()), input.getEmail()));
    }
}
