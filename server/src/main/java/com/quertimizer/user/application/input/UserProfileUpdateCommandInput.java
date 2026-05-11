package com.quertimizer.user.application.input;

import lombok.Data;

@Data
public class UserProfileUpdateCommandInput {

    private final String handle;
    private final UserProfileUpdateInput profile;
}
