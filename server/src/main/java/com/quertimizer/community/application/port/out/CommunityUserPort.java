package com.quertimizer.community.application.port.out;

import com.quertimizer.user.domain.model.UserRole;

public interface CommunityUserPort {

    UserRole findRole(String handle);

}
