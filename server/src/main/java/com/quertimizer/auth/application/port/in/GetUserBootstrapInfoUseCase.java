package com.quertimizer.auth.application.port.in;

import com.quertimizer.auth.application.output.UserBootstrapOutput;

public interface GetUserBootstrapInfoUseCase {

    UserBootstrapOutput execute(String email);
}
