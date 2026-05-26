package com.quertimizer.community.adapter.out.user;

import com.quertimizer.community.application.port.out.CommunityUserPort;
import com.quertimizer.user.application.port.in.GetCommunityUserRoleUseCase;
import com.quertimizer.user.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("communityUserGateway")
@RequiredArgsConstructor
public class UserGateway implements CommunityUserPort {

    private final GetCommunityUserRoleUseCase getCommunityUserRole;

    @Override
    public UserRole findRole(String handle) {
        // user 공개 use case 기준 handle 역할 조회
        return getCommunityUserRole.execute(handle);
    }
}
