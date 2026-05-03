package com.quertimizer.user.application.port.in;

import com.quertimizer.auth.domain.entity.BlockedIp;
import com.quertimizer.user.domain.entity.User;

public interface BlockUserUseCase {

    void execute(String handle);
}
