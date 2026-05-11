package com.quertimizer.user.application.input;

import lombok.Data;

@Data
public class UserProfileActivityPageInput {

    private final String targetHandle;
    private final String currentHandle;
    private final int page;
    private final Integer pageSize;
}
