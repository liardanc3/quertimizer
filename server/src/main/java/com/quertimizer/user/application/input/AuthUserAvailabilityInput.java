package com.quertimizer.user.application.input;

import lombok.Data;

@Data
public class AuthUserAvailabilityInput {

    private final String email;
    private final String handle;
}
