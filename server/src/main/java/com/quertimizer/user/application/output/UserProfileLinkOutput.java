package com.quertimizer.user.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileLinkOutput {

    private final String type;
    private final String value;
}
