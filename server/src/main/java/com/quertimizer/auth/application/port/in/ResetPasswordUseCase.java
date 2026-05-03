package com.quertimizer.auth.application.port.in;

import com.quertimizer.auth.application.input.ResetPasswordInput;

public interface ResetPasswordUseCase {

    void execute(ResetPasswordInput input);
}
