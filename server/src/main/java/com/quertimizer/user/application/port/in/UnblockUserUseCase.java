package com.quertimizer.user.application.port.in;

import com.quertimizer.user.domain.entity.User;

public interface UnblockUserUseCase {

    void execute(String handle);
}
