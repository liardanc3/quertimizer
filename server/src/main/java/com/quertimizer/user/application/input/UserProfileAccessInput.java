package com.quertimizer.user.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserProfileAccessInput {

    private final String targetHandle;
    private final String currentHandle;
}
