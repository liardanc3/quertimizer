package com.quertimizer.judge.application.port.in;

import com.quertimizer.judge.application.input.CreateSqlExecutionHashInput;
import com.quertimizer.judge.application.output.SqlExecutionHashResult;

public interface CreateSqlExecutionHashUseCase {

    SqlExecutionHashResult execute(CreateSqlExecutionHashInput input);
}
