package com.quertimizer.monitoring.application.service;

import com.quertimizer.monitoring.application.input.MonitoringLogSearchInput;
import com.quertimizer.monitoring.application.output.ServerLogOutput;
import com.quertimizer.monitoring.application.port.in.GetServerLogsUseCase;
import com.quertimizer.monitoring.application.port.out.ServerLogPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetServerLogs implements GetServerLogsUseCase {

    private final ServerLogPort serverLogPort;

    /**
     * 서버 로그 파일을 조회한다.
     *
     * @param input 로그 레벨, 날짜, 조회 줄 수
     */
    @Override
    public ServerLogOutput execute(MonitoringLogSearchInput input) {
        return serverLogPort.readLogs(input);
    }
}
