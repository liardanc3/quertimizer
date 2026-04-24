package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.service.AuthManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UpdateProblemGeneratorPermissions {

    private final AuthManageService authManageService;

    public void execute(String handle, List<String> permissionKeys) {
        // ProblemGenerator 문제 권한을 수정
        authManageService.updateProblemGeneratorPermissions(handle, permissionKeys);
    }
}
