package com.quertimizer.auth.application.service;

import com.quertimizer.global.constant.UserRole;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

import static com.quertimizer.auth.domain.model.AuthManageFailReason.INVALID_ROLE;
import static com.quertimizer.auth.domain.model.AuthManageFailReason.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthManageService {

    private final UserRepositoryPort userRepository;
    public User findUser(String handle) {
        // handle 기준 변경 대상 사용자 조회
        return userRepository.findByHandle(handle)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
    }

    public UserRole normalizeRole(String role) {
        // 요청 역할 문자열을 내부 역할 값으로 정규화
        String normalizedRole = Optional.ofNullable(role)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.replace("_", "").replace("-", "").toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BusinessException(INVALID_ROLE.getMessage(), HttpStatus.BAD_REQUEST));

        return switch (normalizedRole) {
            case "ADMIN" -> UserRole.ADMIN;
            case "USER" -> UserRole.USER;
            default -> throw new BusinessException(INVALID_ROLE.getMessage(), HttpStatus.BAD_REQUEST);
        };
    }

    public String resolveRoleValue(UserRole role) {
        // 화면에서 사용하는 역할 문자열로 변환
        return switch (role) {
            case ADMIN -> "admin";
            default -> "user";
        };
    }
}
