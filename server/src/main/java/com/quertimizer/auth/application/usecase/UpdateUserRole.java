package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.service.AuthManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateUserRole {

    private final AuthManageService authManageService;

    public void execute(String handle, String role) {
        // 사용자 역할을 수정
        authManageService.updateUserRole(handle, role);
    }
}
