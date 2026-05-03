package com.quertimizer.auth.application.port.in;

import com.quertimizer.auth.application.input.VerifyCodeInput;

public interface VerifyFindPasswordCodeUseCase {

    void execute(VerifyCodeInput input);
}
