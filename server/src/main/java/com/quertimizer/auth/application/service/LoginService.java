package com.quertimizer.auth.application.service;

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

    public void updateLastAccess(String authenticatedEmail, String accessIp) {
        userRepository
                .findByEmailIgnoreCase(authenticatedEmail)
                .ifPresent(user -> user.updateLastAccess(accessIp.trim(), LocalDateTime.now()));
    }

}
