package com.quertimizer.community.adapter.out.user;

import com.quertimizer.community.application.port.out.CommunityUserPort;
import com.quertimizer.user.domain.model.UserRole;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("communityUserGateway")
@RequiredArgsConstructor
public class UserGateway implements CommunityUserPort {

    private final UserRepositoryPort userRepository;

    @Override
    public UserRole findRole(String handle) {
        // user 저장소 기준 handle 역할 조회
        return userRepository.findByHandle(handle)
                .map(User::getResolvedRole)
                .orElse(UserRole.USER);
    }

}
