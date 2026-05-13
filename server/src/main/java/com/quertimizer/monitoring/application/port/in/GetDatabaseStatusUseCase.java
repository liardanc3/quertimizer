package com.quertimizer.monitoring.application.port.in;

import com.quertimizer.monitoring.application.output.DatabaseStatusOutput;

public interface GetDatabaseStatusUseCase {

    DatabaseStatusOutput execute();
}
