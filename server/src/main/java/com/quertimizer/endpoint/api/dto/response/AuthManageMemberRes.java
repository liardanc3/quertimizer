package com.quertimizer.endpoint.api.dto.response;

import com.quertimizer.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthManageMemberRes {

    private final String userId;

    public static AuthManageMemberRes from(User user) {
        return new AuthManageMemberRes(user.getUserId());
    }

}
