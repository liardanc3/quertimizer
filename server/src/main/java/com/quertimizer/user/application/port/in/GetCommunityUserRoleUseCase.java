package com.quertimizer.user.application.port.in;

import com.quertimizer.user.domain.model.UserRole;

public interface GetCommunityUserRoleUseCase {

    UserRole execute(String handle);
}
