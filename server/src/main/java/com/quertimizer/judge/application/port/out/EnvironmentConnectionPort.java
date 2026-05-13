package com.quertimizer.judge.application.port.out;

import com.quertimizer.judge.application.model.ExecutionEnvironment;
import com.quertimizer.judge.application.model.EnvironmentConnection;

public interface EnvironmentConnectionPort {

    void waitUntilReady(ExecutionEnvironment environment, int startupTimeoutSeconds);

    EnvironmentConnection open(ExecutionEnvironment environment, int timeoutSeconds);
}
