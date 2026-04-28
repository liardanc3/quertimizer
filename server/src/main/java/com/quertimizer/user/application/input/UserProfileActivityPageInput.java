package com.quertimizer.user.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserProfileActivityPageInput {

    private final String targetHandle;
    private final String currentHandle;
    private final int page;
    private final Integer pageSize;
}
