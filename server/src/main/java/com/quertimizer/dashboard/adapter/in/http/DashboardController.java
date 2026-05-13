package com.quertimizer.dashboard.adapter.in.http;

import com.quertimizer.dashboard.application.port.in.GetDashboardUseCase;
import com.quertimizer.dashboard.adapter.in.http.support.DashboardSupport;
import com.quertimizer.dashboard.adapter.in.http.response.DashboardRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final GetDashboardUseCase getDashboard;

    private final DashboardSupport dashboardSupport;

    /**
     * 로그인 여부에 맞는 대시보드 데이터를 반환한다.
     *
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardRes> getDashboard(Authentication authentication) {
        String currentHandle = dashboardSupport.resolveCurrentHandle(authentication);
        return ResponseEntity.ok(DashboardRes.from(getDashboard.execute(currentHandle)));
    }
}
