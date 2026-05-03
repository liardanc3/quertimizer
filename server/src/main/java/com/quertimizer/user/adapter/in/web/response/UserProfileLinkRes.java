package com.quertimizer.user.adapter.in.web.response;

import com.quertimizer.user.application.output.UserProfileLinkOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileLinkRes {

    private final String type;
    private final String value;

    public static UserProfileLinkRes from(UserProfileLinkOutput result) {
        return new UserProfileLinkRes(result.getType(), result.getValue());
    }
}
