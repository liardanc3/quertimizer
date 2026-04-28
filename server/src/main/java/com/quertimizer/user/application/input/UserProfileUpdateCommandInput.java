package com.quertimizer.user.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserProfileUpdateCommandInput {

    private final String handle;
    private final UserProfileUpdateInput profile;
}
