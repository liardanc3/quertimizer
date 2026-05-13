package com.quertimizer.monitoring.adapter.in.http;

import com.quertimizer.monitoring.adapter.in.http.request.DatabaseNodeConfigUpdateReq;
import com.quertimizer.monitoring.adapter.in.http.response.DatabaseStatusRes;
import com.quertimizer.monitoring.adapter.in.http.response.DatabaseNodeConfigRes;
import com.quertimizer.monitoring.adapter.in.http.response.ServerLogRes;
import com.quertimizer.monitoring.adapter.in.http.response.SystemResourceRes;
import com.quertimizer.monitoring.application.input.MonitoringLogSearchInput;
import com.quertimizer.monitoring.application.port.in.GetDatabaseStatusUseCase;
import com.quertimizer.monitoring.application.port.in.GetServerLogsUseCase;
import com.quertimizer.monitoring.application.port.in.GetSystemResourcesUseCase;
import com.quertimizer.judge.application.port.in.UpdateDatabaseNodeConfigUseCase;
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
    private final GetDatabaseStatusUseCase getDatabaseStatus;
    private final GetServerLogsUseCase getServerLogs;
    private final UpdateDatabaseNodeConfigUseCase updateDatabaseNodeConfig;

    /**
     * 관리자 서버 리소스 상태를 반환한다.
     */
    @GetMapping("/admin/monitoring/resources")
    public ResponseEntity<SystemResourceRes> getSystemResources() {
        return ResponseEntity.ok(SystemResourceRes.from(getSystemResources.execute()));
    }

    /**
     * 관리자 DB 실행 환경 상태를 반환한다.
     */
    @GetMapping("/admin/monitoring/database-status")
    public ResponseEntity<DatabaseStatusRes> getDatabaseStatus() {
        return ResponseEntity.ok(DatabaseStatusRes.from(getDatabaseStatus.execute()));
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
     * 관리자 DB 실행 환경 설정을 변경한다.
     *
     * @param databaseId 변경할 DB 노드 ID
     * @param request 변경할 DB 노드 설정 요청
     */
    @PutMapping("/admin/monitoring/database-node-configs/{databaseId}")
    public ResponseEntity<DatabaseNodeConfigRes> updateDatabaseNodeConfig(@PathVariable String databaseId,
                                                              @Valid @RequestBody DatabaseNodeConfigUpdateReq request) {
        return ResponseEntity.ok(DatabaseNodeConfigRes.from(updateDatabaseNodeConfig.execute(request.toInput(databaseId))));
    }
}
