package com.quertimizer.auth.application.input;

import lombok.Data;

@Data
public class AuthIpBlockInput {

    private final String ipAddress;
    private final String handle;
}
