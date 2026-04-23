package com.quertimizer.dashboard.presentation.controller;

import com.quertimizer.dashboard.application.usecase.GetDashboard;
import com.quertimizer.dashboard.presentation.dto.response.DashboardRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final GetDashboard getDashboard;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardRes> getDashboard(Authentication authentication) {

        // 로그인 여부에 맞는 커뮤니티/문제 대시보드 데이터 반환
        return ResponseEntity.ok(DashboardRes.from(getDashboard.execute(authentication)));
    }

}
