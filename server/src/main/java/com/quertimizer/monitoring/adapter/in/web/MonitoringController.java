package com.quertimizer.monitoring.adapter.in.web;

import com.quertimizer.monitoring.adapter.in.web.request.JudgeConfigUpdateReq;
import com.quertimizer.monitoring.adapter.in.web.response.DbRuntimeRes;
import com.quertimizer.monitoring.adapter.in.web.response.JudgeConfigRes;
import com.quertimizer.monitoring.adapter.in.web.response.ServerLogRes;
import com.quertimizer.monitoring.adapter.in.web.response.SystemResourceRes;
import com.quertimizer.monitoring.application.input.MonitoringLogSearchInput;
import com.quertimizer.monitoring.application.port.in.GetDbRuntimeUseCase;
import com.quertimizer.monitoring.application.port.in.GetServerLogsUseCase;
import com.quertimizer.monitoring.application.port.in.GetSystemResourcesUseCase;
import com.quertimizer.monitoring.application.port.in.UpdateJudgeConfigUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MonitoringController {

    private final GetSystemResourcesUseCase getSystemResources;
    private final GetDbRuntimeUseCase getDbRuntime;
    private final GetServerLogsUseCase getServerLogs;
    private final UpdateJudgeConfigUseCase updateJudgeConfig;

    /**
     * 관리자 서버 리소스 상태를 반환한다.
     */
    @GetMapping("/admin/monitoring/resources")
    public ResponseEntity<SystemResourceRes> getSystemResources() {
        return ResponseEntity.ok(SystemResourceRes.from(getSystemResources.execute()));
    }

    /**
     * 관리자 DB runtime 상태를 반환한다.
     */
    @GetMapping("/admin/monitoring/db-runtime")
    public ResponseEntity<DbRuntimeRes> getDbRuntime() {
        return ResponseEntity.ok(DbRuntimeRes.from(getDbRuntime.execute()));
    }

    /**
     * 관리자 서버 로그를 반환한다.
     *
     * @param level 조회할 로그 레벨
     * @param date 조회할 로그 날짜
     * @param size 조회할 마지막 줄 수
     */
    @GetMapping("/admin/monitoring/logs")
    public ResponseEntity<ServerLogRes> getServerLogs(@RequestParam(required = false) String level,
                                                      @RequestParam(required = false) String date,
                                                      @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(ServerLogRes.from(getServerLogs.execute(MonitoringLogSearchInput.of(level, date, size))));
    }

    /**
     * 관리자 judge runtime 설정을 변경한다.
     *
     * @param databaseId 변경할 runtime database ID
     * @param request 변경할 runtime 설정 요청
     */
    @PutMapping("/admin/monitoring/judge-configs/{databaseId}")
    public ResponseEntity<JudgeConfigRes> updateJudgeConfig(@PathVariable String databaseId,
                                                            @Valid @RequestBody JudgeConfigUpdateReq request) {
        return ResponseEntity.ok(JudgeConfigRes.from(updateJudgeConfig.execute(request.toInput(databaseId))));
    }
}
