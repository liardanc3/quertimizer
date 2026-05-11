package com.quertimizer.monitoring.application.service;

import com.quertimizer.monitoring.application.output.SystemResourceOutput;
import com.quertimizer.monitoring.application.port.in.GetSystemResourcesUseCase;
import com.quertimizer.monitoring.application.port.out.SystemResourcePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetSystemResources implements GetSystemResourcesUseCase {

    private final SystemResourcePort systemResourcePort;

    /**
     * 서버 리소스 상태를 조회한다.
     */
    @Override
    public SystemResourceOutput execute() {
        return systemResourcePort.getSystemResources();
    }
}
