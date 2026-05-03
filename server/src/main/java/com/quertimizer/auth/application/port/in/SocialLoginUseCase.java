package com.quertimizer.auth.application.port.in;

import com.quertimizer.auth.application.input.SocialLoginInput;
import com.quertimizer.auth.application.output.AuthenticatedUserOutput;

public interface SocialLoginUseCase {

    AuthenticatedUserOutput execute(SocialLoginInput input);
}
