package com.quertimizer.user.application.input;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileLinkInput {

    private final String type;
    private final String value;
}
