package com.quertimizer.user.application.port.in;

import com.quertimizer.user.application.input.AuthUserSaveInput;
import com.quertimizer.user.application.output.AuthUserOutput;

public interface SaveAuthUserUseCase {

    AuthUserOutput execute(AuthUserSaveInput input);
}
