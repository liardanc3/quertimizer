package com.quertimizer.user.application.input;

import lombok.Data;

@Data
public class BlockedAccountPageInput {

    private final int page;
    private final Integer pageSize;
}
