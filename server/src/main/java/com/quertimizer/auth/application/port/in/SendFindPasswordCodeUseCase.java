package com.quertimizer.auth.application.port.in;

import com.quertimizer.auth.application.input.SendCodeInput;

public interface SendFindPasswordCodeUseCase {

    void execute(SendCodeInput input);
}
