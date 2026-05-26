package com.quertimizer.auth.application.port.in;

import com.quertimizer.auth.application.input.AuthIpBlockInput;

public interface BlockAuthIpUseCase {

    void execute(AuthIpBlockInput input);
}
