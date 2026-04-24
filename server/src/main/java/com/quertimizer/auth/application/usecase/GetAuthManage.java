package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.output.AuthManageOutput;
import com.quertimizer.auth.application.service.AuthManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetAuthManage {

    private final AuthManageService authManageService;

    public AuthManageOutput execute() {
        // 권한 설정 화면 데이터를 조회
        return authManageService.getAuthManage();
    }
}
