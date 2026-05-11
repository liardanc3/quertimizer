package com.quertimizer.auth.application.port.in;

import com.quertimizer.auth.application.input.SetupHandleInput;

public interface SetupHandleUseCase {

    void execute(SetupHandleInput input);
}
