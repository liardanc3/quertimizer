package com.quertimizer.user.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BlockedAccountPageInput {

    private final int page;
    private final Integer pageSize;
}
