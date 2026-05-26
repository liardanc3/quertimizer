package com.quertimizer.user.application.service;

import com.quertimizer.user.application.input.AuthUserAvailabilityInput;
import com.quertimizer.user.application.output.AuthUserAvailabilityOutput;
import com.quertimizer.user.application.port.in.CheckAuthUserAvailabilityUseCase;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CheckAuthUserAvailability implements CheckAuthUserAvailabilityUseCase {

    private final UserRepositoryPort userRepository;

    /**
     * 인증 context에서 필요한 사용자 중복 여부를 조회한다.
     *
     * @param input 중복 확인 대상 이메일과 handle
     */
    @Override
    @Transactional(readOnly = true)
    public AuthUserAvailabilityOutput execute(AuthUserAvailabilityInput input) {
        return new AuthUserAvailabilityOutput(
                input.getEmail() != null && userRepository.existsByEmailIgnoreCase(input.getEmail()),
                input.getHandle() != null && userRepository.existsByHandle(input.getHandle())
        );
    }
}
