package com.quertimizer.judge.application.port.in;

import com.quertimizer.judge.application.input.DatabaseNodeConfigUpdateInput;
import com.quertimizer.judge.application.output.DatabaseNodeConfigOutput;

public interface UpdateDatabaseNodeConfigUseCase {

    DatabaseNodeConfigOutput execute(DatabaseNodeConfigUpdateInput input);
}
