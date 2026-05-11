package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.port.out.AuthenticationPort;
import com.quertimizer.auth.application.port.out.AuthUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class LoginService {

    private final AuthenticationPort authenticationPort;
    private final AuthUserPort userRepository;

    public void updateLastAccess(String authenticatedEmail, String accessIp) {
        // IP, 현재시간 기록
        userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .ifPresent(user -> {
                    user.updateLastAccess(accessIp.trim(), LocalDateTime.now());
                    userRepository.save(user);
                });
    }

    public String authenticateByEmailPassword(String email, String password) {
        // 이메일 로그인 인증 결과에서 인증 이메일 반환
        return authenticationPort.authenticateByEmailPassword(email, password);
    }
}
