package com.quertimizer.monitoring.application.port.in;

import com.quertimizer.monitoring.application.output.DbRuntimeOutput;

public interface GetDbRuntimeUseCase {

    DbRuntimeOutput execute();
}
