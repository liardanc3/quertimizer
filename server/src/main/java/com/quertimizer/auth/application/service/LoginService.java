package com.quertimizer.auth.application.service;

import com.quertimizer.global.util.CanonicalCode;
import com.quertimizer.user.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class LoginService {

    private final UserRepository userRepository;

    @CanonicalCode
    public void updateLastAccess(String authenticatedEmail, String accessIp) {
        // IP, 현재시간 기록
        userRepository.findByEmailIgnoreCase(authenticatedEmail)
                      .ifPresent(user -> user.updateLastAccess(accessIp.trim(), LocalDateTime.now()));
    }

}
