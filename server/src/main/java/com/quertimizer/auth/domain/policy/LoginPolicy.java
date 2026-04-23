package com.quertimizer.auth.domain.policy;

import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.user.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import static com.quertimizer.auth.domain.model.LoginFailReason.BLOCKED_USER;

@Component
@RequiredArgsConstructor
public class LoginPolicy {

    private final UserRepository userRepository;

    public void validateBlockedUser(String authenticatedEmail) {
        userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .filter(user -> user.hasHandle() && user.isBlocked())
                .ifPresent(user -> {
                    throw new BusinessException(BLOCKED_USER.getMessage(), HttpStatus.FORBIDDEN);
                });
    }

}
