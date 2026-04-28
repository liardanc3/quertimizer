package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.UpdateUserRoleInput;
import com.quertimizer.auth.application.service.AuthManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateUserRole {

    private final AuthManageService authManageService;

    /**
     * 사용자 역할을 수정한다.
     *
     * @param input 역할 변경 대상과 다음 역할 입력
     */
    public void execute(UpdateUserRoleInput input) {
        authManageService.updateUserRole(input);
    }
}
