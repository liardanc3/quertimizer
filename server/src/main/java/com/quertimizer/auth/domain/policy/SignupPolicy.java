package com.quertimizer.auth.domain.policy;

import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.user.application.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Locale;

import static com.quertimizer.auth.domain.model.SignupFailReason.DUPLICATED_HANDLE;
import static com.quertimizer.auth.domain.model.SignupFailReason.DUPLICATED_EMAIL;

@Component
@RequiredArgsConstructor
public class SignupPolicy {

    private final UserRepository userRepository;

    /**
     * 회원가입 가능한 Handle인지 검증한다.
     *
     * @param handle 중복 여부를 확인할 Handle
     */
    public void validateAvailableHandle(String handle) {
        userRepository.findByHandle(handle.trim())
                      .ifPresent(user -> { throw new BusinessException(DUPLICATED_HANDLE.getMessage(), HttpStatus.CONFLICT); });
    }

    /**
     * 회원가입 가능한 이메일인지 검증한다.
     *
     * @param email 중복 여부를 확인할 이메일
     */
    public void validateAvailableEmail(String email) {
        userRepository.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
                      .ifPresent(user -> { throw new BusinessException(DUPLICATED_EMAIL.getMessage(), HttpStatus.CONFLICT); });
    }
}
