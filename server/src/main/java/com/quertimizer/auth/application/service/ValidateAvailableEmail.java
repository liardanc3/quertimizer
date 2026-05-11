package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.port.in.ValidateAvailableEmailUseCase;
import com.quertimizer.auth.domain.policy.SignupPolicy;
import com.quertimizer.auth.application.port.out.AuthUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidateAvailableEmail implements ValidateAvailableEmailUseCase {

    private final SignupPolicy signupPolicy;
    private final AuthUserPort userRepository;

    /**
     * 회원가입 가능한 이메일인지 검증한다.
     *
     * @param email 검증할 이메일
     */
    @Override
    public void execute(String email) {
        signupPolicy.validateAvailableEmail(email, userRepository.existsByEmailIgnoreCase(email.trim()));
    }
}
