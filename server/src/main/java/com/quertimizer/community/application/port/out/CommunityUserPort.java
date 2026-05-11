package com.quertimizer.community.application.port.out;

import com.quertimizer.global.constant.UserRole;

public interface CommunityUserPort {

    UserRole findRole(String handle);

}
