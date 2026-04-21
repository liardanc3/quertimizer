package com.quertimizer.service;

import com.quertimizer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class UserAccessService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public String resolveCurrentUserId(String authenticatedEmail) {
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            return null;
        }

        return userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .map(user -> user.getUserId() == null || user.getUserId().isBlank() ? null : user.getUserId())
                .orElse(null);
    }

    public void recordAccess(String authenticatedEmail, String accessIp) {
        if (authenticatedEmail == null || authenticatedEmail.isBlank() || accessIp == null || accessIp.isBlank()) {
            return;
        }

        userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .ifPresent(user -> user.recordAccess(accessIp.trim(), LocalDateTime.now()));
    }

}
