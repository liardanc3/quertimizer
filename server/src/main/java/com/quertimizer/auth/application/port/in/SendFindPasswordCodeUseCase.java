package com.quertimizer.auth.application.port.in;

import com.quertimizer.auth.application.input.SendCodeInput;
import com.quertimizer.user.domain.entity.User;

public interface SendFindPasswordCodeUseCase {

    void execute(SendCodeInput input);
}
