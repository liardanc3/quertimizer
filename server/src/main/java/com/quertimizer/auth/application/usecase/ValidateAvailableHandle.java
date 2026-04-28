package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.domain.policy.SignupPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidateAvailableHandle {

    private final SignupPolicy signupPolicy;

    /**
     * 회원가입 가능한 Handle인지 검증한다.
     *
     * @param handle 검증할 Handle
     */
    public void execute(String handle) {
        signupPolicy.validateAvailableHandle(handle);
    }
}
