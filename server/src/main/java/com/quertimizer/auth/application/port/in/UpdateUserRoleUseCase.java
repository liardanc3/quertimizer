package com.quertimizer.auth.application.port.in;

import com.quertimizer.auth.application.input.UpdateUserRoleInput;

public interface UpdateUserRoleUseCase {

    void execute(UpdateUserRoleInput input);
}
