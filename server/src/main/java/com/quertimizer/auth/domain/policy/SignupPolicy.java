package com.quertimizer.auth.domain.policy;

import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.global.util.CanonicalCode;
import com.quertimizer.user.application.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Locale;

import static com.quertimizer.auth.domain.model.SignupFailReason.DUPLICATED_HANDLE;
import static com.quertimizer.auth.domain.model.SignupFailReason.DUPLICATED_EMAIL;

@CanonicalCode
@Component
@RequiredArgsConstructor
public class SignupPolicy {

    private final UserRepository userRepository;

    public void validateAvailableHandle(String handle) {
        // 회원가입 가능 Handle인지 확인
        userRepository.findByHandle(handle.trim())
                      .ifPresent(user -> { throw new BusinessException(DUPLICATED_HANDLE.getMessage(), HttpStatus.CONFLICT); });
    }

    public void validateAvailableEmail(String email) {
        // 회원가입 가능 이메일인지 확인
        userRepository.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
                      .ifPresent(user -> { throw new BusinessException(DUPLICATED_EMAIL.getMessage(), HttpStatus.CONFLICT); });
    }
}
