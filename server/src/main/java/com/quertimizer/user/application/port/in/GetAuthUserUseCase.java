package com.quertimizer.user.application.port.in;

import com.quertimizer.user.application.input.AuthUserLookupInput;
import com.quertimizer.user.application.output.AuthUserOutput;

import java.util.Optional;

public interface GetAuthUserUseCase {

    Optional<AuthUserOutput> execute(AuthUserLookupInput input);
}
