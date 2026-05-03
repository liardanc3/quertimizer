package com.quertimizer.auth.application.port.in;

import com.quertimizer.auth.application.input.SignupInput;

public interface SignupUseCase {

    void execute(SignupInput input);
}
