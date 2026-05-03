package com.quertimizer.auth.application.port.in;

import com.quertimizer.auth.application.input.UpdateUserRoleInput;
import com.quertimizer.global.constant.UserRole;
import com.quertimizer.user.domain.entity.User;

public interface UpdateUserRoleUseCase {

    void execute(UpdateUserRoleInput input);
}
