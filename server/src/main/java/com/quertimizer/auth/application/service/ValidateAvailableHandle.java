package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.port.in.ValidateAvailableHandleUseCase;
import com.quertimizer.auth.domain.policy.SignupPolicy;
import com.quertimizer.auth.application.port.out.AuthUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidateAvailableHandle implements ValidateAvailableHandleUseCase {

    private final SignupPolicy signupPolicy;
    private final AuthUserPort userRepository;

    /**
     * 회원가입 가능한 Handle인지 검증한다.
     *
     * @param handle 검증할 Handle
     */
    @Override
    public void execute(String handle) {
        signupPolicy.validateAvailableHandle(handle, userRepository.existsByHandle(handle.trim()));
    }
}
