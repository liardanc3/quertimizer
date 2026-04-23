package com.quertimizer.admin.presentation.dto.response;

import com.quertimizer.user.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthManageMemberRes {

    private final String handle;

    public static AuthManageMemberRes from(User user) {
        return new AuthManageMemberRes(user.getHandle());
    }

}
