package com.quertimizer.user.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileLinkRes {

    private final String type;
    private final String value;

}
