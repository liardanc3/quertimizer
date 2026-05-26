package com.quertimizer.user.application.input;

import lombok.Data;

@Data
public class AuthUserLookupInput {

    private final Type type;
    private final String value;

    public enum Type {
        ID,
        EMAIL,
        EMAIL_IGNORE_CASE,
        HANDLE
    }
}
