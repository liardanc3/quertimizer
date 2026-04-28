package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.domain.policy.SignupPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidateAvailableEmail {

    private final SignupPolicy signupPolicy;

    /**
     * 회원가입 가능한 이메일인지 검증한다.
     *
     * @param email 검증할 이메일
     */
    public void execute(String email) {
        signupPolicy.validateAvailableEmail(email);
    }
}
