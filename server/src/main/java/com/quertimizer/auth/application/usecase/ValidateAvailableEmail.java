package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.domain.policy.SignupPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidateAvailableEmail {

    private final SignupPolicy signupPolicy;

    public void execute(String email) {
        // 회원가입 가능 이메일인지 확인
        signupPolicy.validateAvailableEmail(email);
    }
}
