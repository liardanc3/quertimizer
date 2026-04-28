package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.UpdateProblemGeneratorPermissionsInput;
import com.quertimizer.auth.application.service.AuthManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateProblemGeneratorPermissions {

    private final AuthManageService authManageService;

    /**
     * ProblemGenerator 문제 권한을 수정한다.
     *
     * @param input 권한 변경 대상과 교체할 권한 목록 입력
     */
    public void execute(UpdateProblemGeneratorPermissionsInput input) {
        authManageService.updateProblemGeneratorPermissions(input);
    }
}
