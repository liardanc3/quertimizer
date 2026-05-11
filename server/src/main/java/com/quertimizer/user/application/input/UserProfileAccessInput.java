package com.quertimizer.user.application.input;

import lombok.Data;

@Data
public class UserProfileAccessInput {

    private final String targetHandle;
    private final String currentHandle;
}
