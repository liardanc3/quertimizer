package com.quertimizer.user.adapter.in.web.response;

import com.quertimizer.auth.application.output.BlockedUserItemOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BlockedUserItemRes {

    private final String handle;
    private final String ipAddress;
    private final LocalDateTime blockedAt;

    public static BlockedUserItemRes from(BlockedUserItemOutput result) {
        return new BlockedUserItemRes(
                result.getHandle(),
                result.getIpAddress(),
                result.getBlockedAt()
        );
    }
}
