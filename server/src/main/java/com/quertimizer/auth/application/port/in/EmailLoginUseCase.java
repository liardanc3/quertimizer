package com.quertimizer.auth.application.port.in;

import com.quertimizer.auth.application.input.EmailLoginInput;
import com.quertimizer.auth.application.output.AuthenticatedUserOutput;

public interface EmailLoginUseCase {

    AuthenticatedUserOutput execute(EmailLoginInput input);
}
