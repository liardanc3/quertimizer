package com.quertimizer.auth.application.port.in;

import com.quertimizer.auth.application.input.SetupHandleInput;
import com.quertimizer.user.domain.entity.User;

public interface SetupHandleUseCase {

    void execute(SetupHandleInput input);
}
