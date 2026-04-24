package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.domain.policy.SignupPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidateAvailableHandle {

    private final SignupPolicy signupPolicy;

    public void execute(String handle) {
        // 회원가입 가능 Handle인지 검증
        signupPolicy.validateAvailableHandle(handle);
    }
}
