package com.quertimizer.auth.application.service;

import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.global.util.CanonicalCode;
import com.quertimizer.user.domain.entity.User;
import com.quertimizer.user.application.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.quertimizer.auth.domain.model.LoginFailReason.INVALID_EMAIL_OR_PASSWORD;

@Service
@RequiredArgsConstructor
@Transactional
public class LoginService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    @CanonicalCode
    public void updateLastAccess(String authenticatedEmail, String accessIp) {
        // IP, 현재시간 기록
        userRepository.findByEmailIgnoreCase(authenticatedEmail)
                      .ifPresent(user -> user.updateLastAccess(accessIp.trim(), LocalDateTime.now()));
    }

    public Authentication getAuthentication(String email, String password) {
        // 이메일 로그인 인증결과를 생성
        try {
            // 이메일 + 패스워드 기반 인증결과 생성
            return authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, password)
            );
        } catch (AuthenticationException exception) {
            throw new BusinessException(INVALID_EMAIL_OR_PASSWORD.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    public Authentication getAuthentication(User user) {
        // 내부 사용자 정보를 Spring Security 인증객체로 변환
        return UsernamePasswordAuthenticationToken.authenticated(
                new org.springframework.security.core.userdetails.User(
                        user.getEmail(),
                        user.getPassword(),
                        AuthorityUtils.createAuthorityList("ROLE_" + user.getResolvedRole().name())
                ),
                user.getPassword(),
                AuthorityUtils.createAuthorityList("ROLE_" + user.getResolvedRole().name())
        );
    }

}
