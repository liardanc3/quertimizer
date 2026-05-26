package com.quertimizer.user.application.output;

import lombok.Data;

@Data
public class AuthUserAvailabilityOutput {

    private final boolean emailExists;
    private final boolean handleExists;
}
